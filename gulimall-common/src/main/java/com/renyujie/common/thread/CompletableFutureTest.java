package com.renyujie.common.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName CompletableFutureTest.java
 * @Description CompletableFuture异步编程学习
 * 每个注释下执行，其他的都要//掉
 * @createTime 2022年02月24日 17:30:00
 */
public class CompletableFutureTest {
    //线程池 每个系统1 - 2个
    public static ExecutorService executor = Executors.newFixedThreadPool(10);
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        /**异步编排1 无返回值runAsync()**/
        //System.out.println("开始");
        //CompletableFuture<Void> run1 = CompletableFuture.runAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 2;
        //    System.out.println("结果运行：" + i);
        //}, executor);
        //System.out.println("结束");

        /**异步编排2 有返回值 supplyAsync()  方法完成后的感知**/
        //System.out.println("开始");
        //CompletableFuture<Integer> supply1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 2;
        //    System.out.println("结果运行：" + i);
        //    return i;
        //}, executor);
        //Integer res = supply1.get();
        //System.out.println("结束 返回结果" + res);

        /**异步编程3 链式调用whenComplete，exceptionally**/
        //System.out.println("开始");
        //CompletableFuture<Integer> supply2 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 0;
        //    System.out.println("结果运行：" + i);
        //    return i;
        //}, executor).whenComplete((result, exception) -> {
        //    //虽然能得到异常信息，却不能修改返回数据，类似监听器  同时whenComplete与上一步的线程是同一个
        //    System.out.println("异步任务完成了，结果是：" + result + "，异常是：" + exception + " 当前线程" + Thread.currentThread().getId());
        //}).exceptionally((throwable -> {
        //    //可以感知异常，同时返回默认数据 最终的链式结果以此处为准
        //    return 10;
        //}));
        //System.out.println("最终链式的结果是" + supply2.get());
        //System.out.println("结束");

        /**异步编程4 handle()方法执行后的处理(无论成功完成还是失败完成) 就算有异常 也想要结果**/
        //System.out.println("开始");
        //CompletableFuture<Integer> handle1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 2;
        //    //int i = 10 / 0;
        //    System.out.println("结果运行：" + i);
        //    return i;
        //}, executor).handle((res, throwable) -> {
        //    if (res != null) {
        //        return res * 2;
        //    }
        //    if (throwable != null) {
        //        //出现了异常
        //        return 1;
        //    }
        //    //默认返回
        //    return 0;
        //});
        //System.out.println("最终链式的结果是" + handle1.get());
        //System.out.println("结束");



        /**串行化1
         A任务完成后 -> B任务执行      带Async的意思是：再开一个线程； 否则和A线程共用一个线程
         thenRunAsync() 不能获取到上一步的执行结果 无返回值
         **/
        //System.out.println("开始");
        //CompletableFuture<Void> thenRun = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 4;
        //    System.out.println("结果运行：" + i);
        //    return i;
        //}, executor).thenRunAsync(() -> {
        //    System.out.println("任务2启动了 " + Thread.currentThread().getId());
        //}, executor);
        //System.out.println("结束,但无法最终获得A串行B后的返回值" + thenRun.get());


        /**
         串行化2
         thenAcceptAsync() 能接受上一个任务的结果，但是无返回值
         **/
        //System.out.println("开始");
        //CompletableFuture<Void> thenAccept = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 4;
        //    System.out.println("结果运行：" + i);
        //    return i;
        //}, executor).thenAcceptAsync((res) -> {
        //    System.out.println("任务2启动了" + "感知上一步执行的结果是：" + res + " 当前线程" + Thread.currentThread().getId());
        //}, executor);
        //
        //System.out.println("结束,但无法最终获得A串行B后的返回值" + thenAccept.get());


        /**
         串行化3
         thenApplyAsync() 能接受上一个任务的结果，有返回值
         **/
        //System.out.println("开始");
        //CompletableFuture<String> thenApply = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 4;
        //    System.out.println("结果运行：" + i);
        //    return i;
        //}, executor).thenApplyAsync((res) -> {
        //    System.out.println("任务2启动了" + "上一步执行的结果是：" + res + " 当前线程" + Thread.currentThread().getId());
        //    return "hello " + res;
        //}, executor);
        ////阻塞后得到
        //System.out.println("结束,但可以最终获得A串行B后的返回值" + thenApply.get());



