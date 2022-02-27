package com.renyujie.gulimall.member.exception;

/**
 用户的用户名已存在异常
 在注册的时候   由于用户名可以作为登录号码  所以在数据库中不允许重复
 利用异常机制 一层层往上抛  一旦重复  throw此异常  最终由controller层感知  在别人远程调用此方法时报异常
 */
public class UsernameExistException extends RuntimeException {

    public UsernameExistException() {
        super("用户名已经存在");
    }
}
