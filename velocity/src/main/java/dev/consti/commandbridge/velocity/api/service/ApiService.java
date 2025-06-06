package dev.consti.commandbridge.velocity.api.service;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.consti.commandbridge.velocity.api.security.SecurityConfig;
import dev.consti.foundationlib.utils.ConfigManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiService {

    private final Gson gson;
    private final SecurityConfig securityConfig;
    private final String adminUsername;
    private final String adminPasswordHash;
    private final ProxyServer proxyServer;
    private final ConfigManager config;

    public ApiService(ConfigManager config, ProxyServer proxyServer) {

        this.config = config;

        this.gson = new Gson();
        this.securityConfig = new SecurityConfig(jwtSecret, maxRequestsPerMinute);
        this.adminUsername = adminUsername;
        this.adminPasswordHash = securityConfig.hashPassword(adminPassword);
        this.proxyServer = proxyServer;
    }

    public void handleApiRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        HttpMethod method = request.method();
        String body = request.content().toString(CharsetUtil.UTF_8);

        HttpResponseStatus status = HttpResponseStatus.OK;
        String json = "{}";

        try {
            if (method.equals(HttpMethod.OPTIONS)) {
                json = "OK";

            } else if (uri.equals("/api/auth/login") && method.equals(HttpMethod.POST)) {
                Map<String, String> data = gson.fromJson(body, Map.class);
                String username = data.get("username");
                String password = data.get("password");

                if (username == null || password == null) {
                    status = HttpResponseStatus.BAD_REQUEST;
                    json = gson.toJson(Map.of("error", "Username and password are required"));
                } else if (username.equals(adminUsername) && securityConfig.checkPassword(password, adminPasswordHash)) {
                    String token = securityConfig.generateToken(username);
                    json = gson.toJson(Map.of("token", token));
                } else {
                    status = HttpResponseStatus.UNAUTHORIZED;
                    json = gson.toJson(Map.of("error", "Invalid credentials"));
                }

            } else if (uri.equals("/api/servers") && method.equals(HttpMethod.GET)) {
                List<Map<String, Object>> servers = proxyServer.getAllServers().stream()
                        .map(ServerMapper::toMap)
                        .collect(Collectors.toList());

                int totalPlayers = servers.stream()
                        .mapToInt(server -> (int) server.get("playerCount"))
                        .sum();

                Map<String, Object> result = baseResponseMetadata(false, 200);
                result.put("serverCount", servers.size());
                result.put("playerTotal", totalPlayers);
                result.put("data", servers);

                json = gson.toJson(result);

            } else if (uri.equals("/api/command") && method.equals(HttpMethod.POST)) {
                Map<String, String> data = gson.fromJson(body, Map.class);
                String command = data.get("command");

                if (command == null || command.isEmpty()) {
                    status = HttpResponseStatus.BAD_REQUEST;
                    json = gson.toJson(Map.of("error", "Command is required"));
                } else {
                    proxyServer.getCommandManager().executeAsync(proxyServer.getConsoleCommandSource(), command);
                    json = gson.toJson(Map.of("message", "Command executed successfully"));
                }

            } else {
                status = HttpResponseStatus.NOT_FOUND;
                json = gson.toJson(Map.of("error", "Endpoint not found"));
            }

        } catch (Exception e) {
            status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
            json = gson.toJson(Map.of("error", "Internal server error", "details", e.getMessage()));
        }

        sendJsonResponse(ctx, request, status, json);
    }

    private void sendJsonResponse(ChannelHandlerContext ctx, FullHttpRequest request,
                                  HttpResponseStatus status, String json) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(json, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set("Access-Control-Allow-Origin", "*");
        response.headers().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.headers().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private Map<String, Object> baseResponseMetadata(boolean error, int statusCode) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("error", error);
        metadata.put("status", statusCode);
        metadata.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS")));
        return metadata;
    }
}
