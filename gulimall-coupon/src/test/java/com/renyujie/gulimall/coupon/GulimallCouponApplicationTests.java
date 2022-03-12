package com.renyujie.gulimall.coupon;


import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//@SpringBootTest
class GulimallCouponApplicationTests {

	@Test
	public void TimeTest() {

		LocalDate now = LocalDate.now();
		LocalTime min = LocalTime.MIN;
		LocalDateTime start = LocalDateTime.of(now, min);
		String format = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		System.out.println(format);

		LocalDate plus2 = now.plusDays(2);
		LocalTime max = LocalTime.MAX;
		LocalDateTime end = LocalDateTime.of(plus2, max);
		String format1 = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		System.out.println(format1);
	}

}
