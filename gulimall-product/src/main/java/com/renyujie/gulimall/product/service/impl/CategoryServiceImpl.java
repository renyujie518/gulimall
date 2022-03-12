package com.renyujie.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.renyujie.gulimall.product.service.CategoryBrandRelationService;
import com.renyujie.gulimall.product.vo.Catalog3Vo;
import com.renyujie.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.product.dao.CategoryDao;
import com.renyujie.gulimall.product.entity.CategoryEntity;
import com.renyujie.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Resource
    CategoryBrandRelationService categoryBrandRelationService;
    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 查出所有分类以及子分类，以树形结构组装起来
     * 备注：
     * (c1, c2) -> { return (c1.getSort() == null ? 0 : c1.getSort()) - (c2.getSort() == null ? 0 : c2.getSort());
     * 按option+/优化成下面的comparingInt 意思与上面的一致
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        //首先查出所有的分类  注意  由于extends ServiceImpl  其中泛型最顶层的父是baseMapper  所以直接用baseMapper即可
        List<CategoryEntity> allEntities = baseMapper.selectList(null);
        //要从所有分类中递归的找到该分类下的子分类  依据parentCid判断  1为一级
        List<CategoryEntity> listWithTree = allEntities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((category) -> {
            category.setChildren(getCurrentMenuChilren(category, allEntities));
            return category;
        }).sorted(
                //升序
                Comparator.comparingInt(c -> (c.getSort() == null ? 0 : c.getSort()))
        ).collect(Collectors.toList());
        return listWithTree;
    }

    /**
     * @Description: 逻辑删除
     */
    @Override
    public void removeCategoryByIds(List<Long> idList) {
        // TODO 1 检查当前删除的菜单，是否被别的地方引用
        // 逻辑删除
        baseMapper.deleteBatchIds(idList);
    }

    /**
     * @Description: 根据当前所属分类catelogId找到完整路径 [父,子，孙] 比如[2,25,225]
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        ArrayList<Long> catelogPath = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, catelogPath);
        Collections.reverse(parentPath);
        //list转成数组
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * @Description: 级联更新 所有数据
     * 比如gms_category中的category_name字段变化也更新pms_category_brand_relation表中的category_name字段
     * 约定：存储同一类型的数据，都可以指定成同一个分区 分区名(即使用注解的时候@Cacheable(value = {"XXX"}中的XXX)默认就是缓存的前缀
     */
    //失效模式 删除category区下【所有】的数据
    @CacheEvict(value = "category", allEntries = true)
    //等价于同时进行多种缓存操作 组合删除
    //@Caching(evict = {
    //        //失效模式：修改删除缓存
    //        @CacheEvict(value = "category", key = "'getLevel1Catrgorys'"),
    //        @CacheEvict(value = "category", key = "'getCatalogJson'")
    //})
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        //首先肯定跟新自己的那张表psm_category
        this.updateById(category);
        //category_name字段变化也更新pms_category_brand_relation表中的category_name字段
        categoryBrandRelationService.updateCategoryNameFromCategoryChange(category.getCatId(), category.getName());

        //TODO 更新其他关联

    }

    /**
     * @Description: "首页"获取一级目录
     * p168加入cache缓存  方法名为key
     * sync = true解决缓存击穿（RedisCache调用加本地锁synchronized的get方法） 默认是false
     */
    @Cacheable(value = {"category"}, key = "#root.method.name", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Catrgorys() {

        System.out.println("调用getLevel1Catrgorys方法  缓存一加下次不会调用");
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }



    /**
     * @Description: "首页"查出所有分类（最原始的查数据库）
     * 根据catalog.json展开的格式: 1级分类作id为key，值是2级分类数组，2级分类存在一个3级分类List的引用
     * 根据以上特点 由于key是动态从数据库中获得的 所以不能直接返回一个对象  所以返回Map
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDB_onlyDB() {

        List<CategoryEntity> categoryEntityList = baseMapper.selectList(null);
        /** 1.  查出1级目录(parentId = 0)  **/
        List<CategoryEntity> level1Categorys = getCatalogWithListAndParentId(categoryEntityList, 0L);
        /** 2. 封装对象为res  **/
        Map<String, List<Catelog2Vo>> res;
        res = level1Categorys.stream().collect(Collectors.toMap(
                //!!!!!key=一级目录id  value=List<Catelog2Vo>!!!!
                k -> k.getCatId().toString(),
                v -> {
                    //!!!!!key=一级目录id  value=List<Catelog2Vo>!!!!
                    /** 2.1 拿到每一个1级分类 查到这个1级分类的2级分类  **/
                    //v此时是l1级别的目录  其catId作为parentId传给下面的方法得到的就是l2
                    List<CategoryEntity> level2Categorys = getCatalogWithListAndParentId(categoryEntityList, v.getCatId());
                    List<Catelog2Vo> l2List = null;
                    if (level2Categorys != null && level2Categorys.size() > 0) {
                        /** 2.2 对于每一个2级分类 最后包装成Catalog2Vo   **/
                        l2List = level2Categorys.stream().map((l2) -> {
                            //Catelog2Vo对象属性：catalog1Id;  catalog3List; id(l2的id); name(l2的name);  都是string
                            Catelog2Vo catelog2Vo = new Catelog2Vo();
                            catelog2Vo.setCatalog1Id(v.getCatId().toString());
                            catelog2Vo.setId(l2.getCatId().toString());
                            catelog2Vo.setName(l2.getName());
                            /** 2.3 对于每一个2级分类 查到这个2级分类的3级分类,包装成Catalog3Vo   **/
                            List<CategoryEntity> level3Categorys = getCatalogWithListAndParentId(categoryEntityList, l2.getCatId());
                            if (level3Categorys != null && level3Categorys.size() > 0) {
                                List<Catalog3Vo> l3List = level3Categorys.stream().map((l3) -> {
                                    //Catelog3Vo对象属性：catalog2Id;  id(l3的id); name(l3的name);  都是string
                                    Catalog3Vo catalog3Vo = new Catalog3Vo();
                                    catalog3Vo.setCatalog2Id(l2.getCatId().toString());
                                    catalog3Vo.setId(l3.getCatId().toString());
                                    catalog3Vo.setName(l3.getName());
                                    return catalog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(l3List);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return l2List;
                }));
        return res;
    }

    /**
     * @Description: 和上面的方法类似  但是查到的数据库再放入缓存
     */
    public Map<String, List<Catelog2Vo>> getDataFromDB_andSetRedis() {
        List<CategoryEntity> categoryEntityList = baseMapper.selectList(null);
        /** 1.  查出1级目录(parentId = 0)  **/
        List<CategoryEntity> level1Categorys = getCatalogWithListAndParentId(categoryEntityList, 0L);
        /** 2. 封装对象为res  **/
        Map<String, List<Catelog2Vo>> res;
        res = level1Categorys.stream().collect(Collectors.toMap(
                //!!!!!key=一级目录id  value=List<Catelog2Vo>!!!!
                k -> k.getCatId().toString(),
                v -> {
                    //!!!!!key=一级目录id  value=List<Catelog2Vo>!!!!
                    /** 2.1 拿到每一个1级分类 查到这个1级分类的2级分类  **/
                    //v此时是l1级别的目录  其catId作为parentId传给下面的方法得到的就是l2
                    List<CategoryEntity> level2Categorys = getCatalogWithListAndParentId(categoryEntityList, v.getCatId());
                    List<Catelog2Vo> l2List = null;
                    if (level2Categorys != null && level2Categorys.size() > 0) {
                        /** 2.2 对于每一个2级分类 最后包装成Catalog2Vo   **/
                        l2List = level2Categorys.stream().map((l2) -> {
                            //Catelog2Vo对象属性：catalog1Id;  catalog3List; id(l2的id); name(l2的name);  都是string
                            Catelog2Vo catelog2Vo = new Catelog2Vo();
                            catelog2Vo.setCatalog1Id(v.getCatId().toString());
                            catelog2Vo.setId(l2.getCatId().toString());
                            catelog2Vo.setName(l2.getName());
                            /** 2.3 对于每一个2级分类 查到这个2级分类的3级分类,包装成Catalog3Vo   **/
                            List<CategoryEntity> level3Categorys = getCatalogWithListAndParentId(categoryEntityList, l2.getCatId());
                            if (level3Categorys != null && level3Categorys.size() > 0) {
                                List<Catalog3Vo> l3List = level3Categorys.stream().map((l3) -> {
                                    //Catelog3Vo对象属性：catalog2Id;  id(l3的id); name(l3的name);  都是string
                                    Catalog3Vo catalog3Vo = new Catalog3Vo();
                                    catalog3Vo.setCatalog2Id(l2.getCatId().toString());
                                    catalog3Vo.setId(l3.getCatId().toString());
                                    catalog3Vo.setName(l3.getName());
                                    return catalog3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(l3List);
                            }
                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return l2List;
                }));
        //TODO 查到的数据库再放入缓存， 将对象转为json放在缓存中
        String jsonString = JSON.toJSONString(res);
        stringRedisTemplate.opsForValue().set("catalogJSON", jsonString, 1, TimeUnit.DAYS);//1天过期
        return res;
    }

    /**
     * @Description:  放到redis（老师最开始的想法 ）
     * 但有以下问题
     * 缓存穿透：设置null
     * 缓存雪崩：设置不同的过期时间
     * 缓存击穿：加分布式锁
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonWithIdea() {
        //先从缓存中取
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            //如果缓存中没有  就去数据库中取  重制缓存  再return
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDB_onlyDB();
            String catalogJsonString = JSON.toJSONString(catalogJsonFromDB);
            stringRedisTemplate.opsForValue().set("catalogJson", catalogJsonString);
            return catalogJsonFromDB;
        }
        //如果缓存中有   反序列化
        Map<String, List<Catelog2Vo>> res = JSON.parseObject(catalogJson,
                new TypeReference<Map<String, List<Catelog2Vo>>>() {});
        return res;
    }

    /**
     * 本地锁  synchronized (this){}  或 public synchronized ....
     *
     * 问题1： 会出现由于时序问题导致的2次查DB,synchronized的意义就失去了
     * 解决1： 数据库查询结果后紧接着放redis，这样getDataFromDB_andSetRedis的过程还是在synchronized锁内的，保证原子性
     *
     * 问题2：本地锁,只能锁住当前进程(即当前启动的vm),分布式情况下，有几个机器上部署了product服务，就会有几个本地锁存在
     *       这几个锁只是管住的自己的进程下是挨个请求DB的，但比如有8个机器，还是会有8次DB
     *       仍然无法实现"缓存击穿前就一个请求DB"
     * 解决2：分布式锁 让一个"入口"统一的管起来
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {

        //加锁 只要是同一把锁，就能锁住，需要这一把锁的所有线程
        //synchronized (this) {springBoot所有的组件，在容器中都是单例的。
        //TODO 本地锁 synchronized JUC(Lock), 在分布式情况下，想要锁住所有，必须使用分布式锁
        synchronized (this) {
            //得到锁以后,我们应该再去缓存中确定一次，如果没有才需要继续查询。
            return getDataFromDB_andSetRedis();
        }
    }

    /**
     * @Description: 压测的时候验证上面本地锁  synchronized是否成功（多vm下会有多次"缓存不命中！。。。。查询数据库。。"）
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonWithSynchronized() {

        //1 加入缓存逻辑， 以后缓存中存放的都是json字符串   json跨平台、跨语言兼容
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJson)) {
            //2 缓存中没有，查询数据库。
            System.out.println("缓存不命中！。。。。查询数据库。。");
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithLocalLock();
            return catalogJsonFromDb;
        }
        System.out.println("缓存命中！。。。。直接返回。");

        //转为指定的对象
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
        return result;
    }


    /**
     * 分布式锁 setnx   setIfAbsent    实际上去redis占坑  用一个键去模拟锁
     * 问题1： 死锁问题  在执行业务的的时候宕机了 delete没有执行 其他进程永远拿不到锁
     * 解决1： 设置过期时间  就算没删除  一定时间后也自动删除了key="lock"  相当于把锁删除了
     *
     * 问题2： 解决1执行的时候断电了  不能成功设置锁的过期时间  还是会死锁
     * 解决2："加锁"和"设置锁的过期时间"  是原子操作  利用setnx语句中的EX过期时间属性：set lock balala EX 300 NX
     *
     * 问题3：删除锁的时候 如果由于业务时间很长,锁自己过期了,就有可能被别人抢占,我们直接删除,有可能把别人正在持有的锁删除了
     * 解决3：占锁的时候,value指定为uuid,每个人匹配是自己的锁才删除。
     *
     * 问题4：删除锁（删除key）的问题  可能会由于调用时长导致在.get("lock")途中如果正好判断是当前值,正要删除锁的时候
     *       锁已经过期,别人已经设置到了新的值。那么我们删除的是别人的锁
     * 解决4：删除锁原子    lua脚本解锁  get对比值 + 对比成功后del = 原子操作
     *
     * 总结： redis实现分布式锁  加锁的时候利用setnx语句中的EX 设置过期
     *                         解锁的时候匹配uuid  通过脚本del
     *
     * 问题5： 锁的续期   执行业务可能时间比较长 锁万一在这时候过期
     * 解决5   设置过期时间长点  300s啥业务也执行完了  但是最终一定在finally别忘了解锁  无论业务奔溃还是执行完毕，别人也要获取啊
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithSetnx() {
        //1 抢占分布式锁（实际上就是占住"lock"这个键 setIfAbsent返回true代表占坑成功）
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功......");
            /*** 加锁成功 占到了坑位 ---> 执行业务***/
            //2 设置过期时间  -- 30s 必须和加锁是同步的 原子的
            //stringRedisTemplate.expire("lock", 30, TimeUnit.MINUTES);
            Map<String, List<Catelog2Vo>> res;
            try {
                //业务代码
                res = getDataFromDB_andSetRedis();
            }finally {
                String script = "if redis.call('get',KEYS[1]) == ARGV[1]  then return redis.call('del',KEYS[1]) else return 0 end";
                //删除锁  返回的0或1不care
                Long ZeroOrOne = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"),
                        uuid);
            }
            ////解决问题3
            //String lockValue = stringRedisTemplate.opsForValue().get("lock");
            //if (uuid.equals(lockValue)) {
            //    //删除我自己的锁
            //    stringRedisTemplate.delete("lock");
            //}
            return res;
        }else {
            /***加锁失败 ----> 重试  就和synchronized()阻塞队列的逻辑一致  没有获得锁就在那自旋等待**/
            //次数多，就让其休眠
            System.out.println("获取分布式锁不成功......等待200ms后重试");
            try {
                //最好睡一会再自己调自己  否则容易"栈空间移除"
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //自旋的方式(自己调自己)
            return getCatalogJsonFromDbWithSetnx();
        }

    }

    /**
     * @Description: 压测的时候验证上面的分布式锁 setnx是否成功 （多vm下仅有1次"缓存不命中！。。。。查询数据库。。"）
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonWithSetnx() {

        //1 加入缓存逻辑， 以后缓存中存放的都是json字符串   json跨平台、跨语言兼容
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJson)) {
            //2 缓存中没有，查询数据库。
            System.out.println("缓存不命中！。。。。查询数据库。。");
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithSetnx();
            return catalogJsonFromDb;
        }
        System.out.println("缓存命中！。。。。直接返回。");

        //转为指定的对象
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
        return result;
    }

    /**
     * 使用redission分布式锁
     *
     * 缓存一致性问题
     * 缓存里面的数据如何和数据库里面的数据保持一致？
     * 1） 双写模式 数据库改完后，缓存也改
     * 2） 失效模式 数据库改完后，把缓存删掉
     *
     * 缓存数据一致性-解决方案
     * 无论是双写模式还是失效模式,都会导致缓存的不一致问题,即多个实例同时更新会出事,怎么办?
     * 1、如果是用户纬度数据(订单数据、用户数据),这种并发几率非常小,不用考虑这个问题,缓存数据加上过期时间,每隔一段时间触发读的主动更新即可
     * 2、如果是菜单,商品介绍等基础数据,也可以去使用canal订阅binlog的方式。
     * 3、缓存数据+过期时间也足够解决大部分业务对于缓存的要求。
     * 4、通过加效保证并发读写,写写的时候按顺序排好队,读读无所谓,所以适合使用读写锁,(业务不关心脏数据,允许临时脏数据可忽略);
     *
     * 总结。
     * 我们能放入缓存的数据本就不应该是实时性、一致性要求超高的,所以缓存数据的时候加上过期时间,保证每天拿到当前最新数据即可,
     * 我们不应该过度设计,增加系统的复杂性
     * 遇到实时性、一致性要求高的数据,就应该查数据库,即使慢点。
     *
     * ！！！！！！！本系统的一致性解决方案:
     * 1、缓存的所有数据都有过期时间,数据过期下一次查询触发主动更新
     * 2、读写数据的时候,加上分布式的读写锁。
     *          经常写,经常读 有极大影响   但菜单这玩意本身就是度多写少 非常合适
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        //1 锁的名字，锁的粒度，越细越快(就是和方法业务强相关)
        RLock lock = redisson.getLock("catalogJson-lock");
        //加锁
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDB;
        try {
            //业务代码
            dataFromDB = getDataFromDB_andSetRedis();
        }finally {
            lock.unlock();
        }
        return dataFromDB;
    }


    /**
     * @Description: "首页"查出所有分类(最终采用Cacheable)
     * 但如果非要使用双写模式 (DB修改后再放入缓存) 可以使用@CachePut
     * 补充：
     * @Cacheable 表示这个东西是结果是需要缓存的，如果缓存中有，注解不调用；否则调用注解，放入缓存（不需要在DB后手动放缓存  Cache全部做好了）
     * value 每一个需要缓存的数据我们都要来指定要放在哪个名字的缓存里【缓存的分区(按照业务类型区分)】
     * 3)、默认行为
     * 1）、如果缓存中有 方法不用调用
     * 2）、 key默认生成 缓存的名字::simplekey 自动生成的key值
     * 3）、缓存的value值 默认使用java虚拟化机制 将序列化的数据存到redis
     * 4）、默认过期时间为-1
     * <p>
     * 自定义
     * 1）、指定生成的缓存使用的key key的属性指定接受一个SpEL表达式
     * SpEL表达式地址
     * 2)、指定缓存的数据存活时间 配置文件中修改TTL
     * 3）、将数据修改为json格式
     * <p>
     * SpringCache的不足
     * 1、读模式（Spring-Cache都解决了）:
     * 缓存穿透:查询一个DB不存在的数据。解决:缓存空数据;配置文件中的spring.cache.redis.cache-null-values=true解决【布隆过滤器】
     * 缓存击穿:大量并发进来同时查询一个正好过期的数据。解决:加锁，允许少量去查DB; 默认未加锁【sync = true】实质上是一种本地锁（够用）
     * 缓存雪崩:大量的key同时过期。解决:加上过期时间。配置文件中的spring.cache.redis.time-to-live= 360000s
     * <p>
     * 2、写模式:（Spring-Cache没有解决）
     * <p>
     * <p>
     * 总结:
     * 常规数据（读多写少，即时性，一致性要求不高的数据）﹔完全可以使用Spring-Cache
     * 写模式Spring-Cache没做针对性措施，一般缓存的数据有过期时间会触发主动更新，不是强一致的要求下也够用
     * 针对写模式  为保证缓存与数据库一致，可采取的其他方案（p166提及）
     * * 	1)、加读写锁。
     * * 	2)、引入canal，感知mysql的更新去更新缓存
     * * 	3)、读多写多，直接去查询数据库就行
     * 特殊数据:特殊设计
     */
    @Override
    @Cacheable(value = "category", key = "#root.methodName", sync = true)
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        return getCatalogJsonFromDB_onlyDB();
    }


        /**
         * @Description: 递归寻找完整路径 但注意是倒序的
         */
    private List<Long> findParentPath(Long catelogId, List<Long> catelogPath) {
        catelogPath.add(catelogId);
        CategoryEntity currentCategoryEntity = this.getById(catelogId);
        if (currentCategoryEntity.getParentCid() != 0) {
            findParentPath(currentCategoryEntity.getParentCid(), catelogPath);
        }
        return catelogPath;
    }

    /**
     * @param all 所有的目录
     * @Description: 递归查找当前目录curr的所有子目录children
     */
    private List<CategoryEntity> getCurrentMenuChilren(CategoryEntity curr, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(category -> {
            //对于all中的每个实体  其ParentCid = 当前目录的id  即该实体为curr的children
            // 注意此处应该用longValue()来比较，否则会出先bug，因为parentCid和catId是long类型
            return category.getParentCid().longValue() == curr.getCatId().longValue();
        }).map(category -> {
            //children可能还有children 所以做递归
            category.setChildren(getCurrentMenuChilren(category, all));
            return category;
        }).sorted(
                //升序
                Comparator.comparingInt(c -> (c.getSort() == null ? 0 : c.getSort()))
        ).collect(Collectors.toList());
        return children;
    }

    /**
     * @Description: 工具性方法 由于经常要查询1，2，3级目录  所有的catagory信息都在CatelogEntry中
     * 所以可以依据baseMapper.selectList统一查出List<CategoryEntity>（不多且基本固定）再依据id查询分级category
     */
    private List<CategoryEntity> getCatalogWithListAndParentId(List<CategoryEntity> entityList, Long parentId) {
        List<CategoryEntity> result = entityList.stream().filter((entity) -> {
            return (entity.getParentCid().equals(parentId));
        }).collect(Collectors.toList());
        return result;
    }

}