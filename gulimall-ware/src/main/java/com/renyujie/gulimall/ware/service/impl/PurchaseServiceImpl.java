package com.renyujie.gulimall.ware.service.impl;

import com.renyujie.common.constant.WareConstant;
import com.renyujie.gulimall.ware.entity.PurchaseDetailEntity;
import com.renyujie.gulimall.ware.service.PurchaseDetailService;
import com.renyujie.gulimall.ware.service.WareSkuService;
import com.renyujie.gulimall.ware.vo.MergeVo;
import com.renyujie.gulimall.ware.vo.PurchaseDoneVo;
import com.renyujie.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.ware.dao.PurchaseDao;
import com.renyujie.gulimall.ware.entity.PurchaseEntity;
import com.renyujie.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;
    @Autowired
    WareSkuService wareSkuService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 查询未领取的采购单
     */
    @Override
    public PageUtils unreceiveList(Map<String, Object> params) {
        //状态为"领取"为2  所以未领取的状态吗就是0，1
        QueryWrapper<PurchaseEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0).or().eq("status", 1);

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params), wrapper
        );

        return new PageUtils(page);

    }

    /**
     * @Description: 将采购需求合并成一个采购单
     * 这里前端的逻辑比较特别  如果"合并到整单"选择了采购单，就会传回purchaseId  否则就新建一个采购单
     */
    @Transactional(rollbackFor=Exception.class)
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        if (purchaseId == null) {
            /**需要新建一个采购单的情况**/
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            //设置状态为新建
            purchaseEntity.setStatus(WareConstant.purchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            //跟新purchaseId
            purchaseId = purchaseEntity.getId();
        }
        //传回purchaseId了 则就直接合并到传回的订单id
        //此时的finalPurchaseId不管是新建还是传来的肯定不为null 以后的逻辑就用这个新id
        Long finalPurchaseId = purchaseId;
        /**需要合并到指定的采购单  purchaseId 的情况**/
        //确认采购单状态是 0 / 1  即"新建"/"已分配"状态 才允许选中的采购需求合并到purchaseId
        if(purchaseCanMerged(finalPurchaseId)){
            List<Long> items = mergeVo.getItems();
            if (items != null && items.size() > 0) {
                List<PurchaseDetailEntity> purchaseDetailEntities = items.stream().map((item) -> {
                    //注意啊  虽然名字叫item 实际上就是前端传来的"采购需求ids"
                    PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                    purchaseDetailEntity.setId(item);
                    /**所谓合并  其实就是将采购需求中的PurchaseId指定为同一个**/
                    purchaseDetailEntity.setPurchaseId(finalPurchaseId);
                    //合并采购需求指定到一个具体的采购单（采购负责人在前端配置的）  相当于这个采购需求"已分配"
                    purchaseDetailEntity.setStatus(WareConstant.purchaseDetailStatusEnum.ASSIGNED.getCode());
                    return purchaseDetailEntity;
                }).collect(Collectors.toList());
                //批量跟新pms_purchase_detail表
                purchaseDetailService.updateBatchById(purchaseDetailEntities);
            }
            //这里注意一点  上一个 if (purchaseId == null) 中的save仅仅是"需要新建采购单"的情况存了
            //所以针对需要合并到指定的采购单  purchaseId 的情况只需要记录时间和更新purchaseId（finalPurchaseId也行）
            //（备注：采购负责人在前端配置后  采购单的状态自动变为"已分配"）
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setId(finalPurchaseId);
            purchaseEntity.setUpdateTime(new Date());
            // 更新采购单
            this.updateById(purchaseEntity);
        }
    }

    /**
     * @Description: app端 发过来 工作人员领取采购单ids
     * 后端需要采购单的状态为"已领取"  同时改变所属采购单下所有的采购需求状态为"正在采购"
     *
     */
    @Override
    public void received(List<Long> purchaseIds) {
        /**改变采购单的状态"已领取"  但是要保证采购单状态是 0 / 1 才可被领取**/
        if (purchaseIds != null && purchaseIds.size() > 0) {
            List<PurchaseEntity> purchaseEntities = purchaseIds.stream().filter(this::purchaseCanMerged).map((id) -> {
                PurchaseEntity purchaseEntity = this.getById(id);
                purchaseEntity.setStatus(WareConstant.purchaseStatusEnum.RECEIVE.getCode());
                purchaseEntity.setUpdateTime(new Date());
                return purchaseEntity;
            }).collect(Collectors.toList());
            this.updateBatchById(purchaseEntities);

            /**改变每个采购单下所有的采购需求状态为"正在采购"**/
            purchaseEntities.forEach((purchaseEntity)->{
                //查询该purchaseId下所有的detail
                List<PurchaseDetailEntity> purchaseDetailEntities =
                        purchaseDetailService.DetailListByPurchaseId(purchaseEntity.getId());
                //改变状态为"正在采购"
                if (purchaseDetailEntities != null && purchaseDetailEntities.size() > 0) {
                    purchaseDetailEntities.forEach((purchaseDetailEntity)->{
                        purchaseDetailEntity.setStatus(WareConstant.purchaseDetailStatusEnum.BUYING.getCode());
                    });
                    purchaseDetailService.updateBatchById(purchaseDetailEntities);
                }
            });
        }
    }

    /**
     * PurchaseDoneVo的封装：
     * {
     *    id: 123,//采购单id
     *    items: [{itemId:1,status:3,reason:""},{itemId:2,status:4,reason:"无货"}]
     *           //完成或失败的需求detail详情   itemId对应purchaseDetailId
     * }
     * @Description: 完成采购
     * 1.改变"采购需求" purchaseDetail状态
     * 2.将成功采购的item信息进行入库  即更新wms_ware_sku数据库
     * 3.改变采购单purchase状态
     *
     * 要注意 一旦有一个item的status为 4："采购失败"  采购单purchase的状态应该是 4："有异常"
     *       只有所有item的status为都是 3:"已完成"     purchase的状态才为 3:"已完成"   所以设置了标志位
     */
    @Transactional(rollbackFor=Exception.class)
    @Override
    public void done(PurchaseDoneVo doneVo) {
        //获得前端传来的采购单id
        Long purchaseId = doneVo.getId();

        Boolean flag = true;
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> updateReady = new ArrayList<>();

        if (items != null && items.size() > 0) {
            for (PurchaseItemDoneVo item : items) {
                if (item != null) {
                    PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
                    detailEntity.setId(item.getItemId());
                    //一旦有一个item的status为 "采购失败"  flag为false 顺手也把status更新了
                    if (item.getStatus() == WareConstant.purchaseDetailStatusEnum.HASERROR.getCode()) {
                        flag = false;
                        /**1.改变"采购需求" purchaseDetail状态**/
                        detailEntity.setStatus(item.getStatus());
                        updateReady.add(detailEntity);
                    } else {
                        //否则说明现在正在判断的item是"已完成"  顺手也把status更新了
                        /**1.改变"采购需求" purchaseDetail状态**/
                        detailEntity.setStatus(WareConstant.purchaseDetailStatusEnum.FINISH.getCode());
                        updateReady.add(detailEntity);

                        /**2.将成功采购的该item信息进行入库**/
                        PurchaseDetailEntity purchaseDetailEntity = purchaseDetailService.getById(item.getItemId());
                        wareSkuService.addStock(purchaseDetailEntity.getSkuId(),
                                purchaseDetailEntity.getWareId(), purchaseDetailEntity.getSkuNum());
                    }
                }
            }
        }
        //for循环结束  不管这个item的status是3还是4   统一批量更新
        if (updateReady != null && updateReady.size() >0) {
            purchaseDetailService.updateBatchById(updateReady);
        }

        /**3.改变采购单purchase状态**/
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setStatus(flag ? WareConstant.purchaseStatusEnum.FINISH.getCode() : WareConstant.purchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    /**
     * @Description: 采购单状态是 0 / 1  即"新建"/"已分配"状态 返回true
     */
    private boolean purchaseCanMerged(Long purchaseId) {
        PurchaseEntity purchaseEntity = this.baseMapper.selectById(purchaseId);
        return purchaseEntity.getStatus() == WareConstant.purchaseStatusEnum.CREATED.getCode()
                || purchaseEntity.getStatus() == WareConstant.purchaseStatusEnum.ASSIGNED.getCode();
    }

}