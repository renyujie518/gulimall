package com.renyujie.gulimall.cart.service;

import com.renyujie.gulimall.cart.vo.Cart;
import com.renyujie.gulimall.cart.vo.CartItem;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName Cartservice.java
 * @Description TODO
 * @createTime 2022年03月01日 14:58:00
 */
public interface CartService {

    /**
     * @Description: 将商品添加到购物车(商品详情页上的skuid+数量值)
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * @Description: 根据skuid从购物车中获取对应的购物项
     * 添加成功后，查询购物车信息，显示到success.html页面
     */
    CartItem getCartItem(Long skuId);

    /**
     * @Description: 获取购物车（"首页"鼠标悬浮在"购物车"图标上显示的信息  点击"购物车"后显示的数据）
     */
    Cart getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车
     */
    void clearCart(String cartKey);
    /**
     * 勾选购物项目  前端传来被勾选的是哪个sku  是否是被选中状态 1选中 0未选中
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 修改购物项目的数量
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物车里的某一项
     */
    void deleteItem(Long skuId);

    /**
     * 给远程gulimall-order调用
     * 获取当前用户所有购物项（已选中的）
     */
    List<CartItem> getUserCartItems();
}
