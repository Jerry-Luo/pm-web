package org.springframework.web.reactive.function.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.assertj.core.util.Maps;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 18:52
 */
public class WebClientTest {
    private static final Logger log = LoggerFactory.getLogger(WebClientTest.class);

    /**
     * 携带cookie
     */
    @Test
    public void testWithCookie(){
        Mono<String> resp = WebClient.create()
                .method(HttpMethod.GET)
                .uri("http://baidu.com")
                .cookie("token","xxxx")
                .cookie("JSESSIONID","XXXX")
                .retrieve()
                .bodyToMono(String.class);
        log.info("result:{}",resp.block());
    }

    /**
     * 携带basic auth
     */
    @Test
    public void testWithBasicAuth(){
        String basicAuth = "Basic "+ Base64.getEncoder().encodeToString("user:pwd".getBytes(StandardCharsets.UTF_8));
        log.info(basicAuth);
        Mono<String> resp = WebClient.create()
                .get()
                .uri("http://baidu.com")
                .header(HttpHeaders.AUTHORIZATION,basicAuth)
                .retrieve()
                .bodyToMono(String.class);
        log.info("result:{}",resp.block());
    }

    /**
     * 设置全局 user-agent
     */
    @Test
    public void testWithHeaderFilter(){

        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                .filter(ExchangeFilterFunctions.basicAuthentication("user","password"))
                .filter((clientRequest, next) -> {
                    log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
                    clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
                    return next.exchange(clientRequest);
                }).build();

        Mono<String> resp = webClient.get()
                .uri("https://baidu.com")
                .retrieve()
                .bodyToMono(String.class);

        log.info("result:{}",resp.block());
    }

    /**
     * get请求
     * 使用placeholder传递参数
     */
    @Test
    public void testUrlPlaceholder(){
        Mono<String> resp = WebClient.create()
                .get()
                //多个参数也可以直接放到map中,参数名与placeholder对应上即可
                .uri("http://www.baidu.com/s?wd={key}&other={another}","北京天气","test") //使用占位符
                .retrieve()
                .bodyToMono(String.class);
        log.info("result:{}",resp.block());
    }

