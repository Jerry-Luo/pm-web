package cn.pinming.web.util;


import cn.pinming.web.exception.CheckException;

import java.util.stream.Stream;

public class CheckUtil {

	private static final String[] INVALID_NAMES = { "admin", "guanliyuan" };

	/**
	 * 校验名字, 不成功抛出校验异常
	 */
	public static void checkName(String value) {
		Stream.of(INVALID_NAMES).filter(name -> name.equalsIgnoreCase(value))
				.findAny().ifPresent(name -> {
					throw new CheckException("name", value);
				});
	}

}
