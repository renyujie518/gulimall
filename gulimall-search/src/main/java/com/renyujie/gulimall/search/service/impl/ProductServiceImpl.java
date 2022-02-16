package com.renyujie.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.renyujie.common.dto.es.SkuEsModel;
import com.renyujie.gulimall.search.config.GulimallElasticSearchConfig;
import com.renyujie.gulimall.search.constant.EsConstant;
import com.renyujie.gulimall.search.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    RestHighLevelClient restHighLevelClient;

    /**
     * @Description: 上架商品
     */
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //将数据保存带es中
        //1 给es中建立索引 "product" 建立好映射关系(在Kibana中执行productMapping.txt)
        //2 保存数据到es中  bulk 批量操作
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel skuEsModel : skuEsModels) {
            //设置索引
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(skuEsModel.getSkuId().toString());
            String jsonString = JSON.toJSONString(skuEsModel);
            indexRequest.source(jsonString, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }

        //执行（此处向上抛异常）
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        
        //TODO 如果批量错误
        boolean b = bulk.hasFailures();
        List<String> ItemsIds = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        //商品上架完成,执行成功的ItemsIds：[1, 2, 3, 4, 5, 6, 7, 8]，返回数据:org.elasticsearch.action.bulk.BulkResponse@2653ffa1
        log.info("商品上架完成,执行成功的ItemsIds：{}，返回数据:{}", ItemsIds, bulk.toString());

        //如果出现错误 即hasFailures  返回true
        return b;
    }
}
