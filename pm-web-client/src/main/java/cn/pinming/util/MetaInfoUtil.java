package cn.pinming.util;

import cn.pinming.annotation.ApiServer;
import cn.pinming.annotation.PlainRequestBody;
import cn.pinming.annotation.RequestForm;
import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public static ServerInfo extractServerInfo(Class<?> type, DefaultListableBeanFactory beanFactory) {
        ServerInfo serverInfo = new ServerInfo();
        ApiServer anno = type.getAnnotation(ApiServer.class);
        serverInfo.setUrl(beanFactory.resolveEmbeddedValue(anno.value()));
        serverInfo.setClientInterfaceName(type.getSimpleName());
        return serverInfo;
    }

    /**
     * 根据方法定义和调用参数得到调用的相关信息
     *
     * @param method 接口中的方法
     * @param args   方法的参数
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
     *
     * @param method     接口中定义的方法
     * @param methodInfo {@link MethodInfo}
     */
    public static void extractReturnInfo(Method method, MethodInfo methodInfo) {
        // 返回flux还是mono
        // isAssignableFrom 判断类型是否某个的子类
        // instanceof 判断实例是否某个的子类
        boolean isFlux = method.getReturnType().isAssignableFrom(Flux.class);
        methodInfo.setReturnFlux(isFlux);
        // 得到返回对象的实际类型
//        Class<?> elementType = extractElementType(method.getGenericReturnType());
        methodInfo.setReturnElementType(extractElementType(method.getGenericReturnType()));
    }

    /**
     * 得到泛型类型的实际类型
     *
     * @param genericType 带泛型的类型
     * @return 实际的参数类型
     */
    public static ParameterizedTypeReference<?> extractElementType(Type genericType) {

        // 非泛型
        if (genericType instanceof Class) {
            return ParameterizedTypeReference.forType(genericType);
        } else if (genericType instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
//        return (Class<?>) actualTypeArguments[0];
            return ParameterizedTypeReference.forType(actualTypeArguments[0]);
        }

        throw new ClassCastException("未知的参数" + genericType);
    }

    /**
     * 得到请求的param和body
     *
     * @param method     接口中定义的方法
     * @param args       方法参数
     * @param methodInfo {@link MethodInfo}
     */
    @SuppressWarnings("unchecked")
    public static void extractRequestParamAndBody(Method method, Object[] args, MethodInfo methodInfo) {
        // 得到调用的参数和body
        Parameter[] parameters = method.getParameters();
        // 参数和值对应的 map
        Map<String, Object> params = new LinkedHashMap<>();
        methodInfo.setParams(params);

        for (int i = 0; i < parameters.length; i++) {
            // RequestHeader 的支持
            RequestHeader annoHeader = parameters[i].getAnnotation(RequestHeader.class);
            if (Objects.nonNull(annoHeader)) {
                if (Objects.isNull(methodInfo.getRequestHeaders())) {
                    methodInfo.setRequestHeaders(new LinkedHashMap<>());
                }
                if (args[i] instanceof Map) {
                    ((Map<String, String>) args[i]).forEach((k, v) -> {
                        methodInfo.getRequestHeaders().put(k, v);
                    });
                } else if (args[i] instanceof String) {
                    methodInfo.getRequestHeaders().put(annoHeader.value(), (String) args[i]);
                }
            }

            // 是否带 @PathVariable
            PathVariable annoPath = parameters[i].getAnnotation(PathVariable.class);
            if (Objects.nonNull(annoPath)) {
                params.put(annoPath.value(), args[i]);
            }
            // 是否带了 RequestBody
            RequestBody annoBody = parameters[i].getAnnotation(RequestBody.class);
            if (Objects.nonNull(annoBody)) {
                methodInfo.setBody((Mono<?>) args[i]);
                // 请求对象的实际类型
                methodInfo.setBodyElementType(extractElementType(parameters[i].getParameterizedType()));
                methodInfo.setReqeustContentType(MediaType.APPLICATION_JSON_VALUE);
            }
            RequestForm annoForm = parameters[i].getAnnotation(RequestForm.class);
            if (Objects.nonNull(annoForm)) {
                //MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                //Mono<Map<String, String>> p = (Mono<Map<String, String>>)args[i];
                //p.subscribe(m -> m.forEach(formData::add));
                //methodInfo.setFormData(Mono.just(formData));
                methodInfo.setFormData((Mono<MultiValueMap<String, ?>>) args[i]);
                methodInfo.setReqeustContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            }
            PlainRequestBody annoPlainBody = parameters[i].getAnnotation(PlainRequestBody.class);
            if (Objects.nonNull(annoPlainBody)) {
                methodInfo.setBody((Mono<?>) args[i]);
                methodInfo.setBodyElementType(extractElementType(parameters[i].getParameterizedType()));
                methodInfo.setReqeustContentType(annoPlainBody.contentType());
                methodInfo.setRawRequest(true);
            }
        }
    }

    /**
     * 提取请求的 URL 和方法
     *
     * @param method     接口中定义的方法
     * @param methodInfo {@link MethodInfo}
     */
    public static void extractUrlAndMethod(Method method, MethodInfo methodInfo) {
        // 得到请求URL和请求方法
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            // GET
            if (annotation instanceof GetMapping) {
                GetMapping a = (GetMapping) annotation;
                extractRequestContentType(methodInfo, a);
                methodInfo.setUrl(a.value()[0]);
                methodInfo.setMethod(HttpMethod.GET);
            }
            // PUT
            else if (annotation instanceof PutMapping) {
                PutMapping a = (PutMapping) annotation;
                extractRequestContentType(methodInfo, a);
                methodInfo.setUrl(a.value()[0]);
                methodInfo.setMethod(HttpMethod.PUT);
            }
            // POST
            else if (annotation instanceof PostMapping) {
                PostMapping a = (PostMapping) annotation;
                extractRequestContentType(methodInfo, a);
                methodInfo.setUrl(a.value()[0]);
                methodInfo.setMethod(HttpMethod.POST);
            }
            // DELETE
            else if (annotation instanceof DeleteMapping) {
                DeleteMapping a = (DeleteMapping) annotation;
                extractRequestContentType(methodInfo, a);
                methodInfo.setUrl(a.value()[0]);
                methodInfo.setMethod(HttpMethod.DELETE);
            }
        }
    }

    private static void extractRequestContentType(MethodInfo methodInfo, Object a) {
        String[] produces = null;
        if (a instanceof GetMapping) {
            GetMapping m = (GetMapping) a;
            produces = m.produces();
        }
        if (a instanceof PutMapping) {
            PutMapping m = (PutMapping) a;
            produces = m.produces();
        }
        if (a instanceof PostMapping) {
            PostMapping m = (PostMapping) a;
            produces = m.produces();
        }
        if (a instanceof DeleteMapping) {
            DeleteMapping m = (DeleteMapping) a;
            produces = m.produces();
        }
        if (Objects.nonNull(produces) && produces.length > 0) {
            methodInfo.setReqeustContentType(produces[0]);
        }
    }
}
