package cn.pinming.annotation;

import cn.pinming.client.ClientFactoryBean;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 14:07
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(PmWebClientRegistrar.class)
public @interface EnablePmWebClient {

    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    Class<? extends Annotation> annotationClass() default ApiServer.class;

    Class<? extends ClientFactoryBean> factoryBean() default ClientFactoryBean.class;

    Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

    Class<?> markerInterface() default Class.class;

}
