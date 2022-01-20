package com.renyujie.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.renyujie.common.utils.PageUtils;
import com.renyujie.gulimall.member.entity.GrowthChangeHistoryEntity;

import java.util.Map;

/**
 * 成长值变化历史记录
 *
 * @author renyujie518
 * @email renyujie518@gmail.com
 * @date 2022-01-20 21:19:14
 */
public interface GrowthChangeHistoryService extends IService<GrowthChangeHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

