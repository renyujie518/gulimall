package com.renyujie.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.renyujie.common.dto.mq.OrderTo;
import com.renyujie.common.dto.mq.StockDetailTo;
import com.renyujie.common.dto.mq.StockLockedTo;
import com.renyujie.common.exception.NoStockException;
import com.renyujie.common.utils.R;
import com.renyujie.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.renyujie.gulimall.ware.entity.WareOrderTaskEntity;
import com.renyujie.gulimall.ware.feign.OrderFeignService;
import com.renyujie.gulimall.ware.feign.ProductFeignService;
import com.renyujie.gulimall.ware.service.WareOrderTaskDetailService;
import com.renyujie.gulimall.ware.service.WareOrderTaskService;
import com.renyujie.gulimall.ware.vo.OrderItemVo;
import com.renyujie.gulimall.ware.vo.OrderVo;
import com.renyujie.gulimall.ware.vo.SkuHasStockVo;
import com.renyujie.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.ware.dao.WareSkuDao;
import com.renyujie.gulimall.ware.entity.WareSkuEntity;
import com.renyujie.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;
    @Resource
    OrderFeignService orderFeignService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        // wareId: 123,//仓库id
        // skuId: 123//商品id
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params), wrapper
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 采购成功的sku更新入库
     * skuId 对应 sku_id
     * wareId 对应 ware_id
     * skuNum 对应 stock
     *
     * sku_name  通过feign去查
     * stock_locked  锁定库存默认为0
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //依据skuid和仓库id精准查
        List<WareSkuEntity> wareSkuEntities = baseMapper.selectList(
                new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));

        //如果还没有这个库存记录"insert"  否则是"update"
        if (wareSkuEntities == null || wareSkuEntities.size() == 0) {
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStockLocked(0);
            //远程查询sku的名字，如果失败，整个事务无需回滚(用trt_catch抓到异常不抛出，因为此导致的异常@Transactional注解失效)
            //TODO 让异常出现以后不回滚？高级 sential
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    //code代表无异常  具体 为什么这么取去看product.controller.SkuInfoController.info方法
                    skuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
            }

            baseMapper.insert(skuEntity);
        } else {
            baseMapper.addStock(skuId, wareId, skuNum);
        }
    }

    /**
     * @Description: 批量查询sku是否有库存
     */
    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> collect = skuIds.stream().map((skuId) -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            //select sum(stock-stock_locked) from wms_ware_sku where sku_id=1
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            //查询当前sku的总库存量
            return vo;
        }).collect(Collectors.toList());

        return collect;

    }

    /**
     * 为某个订单锁定库存
     * rollbackFor 代表由于此异常引起的一定要回滚的
     * 默认只要是RuntimeException 都回滚  由于NoStockException继承了运行时异常 所以这里的rollbackFor加与不加的效果一样
     * 但是根据阿里手册 最好指定上
     */
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /** 0 保存库存工作单的详情 实际上是操作wms_ware_order_task 主要存了OrderSn
         * 相当于冗余表 用于追溯订单信息 这个订单要发生锁库存**/
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);

        /**1、 本应该按照下单的收货地址，找到一个最近的仓库，锁定库存
         * 但此处不做这么复杂的逻辑 找到每个商品在哪个仓库都有库存（一个商品可能在多个仓库都有库存）
         * 将该需要锁定的sku的有用信息封装到SkuWareHasStock    而且订单传来的是一组待结账的sku  **/
        List<OrderItemVo> locks = vo.getLocks();
        //将vo转化为SkuWareHasStock
        List<SkuWareHasStock> skuWareHasStocks = locks.stream().map(lockItem -> {
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            skuWareHasStock.setSkuId(lockItem.getSkuId());
            //查询出这个sku在那个仓库中有库存（即已有-锁定>0）
            List<Long> wareIds = this.baseMapper.wareIdsHasSkuStock(lockItem.getSkuId());
            skuWareHasStock.setWareId(wareIds);
            skuWareHasStock.setNum(lockItem.getCount());
            return skuWareHasStock;
        }).collect(Collectors.toList());

        /**  2. 对每一个待结账的sku 依据这个人订单中该sku的数量做锁定库存（结账完成前这些商品lock,不能被被人选中）**/
        for (SkuWareHasStock sku : skuWareHasStocks) {
            Boolean skuHasStockedFlag = false;
            Integer needLockNum = sku.getNum();
            Long skuId = sku.getSkuId();
            List<Long> wareIds = sku.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                //没有任何仓库有该sku的库存
                throw new NoStockException(skuId);
            }
            //一个sku可能在多个仓库都有库存  遍历所有有该sku的仓库
            for (Long wareId : wareIds) {
                //成功 代表影响行数为1  否则相当于update失败  返回0
                Long count = this.baseMapper.lockSkuStockInThisWare(skuId, wareId, needLockNum);
                if (count == 1) {
                    skuHasStockedFlag = true;
                    /**  3. 一旦该sku锁库存成功 保存锁成功了的sku在锁库存中发生的所有详细消息 实际上操作wms_ware_order_task_detial
                         里面主要保存了该sku发生锁库存的详细信息  包括发生锁库存的sku,发生锁库存的wareId,发生锁库存sku的状态(1-已锁定  2-已解锁  3-扣减)**/
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", needLockNum, wareOrderTaskEntity.getId(), wareId, 1);
                    wareOrderTaskDetailService.save(taskDetailEntity);

                    /**  4. 一旦该sku锁库存成功  告诉MQ锁定成功 发消息  将当前sku锁定了几件等等消息（详见StockLockedTo.class）发出去
                     * 一旦需要解锁 会触发unLockStock
                     *
                     * 这里注意 如果该sku锁库存失败 前面保存的工作单（wareOrderTaskEntity）的信息就回滚了。
                     * 发送出去的消息中是wareOrderTaskEntity的id，即使要解锁记录,即在unLockStock的逻辑中，
                     * 由于去数据库查不到wms_ware_order_task中的ID（事务保证回滚了嘛），所以unLockStock是无法生效的
                     * 所以  在这里 好像是ku锁库存成功/失败了都发消息了  但实际一分析  失败了也发mq不会导致问题
                     * 但这种实现本身就不合理 所以在mq发出的消息stockLockedTo中要包含该sku在锁库存中发生的所有详细消息
                     * 这样防止回滚以后找不到需要的回滚数据（即stockLockedTo），因为我可以从mq的消息中取啊  不一定非要去查表
                     *
                     **/
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);

                    //验证该仓库下是否就有足够的的库存就把sku.num干掉了，也不必再查别的仓库,跳出针对仓库的循环
                    break;
                } else {
                    //count == 0  当前仓库没有解决needLockNum  这里什么都不做 等待下一个wareId去验证
                }
            }
            //执行到这里  所有的仓库遍历完毕（中途break代表解决了needLockNum）再看看skuHasStockedFlag
            if (skuHasStockedFlag == false) {
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }
        //代码能够到这里说明中途没有抛出NoStockException，全部sku都去尝试解决needLockNum 成功
        return true;
    }

    @Data
    class SkuWareHasStock {
        private Long skuId;
        //待结账sku的数量(被锁的数量)
        private Integer num;
        //哪些仓库有库存
        private List<Long> wareId;
    }



    /**
     * 库存解锁的场景1
     下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     * 库存解锁的场景2
     下订单成功，订单过期，没有支付被系统自动取消/被用户手动取消（OrderEntity.statu=4）

     stockLockedTo就是上一个方法  锁成功了的sku在锁库存中发生的所有详细消息+库存工作单id
     */
    @Override
    public void unLockStock(StockLockedTo stockLockedTo) {
        StockDetailTo orderTaskDetailTo = stockLockedTo.getDetailTo();

        /**1.查询数据库的锁库存的详细消息
         * 有 证明 库存锁定OK  执行解锁  但是还是要判定订单的状态
         * 没有 库存锁定失败，代表上一步的 wareOrderTaskDetailService.save执行了回滚  就不存在库存解锁的需求  无需解锁**/
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = wareOrderTaskDetailService.getById(orderTaskDetailTo.getId());
        if (wareOrderTaskDetailEntity != null) {
            //有 证明 库存锁定OK
            /** 2. 库存不能随便解锁 本场景是解决订单与库存的回滚不一致问题 所以再次查看订单的状况
             *    2.1 查询没有这个订单  说明发生了库存解锁的场景1 解锁库存
             *    2.2  查询到订单 但是订单是已取消(status=4) 说明发生场景2  解锁库存   没取消不能解锁（用户只是等会付款）  **/
            Long orderTaskId = stockLockedTo.getId();
            WareOrderTaskEntity wareOrderTaskEntity = wareOrderTaskService.getById(orderTaskId);
            String orderSn = wareOrderTaskEntity.getOrderSn();
            //根据订单号 远程查询订单详情（主要关心的是里面的statu字段的信息）
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                OrderVo orderVo = r.getData(new TypeReference<OrderVo>() {});
                //订单不存在||订单是取消状态 解锁库存  status: 4->已关闭（对应场景2）
                if (orderVo == null || orderVo.getStatus() == 4) {

                    /** 3. 再去检查数据库中的wareOrderTaskDetailEntity 中的工作单详情中的状态  状态为1(已锁定)
                     *  代表此时恰好发生场景1中的库存锁定成功，但是 订单不存在或者订单取消 最终解锁库存 **/
                    // 当前工作单详情，状态为1(已锁定)
                    if (wareOrderTaskDetailEntity.getLockStatus() == 1) {
                        //实际的解锁库存
                        unLockStock(orderTaskDetailTo.getSkuId(), orderTaskDetailTo.getWareId(), orderTaskDetailTo.getSkuNum(), orderTaskDetailTo.getId());
                    }
                }
            } else {
                /** 4. 远程查询订单详情失败  将导致本方法解锁库存失败  把异常抛出去
                 *     在库存服务中开启了manual手动确认机制防止消息丢失
                 *     把异常抛上去 消息拒绝重新放入队列 所以这个消息会继续被消费解锁（重试解锁）**/
                throw new RuntimeException("远程订单服务失败");
            }

        } else {
            /** 5. 对应1  没有库存单 无需解锁  但是完成unLockStock方法了 回调到StockReleaseListener的37行，在执行basicAck
             *  代表这个mq消息已经被消费了（相当于监听到一个无用消息）**/
        }
    }


    /**
     * 防止订单服务卡顿，导致库存订单状态一直改变不了为4（已取消），库存消息优先到，查订单状态，查到的是新建状态0
     * 这会导致上面的解锁服务什么都不做就走了，但是该mq已消费，导致这个取消的但是卡顿到的订单，永远不能解锁库存
     *
     * 即在处理OrderServiceImpl#closeOrder问题
     *
     * 执行了下面这个方法  立即解锁了库存  上面的解锁库存的方式相当于一种软补充
     * 两个方法互为补充 上一个解锁方法幂等的原因是在真正执行解库存的时候判断了很多条件
     * 包括"订单不存在||订单是取消状态"+"当前工作单详情，状态为1(已锁定)"所以也不会重复解锁库存
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unLockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查一下最新的状态，防止重复解锁库存
        WareOrderTaskEntity wareOrderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long wareOrderTaskId = wareOrderTaskEntity.getId();
        //按照工作单Id，去wms_ware_order_task_detial找到所有没有解锁的库存（lock_status=1） 进行解锁
        List<WareOrderTaskDetailEntity> entities = wareOrderTaskDetailService.list(
                new QueryWrapper<WareOrderTaskDetailEntity>()
                        .eq("task_id", wareOrderTaskId).eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            unLockStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum(), entity.getId());
        }
    }


    /**
     * @description 最终的解锁方案 最终一致性方案中的补偿项
     */
    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {

        //库存恢复
        this.baseMapper.unLockStock(skuId, wareId, num);
        //更新库存工作详情单的状态
        WareOrderTaskDetailEntity entity = new WareOrderTaskDetailEntity();
        entity.setId(taskDetailId);
        //变为已解锁（保证幂等）
        entity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(entity);
    }


}