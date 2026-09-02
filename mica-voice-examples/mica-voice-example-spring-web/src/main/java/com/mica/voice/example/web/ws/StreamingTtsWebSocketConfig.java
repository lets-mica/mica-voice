package com.mica.voice.example.web.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 流式 TTS WebSocket 配置：注册 {@link StreamingTtsWebSocketHandler}。
 *
 * <p>端点：{@code /mica/voice/ws/streaming-tts}
 *
 * @author dreamlu
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@RequiredArgsConstructor
public class StreamingTtsWebSocketConfig implements WebSocketConfigurer {

	private final StreamingTtsWebSocketHandler streamingTtsHandler;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(streamingTtsHandler, "/mica/voice/ws/streaming-tts")
			.setAllowedOriginPatterns("*");
	}
}
