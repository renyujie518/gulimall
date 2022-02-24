package com.renyujie.gulimall.search.controller;


import com.renyujie.gulimall.search.service.MallSearchService;
import com.renyujie.gulimall.search.vo.SearchParam;
import com.renyujie.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;



@Controller
public class SearchController {
    @Autowired
    MallSearchService mallSearchService;

    @GetMapping({"/", "/list.html"})
    public String listPage(SearchParam param, Model model,HttpServletRequest request) {
        //拿到网站（http://localhost:12000/）后面的完整的查询字符串
        String queryString = request.getQueryString();
        param.set_queryString(queryString);

        //根据传递来的页面的查询参数，去ES中检索商品
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result", result);
        return "list";
    }
}
