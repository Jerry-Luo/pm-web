package cn.pinming.http.handler;


import cn.pinming.autoconfigure.PmWebClientProperties;
import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;
import cn.pinming.interfaces.HttpHandler;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 11:10
 */
@Slf4j
public class WebClientHttpHandler implements HttpHandler {

	private WebClient client;
	private RequestBodySpec request;

	/**
	 * 初始化webclient
	 */
	@Override
	public void init(ServerInfo serverInfo, PmWebClientProperties properties) {
		//By default, HttpClient participates in the global Reactor Netty resources held in
		//reactor.netty.http.HttpResources, including event loop threads and a connection pool. This is the
		//recommended mode, since fixed, shared resources are preferred for event loop concurrency. In this
		//mode global resources remain active until the process exits.

		//ReactorResourceFactory factory = new ReactorResourceFactory();
		//factory.setUseGlobalResources(false);
		// TODO: 2020/10/23 配置移到配置文件
		//配置动态连接池
		//ConnectionProvider provider = ConnectionProvider.elastic("elastic pool");
		//配置固定大小连接池，如最大连接数、连接获取超时、空闲连接死亡时间等
		ConnectionProvider provider = ConnectionProvider.fixed("fixed", 45, 4000);
		HttpClient httpClient = HttpClient.create(provider).tcpConfiguration(tcpClient -> {
			//指定 Netty 的 select 和 work线程数量
			LoopResources loop = LoopResources.create("pm-event-loop", 1, 4, true);
			return tcpClient.doOnConnected(connection -> {
				//读写超时设置
				connection.addHandlerLast(new ReadTimeoutHandler(10))
						.addHandlerLast(new WriteTimeoutHandler(10));
			})
			//连接超时设置
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000)
			.option(ChannelOption.TCP_NODELAY, true)
			.runOn(loop);
		});

		this.client = WebClient.builder()
				.baseUrl(serverInfo.getUrl())
				// Spring WebFlux configures limits for buffering data in-memory in codec to avoid application
				// memory issues. By the default this is configured to 256KB and if that’s not enough for your use case,
				// you’ll see the following: org.springframework.core.io.buffer.DataBufferLimitException: Exceeded limit on max
				// bytes to buffer
				.codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
				//.filter()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
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
		retrieve.onStatus(status -> !status.is2xxSuccessful(), response -> Mono.just(new RuntimeException("Not Found")));

		// 处理body
		if (methodInfo.isReturnFlux()) {
			result = retrieve.bodyToFlux(methodInfo.getReturnElementType());
		} else {
			result = retrieve.bodyToMono(methodInfo.getReturnElementType());
		}

		return result;
	}

	/**
	 * 处理 from 表单请求
	 */
	@Override
	public Object invokeForm(MethodInfo methodInfo) {
		// To send form data, you can provide a MultiValueMap<String, String> as the body. Note that the
		// content is automatically set to application/x-www-form-urlencoded by the FormHttpMessageWriter. The
		// following example shows how to use MultiValueMap<String, String>:
		//
		// MultiValueMap<String, String> formData = ... ;
		// Mono<Void> result = client.post()
		// .uri("/path", id)
		// .bodyValue(formData)
		// .retrieve()
		// .bodyToMono(Void.class);

		// You can also supply form data in-line by using BodyInserters, as the following example shows:
		// Mono<Void> result = client.post()
		//  .uri("/path", id)
		//  .body(fromFormData("k1", "v1").with("k2", "v2"))
		//  .retrieve()
		//  .bodyToMono(Void.class);

		// 返回结果
		Object result = null;

		request = this.client
				.method(methodInfo.getMethod())
				.uri(methodInfo.getUrl(), methodInfo.getParams())
				.accept(MediaType.ALL);

		ResponseSpec retrieve = null;

		// 判断是否带了 body
		if (methodInfo.getFormData() != null) {
			MultiValueMap<String, String> formDate = methodInfo.getFormData().block();
			// 发出请求
			retrieve = request.body(BodyInserters
					.fromPublisher(methodInfo.getFormData(),
							new ParameterizedTypeReference<MultiValueMap<String, String>>() {}))
					.retrieve();
		} else {
			retrieve = request.retrieve();
		}

		// 处理异常
		retrieve.onStatus(status -> !status.is2xxSuccessful(), response -> {
			log.info("请求出错, status:{}, body:{}", response.statusCode(), response.bodyToMono(String.class));
			return Mono.just(new RuntimeException("请求出错"));
		});

		// 处理body
		if (methodInfo.isReturnFlux()) {
			result = retrieve.bodyToFlux(methodInfo.getReturnElementType());
		} else {
			result = retrieve.bodyToMono(methodInfo.getReturnElementType());
		}

		return result;
	}

}