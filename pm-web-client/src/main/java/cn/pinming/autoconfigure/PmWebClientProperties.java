package cn.pinming.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
