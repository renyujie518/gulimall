package com.renyujie.common.valid;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 *
 * @author 自定义校验注解
 * 此注解的功能是针对有限的列举的值 vals规定了这些值  比如0，1  必须提交指定的值
 * 使用案例：@ListValue(vals = {0,1},groups = {AddGroup.class, UpdateStatusGroup.class})
 */
@Documented
//指定校验器
@Constraint(
        validatedBy = {ListValueConstraintValidator.class}
)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ListValue {
    //校验错误的信息去哪取
    String message() default "必须指定提交的值";

    //允许分组
    Class<?>[] groups() default {};

    //自定义负载信息
    Class<? extends Payload>[] payload() default {};

    int[] vals() default {};

}
