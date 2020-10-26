package cn.pinming.http.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LoggingHandler;

import static java.nio.charset.Charset.defaultCharset;

/**
 * @author <a href="mailto:luojianwei@pinming.cn">LuoJianwei</a>
 * @since 2020/10/26 10:30
 */
public class CustomLogger extends LoggingHandler {

    public CustomLogger(Class<?> clazz) {
        super(clazz);
    }

    @Override
    protected String format(ChannelHandlerContext ctx, String event, Object arg) {
        if (arg instanceof ByteBuf) {
            ByteBuf msg = (ByteBuf) arg;
            return msg.toString(defaultCharset());
        }
        return super.format(ctx, event, arg);
    }
}
