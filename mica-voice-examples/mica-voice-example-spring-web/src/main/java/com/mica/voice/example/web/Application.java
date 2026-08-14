package com.mica.voice.example.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * mica-voice Spring Web 示例入口。
 *
 * <p>启动后默认会按 {@code application.yml} 装配 mica-voice-spring-boot-starter：
 * <ul>
 *     <li>{@code mica.voice.asr.offline.enabled=true} → {@code AsrService} Bean</li>
 *     <li>{@code mica.voice.tts.enabled=true} → {@code TtsService} Bean</li>
 *     <li>{@code mica.voice.speaker.enabled=true} → {@code SpeakerService} Bean</li>
 *     <li>{@code mica.voice.web.enabled=true} → 暴露 {@code /mica/voice/*} REST 端点</li>
 * </ul>
 *
 * @author dreamlu
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
