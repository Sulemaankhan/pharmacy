package com.pharmacy.cloudgateway.config;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class NettyClientAbortConfig {
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> quietClientAbort() {
        return factory -> factory.addServerCustomizers(httpServer ->
                httpServer.doOnChannelInit((observer, channel, address) ->
                        channel.pipeline().addLast("client-abort", new ClientAbortHandler())));
    }

    static final class ClientAbortHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (isClientAbort(cause)) {
                ctx.close();
                return;
            }
            ctx.fireExceptionCaught(cause);
        }

        private static boolean isClientAbort(Throwable cause) {
            while (cause != null) {
                if (cause instanceof IOException) {
                    String message = String.valueOf(cause.getMessage()).toLowerCase();
                    if (message.contains("connection was aborted")
                            || message.contains("connection reset")
                            || message.contains("broken pipe")
                            || message.contains("forcibly closed")) {
                        return true;
                    }
                }
                cause = cause.getCause();
            }
            return false;
        }
    }
}
