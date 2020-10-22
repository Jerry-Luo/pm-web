package cn.pinming.rest.handler;


import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;
import cn.pinming.interfaces.RestHandler;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 11:10
 */
public class WebClientRestHandler implements RestHandler {

	private WebClient client;
	private RequestBodySpec request;

	/**
	 * 初始化webclient
	 */
	@Override
	public void init(ServerInfo serverInfo) {
		this.client = WebClient.create(serverInfo.getUrl());
	}

	/**
	 * 处理rest请求
	 */
	@Override
	public Object invokeRest(MethodInfo methodInfo) {
		// 返回结果
		Object result = null;

		request = this.client
				// 请求方法
				.method(methodInfo.getMethod())
				// 请求url 和 参数
				.uri(methodInfo.getUrl(), methodInfo.getParams())
				//
				.accept(MediaType.APPLICATION_JSON);

		ResponseSpec retrieve = null;

		// 判断是否带了 body
		if (methodInfo.getBody() != null) {
			// 发出请求
			retrieve = request.body(methodInfo.getBody(), methodInfo.getBodyElementType()).retrieve();
		} else {
			retrieve = request.retrieve();
		}

		// 处理异常
		retrieve.onStatus(status -> status.value() == 404, response -> Mono.just(new RuntimeException("Not Found")));

		// 处理body
		if (methodInfo.isReturnFlux()) {
			result = retrieve.bodyToFlux(methodInfo.getReturnElementType());
		} else {
			result = retrieve.bodyToMono(methodInfo.getReturnElementType());
		}

		return result;
	}

}
