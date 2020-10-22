package cn.pinming.proxy;


import cn.pinming.autoconfigure.PmWebClientProperties;
import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;
import cn.pinming.interfaces.HttpHandler;
import cn.pinming.interfaces.ProxyCreator;
import cn.pinming.rest.handler.WebClientHttpHandler;
import cn.pinming.util.MetaInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

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

	public JDKProxyCreator(PmWebClientProperties properties, DefaultListableBeanFactory beanFactory){
		this.properties = properties;
		this.beanFactory = beanFactory;
	}

	@Override
	public Object createProxy(Class<?> type) {
		log.info("createProxy:" + type);
		// 根据接口得到API服务器信息
		ServerInfo serverInfo = MetaInfoUtil.extractServerInfo(type, beanFactory);
		log.info("serverInfo:" + serverInfo);
		// 给每一个代理类一个实现
		// TODO: 2020/10/22 让 webClient 参数可配置

		HttpHandler handler = new WebClientHttpHandler();
		// 初始化服务器信息(初始化webclient)
		handler.init(serverInfo);
		return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { type },
				(proxy, method, args) -> {
					// 根据方法和参数得到调用信息
					MethodInfo methodInfo = MetaInfoUtil.extractMethodInfo(method, args);
					log.info("methodInfo:" + methodInfo);
					// 调用rest
					return handler.invokeRest(methodInfo);
		});
	}
}
