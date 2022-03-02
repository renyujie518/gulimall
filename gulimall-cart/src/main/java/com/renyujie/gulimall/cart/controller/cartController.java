package com.renyujie.gulimall.cart.controller;

import com.renyujie.gulimall.cart.service.CartService;
import com.renyujie.gulimall.cart.vo.Cart;
import com.renyujie.gulimall.cart.vo.CartItem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName cartController.java
 * @Description TODO
 * @createTime 2022年03月01日 15:04:00
 */
@Controller
public class cartController {
    @Resource
    CartService cartService;

    /**
     * 浏览器有一个cookie：user-key:标识用户身份code  一个月过期
     * 假如是第一次登录，都会给一个临时身份(一个临时的user-key)
     * 浏览器保存以后，每次访问都会带上这个cookie
     *
     * 拦截器逻辑：
     * 登录：session有
     * 没登录：按照cookie里面带来user-key来做。
     * 第一次：如果没有临时用户，帮忙创建一个临时用户。
     */




    /**
     * @Description: 获取购物车（"首页"鼠标悬浮在"购物车"图标上显示的信息  点击"购物车"后显示的数据）
     */
    @GetMapping("/cart.html")
    public String cartListPage(Map<String, Cart> map) throws ExecutionException, InterruptedException {

        Cart cart = cartService.getCart();
        map.put("cart", cart);
        return "cartList";
    }


    /**
     * 将商品添加到购物车(商品详情页)
     * 防止用户恶意刷新，可以使用重定向的办法，本方法不跳转页面，只是执行完业务代码后，跳转到别的方法，让那个方法跳转页面
     * redirectAttributes.addFlashAttribute() 模拟session 将数据保存在session里面可以在页面取出，但是只能取一次
     * redirectAttributes.addAttribute("skuId", skuId); 将数据当做param放在URL后面
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {

        //业务代码
        CartItem cartItem = cartService.addToCart(skuId, num);
        //将数据当做param放在URL后面
        //model.addAttribute("skuId", skuId);
        redirectAttributes.addAttribute("skuId", skuId);

        //return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
        return "redirect:http://127.0.0.1:40000/addToCartSuccess.html";
    }


    /**
     * @Description: 根据skuid从购物车中获取对应的购物项
     * 添加成功后，查询购物车信息，显示到success.html页面
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccess(@RequestParam("skuId") Long skuId,
                                   Model model) {
        //addToCart会重定向到此  在这里不操作cart  仅仅是查询（从redis中查）
        CartItem cartItem = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItem);
        return "success";
    }

    /**
     * 勾选购物项目  前端传来被勾选的是哪个sku  是否是被选中状态 1选中 0未选中
     */
    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check) {

        cartService.checkItem(skuId, check);
        //相当于在刷新获取一次购物车页面 展示
        return "redirect:http://127.0.0.1:40000/cart.html";
    }


    /**
     * 修改购物项目的数量
     */
    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num) {

        cartService.changeItemCount(skuId, num);
        //相当于在刷新获取一次购物车页面 展示
        return "redirect:http://127.0.0.1:40000/cart.html";
    }

    /**
     * 删除购物车里的某一项
     */
    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {

        cartService.deleteItem(skuId);
        //相当于在刷新获取一次购物车页面 展示
        return "redirect:http://127.0.0.1:40000/cart.html";
    }



}
