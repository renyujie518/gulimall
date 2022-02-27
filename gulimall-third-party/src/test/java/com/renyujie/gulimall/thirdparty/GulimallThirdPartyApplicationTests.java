package com.renyujie.gulimall.thirdparty;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;


import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.google.gson.Gson;
import com.renyujie.gulimall.thirdparty.component.SmsComponent;
import com.renyujie.gulimall.thirdparty.utils.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class GulimallThirdPartyApplicationTests {
	@Resource
	OSSClient ossClient;
	@Resource
	SmsComponent smsComponent;

	@Test
	void contextLoads() {
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
		ossClient.putObject("gulimall-ryj", "test130.png", inputStream);

		// 关闭OSSClient。
		ossClient.shutdown();

		System.out.println("上传完成...");

	}


	@Test
	public void testSendSms(){

		String host = "https://smssend.shumaidata.com";
		String path = "/sms/send";
		String method = "POST";
		String appcode = "f2ecc815f63d46eeb5aa82ad974817af";
		Map<String, String> headers = new HashMap<String, String>();
		//最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
		headers.put("Authorization", "APPCODE " + appcode);
		Map<String, String> querys = new HashMap<String, String>();
		//手机号
		querys.put("receive", "18810508220");
		//验证码
		querys.put("tag", "1314520");
		//短信模板
		querys.put("templateId", "M09DD535F4");
		Map<String, String> bodys = new HashMap<String, String>();


		try {
			/**
			 * 重要提示如下:
			 * HttpUtils请从
			 * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
			 * 下载
			 *
			 * 相应的依赖请参照
			 * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
			 */
			HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
			System.out.println(response.toString());
			//获取response的body
			System.out.println(EntityUtils.toString(response.getEntity()));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testSmsComponent(){

		smsComponent.sendSmsCode("18810508220", "111111");
	}



}
