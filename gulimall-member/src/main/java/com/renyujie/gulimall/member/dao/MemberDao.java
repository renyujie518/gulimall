package com.renyujie.gulimall.member.dao;

import com.renyujie.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:19:14
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