        /**
         组合1
         两个任务都完成 然后执行第三个 A + B -> C
         runAfterBothAsync 不能感知前两步的执行结果 自己也没有返回值
         **/
        //System.out.println("开始");
        //CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务1开始。当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 2;
        //    System.out.println("任务1结束。结果运行：" + i);
        //    return i;
        //}, executor);
        //CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务2开始。当前线程：" + Thread.currentThread().getId());
        //    System.out.println("任务2结束");
        //    return "hello";
        //}, executor);
        //CompletableFuture<Void> task3 = task1.runAfterBothAsync(task2, () -> {
        //    System.out.println("任务3开始。当前线程：" + Thread.currentThread().getId());
        //}, executor);
        //System.out.println("结束,仅仅是前连个任务结束后 自己执行自己的 也没返回值" + task3.get());


        /**
         组合2
         两个任务都完成 然后执行第三个 A + B -> C
         thenAcceptBothAsync() 能感知前两步的执行结果 自己没有返回值
         **/
        //System.out.println("开始");
        //CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务1开始。当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 2;
        //    System.out.println("任务1结束。结果运行：" + i);
        //    return i;
        //}, executor);
        //CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务2开始。当前线程：" + Thread.currentThread().getId());
        //    System.out.println("任务2结束");
        //    return "hello";
        //}, executor);
        //CompletableFuture<Void> task3 = task1.thenAcceptBothAsync(task2, (f1, f2) -> {
        //    System.out.println("任务3开始。之前的结果：f1=" + f1 + "；f2=" + f2 + " 当前线程" + Thread.currentThread().getId());
        //}, executor);
        //System.out.println("结束,能感知前两步的返回值，但自己没返回值" + task3.get());


        /**
         组合3
         两个任务都完成 然后执行第三个 A + B -> C
         thenCombineAsync()能感知前两步的执行结果， 还能处理前面两个任务的返回值，并生成返回值 自己有返回值
         **/
        //System.out.println("开始");
        //CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务1开始。当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 2;
        //    System.out.println("任务1结束。结果运行：" + i);
        //    return i;
        //}, executor);
        //CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务2开始。当前线程：" + Thread.currentThread().getId());
        //    System.out.println("任务2结束");
        //    return "hello";
        //}, executor);
        //CompletableFuture<String> task3 = task1.thenCombineAsync(task2, (f1, f2) -> {
        //    System.out.println("任务3开始。之前的结果：f1=" + f1 + "；f2=" + f2 + " 当前线程" + Thread.currentThread().getId());
        //    return f1 + ": " + f2 + "->ww  我是任务3";
        //}, executor);
        //System.out.println("结束,能感知前两步的返回值，自己有返回值：  " + task3.get());


        /**
         只要执行完就放行1
         两个任务 只要有一个完成就行 就能执行第三个任务 A || B = C
         runAfterEitherAsync() 不感知前面任务的结果，自己也没有返回值
         **/
        //System.out.println("开始");
        //CompletableFuture<Integer> task1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务1开始。当前线程：" + Thread.currentThread().getId());
        //    int i = 10 / 2;
        //    System.out.println("任务1结束。结果运行：" + i);
        //    return i;
        //}, executor);
        //CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务2开始。当前线程：" + Thread.currentThread().getId());
        //    try {
        //        Thread.sleep(3000);
        //        System.out.println("任务2结束");
        //    } catch (InterruptedException e) {
        //        e.printStackTrace();
        //    }
        //    return "hello";
        //}, executor);
        //CompletableFuture<Void> task3 = task1.runAfterEitherAsync(task2, () -> {
        //    System.out.println("任务3开始。当前线程：" + Thread.currentThread().getId());
        //}, executor);
        //System.out.println("任务3结束,仅仅是前2个任务只要有1个结束后 自己执行自己的 也没返回值： " + task3.get());


