package cn.pinming;

import cn.pinming.annotation.ApiServer;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
}
