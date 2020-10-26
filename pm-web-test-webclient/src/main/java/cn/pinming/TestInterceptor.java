package cn.pinming;

import cn.pinming.http.handler.RequestInfo;
import cn.pinming.interceptor.Interceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/26 18:17
 */
@Component
@Slf4j
public class TestInterceptor implements Interceptor {
    @Override
    public boolean applyPre(ClientRequest clientRequest) {
        int i = ThreadLocalRandom.current().nextInt();
        if (i % 2 == 0){
            return true;
        }
        return false;
    }

    @Override
    public void applyPost(ClientRequest clientRequest, ClientResponse clientResponse, RequestInfo requestInfo) {
        log.info(requestInfo.toString());
    }

    @Override
    public void applyError(ClientRequest clientRequest, ClientResponse clientResponse, RequestInfo requestInfo) {
        log.info(requestInfo.toString());
    }
}
