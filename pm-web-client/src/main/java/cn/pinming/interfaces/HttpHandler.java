package cn.pinming.interfaces;

import cn.pinming.autoconfigure.PmWebClientProperties;
import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;
import cn.pinming.interceptor.InterceptorChain;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 10:31
 */
public interface HttpHandler {

	/**
	 * 初始化服务器信息
	 * @param serverInfo {@link ServerInfo}
	 */
	void init(ServerInfo serverInfo, PmWebClientProperties properties, InterceptorChain interceptorChain);

	/**
	 * 调用rest请求, 返回接口
	 * @param methodInfo {@link MethodInfo}
	 * @return 调用返回结果
	 */
	Object invokeRest(MethodInfo methodInfo);

	/**
	 * 调用普通的表单类型的 http 请求
	 * @param methodInfo {@link MethodInfo}
	 * @return 调用返回结果
	 */
	Object invokeForm(MethodInfo methodInfo);

	/**
	 * 直接推送原始报文
	 * @param methodInfo {@link MethodInfo}
	 * @return 调用返回结果
	 */
	Object invokePlain(MethodInfo methodInfo);

}
