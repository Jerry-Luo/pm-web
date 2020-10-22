package cn.pinming.client;

import cn.pinming.interfaces.ProxyCreator;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 14:29
 */
public class ClientFactoryBean<T>  implements FactoryBean<T> {

    @Autowired
    private ProxyCreator proxyCreator;

    private Class<T> clientInterface;

    public ClientFactoryBean() {
        //intentionally empty
    }

    public ClientFactoryBean(Class<T> mapperInterface) {
        this.clientInterface = mapperInterface;
    }

    @Override
    public T getObject() throws Exception {
        return (T) proxyCreator.createProxy(this.getObjectType());
    }

    @Override
    public Class<?> getObjectType() {
        return this.clientInterface;
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }

    public Class<T> getClientInterface() {
        return clientInterface;
    }

    public void setClientInterface(Class<T> clientInterface) {
        this.clientInterface = clientInterface;
    }
}