        /**
         只要执行完就放行2
         两个任务 只要有一个完成就行 就能执行第三个任务 A || B = C
         acceptEitherAsync() 能感知前面任务的结果，要求任务1、2的返回类型必须相同,自己没有返回值
         任务1、2的返回类型必须相同  取巧的办法就是task1,2的返回泛型都设置为CompletableFuture<Object>
         **/
        //System.out.println("开始");
        //CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务1开始。当前线程：" + Thread.currentThread().getId());
        //    System.out.println("任务1结束");
        //    return "task1";
        //}, executor);
        //CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务2开始。当前线程：" + Thread.currentThread().getId());
        //    try {
        //        Thread.sleep(3000);
        //        System.out.println("任务2结束");
        //    } catch (InterruptedException e) {
        //        e.printStackTrace();
        //    }
        //    return "task2";
        //}, executor);
        //CompletableFuture<Void> task3 = task1.acceptEitherAsync(task2, (result) -> {
        //    System.out.println("任务3开始。之前的结果,要求任务1、2的返回类型必须相同,具体是哪个随机" + result + " 当前线程" + Thread.currentThread().getId());
        //}, executor);
        //System.out.println("任务3结束,能感知前两步的中的某一个返回值（要求任务1、2的返回类型必须相同），但自己没返回值" + task3.get());


        /**
         只要执行完就放行3
         两个任务 只要有一个完成就行 就能执行第三个任务 A || B = C
         applyToEitherAsync() 能感知前面任务的结果，自己有返回值
         任务1、2的返回类型必须相同  取巧的办法就是task1,2的返回泛型都设置为CompletableFuture<Object>
         **/
        //CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务1开始。当前线程：" + Thread.currentThread().getId());
        //    System.out.println("任务1结束");
        //    return "task1";
        //}, executor);
        //CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("任务2开始。当前线程：" + Thread.currentThread().getId());
        //    try {
        //        Thread.sleep(3000);
        //        System.out.println("任务2结束");
        //    } catch (InterruptedException e) {
        //        e.printStackTrace();
        //    }
        //    return "task2";
        //}, executor);
        //CompletableFuture<String> task3 = task1.applyToEitherAsync(task2, (result) -> {
        //    System.out.println("任务3开始。感知之前的结果,要求任务1、2的返回类型必须相同,具体是哪个随机" + result + " 当前线程" + Thread.currentThread().getId());
        //    return result + "任务3特有";
        //}, executor);
        //System.out.println("结束,能感知前两步的中的某一个返回值（要求任务1、2的返回类型必须相同），自己有返回值" + task3.get());


        /**
         * 多任务组合 1
         * allOf() 所有任务结束后才能继续执行
         */
        //System.out.println("开始");
        //CompletableFuture<String> future4_1 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("查询商品的图片信息……");
        //    return "hello.jpg";
        //}, executor);
        //CompletableFuture<String> future4_2 = CompletableFuture.supplyAsync(() -> {
        //    System.out.println("查询商品的属性……");
        //    return "黑色 2+64G";
        //}, executor);
        //CompletableFuture<String> future4_3 = CompletableFuture.supplyAsync(() -> {
        //    try {
        //        Thread.sleep(3000);
        //        System.out.println("查询商品的介绍……");//模拟业务时间超长
        //    } catch (InterruptedException e) {
        //        e.printStackTrace();
        //    }
        //    return "apple";
        //}, executor);
        //
        ////allOf() 所有任务结束后才能继续执行
        //CompletableFuture<Void> future4_4 = CompletableFuture.allOf(future4_1, future4_2, future4_3);
        //System.out.println("必须等待上面3个任务完成 但没有返回值"+future4_4.get());
        //System.out.println("三个的结果都要等待结束后"+future4_1.get() + future4_2.get() + future4_3.get());
        //System.out.println("结束");



        /**
         * 多任务组合 2
         * anyOf() 只要一个任务，结束就可以继续执行
         */
        System.out.println("开始");
        CompletableFuture<String> future4_1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息……");
            return "hello.jpg";
        }, executor);
        CompletableFuture<String> future4_2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性……");
            return "黑色 2+64G";
        }, executor);
        CompletableFuture<String> future4_3 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("查询商品的介绍……");//模拟业务时间超长
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "apple";
        }, executor);
        //anyOf() 只要一个任务，结束就可以继续执行
        CompletableFuture<Object> future4_5 = CompletableFuture.anyOf(future4_1, future4_2, future4_3);
        System.out.println("最先成功的" + future4_5.get());

        System.out.println("结束");
    }





}
