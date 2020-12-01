package cn.pinming.bean;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 10:31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MethodInfo {

	/**
	 * 请求url
	 */
	private String url;

	/**
	 * 请求方法
	 */
	private HttpMethod method;

	/**
	 * 请求体类型
	 */
	private String reqeustContentType;

	/**
	 * 请求头
	 */
	private Map<String, String> requestHeaders;

	/**
	 * 请求参数(url)
	 */
	private Map<String, Object> params;

	/**
	 * 请求body
	 */
	private Mono body;
	
	/**
	 * 请求body的类型
	 */
	private ParameterizedTypeReference<?> bodyElementType;
	
	/**
	 * 返回是flux还是mono
	 */
	private boolean returnFlux;
	
	/**
	 * 返回对象的类型
	 */
	private ParameterizedTypeReference<?> returnElementType;

	/**
	 * form 类型请求参数
	 */
	private Mono<MultiValueMap<String, String>> formData;

	/**
	 * 是否直接发送 String 类型的报文内容
	 */
	private boolean rawRequest = false;

}
