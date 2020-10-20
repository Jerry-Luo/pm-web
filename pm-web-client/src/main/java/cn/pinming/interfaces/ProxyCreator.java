package cn.pinming.interfaces;
/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 10:31
 */
public interface ProxyCreator {
	/**
	 * 创建代理类
	 * 
	 * @param type
	 * @return
	 */
	Object createProxy(Class<?> type);
}
