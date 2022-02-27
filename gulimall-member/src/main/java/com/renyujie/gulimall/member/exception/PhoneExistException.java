package com.renyujie.gulimall.member.exception;

/**
   用户的手机号已存在异常
   在注册的时候   由于手机号可以作为扥录号码  所以在数据库中不允许重复
   利用异常机制 一层层往上抛  一旦重复  throw此异常  最终由controller层感知  在别人远程调用此方法时报异常
 */
public class PhoneExistException extends RuntimeException {

    public PhoneExistException() {
        super("手机号已经存在");
    }
}
