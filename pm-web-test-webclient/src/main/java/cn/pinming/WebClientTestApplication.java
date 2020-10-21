package cn.pinming;

import cn.pinming.interfaces.ProxyCreator;
import cn.pinming.proxy.JDKProxyCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/21 13:47
 */
@SpringBootApplication
public class WebClientTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebClientTestApplication.class, args);
    }

    @Bean
    ProxyCreator jdkProxyCreator() {
        return new JDKProxyCreator();
    }

}
