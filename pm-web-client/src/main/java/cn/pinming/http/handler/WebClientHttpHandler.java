package cn.pinming.http.handler;


import cn.pinming.autoconfigure.PmWebClientProperties;
import cn.pinming.bean.MethodInfo;
import cn.pinming.bean.ServerInfo;
import cn.pinming.exception.PmWebClientException;
import cn.pinming.interceptor.InterceptorChain;
import cn.pinming.interfaces.HttpHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
import reactor.netty.channel.BootstrapHandlers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 11:10
 */
@Slf4j
public class WebClientHttpHandler implements HttpHandler {

    private static final CustomLogger customLogger = new CustomLogger(HttpClient.class);
    private WebClient client;

    private static final String PM_WEBCLIENT_START_TIME = "PM_WEBCLIENT_START_TIME";

    /**
     * 初始化webclient
     */
    @Override
    public void init(ServerInfo serverInfo, PmWebClientProperties properties, InterceptorChain interceptorChain) {
        //By default, HttpClient participates in the global Reactor Netty resources held in
        //reactor.netty.http.HttpResources, including event loop threads and a connection pool. This is the
        //recommended mode, since fixed, shared resources are preferred for event loop concurrency. In this
        //mode global resources remain active until the process exits.

        //ReactorResourceFactory factory = new ReactorResourceFactory();
        //factory.setUseGlobalResources(false);

        //配置动态连接池
        //ConnectionProvider provider = ConnectionProvider.elastic("elastic pool");
        //配置固定大小连接池，如最大连接数、连接获取超时、空闲连接死亡时间等
        ConnectionProvider provider = ConnectionProvider.fixed("fixed", properties.getMaxConnections(), properties.getAcquireTimeout());
        //指定 Netty 的 select 和 work线程数量
        LoopResources loop = LoopResources.create(properties.getEventLoopThreadPrefix() + serverInfo.getClientInterfaceName(),
                properties.getSelectCount(), properties.getWorkerCount(), true);

        HttpClient httpClient = HttpClient.create(provider).tcpConfiguration(tcpClient -> tcpClient
                // bootstrap这里不能这么改，会导致每个请求都创建一个连接池, 现在并不需要这个自定义的 logger 所以暂时去掉；
                // 具体参考 reactor.netty.resources.PooledConnectionProvider#acquire(Bootstrap b)  145 行

                // 每一个TCP创建会配置 TcpClientBootstrap#configure
                .bootstrap(bootstrap -> BootstrapHandlers.updateLogSupport(bootstrap, customLogger))
                .doOnConnected(connection -> {
                    //读写超时设置
                    connection.addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeoutSeconds()))
                            .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeoutSeconds()));
                })
                //连接超时设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutSeconds() * 1000)
                .option(ChannelOption.TCP_NODELAY, true)
                .runOn(loop));
        // Having enabled the wiretap, each request and response will be logged in full detail.
        //.wiretap(true);

        this.client = WebClient.builder()
                .baseUrl(serverInfo.getUrl())
                //.filter(logRequest())
                //.filter(logResponse())
                .filter((clientRequest, exchangeFunction) -> {
                    if (!interceptorChain.applyPre(clientRequest)) {
                        // TODO: 2020/10/26 换个编码
                        log.warn("请求被拦截，本次请求作废 {}", clientRequest.url().toString());
                        //return Mono.just(ClientResponse.create(HttpStatus.resolve(500)).build());
                        throw new PmWebClientException(String.format("请求被拦截，本次请求作废 %s", clientRequest.url().toString()));
                    }
                    return exchangeFunction.exchange(clientRequest).doOnEach((signal) -> {
                        Instant start = signal.getContext().get(PM_WEBCLIENT_START_TIME);
                        ClientResponse clientResponse = signal.get();
                        Throwable throwable = signal.getThrowable();
                        if (signal.isOnComplete()) {
                            final long cost = Duration.between(start, Instant.now()).toMillis();
                            log.info("signal.isOnComplete {} request time: [{}], resp:{}", clientRequest.logPrefix(), cost, clientResponse);
                            interceptorChain.applyPost(clientRequest, clientResponse, new RequestInfo(clientRequest.url().toString(), cost, start.toEpochMilli()));
                        }
                        if (signal.isOnError()) {
                            final long cost = Duration.between(start, Instant.now()).toMillis();
                            log.error("signal.isOnError {} request time: [{}]", clientRequest.logPrefix(), cost, throwable);
                            interceptorChain.applyError(clientRequest, clientResponse, new RequestInfo(clientRequest.url().toString(), cost, start.toEpochMilli()));
                        }
                    }).subscriberContext((context) -> context.put(PM_WEBCLIENT_START_TIME, Instant.now()));
                })
                // Spring WebFlux configures limits for buffering data in-memory in codec to avoid application
                // memory issues. By the default this is configured to 256KB and if that’s not enough for your use case,
                // you’ll see the following: org.springframework.core.io.buffer.DataBufferLimitException: Exceeded limit on max
                // bytes to buffer
                .codecs(codecs -> {
                            ClientCodecConfigurer.ClientDefaultCodecs defaultCodecs = codecs.defaultCodecs();
                            defaultCodecs.maxInMemorySize(properties.getMaxInMemorySizeMegaByte() * 1024 * 1024);
                            // fix 'Content type 'application/octet-stream' not supported for bodyType= [XXX]'
                            ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
                            List<MimeType> mimeTypes = Arrays.asList(
                                    new MimeType("application", "json", StandardCharsets.UTF_8),
                                    new MimeType("text", "html", StandardCharsets.UTF_8),
                                    new MimeType("text", "plain", StandardCharsets.UTF_8),
                                    new MimeType("application", "*+json", StandardCharsets.UTF_8),
                                    new MimeType("application", "octet-stream", StandardCharsets.UTF_8)
                            );
                            Jackson2JsonDecoder jackson2JsonDecoder = new Jackson2JsonDecoder(objectMapper, (MimeType[]) mimeTypes.toArray());
                            defaultCodecs.jackson2JsonDecoder(jackson2JsonDecoder);
                        }
                )
                //.filter()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 处理rest请求
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object invokeRest(MethodInfo methodInfo) {
        Object result;
        RequestBodySpec request = this.client
                .method(methodInfo.getMethod())
                .uri(methodInfo.getUrl(), methodInfo.getParams())
                .contentType(Objects.isNull(methodInfo.getReqeustContentType()) ? null : MediaType.parseMediaType(methodInfo.getReqeustContentType()))
//				.contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (Objects.isNull(methodInfo.getRequestHeaders())) {
                        return;
                    }
                    methodInfo.getRequestHeaders().forEach(headers::add);
                });
        ResponseSpec retrieve;
        if (methodInfo.getBody() != null) {
            // 发出请求
            retrieve = request.body(methodInfo.getBody(), methodInfo.getBodyElementType()).retrieve();
        } else {
            retrieve = request.retrieve();
        }
        // 处理异常
        retrieve.onStatus(status -> !status.is2xxSuccessful(), response -> {
            response.bodyToMono(String.class).subscribe(r -> log.info("请求出错, status:{}, body:{}", response.statusCode(), r));
            return Mono.just(new PmWebClientException(response.statusCode().toString()));
        });
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

        Object result;
        RequestBodySpec request = this.client
                .method(methodInfo.getMethod())
                .uri(methodInfo.getUrl(), methodInfo.getParams())
                // 要支持 multipart 这里不指定，让框架自己决定使用什么 content-type
                //.contentType(MediaType.parseMediaType(methodInfo.getReqeustContentType()))
                .accept(MediaType.ALL)
                .headers(headers -> {
                    if (Objects.isNull(methodInfo.getRequestHeaders())) {
                        return;
                    }
                    methodInfo.getRequestHeaders().forEach(headers::add);
                });
        ResponseSpec retrieve;
        // 判断是否带了 body
        if (methodInfo.getFormData() != null) {
            MultiValueMap<String, ?> formData = methodInfo.getFormData().block();
            // 发出请求
            //retrieve = request.body(BodyInserters
            //		.fromPublisher(methodInfo.getFormData(),
            //				new ParameterizedTypeReference<MultiValueMap<String, String>>() {}))
            //		.retrieve();
            retrieve = request.syncBody(formData).retrieve();
        } else {
            retrieve = request.retrieve();
        }
        // 处理异常
        retrieve.onStatus(status -> !status.is2xxSuccessful(), response -> {
            String msg = String.format("请求出错, status:%s, body:%s", response.statusCode(), response.bodyToMono(String.class).block());
            log.info(msg);
            return Mono.just(new PmWebClientException(msg));
        });
        // 处理body
        if (methodInfo.isReturnFlux()) {
            result = retrieve.bodyToFlux(methodInfo.getReturnElementType());
        } else {
            result = retrieve.bodyToMono(methodInfo.getReturnElementType());
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invokePlain(MethodInfo methodInfo) {
        Object result;
        RequestBodySpec request = this.client
                .method(methodInfo.getMethod())
                .uri(methodInfo.getUrl(), methodInfo.getParams())
                .contentType(MediaType.parseMediaType(methodInfo.getReqeustContentType()))
                .accept(MediaType.ALL)
                .headers(headers -> {
                    if (Objects.isNull(methodInfo.getRequestHeaders())) {
                        return;
                    }
                    methodInfo.getRequestHeaders().forEach(headers::add);
                });
        ResponseSpec retrieve;
        if (methodInfo.getBody() != null) {
            // 发出请求
            retrieve = request.body(BodyInserters.fromPublisher(methodInfo.getBody(), methodInfo.getBodyElementType())).retrieve();
        } else {
            retrieve = request.retrieve();
        }
        // 处理异常
        retrieve.onStatus(status -> !status.is2xxSuccessful(), response -> {
            String msg = String.format("请求出错, status:%s, body:%s", response.statusCode(), response.bodyToMono(String.class).block());
            log.info(msg);
            return Mono.just(new PmWebClientException(msg));
        });
        // 处理body
        if (methodInfo.isReturnFlux()) {
            result = retrieve.bodyToFlux(methodInfo.getReturnElementType());
        } else {
            result = retrieve.bodyToMono(methodInfo.getReturnElementType());
        }
        return result;
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            log.info("{} Request: {} {}", clientRequest.logPrefix(), clientRequest.method(), clientRequest.url());
            //clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
            return next.exchange(clientRequest);
        };
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            try {
                Field logPrefix = clientResponse.getClass().getDeclaredField("logPrefix");
                logPrefix.setAccessible(true);
                String identify = (String) logPrefix.get(clientResponse);
                log.info("{} Response: {}", identify, clientResponse);
            } catch (Exception e) {
                log.info("Response: {}", clientResponse);
            }
            return Mono.just(clientResponse);
        });
    }
}
