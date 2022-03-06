package com.renyujie.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.renyujie.common.dto.mq.OrderTo;
import com.renyujie.common.exception.NoStockException;
import com.renyujie.common.utils.R;
import com.renyujie.common.vo.MemberResVo;
import com.renyujie.gulimall.order.constant.OrderConstant;
import com.renyujie.gulimall.order.entity.OrderItemEntity;
import com.renyujie.gulimall.order.enume.OrderStatusEnum;
import com.renyujie.gulimall.order.feign.CartFeignService;
import com.renyujie.gulimall.order.feign.MemberFeignService;
import com.renyujie.gulimall.order.feign.ProductFeignService;
import com.renyujie.gulimall.order.feign.WareFeignService;
import com.renyujie.gulimall.order.interceptor.LoginUserInterceptor;
import com.renyujie.gulimall.order.service.OrderItemService;
import com.renyujie.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.order.dao.OrderDao;
import com.renyujie.gulimall.order.entity.OrderEntity;
import com.renyujie.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Resource
    MemberFeignService memberFeignService;
    @Resource
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Resource
    WareFeignService wareFeignService;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    RabbitTemplate rabbitTemplate;



    //共享前端页面传过来的确认页vo
    ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 购物车  点击去结算
     * 订单确认页 confire.html 需要返回的数据
     * 使用线程池  异步编排
     *
     * 这里有3个注意点：
     * 1.Feign远程调用请求头丢失  详见GuliFeignConfig
     * 2.feign异步情况丢失请求头 p269中的实验显示  在使用RequestContextHolder.getRequestAttributes的时候
     * 使用请求上下文的本质上也是使用了ThreadLocal共享线程数据（点到RequestContextHolder接口可以看到）
     * 所以一旦使用异步编排  相当于新开了线程  对应的解决1的RequestInterceptor拦截器也是执行在新线程里的
     * 那么新线程（异步任务开的）中必须要放进老线程（浏览器->controller->本服务->feign调远程）的请求头数据（RequestAttributes）
     * 相当于覆盖RequestAttributes
     * 3.远程查看库存
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        /**1 获取用户信息和老线程的请求头数据    注意 每一个新线程都要共享老线程的请求头数据 **/
        MemberResVo userInfo = LoginUserInterceptor.loginUser.get();
        RequestAttributes oldRequestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> addressTask = CompletableFuture.runAsync(() -> {
            /**2 远程获取会员地址信息  **/
            //每一个线程都要共享之前的请求数据
            RequestContextHolder.setRequestAttributes(oldRequestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(userInfo.getId());
            orderConfirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> cartTask = CompletableFuture.runAsync(() -> {
            /**3 远程获取cart 当前用户所有购物项  **/
            //每一个线程都要共享之前的请求数据
            RequestContextHolder.setRequestAttributes(oldRequestAttributes);
            List<OrderItemVo> orderItemVoList = cartFeignService.currentUserItems();
            orderConfirmVo.setItems(orderItemVoList);
        }, executor).thenRunAsync(() -> {
            /**4 远程查看库存  批量  此外部服务不需要登录状态**/
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> itemSkuIds = items.stream().map((item) -> {
                return item.getSkuId();
            }).collect(Collectors.toList());
            R r = wareFeignService.getSkuHasStock(itemSkuIds);
            if (r.getCode() == 0) {
                List<SkuStockVo> skuStockVos = r.getData(new TypeReference<List<SkuStockVo>>() {});
                if (skuStockVos != null) {
                    Map<Long, Boolean> booleanMap = skuStockVos.stream().collect(
                            Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                    orderConfirmVo.setStocks(booleanMap);
                }
            }
        }, executor);

        /**5 获取会员（用户）积分信息  **/
        Integer jifen = userInfo.getIntegration();
        orderConfirmVo.setIntegration(jifen);

        /**6 其他的数据(总件数，订单总额，应付价格)在OrderConfirmVo.class中自动计算  **/

        /**7 防重令牌  **/
        String token = UUID.randomUUID().toString().replace("-", "");
        //给服务器一个 并指定过期时间
        redisTemplate.opsForValue().set(
                OrderConstant.USER_ORDER_TOKEN_PREFIX + userInfo.getId(),
                token,
                30,
                TimeUnit.MINUTES);
        //给页面一个
        orderConfirmVo.setOrderToken(token);

        CompletableFuture.allOf(addressTask, cartTask).get();
        return orderConfirmVo;
    }

    /**
     * 提交订单 去支付
     *
     * @Transactional 是一种本地事物，在分布式系统中，只能控制住自己的回滚，控制不了其他服务的回滚
     * 分布式事物 最大的原因是 网络问题+分布式机器。
     * (isolation = Isolation.REPEATABLE_READ) MySql默认隔离级别 - 可重复读
     */
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        //共享前端页面传过来的orderSubmitVo(后面几步会用到里面的addrId,payPrice,orderToken信息)
        confirmVoThreadLocal.set(orderSubmitVo);
        SubmitOrderResponseVo submitOrderResponseVo = new SubmitOrderResponseVo();
        //错误码 配合controller使用 code=0就是下单成功 此处默认为0  有问题code会被改变
        submitOrderResponseVo.setCode(0);
        /**1 获取用户信息 **/
        MemberResVo userInfo = LoginUserInterceptor.loginUser.get();
        /**2 验证令牌 原子**/
        //获取前端得来的token
        String orderToken = orderSubmitVo.getOrderToken();
        //0失败 - 1删除成功 ｜ 不存在0 存在 删除？1：0
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        //原子验证令牌 和 删除令牌
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + userInfo.getId()),
                orderToken);
        if (result == 0L) {
            //令牌验证失败 失败码设置为1
            submitOrderResponseVo.setCode(1);
            return submitOrderResponseVo;
        } else {
            //令牌验证成功 -> 执行业务代码
            /**3 创建订单**/
            OrderCreatTo orderCreatTo = creatOrder();
            /**4 验价**/
            //订单计算的"应付金额"
            BigDecimal payAmount = orderCreatTo.getOrder().getPayAmount();
            //前端传来的的"应付金额"
            BigDecimal payPrice = orderSubmitVo.getPayPrice();
            //金额对比
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                /**5 保存订单到数据库 **/
                saveOrder(orderCreatTo);
                /**6 封装WareSkuLockVo.Locks供远程调用 **/
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(orderCreatTo.getOrder().getOrderSn());
                List<OrderItemVo> OrderItemNeedLockStock = orderCreatTo.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(OrderItemNeedLockStock);
                /**
                 * 7  远程锁库存 只要有异常回滚本订单数据  实际上是操作wms_ware_sku表
                 */
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0) {
                    //锁成功 订单创建成功 最终要返回submitOrderResponseVo
                    submitOrderResponseVo.setOrder(orderCreatTo.getOrder());
                    //TODO 8 远程扣减积分（模拟，本次不做）
                    /**
                     情况1：远程库存执行成功了，但是网络原因返回超时了（timeout异常）但本处的订单服务感受到异常了，减积分不执行，订单回滚->订单&库存不一致
                     情况2：积分系统异常导致订单回滚，库存不回滚（即远程服务已经执行过了，库存服务该锁定都锁定了）-> 订单&库存不一致,减积分回滚

                     解决方法1：
                     本地事务只能控制自己的回宫  控制不了别的服务回滚  seata(详见文档 p286学习  本项目没有使用，由于锁太多)

                     解决方法2：
                     柔性事务-可靠消息+最终一致性方案（异步确保型)

                     **/
                    //模拟积分系统异常
                    //int i = 1 / 0;

                    /** 9 订单创建成功，发消息给MQ**/
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", orderCreatTo.getOrder());
                    return submitOrderResponseVo;
                } else {
                    //锁定失败  code设置为3 同时r里也封装了msg  （注意 这里抛异常会使@Transactional生效 继而回滚saveOrder）
                    submitOrderResponseVo.setCode(3);
                    String msgNoStock = (String) r.get("msg");
                    throw new NoStockException(msgNoStock);
                }

            } else {
                //验价失败  code设置为2
                submitOrderResponseVo.setCode(2);
                return submitOrderResponseVo;
            }
        }
    }


    /**
     * @Description: 工具方法  生成订单
     */
    private OrderCreatTo creatOrder() {
        OrderCreatTo orderCreatTo = new OrderCreatTo();
        /**1 配置 orderEntity**/
        //生成订单号   时间 ID = Time + ID
        String orderSn = IdWorker.getTimeId();
        OrderEntity order = build_OrderEntity(orderSn);
        orderCreatTo.setOrder(order);

        /**2 配置 List<OrderItemEntity> items**/
        List<OrderItemEntity> orderItems = buildList_OrderItemEntity(orderSn);
        orderCreatTo.setOrderItems(orderItems);

        /**3 配置 BigDecimal payPrice**/
        compute_Price(order, orderItems);

        return orderCreatTo;


    }

    /**
     * @Description: 工具类  配置 orderEntity
     */
    private OrderEntity build_OrderEntity(String orderSn) {
        OrderEntity orderEntity = new OrderEntity();

        orderEntity.setOrderSn(orderSn);
        MemberResVo userInfo = LoginUserInterceptor.loginUser.get();
        //设置会员id
        orderEntity.setMemberId(userInfo.getId());
        //共享comfirm页面传过来的orderSubmitVo(后面几步会用到里面的addrId,payPrice,orderToken信息)
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        //远程获取 收货地址信息+运费信息
        R r = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (r.getCode() == 0) {
            FareVo fareVo = r.getData(new TypeReference<FareVo>() {});
            /**
             * 设置运费信息
             * 注意！！！
             * 由于在confirm.html的表单提交过程遇到跨域问题
             * （根本是我这里的nginx没做内网穿透 导致无法按照视频那样自动转网关再转到库存服务）
             * 但是这个运费信息特别扯淡  老师设置的是取手机号最后一位作为运费
             * 我为了orderSubmitVo中有返回的payPrice值
             * 1.特地把手机号尾号设置为0   2.confirm.html post的是payPrice而不是计算运费后的number
             * 所以这里getFare()必定得到的都是0，相当跳过了运费环节
             *
             * 抱着学习技术的思路 这里这样处理  不用纠结 特此记录
             */
            orderEntity.setFreightAmount(fareVo.getFare());
            orderEntity.setReceiverCity(fareVo.getAddress().getCity());
            orderEntity.setReceiverDetailAddress(fareVo.getAddress().getDetailAddress());
            orderEntity.setReceiverName(fareVo.getAddress().getName());
            orderEntity.setReceiverPhone(fareVo.getAddress().getPhone());
            orderEntity.setReceiverPostCode(fareVo.getAddress().getPostCode());
            orderEntity.setReceiverProvince(fareVo.getAddress().getProvince());
            orderEntity.setReceiverRegion(fareVo.getAddress().getRegion());
            //待付款状态
            orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
            //自动确认时间（天）
            orderEntity.setAutoConfirmDay(7);
        }
        return orderEntity;
    }

    /**
     * @Description: 工具类  配置 List<OrderItemEntity> items 获取购物车商品项
     */
    private List<OrderItemEntity> buildList_OrderItemEntity(String orderSn) {

        //这是最后一次确定每一个购物项的价格了  再就到支付页面
        List<OrderItemVo> orderItemVos = cartFeignService.currentUserItems();
        if (orderItemVos != null && orderItemVos.size() > 0) {
            List<OrderItemEntity> collect = orderItemVos.stream().map((item) -> {
                /**   OrderItemVo(远程获取cart 当前用户所有购物项)转化为OrderItemEntity(OrderCreatTo中要求的)  **/
                OrderItemEntity orderItemEntity = buildOrderItemEntity(item);
                //OrderItemEntity的  订单号 直接传入  就不在上一步转换
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * @Description: 工具类
     * OrderItemVo(远程获取cart 当前用户所有购物项)转化为OrderItemEntity(OrderCreatTo中要求的)
     * 除了orderSn订单号
     */
    private OrderItemEntity buildOrderItemEntity(OrderItemVo item) {

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //OrderItemEntity的  spu
        Long skuId = item.getSkuId();
        /**远程商品服务   获取spu信息**/
        R r = productFeignService.getSpuInfoBuSkuId(skuId);
        if (r.getCode() == 0) {
            SpuInfoVo spuInfoVo = r.getData(new TypeReference<SpuInfoVo>() {});
            orderItemEntity.setSpuId(spuInfoVo.getId());
            //品牌名直接放了品牌id  否则还要再远程查  太麻烦了
            orderItemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
            orderItemEntity.setSpuName(spuInfoVo.getSpuName());
            orderItemEntity.setCategoryId(spuInfoVo.getCatalogId());
            //OrderItemEntity的  sku（刚好都在item里）
            orderItemEntity.setSkuId(item.getSkuId());
            orderItemEntity.setSkuName(item.getTitle());
            orderItemEntity.setSkuPic(item.getImage());
            orderItemEntity.setSkuPrice(item.getPrice());
            //集合转数组
            String skuAttr = StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";");
            orderItemEntity.setSkuAttrsVals(skuAttr);
            orderItemEntity.setSkuQuantity(item.getCount());
            //OrderItemEntity的 优惠信息（不做）
            //OrderItemEntity的 积分信息
            orderItemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount().toString())).intValue());
            orderItemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount().toString())).intValue());
            //OrderItemEntity的 金额信息
            orderItemEntity.setPromotionAmount(new BigDecimal("0"));
            orderItemEntity.setCouponAmount(new BigDecimal("0"));
            orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
            BigDecimal orign = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity()));
            BigDecimal subtract = orign
                    .subtract(orderItemEntity.getCouponAmount())
                    .subtract(orderItemEntity.getPromotionAmount())
                    .subtract(orderItemEntity.getIntegrationAmount());
            //真实价格 = 总价x数量-各优惠
            orderItemEntity.setRealAmount(subtract);
        }

        return orderItemEntity;
    }

    /**
     * @Description: 工具类   配置 BigDecimal fare  验价  计算优惠 ，积分等各种信息
     */
    private void compute_Price(OrderEntity orderEntity, List<OrderItemEntity> orderItems) {

        //总价格
        BigDecimal total = new BigDecimal("0.0");
        //优惠卷
        BigDecimal coupon = new BigDecimal("0.0");
        //积分
        BigDecimal interation = new BigDecimal("0.0");
        //打折
        BigDecimal promotion = new BigDecimal("0.0");
        //赠送积分
        BigDecimal gift = new BigDecimal("0.0");
        //赠送成长值
        BigDecimal growth = new BigDecimal("0.0");
        //订单的总额，叠加每一个订单项的总额信息
        for (OrderItemEntity orderItem : orderItems) {
            coupon = coupon.add(orderItem.getCouponAmount());
            interation = interation.add(orderItem.getIntegrationAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            total = total.add(orderItem.getRealAmount());
            gift = gift.add(new BigDecimal(orderItem.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(orderItem.getGiftGrowth().toString()));
        }

        //1、订单价格相关
        orderEntity.setTotalAmount(total);
        //应付金额 + 运费金额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        //优惠信息
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(interation);
        orderEntity.setCouponAmount(coupon);
        //设置积分信息
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setIntegration(gift.intValue());
        //未删除
        orderEntity.setDeleteStatus(0);
    }

    /**
     * @Description: 工具类  保存到数据库
     */
    private void saveOrder(OrderCreatTo orderCreatTo) {

        //保存第1个属性到数据库
        OrderEntity order = orderCreatTo.getOrder();
        order.setModifyTime(new Date());
        this.save(order);
        //保存第2个属性到数据库
        List<OrderItemEntity> orderItems = orderCreatTo.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * @description 远程库存服务调用  按照订单号查询订单详情
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {

        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }


    /**
     * @description 订单解锁
     * 收到过期的订单，关闭订单(实际上是修改OrderEntity.stuta的状态为4)
     */
    @Override
    public void closeOrder(OrderEntity orderEntityFromMQ) {
        /** 1. 由于只要是创建订单  就会发送mq  经过延时队列（为了实验设置为1min，实际应该是30min）后都会来到 order.release.order.queue从而被此方法监听到
         *  并不是所有的订单都是过期的  所以先去数据库中查订单的状态   如果是过期才需要关闭   **/
        OrderEntity orderEntityFromDB = this.getById(orderEntityFromMQ.getId());
        //需要关单的状态是：待付款 0
        if (orderEntityFromDB.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            //关单（注意  这里应该新建个对象  因为过去了30min才从mq中获得orderEntityFromMQ,这段时间可能导致orderEntityFromMQ与orderEntityFromDB不一样了）
            OrderEntity updateOrder = new OrderEntity();
            updateOrder.setId(orderEntityFromMQ.getId());
            //设置为4：已取消
            updateOrder.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(updateOrder);

            /** 2. 为了实验方便 订单的延时是1min(代表1min过后closeOrder就会收到消息) 库存的延时是2min(订单有问题2min后会执行库存解锁)
             * 但是假设订单创建->订单解锁 期间由于"消息延迟/订单服务器挂了执行此closeOrder有问题"导致 订单解锁在库存解锁之后
             * 那么在解锁库存unLockStock方法中的针对场景2的判断就失效了 但是消息被消费了啊  相当于库存解锁失败
             *
             * 为解决这个问题   订单解锁的时候也发MQ  该订单解锁的消息会到stock.release.stock.queue，这样库存解锁就立马能感知到
             * 详见/order/config/MyRabbitMQConfig.java的orderReleaseOtherBinding()
             *
             * 同时在ware服务也会做相应的处理  一旦感知到下面发的这个OrderTo类型的mq  立马执行解锁
             * **/
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntityFromDB, orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);

            //3 TODO 保证消息100%发送出去，每一个消息都做好日志记录 (给数据库保存每一个消息的详细信息) 定期扫描数据库 将失败的消息再发送一遍


        }
    }
}