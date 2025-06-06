package dev.consti.commandbridge.velocity.api.mapper;

import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerMapper {

    public static Map<String, Object> toMap(Player player) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", player.getUsername());
        map.put("uuid", player.getUniqueId().toString());
        map.put("protocolVersion", player.getProtocolVersion().getProtocol());
        map.put("remoteAddress", player.getRemoteAddress().toString());
        return map;
    }
}
