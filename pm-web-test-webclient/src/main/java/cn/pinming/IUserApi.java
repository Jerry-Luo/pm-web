package cn.pinming;

import cn.pinming.annotation.ApiServer;
import cn.pinming.annotation.PlainRequestBody;
import cn.pinming.annotation.RequestForm;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
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
	Mono<String> createUserByForm(@RequestForm Mono<Map<String, String>> param, @RequestHeader("accessToken") String token, @RequestHeader Map<String,String> headers);

	@PostMapping("/")
	Mono<String> createUserByRawBody(@PlainRequestBody Mono<String> body);
}
