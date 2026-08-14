package com.mica.voice.example.web.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 流式 ASR WebSocket 配置：注册 {@link OnlineAsrWebSocketHandler}。
 *
 * @author dreamlu
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@RequiredArgsConstructor
public class OnlineAsrWebSocketConfig implements WebSocketConfigurer {

	private final OnlineAsrWebSocketHandler onlineAsrHandler;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(onlineAsrHandler, "/mica/voice/ws/online-asr")
			.setAllowedOriginPatterns("*");
	}
}
