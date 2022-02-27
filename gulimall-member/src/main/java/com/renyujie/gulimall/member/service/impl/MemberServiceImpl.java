package com.renyujie.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.renyujie.common.utils.HttpUtils;
import com.renyujie.gulimall.member.dao.MemberLevelDao;
import com.renyujie.gulimall.member.entity.MemberLevelEntity;
import com.renyujie.gulimall.member.exception.PhoneExistException;
import com.renyujie.gulimall.member.exception.UsernameExistException;
import com.renyujie.gulimall.member.vo.MemberLoginVo;
import com.renyujie.gulimall.member.vo.MemberRegistVo;
import com.renyujie.gulimall.member.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.common.utils.Query;

import com.renyujie.gulimall.member.dao.MemberDao;
import com.renyujie.gulimall.member.entity.MemberEntity;
import com.renyujie.gulimall.member.service.MemberService;

import javax.annotation.Resource;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {
    @Resource
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * @Description: 保存会员信息（注册页）
     */
    @Override
    public void regist(MemberRegistVo memberRegistVo) throws PhoneExistException, UsernameExistException {
        //member服务中的表对应结构是MemberEntity
        MemberEntity memberEntity = new MemberEntity();

        //异常机制（为了让controller感知异常 因为登录的时候可以用手机号或是用户名，这两个必须唯一）
        checkPhoneUnique(memberRegistVo.getPhone());
        checkUsernameUnique(memberRegistVo.getUserName());

        //密码需要加密后存储 MD5 密码加密处理
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String passwordEncode = passwordEncoder.encode(memberRegistVo.getPassword());
        memberEntity.setPassword(passwordEncode);

        //刚从"注册页"创建  肯定是默认等级：普通会员   default_status=1
        MemberLevelEntity memberlevelEntity = memberLevelDao.getDefaultLevel();
        if (memberlevelEntity != null) {
            memberEntity.setLevelId(memberlevelEntity.getId());
        }

        //其他默认信息
        memberEntity.setUsername(memberRegistVo.getUserName());
        memberEntity.setNickname(memberRegistVo.getUserName());
        memberEntity.setMobile(memberRegistVo.getPhone());

        //把这个大对象保存到数据库MemberEntity表中
        this.baseMapper.insert(memberEntity);
    }

    /**
     * @Description: 异常机制  确保memberRegistVo携带的phone信息在数据库中唯一
     */
    @Override
    public void checkPhoneUnique(String phone) throws PhoneExistException {
        Integer res = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));

        if (res > 0) {
            //说明数据库有这个手机号
            throw new PhoneExistException();
        }
        //否则什么都不做 检查通过 业务继续进行注册
    }

    /**
     * @Description: 异常机制  确保memberRegistVo携带的username信息在数据库中唯一
     */
    @Override
    public void checkUsernameUnique(String username) throws UsernameExistException {
        Integer res = this.baseMapper.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (res > 0) {
            //说明数据库有这个用户名
            throw new UsernameExistException();
        }
        //否则什么都不做 检查通过 业务继续进行注册
    }

    @Override
    public MemberEntity login(MemberLoginVo memberLoginVo) {
        String loginacct = memberLoginVo.getLoginacct();
        String password = memberLoginVo.getPassword();

        //1 去数据库查询 根据登录账号查
        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));
        if (memberEntity == null) {
            //登录失败，数据库没有这个用户
            return null;
        } else {
            //数据库有这个用户
            //1 获取到数据库中的password
            String passwordDb = memberEntity.getPassword();
            //2 进行密码比对
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encode123456 = passwordEncoder.encode("123456");
            boolean matches = passwordEncoder.matches(password, passwordDb);
            System.out.println(encode123456 + " -> " +matches );
            if (matches) {
                //密码比对成功，登录成功
                return memberEntity;
            } else {
                //用户存在，密码不对，登录失败
                return null;
            }
        }

    }


    /**
     * @Description: "登录页"  社交gitee登录
     */
    @Override
    public MemberEntity giteeLogin(SocialUser vo) throws Exception {
        Long finallId = 0L;
        /**
         * 观察发现  gitee返回的数据中没有uid  只有access_token 所以从远端接收过来的vo一定没有uid
         * 为了和老师一致 这里先用gitee的openApi再去查到当前用户的uid
         * 参考：https://gitee.com/api/v5/swagger#/getV5User
         */
        try {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", vo.getAccess_token());
            HttpResponse response = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", null, params);
            if (response.getStatusLine().getStatusCode() == 200) {
                String giteeWithUid2String = EntityUtils.toString(response.getEntity());
                JSONObject giteeInfo = JSON.parseObject(giteeWithUid2String);
                String socialUid = giteeInfo.getString("id");
                vo.setSocialUid(socialUid);

                //以下的vo都带有socialUid  至于数据库中到底有没有（新/老用户）再来判断
                MemberEntity member = this.getOne(new QueryWrapper<MemberEntity>().eq("social_uid", vo.getSocialUid()));
                if (member != null) {
                    // 说明已经注册过，更新令牌、令牌过期时间（socialUid本来就有，不动）
                    System.out.println("非首次登录本站");
                    MemberEntity newMember = new MemberEntity();
                    newMember.setId(member.getId());
                    newMember.setAccessToken(vo.getAccess_token());
                    newMember.setExpiresIn(vo.getExpires_in());
                    this.updateById(member);
                    finallId = member.getId();
                } else {
                    // 第一次登录，需要注册
                    System.out.println("首次登录本站");
                    MemberEntity newMember = new MemberEntity();
                    newMember.setSocialUid(vo.getSocialUid());
                    //这里只保存了name  其实像是性别，年龄什么的都可以保存  这里懒得做
                    newMember.setNickname(giteeInfo.getString("name"));
                    newMember.setSocialUid(vo.getSocialUid());
                    newMember.setAccessToken(vo.getAccess_token());
                    newMember.setExpiresIn(vo.getExpires_in());
                    this.save(newMember);
                    finallId = newMember.getId();
                }
            }
        } catch (Exception e) {
            throw e;
        }
        //不管是第一次登录还是已登录 经过上述步骤  数据库中的这条信息一定是最新的  都是当前用户信息
        // 由于auth服务和前端需要用到MemberEntity的信息  由于这个表不大  所以再查一次即可
        return this.baseMapper.selectById(finallId);
    }


}