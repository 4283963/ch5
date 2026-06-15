package com.conference.mic.websocket;

import com.alibaba.fastjson2.JSON;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/ws/tracking")
public class TrackingWebSocket {

    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.put(session.getId(), session);
        log.info("WebSocket 连接建立, sessionId={}, 当前在线: {}", session.getId(), SESSIONS.size());
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session.getId());
        log.info("WebSocket 连接关闭, sessionId={}, 当前在线: {}", session.getId(), SESSIONS.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 错误, sessionId={}, error={}", session.getId(), error.getMessage());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("收到客户端消息: {}", message);
    }

    public static void broadcast(Object message) {
        String json = JSON.toJSONString(message);
        for (Session session : SESSIONS.values()) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    log.error("WebSocket 广播失败: {}", e.getMessage());
                }
            }
        }
        log.debug("WebSocket 广播完成, 接收端: {}, 消息: {}", SESSIONS.size(), json);
    }
}
