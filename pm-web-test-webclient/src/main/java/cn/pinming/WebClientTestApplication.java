package cn.pinming;

import cn.pinming.annotation.EnablePmWebClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/21 13:47
 */
@SpringBootApplication
@EnablePmWebClient("cn.pinming")
public class WebClientTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebClientTestApplication.class, args);
    }
}
