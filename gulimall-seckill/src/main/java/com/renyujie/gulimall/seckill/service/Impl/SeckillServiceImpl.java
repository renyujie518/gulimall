package com.renyujie.gulimall.seckill.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.renyujie.common.dto.mq.SeckillOrderTo;
import com.renyujie.common.utils.R;
import com.renyujie.common.vo.MemberResVo;
import com.renyujie.gulimall.seckill.feign.CouponFeignService;
import com.renyujie.gulimall.seckill.feign.ProductFeignService;
import com.renyujie.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.renyujie.gulimall.seckill.service.SeckillService;
import com.renyujie.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.renyujie.gulimall.seckill.vo.SeckillSkuRedisTo;
import com.renyujie.gulimall.seckill.vo.SkuInfoVo;
import com.sun.xml.internal.bind.v2.TODO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName SeckillServiceImpl.java
 * @Description 秒杀服务实现类
 * @createTime 2022年03月07日 15:50:00
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {
    @Resource
    CouponFeignService couponFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Resource
    ProductFeignService productFeignService;
    @Autowired
    RedissonClient redissonClient;
    @Autowired
    RabbitTemplate rabbitTemplate;



    //活动信息
    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    //sku信息
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";
    //高并发下针对stock的 信号量
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";


    /**
     * @description 秒杀的定时上架任务:每天晚上3:00 ：上架最近三天需要秒杀的商品
     * 上架就是放到缓存里
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1 远程获取需要参与秒杀的活动和sku信息
        R r = couponFeignService.getLatest3DaysSession();
        if (r.getCode() == 0) {
            List<SeckillSessionsWithSkus> seckillSessionWithSkus = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //2缓存到redis
            //2.1 缓存参与秒杀的日期信息
            saveSessionInfos(seckillSessionWithSkus);
            //2.2缓存参与秒杀的商品信息
            saveSessionSkuInfos(seckillSessionWithSkus);
        }
    }


    /**
     * @description 缓存参与秒杀的日期信息  key是seckill:sessions:start_endtime     vallue是[sku所在的场次id+skuId]
     */
    private void saveSessionInfos(List<SeckillSessionsWithSkus> seckillSessionsWithSkus) {
        if (seckillSessionsWithSkus != null && seckillSessionsWithSkus.size() > 0) {
            seckillSessionsWithSkus.stream().forEach(seckillSession -> {
                Long start = seckillSession.getStartTime().getTime();
                Long endTime = seckillSession.getEndTime().getTime();
                String key = SESSION_CACHE_PREFIX + start + "_" + endTime;
                //幂等性  上架(放缓存)前先判断key是否存在，如果存在就不上架了  否则会由于定时任务执行导致重复上架(放缓存)
                Boolean hasKey = redisTemplate.hasKey(key);
                if (!hasKey) {
                    //避免重复加
                    List<String> values = seckillSession.getSeckillSkuRelationEntities().stream().map(
                            (SeckillSkuVo) -> {
                                return SeckillSkuVo.getPromotionSessionId() + "场->" + SeckillSkuVo.getSkuId().toString();
                            }).collect(Collectors.toList());
                    redisTemplate.opsForList().leftPushAll(key, values);
                }
            });
        }
    }

    /**
     * @description 缓存参与秒杀的sku信息  Hash结构  大目录是seckill:skus 下面的key是sku所在的场次id+skuId  value是seckillSkuRedisTo大对象
     * <p>
     * BUG描述：商品信息是hash类型，大目录是seckill:skus   map的key原先是skuId，但是当多个场次包含同样skuId的时候，商品信息和库存信号量都只有一份。
     * 这是不对的  多个场次有这个sku  那不能只上架一次啊  比如1点场，5点场都有有华为手机  不能只在1点场上架过 5点场应该也有 而且对应的数量也要保证对
     * <p>
     * 原因： map的key中  skuId和场次id恰好一样导致的
     * 解决：将key带上sessionId  即最终变为场次id+skuId  相当于把这个key变得丰富了，信息更全了 这样两两组合就不会有重复问题
     * 1、保存商品信息时的key为 sessionId+skuId
     * 2、为了方便，保存场次信息的时候，value值也改成sessionId_skuId（一致）
     * 3、然后库存量信息只有一份的原因就是解决幂等性的时候判断hasKey，在这个 if (!hasKey) {}里面 保证了"每个场次+每个sku"只有放一次semaphore 所以不会重复增加信号量。
     */
    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> seckillSessionWithSkus) {
        if (seckillSessionWithSkus != null && seckillSessionWithSkus.size() > 0) {
            seckillSessionWithSkus.stream().forEach(seckillSessionsWithSkus -> {
                /** 1. 准备Hash **/
                BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                /** 2.从coupon服务传来的大对象中取出每个sku的秒杀详情  再次封装为大对象seckillSkuRedisTo  再放入redis **/
                seckillSessionsWithSkus.getSeckillSkuRelationEntities().stream().forEach(
                        (seckillSkuVo) -> {
                            /** 3.生成随机码 token   和放入redis的key**/
                            String token = UUID.randomUUID().toString().replace("-", "");
                            String key = seckillSkuVo.getPromotionSessionId().toString() + "场->" + seckillSkuVo.getSkuId().toString();
                            //幂等性  上架(放缓存)前先判断key是否存在，如果存在就不上架了  否则会由于定时任务执行导致重复上架(放缓存)
                            Boolean hasKey = ops.hasKey(key);
                            if (!hasKey) {
                                //最终存到redis的sku的最详细的信息 包括该sku在秒杀活动中的详情+sku本身的详情
                                SeckillSkuRedisTo seckillSkuRedisTo = new SeckillSkuRedisTo();
                                /** 4.1  缓存sku的本身的详情  需要从product端获取  这样方便在首页展示秒杀的sku详情 不用打开首页一遍遍的查 在这里查好放到缓存 **/
                                R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                                if (r.getCode() == 0) {
                                    SkuInfoVo skuInfoVo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                                    });
                                    seckillSkuRedisTo.setSkuInfoVo(skuInfoVo);
                                }
                                /** 4.2  缓存sku的秒杀详情 **/
                                BeanUtils.copyProperties(seckillSkuVo, seckillSkuRedisTo);

                                /** 4.3  缓存sku的秒杀时间信息 **/
                                seckillSkuRedisTo.setStartTime(seckillSessionsWithSkus.getStartTime().getTime());
                                seckillSkuRedisTo.setEndTime(seckillSessionsWithSkus.getEndTime().getTime());

                                /** 4.4 秒杀随机码 : 防止恶意多刷 只有商品秒杀开始时暴露之后才暴露随机码，秒杀请求需要随机码才放行（没有随机码的请求被打回）**/
                                seckillSkuRedisTo.setRandomCode(token);

                                /** 5  把每个sku的信息丰富好 封装成大对象seckillSkuRedisTo后 ，再放入redis**/
                                String value = JSON.toJSONString(seckillSkuRedisTo);
                                //真正上线可以设置随机时间就是活动时间的间隔 dev阶段没设置
                                ops.put(key, value);
                                /**
                                 * 6
                                 * 100W请求，假设只有100件参与秒杀，那么最终实际上只会有100次扣库存的操作，100W个请求来了，不应该直接去查询DB是否有库存
                                 * 使用分布式锁的信号量，自增。100W请求，只有100个放行，放行后的才会到达DB减库存，达到限流的作用
                                 * 实现：使用redisson实现，redis.getSemaphore(商品随机码)
                                 * 注意：key不可以是skuId，要使用随机码，也是为了恶意多刷。
                                 *      如果当前这个场次的商品的库存信息已经上架就不需要上架
                                 */
                                RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                                //信号量放置参与秒杀的sku的数量  即那个100
                                semaphore.trySetPermits(Integer.parseInt(seckillSkuVo.getSeckillCount().toString()));

                            }
                        });
            });
        }
    }


    /**
     * 给首页返回当前时间可以参与的秒杀商品信息
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        /** 1.确定当前的秒杀场次 **/
        //Date.getTime()可以得到得到1970年01月1日0点零分以来的毫秒数  但是IDEA提示使用System.currentTimeMillis()
        //long now = new Date().getTime();
        long now = System.currentTimeMillis();
        System.out.println("当前时间" + now);
        Set<String> sessionKeys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        for (String sessionKey : sessionKeys) {
            //sessionKey的样子：seckill:sessions:1613757600000_1613761200000
            String[] startAndEnd = sessionKey.replace(SESSION_CACHE_PREFIX, "").split("_");
            long start = Long.parseLong(startAndEnd[0]);
            long end = Long.parseLong(startAndEnd[1]);
            if (start <= now && now <= end) {
                //说明在当前场次的时间区间内
                /** 2. 取出当前场次的时间区间内  参与秒杀的sku信息
                 *  2.1 先获取参与秒杀的日期信息 即saveSessionInfos()方法时放进去的
                 *      里面的value恰好是saveSessionSkuInfos()方法中设置sku的key
                 *      一个秒杀期间的内的活动日期会有多个(但不会超过100个) 所欲批量取  sessionKeyContent的样子： [1场->1,1场->2]
                 *  2.2 针对saveSessionSkuInfos()方法，取出 参与秒杀的sku信息  封装成大对象返回
                 * **/
                //
                List<String> sessionKeyContent = redisTemplate.opsForList().range(sessionKey, -100, 100);
                BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                //在saveSessionSkuInfos()方法中设置sku的key就是按照"1场->1"设置的  批量取出来
                List<String> skuListFromRedis = hashOps.multiGet(sessionKeyContent);
                if (skuListFromRedis != null && skuListFromRedis.size() > 0) {
                    List<SeckillSkuRedisTo> res = skuListFromRedis.stream().map((skuInRedis) -> {
                        SeckillSkuRedisTo seckillSkuRedisTo = JSON.parseObject((String) skuInRedis, SeckillSkuRedisTo.class);
                        return seckillSkuRedisTo;
                    }).collect(Collectors.toList());
                    return res;
                }
                break;
            }
        }
        return null;
    }


    /**
     * 给远程服务gulimall-product使用(商品详情页)
     * 获取当前sku的秒杀预告信息
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {

        /** 1. 找到所有需要参与秒杀sku的key**/
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        // [1场->1,1场->2]
        Set<String> skuKeys = hashOps.keys();
        if (skuKeys != null && skuKeys.size() > 0) {
            //正则 d表示匹配一个数字
            String regx = "\\d场->" + skuId;
            for (String skuKey : skuKeys) {
                if (Pattern.matches(regx, skuKey)) {
                    //匹配上了  说明这个sku在缓存中有秒杀信息
                    /** 2. 从缓存中获取sku的详情**/
                    String skuFromRedisString = hashOps.get(skuKey);
                    SeckillSkuRedisTo seckillSkuRedisTo = JSON.parseObject(skuFromRedisString, SeckillSkuRedisTo.class);
                    /** 3.随机码需要处理 是不是在秒杀时间内 如果不在活动时间内部 随机码置空**/
                    long now = System.currentTimeMillis();
                    if (now < seckillSkuRedisTo.getStartTime() || now > seckillSkuRedisTo.getEndTime()) {
                        seckillSkuRedisTo.setRandomCode(null);
                    }
                    return seckillSkuRedisTo;
                }
            }
        }
        return null;
    }


    /**
     * 商品详情页 点击"立刻抢购"  执行秒杀
     * 参数 killSkuKey 对应redis中的sku的键  比如 1场->1
     *     code是参与秒杀商品的随机码
     *     num是抢购的数量
     *
     * 最终返回秒杀成功生成的订单号

     */
    @Override
    public String kill(String killSkuKey, String code, Integer num) {
        long allBegin = System.currentTimeMillis();
        /** 1. 登录逻辑是拦截器处理的 所以到这一定是登录的  从拦截器的localthread中获取登录用户信息**/
        MemberResVo userInfo = LoginUserInterceptor.loginUser.get();
        /** 2. 获取获取当前秒杀sku的详细信息（主要是为了下面的验证用）**/
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        //killSkuId 对应redis中的sku的键  比如 1场->1 这是在前端拼好的 详见商品服务item.html中的"#seckillA"
        String skuIdInRedis = hashOps.get(killSkuKey);
        if (!StringUtils.isEmpty(skuIdInRedis)) {
            /** 3.1 校验时间合法 **/
            SeckillSkuRedisTo seckillSkuRedisTo = JSON.parseObject(skuIdInRedis, SeckillSkuRedisTo.class);
            long now = System.currentTimeMillis();
            Long startTime = seckillSkuRedisTo.getStartTime();
            Long endTime = seckillSkuRedisTo.getEndTime();
            Long ttl = endTime - startTime;
            if (startTime <= now && now <= endTime) {
                /** 3.2 校验随机码 和sku在redis中的键（真实环境sku可能会过期）实际上不用 因为2步骤就已经保证了  这里保险起见再判断一次 反正很快**/
                String randomCode = seckillSkuRedisTo.getRandomCode();
                String realSkuKey = seckillSkuRedisTo.getPromotionSessionId() + "场->" + seckillSkuRedisTo.getSkuId();
                if (randomCode.equals(code) && killSkuKey.equals(realSkuKey)) {
                    /** 3.3 校验购买数量  应该<=limit**/
                    BigDecimal limit = seckillSkuRedisTo.getSeckillLimit();
                    if (num <= Integer.parseInt(limit.toString())) {
                        /** 3.4 校验此人是否购买过 幂等性；如果秒杀成功，就去redis仅仅利用setIfAbsent（不存在才放，原子性）占位置
                         *   返回true,能放进去 说明没有重复的键 所以该用户没买过
                         *   新生成一个键  userId_1场->1  值无所谓 就放num即可  过期时间为活动时间  毫秒**/
                        String newKeyForIdempotent = userInfo.getId() + "_" + realSkuKey;
                        Boolean isNotRepeat = redisTemplate.opsForValue().setIfAbsent(newKeyForIdempotent, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (isNotRepeat) {
                            /** 3.5 校验库存的触发信号量  注意触发量在redis的存的时候的键值**/
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
                            //注意这里用tryAcquire（快速尝试获取）  不能阻塞住影响性能  返回true  代表获取到了库存的触发信号量，可以继续
                            boolean isGetStockSemaphore = semaphore.tryAcquire(num);
                            if (isGetStockSemaphore) {
                                /** 4. 执行到这 相当于秒杀服务处理完了 订单的处理交给MQ解耦**/

                                //创建订单对象
                                String orderSn = IdWorker.getTimeId();
                                SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                seckillOrderTo.setOrderSn(orderSn);
                                seckillOrderTo.setMemberId(userInfo.getId());
                                seckillOrderTo.setNum(num);
                                seckillOrderTo.setPromotionSessionId(seckillSkuRedisTo.getPromotionSessionId());
                                seckillOrderTo.setSkuId(seckillSkuRedisTo.getSkuId());
                                seckillOrderTo.setSeckillPrice(seckillSkuRedisTo.getSeckillPrice());

                                /** 5. 发送订单消息到 MQ 整个操作时间在 10ms 左右**/
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", seckillOrderTo);
                                long finallyEnd = System.currentTimeMillis();
                                log.info("秒杀创建耗时：{}", (finallyEnd - allBegin));
                                return orderSn;
                                //TODO 上架秒杀商品的时候，每一个数据都有过期时间
                                //TODO 秒杀的后续流程，简化了收货地址等信息
                                //TODO 如果没有秒杀完(库存信号量还有)，让库存解除锁定
                            } else {
                                //库存的触发信号量减完了
                                return null;
                            }
                        } else {
                            //此人买过了
                            return null;
                        }
                    } else {
                        //校验校验购买数量 买多了
                        return null;
                    }
                } else {
                    //校验随机码 和sku在redis中的键 校验未通过
                    return null;
                }
            } else {
                //时间校验未通过
                return null;
            }
        } else {
            //缓存中就压根没有该sku
            return null;
        }
    }



}
