package cn.pinming.autoconfigure;

import cn.pinming.interceptor.Interceptor;
import cn.pinming.interceptor.InterceptorChain;
import cn.pinming.interfaces.ProxyCreator;
import cn.pinming.proxy.JDKProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 13:48
 */
@Configuration
@EnableConfigurationProperties(PmWebClientProperties.class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class PmWebClientAutoConfiguration{

    // TODO: 2020/10/22 后期可有其他 creator 的时候可以做成按条件初始化
    @Bean
    public ProxyCreator jdkProxyCreator(PmWebClientProperties properties, DefaultListableBeanFactory beanFactory, InterceptorChain interceptorChain) {
        return new JDKProxyCreator(properties, beanFactory, interceptorChain);
    }

    @Bean
    public InterceptorChain interceptorChain(@Autowired(required = false) List<Interceptor> interceptors){
        return new InterceptorChain(interceptors);
    }
}