    /**
     * 使用uriBuilder传递参数
     */
    @Test
    public void testUrlBiulder(){
        Mono<String> resp = WebClient.create()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("www.baidu.com")
                        .path("/s")
                        .queryParam("wd", "北京天气")
                        .queryParam("other", "test")
                        .build())
                .retrieve()
                .bodyToMono(String.class);
        log.info("result:{}",resp.block());
    }

    /**
     * post表单
     */
    @Test
    public void testFormParam(){
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("name1","value1");
        formData.add("name2","value2");
        Mono<String> resp = WebClient.create().post()
                .uri("http://www.w3school.com.cn/test/demo_form.asp")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve().bodyToMono(String.class);
        log.info("result:{}",resp.block());
    }

    /**
     * post json
     * 使用bean来post
     */
    static class Book {
        String name;
        String title;
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @Test
    public void testPostJson(){
        Book book = new Book();
        book.setName("name");
        book.setTitle("this is title");
        Mono<String> resp = WebClient.create().post()
                .uri("http://localhost:8080/demo/json")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(Mono.just(book),Book.class)
                .retrieve().bodyToMono(String.class);
        log.info("result:{}",resp.block());
    }

    /**
     * 直接post raw json
     */
    @Test
    public void testPostRawJson(){
        Mono<String> resp = WebClient.create().post()
                .uri("http://localhost:8080/demo/json")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(BodyInserters.fromObject("{\n" +
                        " \"title\" : \"this is title\",\n" +
                        " \"author\" : \"this is author\"\n" +
                        "}"))
                .retrieve().bodyToMono(String.class);
        log.info("result:{}",resp.block());
    }

    /**
     * post二进制--上传文件
     */
    @Test
    public void testUploadFile(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        HttpEntity<ClassPathResource> entity = new HttpEntity<>(new ClassPathResource("parallel.png"), headers);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", entity);
        Mono<String> resp = WebClient.create().post()
                .uri("http://localhost:8080/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(parts))
                .retrieve().bodyToMono(String.class);
        log.info("result:{}",resp.block());
    }

    /**
     * 下载二进制图片
     */
    @Test
    public void testDownloadImage() throws IOException {
        Mono<Resource> resp = WebClient.create().get()
                .uri("http://www.toolip.gr/captcha?complexity=99&size=60&length=9")
                .accept(MediaType.IMAGE_PNG)
                .retrieve().bodyToMono(Resource.class);
        Resource resource = resp.block();
        BufferedImage bufferedImage = ImageIO.read(resource.getInputStream());
        ImageIO.write(bufferedImage, "png", new File("captcha.png"));
    }

    /**
     * 下载二进制文件
     */
    @Test
    public void testDownloadFile() throws IOException {
        Mono<ClientResponse> resp = WebClient.create().get()
                .uri("http://localhost:8080/file/download")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchange();
        ClientResponse response = resp.block();
        String disposition = response.headers().asHttpHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        String fileName = disposition.substring(disposition.indexOf("=")+1);
        Resource resource = response.bodyToMono(Resource.class).block();
        File out = new File(fileName);
        //FileUtils.copyInputStreamToFile(resource.getInputStream(),out);
        log.info(out.getAbsolutePath());
    }

    /**
     * 错误处理
     * 可以使用onStatus根据 status code 进行异常适配
     * 可以使用doOnError异常适配
     * 可以使用onErrorReturn返回默认值
     */
    @Test
    public void testRetrieve4xx(){
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.github.v3+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "Spring 5 WebClient")
                .build();
        WebClient.ResponseSpec responseSpec = webClient.method(HttpMethod.GET)
                .uri("/user/repos?sort={sortField}&direction={sortDirection}",
                        "updated", "desc")
                .retrieve();
        Mono<String> mono = responseSpec
                .onStatus(e -> e.is4xxClientError(),resp -> {
                    log.error("error:{},msg:{}",resp.statusCode().value(),resp.statusCode().getReasonPhrase());
                    return Mono.error(new RuntimeException(resp.statusCode().value() + " : " + resp.statusCode().getReasonPhrase()));
                })
                .bodyToMono(String.class)
                .doOnError(WebClientResponseException.class, err -> {
                    log.info("ERROR status:{},msg:{}",err.getRawStatusCode(),err.getResponseBodyAsString());
                    throw new RuntimeException(err.getMessage());
                })
                .onErrorReturn("fallback");
        String result = mono.block();
        log.info("result:{}",result);
    }

    /**
     * HTTP底层库选择
     */
    @Test
    public void test1(){
        WebClient.builder()
                .clientConnector(new JettyClientHttpConnector())
                .build();
    }

    /**
     * 基础配置
     */
    @Test
    public void test2(){
        WebClient.builder()
                .defaultCookie("kl","kl")
                .defaultUriVariables(Maps.newHashMap("name","kl"))
                .defaultHeader("header","kl")
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add("header1","kl");
                    httpHeaders.add("header2","kl");
                })
                .defaultCookies(cookie ->{
                    cookie.add("cookie1","kl");
                    cookie.add("cookie2","kl");
                })
                .baseUrl("http://www.kailing.pub")
                .build();
    }

    /**
     * 底层依赖Netty库配置
     */
    @Test
    public void test3(){
        //配置动态连接池
        //ConnectionProvider provider = ConnectionProvider.elastic("elastic pool");
        //配置固定大小连接池，如最大连接数、连接获取超时、空闲连接死亡时间等
        ConnectionProvider provider = ConnectionProvider.fixed("fixed", 45, 4000);
        HttpClient httpClient = HttpClient.create(provider)
                .secure(sslContextSpec -> {
                    SslContextBuilder sslContextBuilder = SslContextBuilder.forClient()
                            .trustManager(new File("E://server.truststore"));
                    sslContextSpec.sslContext(sslContextBuilder);
                }).tcpConfiguration(tcpClient -> {
                    //指定Netty的select 和 work线程数量
                    LoopResources loop = LoopResources.create("kl-event-loop", 1, 4, true);
                    return tcpClient.doOnConnected(connection -> {
                        //读写超时设置
                        connection.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(10));
                    })
                    //连接超时设置
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .runOn(loop);
                });
        WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 编解码配置
     */
    @Test
    public void test4(){
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.customCodecs().decoder(new Jackson2JsonDecoder());
                    configurer.customCodecs().encoder(new Jackson2JsonEncoder());
                }).build();

        WebClient.builder()
                .exchangeStrategies(strategies)
                .build();
    }

    /**
     * get请求示例
     */
    @Test
    public void test5(){
        WebClient client = WebClient.create("http://www.kailing.pub");
        Mono<String> result = client.get()
                .uri("/article/index/arcid/{id}.html", 256)
                .acceptCharset(StandardCharsets.UTF_8)
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .bodyToMono(String.class);
        result.subscribe(System.err::println);


        // 携带复杂参数
        //定义query参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "kl");
        params.add("age", "19");
        //定义url参数
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("id", 200);
        String uri = UriComponentsBuilder.fromUriString("/article/index/arcid/{id}.html")
                .queryParams(params)
                .uriVariables(uriVariables)
                .toUriString();

        // 下载文件时，因为不清楚各种格式文件对应的MIME Type，可以设置accept为MediaType.ALL，然后使用Spring的Resource来接收数据即可，如：
        WebClient.create("https://kk-open-public.oss-cn-shanghai.aliyuncs.com/xxx.xlsx")
                .get()
                .accept(MediaType.ALL)
                .retrieve()
                .bodyToMono(Resource.class)
                .subscribe(resource -> {
                    try {
                        File file = new File("E://abcd.xlsx");
                        FileCopyUtils.copy(StreamUtils.copyToByteArray(resource.getInputStream()), file);
                    }catch (IOException ex){}
                });
    }

    /**
     * post请求示例
     */
    @Test
    public void test6(){
        WebClient client = WebClient.create("http://www.kailing.pub");
        BodyInserters.FormInserter<Object> formInserter = BodyInserters
                .fromMultipartData("name", "kl")
                .with("age", 19)
                .with("map", Maps.newHashMap("xx", "xx"))
                .with("file", new File("E://xxx.doc"));

        Mono<String> result = client.post()
                .uri("/article/index/arcid/{id}.html", 256)
                .contentType(MediaType.APPLICATION_JSON)
                .body(formInserter)
                //.bodyValue(ImmutableMap.of("name","kl"))
                .retrieve()
                .bodyToMono(String.class);

        result.subscribe(System.err::println);

        // 同步返回结果
        WebClient client1 =  WebClient.create("http://www.kailing.pub");
        String result1 = client .get()
                .uri("/article/index/arcid/{id}.html", 256)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        System.err.println(result);

        // 但是，如果需要进行多个调用，则更高效地方式是避免单独阻塞每个响应，而是等待组合结果，如：
        WebClient client2 =  WebClient.create("http://www.kailing.pub");
        Mono<String> result1Mono = client2.get()
                .uri("/article/index/arcid/{id}.html", 255)
                .retrieve()
                .bodyToMono(String.class);
        Mono<String> result2Mono = client .get()
                .uri("/article/index/arcid/{id}.html", 254)
                .retrieve()
                .bodyToMono(String.class);
        Map<String,String>  map = Mono.zip(result1Mono, result2Mono, (result01, result02) -> {
            Map<String, String> arrayList = new HashMap<>();
            arrayList.put("result1", result01);
            arrayList.put("result2", result02);
            return arrayList;
        }).block();
        System.err.println(map.toString());
    }

    /**
     * Filter过滤器
     */
    @Test
    public void test7(){
        WebClient.builder()
                .baseUrl("http://www.kailing.pub")
                .filter((request, next) -> {
                    ClientRequest filtered = ClientRequest.from(request)
                            .header("foo", "bar")
                            .build();
                    return next.exchange(filtered);
                })
                .filters(filters ->{
                    filters.add(ExchangeFilterFunctions.basicAuthentication("username","password"));
                    filters.add(ExchangeFilterFunctions.limitResponseSize(800));
                })
                .build().get()
                .uri("/article/index/arcid/{id}.html", 254)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(System.err::println);
    }

    /**
     * websocket支持
     */
    @Test
    public void test8() throws URISyntaxException {
        // WebClient不支持 websocket 请求，请求 websocket 接口时需要使用 WebSocketClient，如：
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI url = new URI("ws://localhost:8080/path");
        client.execute(url, session ->
                session.receive()
                        .doOnNext(System.out::println)
                        .then());
    }
}
