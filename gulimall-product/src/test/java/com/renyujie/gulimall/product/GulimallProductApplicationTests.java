package com.renyujie.gulimall.product;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.renyujie.gulimall.product.entity.BrandEntity;
import com.renyujie.gulimall.product.service.BrandService;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallProductApplicationTests {

	@Resource
	BrandService brandService;

	@Test
	public void contextLoads() {
		//BrandEntity brandEntity = new BrandEntity();
		//brandEntity.setName("华为测试");
		//brandService.save(brandEntity);
		//System.out.println("保存成功！！")
		List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().
				eq("brand_id", 1L));
		list.forEach((item) -> {
			System.out.println(item);
		});

	}

}
