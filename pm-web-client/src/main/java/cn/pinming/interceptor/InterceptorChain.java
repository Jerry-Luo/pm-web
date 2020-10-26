package cn.pinming.interceptor;

import cn.pinming.http.handler.RequestInfo;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.List;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/26 17:47
 */
public class InterceptorChain {

    private final List<Interceptor> interceptors;

    public InterceptorChain(List<Interceptor> interceptors){
        this.interceptors = interceptors;
    }

    public boolean applyPre(ClientRequest clientRequest){
        if (CollectionUtils.isEmpty(interceptors)){
            return true;
        }
        for(Interceptor interceptor: interceptors){
            if (!interceptor.applyPre(clientRequest)){
                return false;
            }
        }
        return true;
    }

    public void applyPost(ClientRequest clientRequest, ClientResponse clientResponse, RequestInfo requestInfo){
        if (CollectionUtils.isEmpty(interceptors)){
            return;
        }
        interceptors.forEach(interceptor -> interceptor.applyPost(clientRequest, clientResponse, requestInfo));
    }

    public void applyError(ClientRequest clientRequest, ClientResponse clientResponse, RequestInfo requestInfo){
        if (CollectionUtils.isEmpty(interceptors)){
            return;
        }
        interceptors.forEach(interceptor -> interceptor.applyError(clientRequest, clientResponse, requestInfo));
    }
}
