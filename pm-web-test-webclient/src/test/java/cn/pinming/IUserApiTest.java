package cn.pinming;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/12/23 19:36
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class IUserApiTest {

    @Resource
    IUserApi userApi;

    @Test
    public void createUser() throws IOException {
        AtomicInteger count = new AtomicInteger();
        IntStream.rangeClosed(1, 1000).forEach(__->{
            userApi.createUser(Mono.just(new User.UserBuilder().age(count.getAndIncrement()).build())).subscribe(r->{
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    System.out.println(objectMapper.writeValueAsString(r));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        });

        System.in.read();
    }
}