package cn.pinming;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class TestController {


	// 直接注入定义的接口
	@Autowired
	IUserApi userApi;

	@GetMapping("/")
	public void test() {
		// 测试信息提取
		// 不订阅, 不会实际发出请求, 但会进入我们的代理类
		// userApi.getAllUser();
		// userApi.getUserById("11111111");
		// userApi.deleteUserById("222222222");
		// userApi.createUser(
		// Mono.just(User.builder().name("xfq").age(33).build()));

		// 直接调用调用 实现调用rest接口的效果
		// Flux<User> users = userApi.getAllUser();
		// users.subscribe(System.out::println);

		String id = "5f913f5f10eb33353c390595";
		userApi.getUserById(id).subscribe(user -> {
			log.info("找到用户:" + user);
		}, e -> {
			log.error("找不到用户:", e);
		});

		id = "5ad1b77560a0791f046e425c";
		userApi.getUserById(id).subscribe(user -> {
			log.info("找到用户:" + user);
		}, e -> {
			log.error("找不到用户:", e);
		});
		//
		// userApi.deleteUserById(id).subscribe();

		// 创建用户
		// userApi.createUser(
		// Mono.just(User.builder().name("Jerry").age(18).build()))
		// .subscribe(System.out::println);

	}

	@PostMapping("/form")
	public void testForm(){
		MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
		param.put("name", Collections.singletonList("name-from-test-form"));
		param.put("age", Collections.singletonList("100"));
		Map<String, String> headers = new HashMap<>();
		headers.put("testHeader1", "testHeaderValue1");
		headers.put("testHeader2", "testHeaderValue2");
		headers.put("testHeader3", "testHeaderValue3");
		Mono<String> userByForm = userApi.createUserByForm(Mono.just(param), "hello access token", headers);
		userByForm.subscribe(r->{
			log.info("调用 form 返回结果 : " + r);
		}, e -> {
			log.error("请求异常:", e);
		});

		log.info("传个文件试试");
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
   		builder.part("fieldPart", "fieldValue");
   		builder.part("filePart", new FileSystemResource("C:/Users/pms/Desktop/images/n_v28fd94202c8eb436f87dd6c2e67c8ce73.jpg"));
		Mono<String> userByForm1 = userApi.createUserByForm(Mono.just(builder.build()), "hello access token", headers);
		userByForm1.subscribe(r->{
			log.info("调用 multipart 返回结果 : " + r);
		}, e -> {
			log.error("请求异常:", e);
		});

	}

	@PostMapping("/rawbody")
	public void testRawBody() throws JsonProcessingException {
		Map<String, String> param = new HashMap<>();
		param.put("name", "name-from-test-form");
		param.put("age", "100");
		Mono<String> userByForm = userApi.createUserByRawBody(Mono.just(new ObjectMapper().writeValueAsString(param)));
		userByForm.subscribe(r->{
			log.info("调用 form 返回结果 : " + r);
		},e -> {
			log.error("请求异常:", e);
		});
	}
}
