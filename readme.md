## pinming web client

封装响应式网络请求客户端。

### 使用方式:

### 1. 加入依赖

```xml
<dependency>
    <groupId>cn.pinming</groupId>
    <artifactId>pm-web-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 启用 pm-web-client
在配置类上加上 `@EnablePmWebClient` 注解，并指定要扫描客户端的包:
```java
@SpringBootApplication
@EnablePmWebClient("cn.pinming")
public class WebClientTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebClientTestApplication.class, args);
    }
}
```

### 3. 根据具体请求情况定义相应的接口并加上相应的注解, 以下是目前提供支持的所有操作方式:
```java
/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/27 18:18
 */
@ApiServer("${user.api.baseUrl}")
public interface IUserApi {

	@GetMapping("/")
	Flux<User> getAllUser();

	@GetMapping("/{id}")
	Mono<User> getUserById(@PathVariable("id") String id);

	@DeleteMapping("/{id}")
	Mono<Void> deleteUserById(@PathVariable("id") String id);

	@PostMapping("/")
	Mono<User> createUser(@RequestBody Mono<User> user);

	@PostMapping("/form")
	Mono<String> createUserByForm(@RequestForm Mono<MultiValueMap<String, ?>> param, @RequestHeader("accessToken") String token, @RequestHeader Map<String,String> headers);

	@PostMapping("/")
	Mono<String> createUserByRawBody(@PlainRequestBody Mono<String> body);
}
```

#### 4. 如果需要对请求做拦截只需要定义实现 `cn.pinming.interceptor.Interceptor` 的 spring bean 即可, 支持定义多个;
样例：
```java
/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/26 18:17
 */
@Component
@Slf4j
public class TestInterceptor implements Interceptor {
    @Override
    public boolean applyPre(ClientRequest clientRequest) {
        int i = ThreadLocalRandom.current().nextInt();
        if (i % 2 == 0){
            return true;
        }
        return false;
    }

    @Override
    public void applyPost(ClientRequest clientRequest, ClientResponse clientResponse, RequestInfo requestInfo) {
        log.info(requestInfo.toString());
    }

    @Override
    public void applyError(ClientRequest clientRequest, ClientResponse clientResponse, RequestInfo requestInfo) {
        log.info(requestInfo.toString());
    }
}
```

#### 5. 配置项请参考 `cn.pinming.autoconfigure.PmWebClientProperties`
如下:
```java
/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/22 13:54
 */
@ConfigurationProperties("pm.webclient")
@Data
public class PmWebClientProperties {
    /**
     * the maximum number of connections before starting pending
     */
    private int maxConnections = 100;
    /**
     * the maximum time in millis to wait for acquiring
     */
    private long acquireTimeout = 3000;
    /**
     * the event loop thread name prefix
     */
    private String eventLoopThreadPrefix = "pm-event-loop-";
    /**
     * number of selector threads
     */
    private int selectCount = 1;
    /**
     * number of worker threads
     */
    private int workerCount = 4;
    /**
     * read timeout in seconds
     */
    private int readTimeoutSeconds = 6;
    /**
     * write timeout in seconds
     */
    private int writeTimeoutSeconds = 6;
    /**
     *  connect timeout in seconds
     */
    private int connectTimeoutSeconds = 6;
    /**
     * max in memory size in MegaByte
     */
    private int maxInMemorySizeMegaByte = 10;
}
```