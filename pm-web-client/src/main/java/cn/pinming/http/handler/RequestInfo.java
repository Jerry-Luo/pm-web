package cn.pinming.http.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/26 14:11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestInfo {
    private String apiName;
    private long responseTime;
    private long timestamp;
}
