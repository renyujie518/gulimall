package com.renyujie.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.renyujie.common.dto.mq.OrderTo;
import com.renyujie.common.dto.mq.SeckillOrderTo;
import com.renyujie.common.exception.NoStockException;
import com.renyujie.common.utils.R;
import com.renyujie.common.vo.MemberResVo;
import com.renyujie.gulimall.order.constant.OrderConstant;
import com.renyujie.gulimall.order.entity.OrderItemEntity;
import com.renyujie.gulimall.order.entity.PaymentInfoEntity;
import com.renyujie.gulimall.order.enume.OrderStatusEnum;
import com.renyujie.gulimall.order.feign.CartFeignService;
import com.renyujie.gulimall.order.feign.MemberFeignService;
import com.renyujie.gulimall.order.feign.ProductFeignService;
import com.renyujie.gulimall.order.feign.WareFeignService;
import com.renyujie.gulimall.order.interceptor.LoginUserInterceptor;
import com.renyujie.gulimall.order.service.OrderItemService;
import com.renyujie.gulimall.order.service.PaymentInfoService;
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
    @Autowired
    PaymentInfoService paymentInfoService;



    //???????????????????????????????????????vo
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
     * @Description: ?????????  ???????????????
     * ??????????????? confire.html ?????????????????????
     * ???????????????  ????????????
     *
     * ?????????3???????????????
     * 1.Feign???????????????????????????  ??????GuliFeignConfig
     * 2.feign??????????????????????????? p269??????????????????  ?????????RequestContextHolder.getRequestAttributes?????????
     * ????????????????????????????????????????????????ThreadLocal???????????????????????????RequestContextHolder?????????????????????
     * ??????????????????????????????  ????????????????????????  ???????????????1???RequestInterceptor???????????????????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????->controller->?????????->feign?????????????????????????????????RequestAttributes???
     * ???????????????RequestAttributes
     * 3.??????????????????
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        /**1 ????????????????????????????????????????????????    ?????? ????????????????????????????????????????????????????????? **/
        MemberResVo userInfo = LoginUserInterceptor.loginUser.get();
        RequestAttributes oldRequestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> addressTask = CompletableFuture.runAsync(() -> {
            /**2 ??????????????????????????????  **/
            //????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(oldRequestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(userInfo.getId());
            orderConfirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> cartTask = CompletableFuture.runAsync(() -> {
            /**3 ????????????cart ???????????????????????????  **/
            //????????????????????????????????????????????????
            RequestContextHolder.setRequestAttributes(oldRequestAttributes);
            List<OrderItemVo> orderItemVoList = cartFeignService.currentUserItems();
            orderConfirmVo.setItems(orderItemVoList);
        }, executor).thenRunAsync(() -> {
            /**4 ??????????????????  ??????  ????????????????????????????????????**/
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

        /**5 ????????????????????????????????????  **/
        Integer jifen = userInfo.getIntegration();
        orderConfirmVo.setIntegration(jifen);

        /**6 ???????????????(???????????????????????????????????????)???OrderConfirmVo.class???????????????  **/

        /**7 ????????????  **/
        String token = UUID.randomUUID().toString().replace("-", "");
        //?????????????????? ?????????????????????
        redisTemplate.opsForValue().set(
                OrderConstant.USER_ORDER_TOKEN_PREFIX + userInfo.getId(),
                token,
                30,
                TimeUnit.MINUTES);
        //???????????????
        orderConfirmVo.setOrderToken(token);

        CompletableFuture.allOf(addressTask, cartTask).get();
        return orderConfirmVo;
    }

    /**
     * ???????????? ?????????
     *
     * @Transactional ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ??????????????? ?????????????????? ????????????+??????????????????
     * (isolation = Isolation.REPEATABLE_READ) MySql?????????????????? - ????????????
     */
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        //??????????????????????????????orderSubmitVo(??????????????????????????????addrId,payPrice,orderToken??????)
        confirmVoThreadLocal.set(orderSubmitVo);
        SubmitOrderResponseVo submitOrderResponseVo = new SubmitOrderResponseVo();
        //????????? ??????controller?????? code=0?????????????????? ???????????????0  ?????????code????????????
        submitOrderResponseVo.setCode(0);
        /**1 ?????????????????? **/
        MemberResVo userInfo = LoginUserInterceptor.loginUser.get();
        /**2 ???????????? ??????**/
        //?????????????????????token
        String orderToken = orderSubmitVo.getOrderToken();
        //0?????? - 1???????????? ??? ?????????0 ?????? ?????????1???0
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        //?????????????????? ??? ????????????
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + userInfo.getId()),
                orderToken);
        if (result == 0L) {
            //?????????????????? ??????????????????1
            submitOrderResponseVo.setCode(1);
            return submitOrderResponseVo;
        } else {
            //?????????????????? -> ??????????????????
            /**3 ????????????**/
            OrderCreatTo orderCreatTo = creatOrder();
            /**4 ??????**/
            //???????????????"????????????"
            BigDecimal payAmount = orderCreatTo.getOrder().getPayAmount();
            //??????????????????"????????????"
            BigDecimal payPrice = orderSubmitVo.getPayPrice();
            //????????????
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                /**5 ???????????????????????? **/
                saveOrder(orderCreatTo);
                /**6 ??????WareSkuLockVo.Locks??????????????? **/
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
                 * 7  ??????????????? ????????????????????????????????????  ??????????????????wms_ware_sku???
                 */
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0) {
                    //????????? ?????????????????? ???????????????submitOrderResponseVo
                    submitOrderResponseVo.setOrder(orderCreatTo.getOrder());
                    //TODO 8 ?????????????????????????????????????????????
                    /**
                     ??????1?????????????????????????????????????????????????????????????????????timeout???????????????????????????????????????????????????????????????????????????????????????->??????&???????????????
                     ??????2????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????-> ??????&???????????????,???????????????

                     ????????????1???
                     ???????????????????????????????????????  ??????????????????????????????  seata(???????????? p286??????  ???????????????????????????????????????)

                     ????????????2???
                     ????????????-????????????+???????????????????????????????????????)

                     **/
                    //????????????????????????
                    //int i = 1 / 0;

                    /** 9 ?????????????????????????????????MQ**/
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", orderCreatTo.getOrder());
                    return submitOrderResponseVo;
                } else {
                    //????????????  code?????????3 ??????r???????????????msg  ????????? ?????????????????????@Transactional?????? ????????????saveOrder???
                    submitOrderResponseVo.setCode(3);
                    String msgNoStock = (String) r.get("msg");
                    throw new NoStockException(msgNoStock);
                }

            } else {
                //????????????  code?????????2
                submitOrderResponseVo.setCode(2);
                return submitOrderResponseVo;
            }
        }
    }


    /**
     * @Description: ????????????  ????????????
     */
    private OrderCreatTo creatOrder() {
        OrderCreatTo orderCreatTo = new OrderCreatTo();
        /**1 ?????? orderEntity**/
        //???????????????   ?????? ID = Time + ID
        String orderSn = IdWorker.getTimeId();
        OrderEntity order = build_OrderEntity(orderSn);
        orderCreatTo.setOrder(order);

        /**2 ?????? List<OrderItemEntity> items**/
        List<OrderItemEntity> orderItems = buildList_OrderItemEntity(orderSn);
        orderCreatTo.setOrderItems(orderItems);

        /**3 ?????? BigDecimal payPrice**/
        compute_Price(order, orderItems);

        return orderCreatTo;


    }

    /**
     * @Description: ?????????  ?????? orderEntity
     */
    private OrderEntity build_OrderEntity(String orderSn) {
        OrderEntity orderEntity = new OrderEntity();

        orderEntity.setOrderSn(orderSn);
        MemberResVo userInfo = LoginUserInterceptor.loginUser.get();
        //????????????id
        orderEntity.setMemberId(userInfo.getId());
        //??????comfirm??????????????????orderSubmitVo(??????????????????????????????addrId,payPrice,orderToken??????)
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        //???????????? ??????????????????+????????????
        R r = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (r.getCode() == 0) {
            FareVo fareVo = r.getData(new TypeReference<FareVo>() {});
            /**
             * ??????????????????
             * ???????????????
             * ?????????confirm.html???????????????????????????????????????
             * ????????????????????????nginx?????????????????? ?????????????????????????????????????????????????????????????????????
             * ????????????????????????????????????  ??????????????????????????????????????????????????????
             * ?????????orderSubmitVo???????????????payPrice???
             * 1.?????????????????????????????????0   2.confirm.html post??????payPrice???????????????????????????number
             * ????????????getFare()?????????????????????0??????????????????????????????
             *
             * ??????????????????????????? ??????????????????  ???????????? ????????????
             */
            orderEntity.setFreightAmount(fareVo.getFare());
            orderEntity.setReceiverCity(fareVo.getAddress().getCity());
            orderEntity.setReceiverDetailAddress(fareVo.getAddress().getDetailAddress());
            orderEntity.setReceiverName(fareVo.getAddress().getName());
            orderEntity.setReceiverPhone(fareVo.getAddress().getPhone());
            orderEntity.setReceiverPostCode(fareVo.getAddress().getPostCode());
            orderEntity.setReceiverProvince(fareVo.getAddress().getProvince());
            orderEntity.setReceiverRegion(fareVo.getAddress().getRegion());
            //???????????????
            orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
            //???????????????????????????
            orderEntity.setAutoConfirmDay(7);
        }
        return orderEntity;
    }

    /**
     * @Description: ?????????  ?????? List<OrderItemEntity> items ????????????????????????
     */
    private List<OrderItemEntity> buildList_OrderItemEntity(String orderSn) {

        //??????????????????????????????????????????????????????  ?????????????????????
        List<OrderItemVo> orderItemVos = cartFeignService.currentUserItems();
        if (orderItemVos != null && orderItemVos.size() > 0) {
            List<OrderItemEntity> collect = orderItemVos.stream().map((item) -> {
                /**   OrderItemVo(????????????cart ???????????????????????????)?????????OrderItemEntity(OrderCreatTo????????????)  **/
                OrderItemEntity orderItemEntity = buildOrderItemEntity(item);
                //OrderItemEntity???  ????????? ????????????  ????????????????????????
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * @Description: ?????????
     * OrderItemVo(????????????cart ???????????????????????????)?????????OrderItemEntity(OrderCreatTo????????????)
     * ??????orderSn?????????
     */
    private OrderItemEntity buildOrderItemEntity(OrderItemVo item) {

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //OrderItemEntity???  spu
        Long skuId = item.getSkuId();
        /**??????????????????   ??????spu??????**/
        R r = productFeignService.getSpuInfoBuSkuId(skuId);
        if (r.getCode() == 0) {
            SpuInfoVo spuInfoVo = r.getData(new TypeReference<SpuInfoVo>() {});
            orderItemEntity.setSpuId(spuInfoVo.getId());
            //???????????????????????????id  ????????????????????????  ????????????
            orderItemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
            orderItemEntity.setSpuName(spuInfoVo.getSpuName());
            orderItemEntity.setCategoryId(spuInfoVo.getCatalogId());
            //OrderItemEntity???  sku???????????????item??????
            orderItemEntity.setSkuId(item.getSkuId());
            orderItemEntity.setSkuName(item.getTitle());
            orderItemEntity.setSkuPic(item.getImage());
            orderItemEntity.setSkuPrice(item.getPrice());
            //???????????????
            String skuAttr = StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";");
            orderItemEntity.setSkuAttrsVals(skuAttr);
            orderItemEntity.setSkuQuantity(item.getCount());
            //OrderItemEntity??? ????????????????????????
            //OrderItemEntity??? ????????????
            orderItemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount().toString())).intValue());
            orderItemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount().toString())).intValue());
            //OrderItemEntity??? ????????????
            orderItemEntity.setPromotionAmount(new BigDecimal("0"));
            orderItemEntity.setCouponAmount(new BigDecimal("0"));
            orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
            BigDecimal orign = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity()));
            BigDecimal subtract = orign
                    .subtract(orderItemEntity.getCouponAmount())
                    .subtract(orderItemEntity.getPromotionAmount())
                    .subtract(orderItemEntity.getIntegrationAmount());
            //???????????? = ??????x??????-?????????
            orderItemEntity.setRealAmount(subtract);
        }

        return orderItemEntity;
    }

    /**
     * @Description: ?????????   ?????? BigDecimal fare  ??????  ???????????? ????????????????????????
     */
    private void compute_Price(OrderEntity orderEntity, List<OrderItemEntity> orderItems) {

        //?????????
        BigDecimal total = new BigDecimal("0.0");
        //?????????
        BigDecimal coupon = new BigDecimal("0.0");
        //??????
        BigDecimal interation = new BigDecimal("0.0");
        //??????
        BigDecimal promotion = new BigDecimal("0.0");
        //????????????
        BigDecimal gift = new BigDecimal("0.0");
        //???????????????
        BigDecimal growth = new BigDecimal("0.0");
        //?????????????????????????????????????????????????????????
        for (OrderItemEntity orderItem : orderItems) {
            coupon = coupon.add(orderItem.getCouponAmount());
            interation = interation.add(orderItem.getIntegrationAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            total = total.add(orderItem.getRealAmount());
            gift = gift.add(new BigDecimal(orderItem.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(orderItem.getGiftGrowth().toString()));
        }

        //1?????????????????????
        orderEntity.setTotalAmount(total);
        //???????????? + ????????????
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        //????????????
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(interation);
        orderEntity.setCouponAmount(coupon);
        //??????????????????
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setIntegration(gift.intValue());
        //?????????
        orderEntity.setDeleteStatus(0);
    }

    /**
     * @Description: ?????????  ??????????????????
     */
    private void saveOrder(OrderCreatTo orderCreatTo) {

        //?????????1?????????????????????
        OrderEntity order = orderCreatTo.getOrder();
        order.setModifyTime(new Date());
        this.save(order);
        //?????????2?????????????????????
        List<OrderItemEntity> orderItems = orderCreatTo.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * @description ????????????????????????  ?????????????????????????????????
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {

        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }


    /**
     * @description ????????????
     * ????????????????????????????????????(??????????????????OrderEntity.stuta????????????4)
     */
    @Override
    public void closeOrder(OrderEntity orderEntityFromMQ) {
        /** 1. ???????????????????????????  ????????????mq  ??????????????????????????????????????????1min??????????????????30min?????????????????? order.release.order.queue???????????????????????????
         *  ???????????????????????????????????????  ??????????????????????????????????????????   ??????????????????????????????   **/
        OrderEntity orderEntityFromDB = this.getById(orderEntityFromMQ.getId());
        //???????????????????????????????????? 0
        if (orderEntityFromDB.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            //???????????????  ???????????????????????????  ???????????????30min??????mq?????????orderEntityFromMQ,????????????????????????orderEntityFromMQ???orderEntityFromDB???????????????
            OrderEntity updateOrder = new OrderEntity();
            updateOrder.setId(orderEntityFromMQ.getId());
            //?????????4????????????
            updateOrder.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(updateOrder);

            /** 2. ?????????????????? ??????????????????1min(??????1min??????closeOrder??????????????????) ??????????????????2min(???????????????2min????????????????????????)
             * ????????????????????????->???????????? ????????????"????????????/??????????????????????????????closeOrder?????????"?????? ?????????????????????????????????
             * ?????????????????????unLockStock????????????????????????2????????????????????? ???????????????????????????  ???????????????????????????
             *
             * ?????????????????????   ???????????????????????????MQ  ??????????????????????????????stock.release.stock.queue??????????????????????????????????????????
             * ??????/order/config/MyRabbitMQConfig.java???orderReleaseOtherBinding()
             *
             * ?????????ware??????????????????????????????  ?????????????????????????????????OrderTo?????????mq  ??????????????????
             * **/
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntityFromDB, orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);

            //3 TODO ????????????100%??????????????????????????????????????????????????? (????????????????????????????????????????????????) ????????????????????? ?????????????????????????????????


        }
    }

    /**
     * ????????????????????????????????? PayVo
     */
    @Override
    public PayVo getPayOrder(String orderSn) {
        //?????????????????????
        PayVo payVo = new PayVo();

        OrderEntity orderEntity = this.getOrderByOrderSn(orderSn);
        //????????????????????????????????????????????????sku??????????????????setBody???
        List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));

        //???????????????1???????????????????????????????????????????????????????????? ???"??????: ??????;??????: ?????????;??????: 8+128" ???
        payVo.setBody(orderItemEntities.get(0).getSkuAttrsVals());
        //???????????????2????????????
        payVo.setOut_trade_no(orderEntity.getOrderSn());
        //???????????????3?????????????????????????????????+???????????????????????????????????????
        payVo.setSubject("???????????????" + orderItemEntities.get(0).getSkuName());
        //???????????????4?????????????????? ????????????2???+????????????
        BigDecimal payNum = orderEntity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(payNum.toString());

        //??????????????????????????????
        return payVo;

    }

    /**
     * ????????????????????????
     * ???????????????????????????????????????????????????????????????
     * @RequestBody ???????????????????????????
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResVo memberResVo = LoginUserInterceptor.loginUser.get();

        QueryWrapper<OrderEntity> wrapper = new QueryWrapper<>();
        //????????????
        wrapper.eq("member_id", memberResVo.getId()).orderByDesc("id");
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                wrapper
        );

        List<OrderEntity> orderEntities = page.getRecords().stream().map((orderEntity) -> {
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderEntity.getOrderSn()));
            orderEntity.setOrderItemEntities(orderItemEntities);
            return orderEntity;
        }).collect(Collectors.toList());

        //????????????????????????
        page.setRecords(orderEntities);

        return new PageUtils(page);

    }

    /**
     * ??????????????????????????????
     * <p>
     * ??????????????????????????????????????????????????????????????????????????????????????????
     * ??????success??????????????????????????????
     */
    @Override
    public String handlePayResult(PayAsyncVo payAsyncVo) {

        //1.?????????????????????????????? PaymentInfoEntity
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setAlipayTradeNo(payAsyncVo.getTrade_no());
        paymentInfoEntity.setOrderSn(payAsyncVo.getOut_trade_no());//??????????????????????????????
        paymentInfoEntity.setPaymentStatus(payAsyncVo.getTrade_status());
        paymentInfoEntity.setCallbackTime(payAsyncVo.getNotify_time());
        paymentInfoService.save(paymentInfoEntity);

        //2?????????????????????
        if (payAsyncVo.getTrade_status().equals("TRADE_SUCCESS") || payAsyncVo.getTrade_status().equals("TRADE_FINISHED")) {
            //????????????
            String outTradeNo = payAsyncVo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    /**
     * @description ???????????????
     */
    @Override
    public void creatSeckillOrder(SeckillOrderTo seckillOrderTo) {

        //1.??????????????????
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderEntity.setMemberId(seckillOrderTo.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal payAmount = seckillOrderTo.getSeckillPrice().multiply(new BigDecimal("" + seckillOrderTo.getNum()));
        orderEntity.setPayAmount(payAmount);
        this.save(orderEntity);

        //2.?????????????????????
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderItemEntity.setRealAmount(payAmount);
        orderItemEntity.setSkuQuantity(seckillOrderTo.getNum());

        //3.????????????Sku????????????(?????????)
        //productFeignService.getSpuInfoBuSkuId()

        orderItemService.save(orderItemEntity);
    }
}