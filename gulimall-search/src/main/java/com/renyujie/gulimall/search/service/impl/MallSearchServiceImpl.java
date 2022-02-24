package com.renyujie.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.renyujie.common.dto.es.SkuEsModel;
import com.renyujie.common.utils.R;
import com.renyujie.gulimall.search.config.GulimallElasticSearchConfig;
import com.renyujie.gulimall.search.constant.EsConstant;
import com.renyujie.gulimall.search.feign.ProductFeignService;
import com.renyujie.gulimall.search.service.MallSearchService;
import com.renyujie.gulimall.search.vo.AttrResponseVo;
import com.renyujie.gulimall.search.vo.BrandVo;
import com.renyujie.gulimall.search.vo.SearchParam;
import com.renyujie.gulimall.search.vo.SearchResult;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Resource
    RestHighLevelClient client;
    @Resource
    ProductFeignService productFeignService;
    /**
     * @Description: 根据传递来的页面的查询参数，去ES中检索商品
     */
    @Override
    public SearchResult search(SearchParam param) {
        /** 1.动态构建出查询需要的DSL语句  **/
        SearchResult result = null;
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            /** 2.执行检索  **/
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

            /** 3 分析响应数据，封装成我们需要的格式 **/
            result = buildSearchResult(response, param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }



    /**
     * @Description: 构建检索请求 param前端传来的参数
     * 依照 dsl.json
     * 查询:过滤- 按照属性 分类 品牌 价格区间 库存    排序    分页    高亮    聚合分析
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        /** 1.查询 **/
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        /** 1.1 must 模糊匹配 **/
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        /** 1.2 bool = filter 按照品牌id查询 **/
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        /**1.3 bool = filter 按照所指的属性进行查询 attrs=1_5寸:8寸&attrs=2_16G:8G  **/
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            for (String attrStr : param.getAttrs()) {
                String[] s = attrStr.split("_");
                //检索的属性id
                String attrId = s[0];
                //检索的属性值
                String[] attrValues = s[1].split(":");
                /**
                 * 这里与原版不太一样  原版是
                 *  "attrs.attrValue":[
                 *                        "2018",
                 *                        "2019"
                 *                               ],
                 *  但经过实验  自己这里是
                 *   "attrs.attrValue": [
                 *                        "2018;2019"
                 *                       ]
                 *
                 *所以这特殊处理   反解的时候也是
                 */
                String attrValues2Str = org.apache.commons.lang.StringUtils.join(attrValues, ";");
                //!!  每一个attrStr  会对应一个 nestedQuery
                BoolQueryBuilder boolQueryInNested = QueryBuilders.boolQuery();
                boolQueryInNested.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                boolQueryInNested.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues2Str));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", boolQueryInNested, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        /** 1.4 bool = filter 按照是否有库存进行查询  1有库存 在es中对应true **/
        if (param.getHasStock() != null) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        /** 1.5  bool = filter 按照价格区间进行查询  1_500   _500  500_ **/
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] price = param.getSkuPrice().split("_");
            if (price.length == 2) {
                rangeQuery.gte(price[0]).lte(price[1]);
            } else if (price.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    //_500
                    rangeQuery.lte(price[0]);
                }
                if (param.getSkuPrice().endsWith("_")) {
                    //500_
                    rangeQuery.gte(price[0]);
                }
            }
            boolQuery.filter(rangeQuery);
        }
        /** 1 把查询:过滤的条件都拿来进行封装**/
        sourceBuilder.query(boolQuery);

        /** 2.排序 saleCount_asc/desc **/
        if (!StringUtils.isEmpty(param.getSort())) {
            String[] s = param.getSort().split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(s[0], order);
        }

        /** 3.分页  默认每页5个  那么每页的起始页就是0 5 10...  刚好就是（页数-1）*每页个数 **/
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        /** 4.高亮 针对搜索框输入的关键字高亮**/
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder()
                    .field("skuTitle")
                    .preTags("<b style='color:red'>")
                    .postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }

        /** 5.聚合分析**/
        /** 5.1 品牌聚合**/
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(20);
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);
        /** 5.2 分类聚合**/
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);
        /** 5.3 属性聚合**/
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(20);
        //聚合分析出当前所有attrId对应的名字
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        //聚合分析出当前attrid对应的所有可能的属性值 attrvalue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(20));
        attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_agg);

        /**  构建检索请求完毕  **/
        String s = sourceBuilder.toString();
        System.out.println("构建的DSL。。。" + s);
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }



    /**
     * @Description: 封装响应数据
     * 根据es查询到的结果，分析得到页面真正得到的数据模型
     * response：es返回对象   param：前端传来的参数
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();
        /** 1. 封装查询到的所有商品  List<SkuEsModel> products  **/
        ArrayList<SkuEsModel> products = new ArrayList<>();
        SearchHits hits = response.getHits();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                //依据kibana可得真实的商品对象在"_source"中 而且里面的对象本身就是按照SkuEsModel设计的
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                /**
                 * 这里与原版不太一样  原版是
                 *  "attrs.attrValue":[
                 *                        "2018",
                 *                        "2019"
                 *                               ],
                 *  但经过实验  自己这里是
                 *   "attrs.attrValue": [
                 *                        "2018;2019"
                 *                       ]
                 *
                 *所以这特殊处理  只取其中的第一个  因为sku的商品属性肯定是只有一个
                 * 这个错误的根源在于往es中存的时候存错了  为了与老师一致  即sku的AttrValue只有一个  只能取第一个做案例
                 * 依照原视频中的写法会是   attrvalue="110mm;120mm;150mm"
                 * 按照只取第一个： attrvalue="110mm"
                 */
                List<SkuEsModel.Attrs> attrsList = skuEsModel.getAttrs();
                for (SkuEsModel.Attrs attrs : attrsList) {
                    // "2018;2019"
                    String attrValueButWithFenHao = attrs.getAttrValue();
                    //attrvalue="[110mm,120mm,150mm]"
                    //String attrValue = Arrays.toString(attrValueButWithFenHao.split(";"));
                    String attrValue = attrValueButWithFenHao.split(";")[0];
                    //attrvalue="110mm"
                    attrs.setAttrValue(attrValue);
                }
                /** 处理高亮  **/
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    String skuTitle = hit.getHighlightFields().get("skuTitle").getFragments()[0].string();
                    skuEsModel.setSkuTitle(skuTitle);
                }
                products.add(skuEsModel);
            }
        }
        result.setProducts(products);

        /** 2. 封装查询到的当前所有商品所涉及的品牌信息  List<BrandVo> brands **/
        ArrayList<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brand_agg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            //得到品牌id
            long brandId = bucket.getKeyAsNumber().longValue();
            //子聚合 得到品牌name
            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            //子聚合 得到品牌图片url
            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        /** 3. 封装查询到的当前所有商品所涉及到的所有分类信息  List<CatalogVo> catalogs **/
        ArrayList<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        for (Terms.Bucket bucket : catalog_agg.getBuckets()) {

            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            //得到分类id
            long catalogId = bucket.getKeyAsNumber().longValue();
            //子聚合 得到分类名 Aggregation -> ParsedStringTerms
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogId(catalogId);
            catalogVo.setCatalogName(catalogName);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        /** 4. 封装查询到的当前所有商品涉及到的所有属性信息  private List<AttrVo> attrs **/
        ArrayList<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        //nested的第一层 聚合 Aggregation -> ParsedLongTerms
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            //得到属性id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            //子聚合 得到属性name
            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attr_name_agg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            //子聚合 复杂 得到属性values
            /**
             * 这里与原版不太一样  原版是
             *  "attrs.attrValue":[
             *                        "2018",
             *                        "2019"
             *                               ],
             *  但经过实验  自己这里是
             *   "attrs.attrValue": [
             *                        "2018;2019"
             *                       ]
             *
             *所以这特殊处理  只取其中的第一个  因为sku的商品属性肯定是只有一个
             * 这个错误的根源在于往es中存的时候存错了  为了与老师一致  即sku的AttrValue只有一个  只能取第一个做案例
             * 依照原视频中的写法会是   attrvalue="110mm;120mm;150mm"
             * 按照只取第一个： attrvalue="110mm"
             * 这里虽然是聚合  也是类似  因为聚合是根据检索结果聚合 检索结果里目前的状况只会有1个attrvalue 所以只取第一个
             * （其实不用纠结这个  学习本章的目的本来就是学习es和在java中怎么用  不用陷到这里的逻辑）
             */
            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            //"2018;2019"
            String attrValuesAsStringButWithFenHao = attr_value_agg.getBuckets().get(0).getKeyAsString();
            //attrvalue="[110mm]"
            String attrValue = attrValuesAsStringButWithFenHao.split(";")[0];
            List<String> attrValues = new ArrayList<>();
            attrValues.add(attrValue);
            // attrvalue="[110mm;120mm;150mm]"
            //List<String> attrValues = Arrays.asList(attrValuesAsStringButWithFenHao.split(";"));
            attrVo.setAttrValue(attrValues);

            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);

        /** 5 分页信息 - 页码 **/
        result.setPageNum(param.getPageNum());
        /** 6 分页信息 - 总记录数 **/
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        /** 7 分页信息 - 总页码 计算得到 11 / 2 = 5 ... 1 **/
        int totalPages = (int)total % EsConstant.PRODUCT_PAGESIZE == 0?(int)total/EsConstant.PRODUCT_PAGESIZE:(int)(total/EsConstant.PRODUCT_PAGESIZE + 1);
        result.setTotalPages(totalPages);
        /** 8 页码导航**/
        ArrayList<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        /** 9 面包屑导航 **/
        /** 9.1 属性面包屑导航（有点问题 还是"attrs.attrValue": "2018;2019"导致的） **/
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //1 分析每个attrs传过来的查询参数值
                //attrs=2_2018
                String[] s = attr.split("_");
                //由于此时是点击某1个属性标签而生成的url语句 比如点击页面下"2018"标签的时候在url中拼接为http://localhost:12000/?attrs=2_2018
                //所以再通过前端传来的param获得的attrs后的"_"之后只有1个 即s[1]
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                //面包屑导航的时候告诉前端  哪些attr是被选中的（构成查询语句接到url后面的）方便前端做显示处理
                result.getAttrIds().add(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    //正常返回
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {});
                    navVo.setNavName(data.getAttrName());
                } else {
                    //如果失败 id作为名字
                    navVo.setNavName(s[0]);
                }

                //2 取消了面包屑以后 我们要跳转到哪个地方
                //拿到所有的查询条件 去掉当前
                String replace = replaceQueryString(param, attr, "attrs");
                //navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                navVo.setLink("http://localhost:12000/?" + replace);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVos);
        }

        /** 9.2 品牌面包屑导航 （没问题） **/
        if(param.getBrandId() != null && param.getBrandId().size()>0) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            //远程查询
            R r = productFeignService.BrandsInfo(param.getBrandId());
            if (r.getCode() == 0) {
                List<BrandVo> brands = r.getData("brand", new TypeReference<List<BrandVo>>() {});
                StringBuffer buffer = new StringBuffer();
                String replace = "";
                for (BrandVo brandVo : brands) {
                    buffer.append(brandVo.getName() + ";");
                    replace = replaceQueryString(param, brandVo.getBrandId()+"", "brandId");
                }
                navVo.setNavValue(buffer.toString());
                //navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                navVo.setLink("http://localhost:12000/?" + replace);
            }
            navs.add(navVo);
        }

        return result;
    }


    /**
     * @Description: 编写面包屑的功能时，删除指定请求
     * 将请求地址的url里面的当前请求参数置空
     */
    private String replaceQueryString(SearchParam param, String value,String key) {
        String encode = "";
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            //+ 对应浏览器的%20编码
            encode = encode.replace("+","%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //replace(char searchChar, char newChar)   用 newChar 字符替换字符串中出现的所有 searchChar 字符，并返回替换后的新字符串
        String res = param.get_queryString().replace("&" + key + "=" + encode, "");
        System.out.println("替换后" + res);
        return res;
    }
}


