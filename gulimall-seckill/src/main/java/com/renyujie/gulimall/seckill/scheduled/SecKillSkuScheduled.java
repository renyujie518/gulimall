package com.renyujie.gulimall.seckill.scheduled;

import com.renyujie.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName SecKillSkuScheduled.java
 * @Description 秒杀的定时上架任务:
 * 每天晚上3:00 ：上架最近三天需要秒杀的商品
    当天0点 - 23：59
 *  明天0点 - 23：59
 *  后天0点 - 23：59
 *
 *
 * 分布式情况下，秒杀服务器会有多台  定时任务会启动多次【因为场次信息是List类型，会重复添加】
 * 加分布式锁：10S没执行完就释放锁
 * 每台服务器先去获取锁 得到了再去执行uploadSeckillSkuLatest3Days(),没有得到锁再次获取锁发现已经上架成功，就不执行了
 * @createTime 2022年03月07日 15:40:00
 */
@Slf4j
@Service
public class SecKillSkuScheduled {

    @Resource
    SeckillService seckillService;
    @Autowired
    private RedissonClient redissonClient;

    //秒杀商品上架功能的锁
    private final String upload_lock = "seckill:upload:lock";

    //每十秒执行一次
    @Scheduled(cron = "0/10 * * * * ?")
    public void uploadSeckillSkuLatest3Days(){
        //1、重复上架无需处理
        log.info("上架秒杀的商品开始  每十秒一次...");

        //分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        try {
            //加锁 10s预计上架怎么也执行完了  释放锁
            lock.lock(10, TimeUnit.SECONDS);
            seckillService.uploadSeckillSkuLatest3Days();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }


}
