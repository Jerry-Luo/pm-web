package cn.pinming.interceptor;

import cn.pinming.http.handler.RequestInfo;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/26 17:47
 */
public interface Interceptor {

    boolean applyPre(ClientRequest clientRequest);

    void applyPost(ClientRequest clientRequest, ClientResponse clientResponse, RequestInfo requestInfo);

    void applyError(ClientRequest clientRequest, ClientResponse clientResponse, RequestInfo requestInfo);

}
