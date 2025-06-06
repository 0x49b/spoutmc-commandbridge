package dev.consti.commandbridge.velocity.api.mapper;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.HashMap;
import java.util.Map;

public class ServerMapper {

    public static Map<String, Object> toMap(RegisteredServer server) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", server.getServerInfo().getName());
        map.put("address", server.getServerInfo().getAddress().getHostName()
                + ":" + server.getServerInfo().getAddress().getPort());
        map.put("playerCount", server.getPlayersConnected().size());
        map.put("players", server.getPlayersConnected().stream()
                .map(PlayerMapper::toMap)
                .toList());
        return map;
    }
}
