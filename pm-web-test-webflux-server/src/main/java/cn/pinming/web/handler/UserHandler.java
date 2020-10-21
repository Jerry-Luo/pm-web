package cn.pinming.web.handler;


import cn.pinming.web.domain.User;
import cn.pinming.web.repository.UserRepository;
import cn.pinming.web.util.CheckUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.web.reactive.function.server.ServerResponse.notFound;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 13:36
 */
@Component
public class UserHandler {

	private final UserRepository repository;

	public UserHandler(UserRepository rep) {
		this.repository = rep;
	}

	public Mono<ServerResponse> getAllUser(ServerRequest request) {
		return ok().contentType(APPLICATION_JSON_UTF8)
				.body(this.repository.findAll(), User.class);
	}

	public Mono<ServerResponse> getByUserId(ServerRequest request) {
		String id = request.pathVariable("id");
		return ok().contentType(APPLICATION_JSON_UTF8)
				.body(this.repository.findById(id), User.class);
	}

	public Mono<ServerResponse> createUser(ServerRequest request) {
		// 2.0.0 是可以工作, 但是2.0.1 下面这个模式是会报异常
		Mono<User> user = request.bodyToMono(User.class);

		return user.flatMap(u -> {
			// 校验代码需要放在这里
			CheckUtil.checkName(u.getName());
			
			return ok().contentType(APPLICATION_JSON_UTF8)
					.body(this.repository.save(u), User.class);
		});
	}

	public Mono<ServerResponse> deleteUserById(ServerRequest request) {
		String id = request.pathVariable("id");

		return this.repository.findById(id)
				.flatMap(
						user -> this.repository.delete(user).then(ok().build()))
				.switchIfEmpty(notFound().build());
	}

}
