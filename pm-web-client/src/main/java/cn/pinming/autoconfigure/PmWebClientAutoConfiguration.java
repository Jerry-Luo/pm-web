package cn.pinming.autoconfigure;

import cn.pinming.interfaces.ProxyCreator;
import cn.pinming.proxy.JDKProxyCreator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 13:48
 */
@Configuration
//@ConditionalOnClass({PlatformTransactionManager.class })
@EnableConfigurationProperties(PmWebClientProperties.class)
//@AutoConfigureAfter({ DataSourceAutoConfiguration.class })
public class PmWebClientAutoConfiguration {

    // TODO: 2020/10/22 后期可有其他 creator 的时候可以做成按条件初始化
    @Bean
    ProxyCreator jdkProxyCreator(PmWebClientProperties properties) {
        return new JDKProxyCreator(properties);
    }
}
