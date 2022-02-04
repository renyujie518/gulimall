package com.renyujie.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class GulimallThirdPartyApplicationTests {
	@Resource
	OSSClient ossClient;

	@Test
	void contextLoads() {
	}

	@Test
	public void testUpload() throws FileNotFoundException {
		// // Endpoint以杭州为例，其它Region请按实际情况填写。
		// String endpoint = "http://oss-cn-beijing.aliyuncs.com";
		//// 云账号AccessKey有所有API访问权限，建议遵循阿里云安全最佳实践，创建并使用RAM子账号进行API访问或日常运维，请登录 https://ram.console.aliyun.com 创建。
		// String accessKeyId = "LTAI5t8tQGAjN4MpNrfv2k2Q";
		// String accessKeySecret = "66p27jI71YB6KmZG3b48jALPh0o6Dy";
		//
		//// 创建OSSClient实例。
		// OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

		// 上传文件流。
		InputStream inputStream = new FileInputStream("/Users/renyujie/Desktop/test.jpeg");
		ossClient.putObject("gulimall-ryj", "test130.png", inputStream);

		// 关闭OSSClient。
		ossClient.shutdown();

		System.out.println("上传完成...");

	}


}
