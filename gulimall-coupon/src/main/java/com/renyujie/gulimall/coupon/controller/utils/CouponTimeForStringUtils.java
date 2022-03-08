package com.renyujie.gulimall.coupon.controller.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
    日期类的工具类 最后转为string
 */
public class CouponTimeForStringUtils {

    //组合为开始时间
    public static String startTimeString() {
        LocalDate now = LocalDate.now();
        //00:00
        LocalTime min = LocalTime.MIN;
        LocalDateTime start = LocalDateTime.of(now, min);
        String format = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

    public static String endTimeForString() {
        LocalDate now = LocalDate.now();
        //两天后
        LocalDate plus2 = now.plusDays(2);
        // 23:59:59.999999999
        LocalTime max = LocalTime.MAX;
        LocalDateTime end = LocalDateTime.of(plus2, max);
        String format = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }
}
