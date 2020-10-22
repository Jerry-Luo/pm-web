package cn.pinming.interfaces;
/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 10:31
 */
public interface ProxyCreator {
	/**
	 * 创建代理类
	 * 
	 * @param type 用户定义的请求接口
	 * @return 生成的网络请求代理
	 */
	Object createProxy(Class<?> type);
}
