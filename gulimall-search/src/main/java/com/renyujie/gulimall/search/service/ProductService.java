package com.renyujie.gulimall.search.service;



import com.renyujie.common.dto.es.SkuEsModel;

import java.io.IOException;
import java.util.List;


public interface ProductService {
    /**
     * @Description: 上架商品
     */
    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
