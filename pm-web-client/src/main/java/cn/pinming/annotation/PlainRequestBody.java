package cn.pinming.annotation;

import org.springframework.http.MediaType;

import java.lang.annotation.*;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/27 16:34
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PlainRequestBody {
    String contentType() default MediaType.APPLICATION_JSON_VALUE;
}
