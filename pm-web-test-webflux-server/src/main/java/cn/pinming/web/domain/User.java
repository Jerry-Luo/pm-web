package cn.pinming.web.domain;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/20 13:26
 */
@Document(collection = "user")
@Data
@Builder
public class User {

	@Id
	private String id;

	@NotBlank
	private String name;

	@Range(min=10, max=100)
	private int age;

}
