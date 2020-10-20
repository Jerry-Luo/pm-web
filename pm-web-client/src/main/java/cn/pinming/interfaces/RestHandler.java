package cn.pinming.interfaces;

import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 10:31
 */
public interface RestHandler {

	/**
	 * 初始化服务器信息
	 * 
	 * @param serverInfo {@link ServerInfo}
	 */
	void init(ServerInfo serverInfo);

	/**
	 * 调用rest请求, 返回接口
	 * 
	 * @param methodInfo {@link MethodInfo}
	 * @return
	 */
	Object invokeRest(MethodInfo methodInfo);

}
