package com.renyujie.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.renyujie.common.utils.R;
import com.renyujie.gulimall.cart.feign.ProductFeignService;
import com.renyujie.gulimall.cart.interceptor.CartInterceptor;
import com.renyujie.gulimall.cart.service.CartService;
import com.renyujie.gulimall.cart.vo.Cart;
import com.renyujie.gulimall.cart.vo.CartItem;
import com.renyujie.gulimall.cart.vo.SkuInfoVo;
import com.renyujie.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName CartServiceImpl.java
 * @Description 购物车服务
 * @createTime 2022年03月01日 14:58:00
 */
@Service
@Slf4j
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;

    @Resource
    ProductFeignService productFeignService;

    @Resource
    ThreadPoolExecutor executor;

    //购物车存在redis中的大目录的前缀
    private final String CART_PREFIX = "gulimall:cart:";


    /**
     * @Description: 将商品添加到购物车(商品详情页  skuid+数量值)
     * 这里用到CompletableFuture异步编排  注意  放到redis中的操作必须是cartItem构建完  即异步远程任务全部执行完
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        /**获取到我们要操作的购物车（会区分登录/临时用户）  以后操作cartOps 就是操作redis**/
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //先判断是新增商品，还是修改该项商品（的数量）  所以先去redis取出来看看有没有这个商品
        /**redis中存的样子： gulimall:cart:112233目录下    key=skuid    value=CartItem对象串 **/
        String cartItemFromRedis = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(cartItemFromRedis)) {
            //说明购物车没有此商品，新增商品类型,存入redis
            CartItem cartItem = new CartItem();
            /** 远程查询当前要操作的商品信息 获得真正的sku商品信息 并封装**/
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R r = productFeignService.getSkuInfo(skuId);
                if (r.getCode() == 0) {
                    SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                    });
                    cartItem.setSkuId(skuId);
                    cartItem.setCheck(true);
                    cartItem.setCount(num);
                    cartItem.setImage(skuInfo.getSkuDefaultImg());
                    cartItem.setTitle(skuInfo.getSkuTitle());
                    cartItem.setPrice(skuInfo.getPrice());
                }
            }, executor);
            /** 远程查询sku销售属性的组合信息  attr_name:attr_value **/
            CompletableFuture<Void> getSkuSaleAttrValuesTask = CompletableFuture.runAsync(() -> {
                List<String> attrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(attrValues);
            }, executor);
            //等待cartItem构建完  否则可能 cartItem是空对象  再放redis,返回构建好的cartItem
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValuesTask).get();
            /** 操作Redis  redis中存的样子： gulimall:cart:112233目录下    key=skuid    value=CartItem对象串 **/
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        } else {
            //说明购物车有此商品，只是需要修改原购物车中sku的数量（累加）
            CartItem oldCartItemFromRedis = JSON.parseObject(cartItemFromRedis, CartItem.class);
            oldCartItemFromRedis.setCount(oldCartItemFromRedis.getCount() + num);
            /** 更新Redis **/
            cartOps.put(skuId.toString(), JSON.toJSONString(oldCartItemFromRedis));
            return oldCartItemFromRedis;
        }
    }

    /**
     * @Description: 根据skuid从购物车中获取对应的购物项
     * 添加成功后，查询购物车信息，显示到success.html页面
     */
    @Override
    public CartItem getCartItem(Long skuId) {
        /**获取到我们要操作的购物车 （会区分登录/临时用户） 以后操作cartOps 就是操作redis**/
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        /**redis中存的样子： gulimall:cart:112233目录下    key=skuid    value=CartItem对象串 **/
        String cartItemFromRedis = (String) cartOps.get(skuId.toString());
        CartItem cartItem = JSON.parseObject(cartItemFromRedis, CartItem.class);
        return cartItem;
    }

    /**
     * @Description: 获取购物车（"首页"鼠标悬浮在"购物车"图标上显示的信息  点击"购物车"后显示的数据）
     */
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        /** 1. 首先查看用户的登录状态**/
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null) {
            //说明用户是"登录"状态
            String loadCartKey = CART_PREFIX + userInfoTo.getUserId();
            /** 2."已登录"用户 看一下有没有临时用户的购物车 如果有要把"临时"合并到"登录"购物车**/
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null && tempCartItems.size() > 0) {
                /**说明临时用户购物车有数据 需要合并(放入redis)
                 *  调用addToCart()即可  该方法里面有判断是新增商品，还是修改该项商品（的数量）**/
                for (CartItem tempCartItem : tempCartItems) {
                    addToCart(tempCartItem.getSkuId(), tempCartItem.getCount());
                }
                //合并完成，需要清除临时用户
                clearCart(tempCartKey);
            }
            /** 再来获取登录后的购物车数据 **/
            List<CartItem> cartItems = getCartItems(loadCartKey);
            cart.setItems(cartItems);
        } else {
            //说明用户是"未登录"状态
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            /** 3. "未登录"用户 获取临时购物车里的购物项目**/
            List<CartItem> cartItemsInTempUser = getCartItems(cartKey);
            cart.setItems(cartItemsInTempUser);
        }
        return cart;
    }


    /**
     * 工具方法  从redis获取到我们要操作的购物车（会区分登录/临时用户）
     */
    private BoundHashOperations<String,Object,Object>getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //说明用户登录了，Redis存入带UserId    gulimall:cart:11
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            //说明是临时用户，Redis存入带uuid    gulimall:cart:fg1argadr3gdab3dgfsr41ag
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        //让Redis全部操作 cartKey 这个大key  以后操作operations就是操作redis
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        //设置过期时间  这里设置为1天  购物车内容就会清空
        //operations.expire(1, TimeUnit.DAYS);
        return operations;
    }

    /**
     * @Description: 工具方法 输入用户状态（登录/游客），从redis中获取购物车 商品项
     * 输入大目录cartKey用户身份  返回该用户下所有购物项List<CartItem>
     */
    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        /**redis中存的样子： gulimall:cart:112233目录下    key=skuid    value=CartItem对象串
           .values()是获取所有的Object，即不管key,全部获取value=CartItem对象串  **/
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0) {
            List<CartItem> cartItemList = values.stream().map((value) -> {
                String CartItemStr = (String) value;
                CartItem cartItem = JSON.parseObject(CartItemStr, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return cartItemList;
        } else {
            return null;
        }
    }

    /**
     * 清空购物车
     */
    @Override
    public void clearCart(String cartKey) {

        redisTemplate.delete(cartKey);
    }

    /**
     * 勾选购物项目  前端传来被勾选的是哪个sku  是否是被选中状态 1选中 0未选中
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        //获取我们要操作的购物车（会区分登录/临时用户）
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //获取购物车的一个购物项
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1 ? true : false);
        //跟新redis
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    /**
     * 修改购物项目的数量
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        //获取到我们要操作的购物车（会区分登录/临时用户）
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //获取购物车的一个购物项
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        //跟新redis
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    /**
     * 删除购物车里的某一项
     */
    @Override
    public void deleteItem(Long skuId) {

        //获取到我们要操作的购物车（会区分登录/临时用户）
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        //跟新redis
        cartOps.delete(skuId.toString());
    }

    /**
     * 给远程gulimall-order调用
     * 获取当前用户所有购物项（已选中的）
     */
    @Override
    public List<CartItem> getUserCartItems() {
        UserInfoTo userInfo = CartInterceptor.threadLocal.get();
        if (userInfo.getUserId() == null) {
            //说明没登录  没登录不允许结算 所以返回null
            return null;
        } else {
            //登陆了
            String cartKey = CART_PREFIX + userInfo.getUserId();
            List<CartItem> cartItemList = getCartItems(cartKey);
            //筛选商品项列表中  被"勾选"的   同时购物车中的陈列了很久  由于价格变动，一定要获取到最新的价格
            List<CartItem> res = cartItemList.stream()
                    .filter(item -> item.getCheck())
                    .map(itemHasChecked -> {
                        R r = productFeignService.getPrice(itemHasChecked.getSkuId());
                        if (r.getCode() == 0) {
                            String lastPrice = (String) r.get("data");
                            itemHasChecked.setPrice(new BigDecimal(lastPrice));
                        }
                        return itemHasChecked;
                    }
            ).collect(Collectors.toList());
            return res;
        }
    }


}
