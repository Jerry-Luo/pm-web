package cn.pinming.proxy;


import cn.pinming.autoconfigure.PmWebClientProperties;
import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;
import cn.pinming.http.handler.WebClientHttpHandler;
import cn.pinming.interceptor.InterceptorChain;
import cn.pinming.interfaces.HttpHandler;
import cn.pinming.interfaces.ProxyCreator;
import cn.pinming.util.MetaInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.MediaType;

import java.lang.reflect.Proxy;

/**
 * 使用jdk动态代理实现代理类
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 10:42
 */
@Slf4j
public class JDKProxyCreator implements ProxyCreator {

	private PmWebClientProperties properties;
	private DefaultListableBeanFactory beanFactory;
	private InterceptorChain interceptorChain;

	public JDKProxyCreator(PmWebClientProperties properties, DefaultListableBeanFactory beanFactory, InterceptorChain interceptorChain){
		this.properties = properties;
		this.beanFactory = beanFactory;
		this.interceptorChain = interceptorChain;
	}

	@Override
	public Object createProxy(Class<?> type) {
		log.info("createProxy:" + type);
		// 根据接口得到API服务器信息
		ServerInfo serverInfo = MetaInfoUtil.extractServerInfo(type, beanFactory);
		log.info("serverInfo:" + serverInfo);
		// 给每一个代理类一个实现
		HttpHandler handler = new WebClientHttpHandler();
		// 初始化服务器信息(初始化webclient)
		handler.init(serverInfo, properties, interceptorChain);
		return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { type },
				(proxy, method, args) -> {
					// 根据方法和参数得到调用信息
					MethodInfo methodInfo = MetaInfoUtil.extractMethodInfo(method, args);
					log.info("methodInfo:" + methodInfo);
					if(MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(methodInfo.getReqeustContentType())){
						return handler.invokeForm(methodInfo);
					}else{
						if (methodInfo.isRawRequest()){
							return handler.invokePlain(methodInfo);
						}else{
							// 调用rest
							return handler.invokeRest(methodInfo);
						}
					}
		});
	}
}
