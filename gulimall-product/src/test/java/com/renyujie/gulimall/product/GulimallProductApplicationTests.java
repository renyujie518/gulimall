package com.renyujie.gulimall.product;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.renyujie.gulimall.product.entity.BrandEntity;
import com.renyujie.gulimall.product.service.BrandService;

import com.renyujie.gulimall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
class GulimallProductApplicationTests {

	@Resource
	BrandService brandService;
	@Resource
	OSSClient ossClient;
	@Resource
	CategoryService categoryService;

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

	@Test
	public void testUpload() throws FileNotFoundException {
       // // Endpoint以杭州为例，其它Region请按实际情况填写。
       // String endpoint = "http://oss-cn-beijing.aliyuncs.com";
       //// 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
       // String accessKeyId = "";
       // String accessKeySecret = "";
	   //
       //// 创建OSSClient实例。
       // OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

		// 上传文件流。
		InputStream inputStream = new FileInputStream("/Users/renyujie/Desktop/test.jpeg");
		ossClient.putObject("gulimall-ryj", "test131.png", inputStream);

		// 关闭OSSClient。
		ossClient.shutdown();

		System.out.println("上传完成...");

	}

	@Test
	public void findCatelogPathTest() {
		Long[] catelogPath = categoryService.findCatelogPath(225L);
		// 完整路径：[2, 34, 225]
		log.info("完整路径：{}", Arrays.asList(catelogPath));

	}

}
