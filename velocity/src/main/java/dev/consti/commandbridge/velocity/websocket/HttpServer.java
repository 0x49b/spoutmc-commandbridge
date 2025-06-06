package dev.consti.commandbridge.velocity.websocket;

import dev.consti.commandbridge.velocity.api.service.ApiService;
import dev.consti.foundationlib.logging.Logger;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.NotSslRecordException;

import javax.net.ssl.SSLHandshakeException;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


@Sharable
public class HttpServer extends SimpleChannelInboundHandler<FullHttpRequest> {
    private Logger logger;
    private final ApiService apiService;


    public HttpServer(Logger logger, ApiService apiService) {
        this.logger = logger;
        this.apiService = apiService;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        String uri = msg.uri();
        logger.debug("Incoming HTTP request to: {}", uri);

        if ("/ping".equalsIgnoreCase(uri)) {
            sendTextResponse(ctx, OK, "pong");
        } else if (uri.startsWith("/api")) {
            apiService.handleApiRequest(ctx, msg);
        } else if ("websocket".equalsIgnoreCase(msg.headers().get(HttpHeaderNames.UPGRADE))) {
            ctx.fireChannelRead(msg.retain());
        } else {
            sendTextResponse(ctx, NOT_FOUND, "Not Found");
        }
    }

    private void sendTextResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1,
                status,
                ctx.alloc().buffer().writeBytes(content.getBytes())
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof NotSslRecordException || cause instanceof SSLHandshakeException) {
            if (logger.getDebug()) {
                logger.debug("SSL handshake or protocol error", cause);
            } else {
                logger.warn("Received invalid or unsupported SSL/TLS connection (enable debug for full trace)");
            }
        } else {
            if (logger.getDebug()) {
                logger.error("Unexpected error in HttpServer handler", cause);
            } else {
                logger.error("Unexpected error in HttpServer handler: {}", cause.getMessage());
            }
        }
        ctx.close();
    }

}
