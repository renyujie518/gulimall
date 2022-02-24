package com.renyujie.common.thread;

import java.util.concurrent.*;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName ThreadTest.java
 * @Description p193多线程学习
 *
 * 1、实际开发中，只用线程池【高并发状态开启了n个线程，会耗尽资源】
 * 2、当前系统中线程池只有一两个，每个异步任务提交给线程池让他自己去执行
 *
 *
 * 1）、继承Thread
 * 2）、实现 Runnable接口
 * 3）、实现 Callable接口+FutureTask（可以拿到返回结果，可以处理异常）
 * 4）、线程池
 *
 * 区别;
 * 1、2不能得到返回值。3可以获取返回值
 * 1、2、3都不能控制资源
 * 4可以控制资源，性能稳定，不会一下子所有线程一起运行
 *
 * @createTime 2022年02月24日 15:57:00
 */
public class ThreadTest {
    //线程池 每个系统1 - 2个
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("开始");
        //方法一
        //Thread01 thread01 = new Thread01();
        //thread01.start();

        //方法2  run相当于直接执行 同步  想要异步 就得如同方法1  new Thread再start
        //Runable01 runable01 = new Runable01();
        //new Thread(runable01).run();

        //方法3
        //FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
        //new Thread(futureTask).start();
        ////阻塞 前边执行完毕，才可以执行获取返回值这个方法。
        ////等待整个线程执行完成，获取返回结果。
        //Integer integer = futureTask.get();
        //System.out.println("阻塞获取返回值" + integer);

        //方法4 线程池 以池的方式运行 但是得先建池
        //executorService.execute(new Runable01());

        /**
         * corePoolSize 保留在池中的线程数 即使处于空闲状态 除非设置了allowCoreThreadTimeOut
         *
         * maximumPoolSize *池中允许的最大线程数
         *
         * keepalivueTime 存活时间 如果当前线程大于core的数量
         *          释放空闲的线程 maximumPoolsize-corePoolSize 只要线程空闲大于指定的keepAlivuetime
         * unit:时间单位
         * BlockingQUeue<Runnable> workQueue 阻塞队列 如果任务有很多 就会将目前多的任务放在队列里面
         *      只要有线程空闲，就会去队列里面取出新的任务继续执行
         * threadFactory 线程创建工厂
         * RejectedExecutionHandler 如果队列满了 按照我们指定得拒绝策略拒绝指定任务
         *
         * 工作顺序
         * 1)、线程池创建好 准备好core数量的核心线程，准备接受任务
         * 1.1、core满了 就将在进来的任务放入阻塞队列中 空闲的core就会自己去阻塞队列获取任务执行
         * 1.2、阻塞队列满了 就直接开新线程执行 最大只能开到max指定数量
         * 1.3、max满了就用RejectedExecutionHandler 拒绝任务
         * 1.4、max都执行完成，有很多空闲 指定时间以后keepAlivueTime以后 释放max-core(195)这些线程
         *
         *       new LinkedBlockingQueue<>() 默认是Integer最大值 内存不够
         *
         *       一个线程池 core:7 max:20 queue:50 100并发进来怎么分配
         *       7个会立即得到执行 50个进入队列 再开13个进行执行，剩下的30个就使用拒绝策略
         *       如果不想抛弃还要执行 CallerRunsPolicy 同步方式
         */
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(10000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        //快速创建线程池
        Executors.newCachedThreadPool(); //core是0 所有都可回收
        Executors.newFixedThreadPool(10);//固定大小 core=max 都不可以回收
        Executors.newScheduledThreadPool(10); //定时任务的线程池
        Executors.newSingleThreadExecutor(); //单线程的线程池,后台从队列里面获取任务 挨个执行



        System.out.println("结束");

    }
    //方法1
    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前线程" + Thread.currentThread());
            int i = 10 / 2;
            System.out.println("结果运行：" + i);
        }
    }

    //方法2
    public static class Runable01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("结果运行：" + i);
        }
    }

    //方法3
    public static class Callable01 implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("结果运行：" + i);
            return i;
        }
    }








}
