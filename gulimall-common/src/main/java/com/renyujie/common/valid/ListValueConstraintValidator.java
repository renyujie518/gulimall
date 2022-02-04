package com.renyujie.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author 自定义校验器  @ListValue
 *
 * 实现ConstraintValidator接口   第一个泛型是实现的注解 第二个是注解的标注的数据类型
 */
public class ListValueConstraintValidator implements ConstraintValidator<ListValue,Integer> {
    //set容器
    private Set<Integer> set = new HashSet<Integer>();
    /**
     * 初始化
     * @param constraintAnnotation
     */
    @Override
    public void initialize(ListValue constraintAnnotation) {
        int[] vals = constraintAnnotation.vals();
        for (int val : vals) {
            set.add(val);
        }
    }

    /**
     * 真正的校验规则
     * 判断是否校验成功
     * @param integer  属性接收的值
     * @param constraintValidatorContext  校验的上下文信息
     * @return
     */
    @Override
    public boolean isValid(Integer integer, ConstraintValidatorContext constraintValidatorContext) {
        return set.contains(integer);
    }
}
