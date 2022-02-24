package com.renyujie.gulimall.search.service;


import com.renyujie.gulimall.search.vo.SearchParam;
import com.renyujie.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     * @Description: 输入检索参数   返回检索结果
     */
    SearchResult search(SearchParam param);
}
