package cn.pinming.util;

import cn.pinming.annotation.ApiServer;
import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 10:16
 */
public class MetaInfoUtil {

    /**
     * 提取服务器信息
     *
     * @param type 用户定义的接口类型
     * @return {@link ServerInfo}
     */
    public static ServerInfo extractServerInfo(Class<?> type) {
        ServerInfo serverInfo = new ServerInfo();
        ApiServer anno = type.getAnnotation(ApiServer.class);
        serverInfo.setUrl(anno.value());
        return serverInfo;
    }

    /**
     * 根据方法定义和调用参数得到调用的相关信息
     * @param method 接口中的方法
     * @param args 方法的参数
     * @return {@link MethodInfo}
     */
    public static MethodInfo extractMethodInfo(Method method, Object[] args) {
        MethodInfo methodInfo = new MethodInfo();
        MetaInfoUtil.extractUrlAndMethod(method, methodInfo);
        MetaInfoUtil.extractRequestParamAndBody(method, args, methodInfo);
        // 提取返回对象信息
        MetaInfoUtil.extractReturnInfo(method, methodInfo);
        return methodInfo;
    }

    /**
     * 提取返回对象信息
     * @param method 接口中定义的方法
     * @param methodInfo {@link MethodInfo}
     */
    public static void extractReturnInfo(Method method, MethodInfo methodInfo) {
        // 返回flux还是mono
        // isAssignableFrom 判断类型是否某个的子类
        // instanceof 判断实例是否某个的子类
        boolean isFlux = method.getReturnType().isAssignableFrom(Flux.class);
        methodInfo.setReturnFlux(isFlux);
        // 得到返回对象的实际类型
        Class<?> elementType = extractElementType(method.getGenericReturnType());
        methodInfo.setReturnElementType(elementType);
    }

    /**
     * 得到泛型类型的实际类型
     * @param genericReturnType 带泛型的返回类型
     * @return 实际的参数类型
     */
    public static Class<?> extractElementType(Type genericReturnType) {
        Type[] actualTypeArguments = ((ParameterizedType) genericReturnType).getActualTypeArguments();
        return (Class<?>) actualTypeArguments[0];
    }

    /**
     * 得到请求的param和body
     * @param method 接口中定义的方法
     * @param args 方法参数
     * @param methodInfo {@link MethodInfo}
     */
    public static void extractRequestParamAndBody(Method method, Object[] args, MethodInfo methodInfo) {
        // 得到调用的参数和body
        Parameter[] parameters = method.getParameters();

        // 参数和值对应的 map
        Map<String, Object> params = new LinkedHashMap<>();
        methodInfo.setParams(params);

        for (int i = 0; i < parameters.length; i++) {
            // 是否带 @PathVariable
            PathVariable annoPath = parameters[i].getAnnotation(PathVariable.class);
            if (annoPath != null) {
                params.put(annoPath.value(), args[i]);
            }
            // 是否带了 RequestBody
            RequestBody annoBody = parameters[i].getAnnotation(RequestBody.class);
            if (annoBody != null) {
                methodInfo.setBody((Mono<?>) args[i]);
                // 请求对象的实际类型
                methodInfo.setBodyElementType(extractElementType(parameters[i].getParameterizedType()));
            }
        }
    }

    /**
     * 提取请求的 URL 和方法
     * @param method 接口中定义的方法
     * @param methodInfo {@link MethodInfo}
     */
    public static void extractUrlAndMethod(Method method, MethodInfo methodInfo) {
        // 得到请求URL和请求方法
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            // GET
            if (annotation instanceof GetMapping) {
                GetMapping a = (GetMapping) annotation;
                methodInfo.setUrl(a.value()[0]);
                methodInfo.setMethod(HttpMethod.GET);
            }
            // PUT
            else if (annotation instanceof PutMapping) {
                PutMapping a = (PutMapping) annotation;
                methodInfo.setUrl(a.value()[0]);
                methodInfo.setMethod(HttpMethod.PUT);
            }
            // POST
            else if (annotation instanceof PostMapping) {
                PostMapping a = (PostMapping) annotation;
                String[] produces = a.produces();
                if (produces.length > 0){
                    methodInfo.setReqeustContentType(MediaType.parseMediaType(produces[0]));
                }
                methodInfo.setUrl(a.value()[0]);
                methodInfo.setMethod(HttpMethod.POST);
            }
            // DELETE
            else if (annotation instanceof DeleteMapping) {
                DeleteMapping a = (DeleteMapping) annotation;
                methodInfo.setUrl(a.value()[0]);
                methodInfo.setMethod(HttpMethod.DELETE);
            }
        }
    }
}
