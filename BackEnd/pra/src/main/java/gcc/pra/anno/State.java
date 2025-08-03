package gcc.pra.anno;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

import gcc.pra.validation.StateValidation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = {StateValidation.class})
public @interface State {

    // 提供校验失败的信息
    String message() default "{文章状态只能是已发布或者草稿}";

    // 指定分组
    Class<?>[] groups() default {};

    // 指定负载,state的附加信息
    Class<? extends Payload>[] payload() default {};
}
