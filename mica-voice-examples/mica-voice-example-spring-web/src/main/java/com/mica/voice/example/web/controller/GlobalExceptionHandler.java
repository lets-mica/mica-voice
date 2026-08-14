package com.mica.voice.example.web.controller;

import com.mica.voice.example.web.Application;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.exception.AudioFormatException;
import net.dreamlu.mica.voice.exception.EngineException;
import net.dreamlu.mica.voice.exception.MicaVoiceException;
import net.dreamlu.mica.voice.exception.ModelNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * mica-voice 全局异常处理器。
 *
 * <p>把 mica-voice 的异常族映射成结构化的 HTTP 响应：
 * <ul>
 *     <li>{@link ModelNotFoundException} → 404 NOT_FOUND</li>
 *     <li>{@link AudioFormatException} → 415 UNSUPPORTED_MEDIA_TYPE</li>
 *     <li>{@link EngineException} → 500 INTERNAL_SERVER_ERROR</li>
 *     <li>其他 {@link MicaVoiceException} → 400 BAD_REQUEST</li>
 * </ul>
 *
 * @author dreamlu
 */
@Slf4j
@RestControllerAdvice(basePackageClasses = Application.class)
public class GlobalExceptionHandler {

	private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, Exception e) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("code", code);
		body.put("message", e.getMessage());
		body.put("status", status.value());
		body.put("timestamp", System.currentTimeMillis());
		if (e instanceof ModelNotFoundException) {
			body.put("modelDirName", ((ModelNotFoundException) e).getModelDirName());
			body.put("candidates", ((ModelNotFoundException) e).getCandidates());
		}
		return ResponseEntity.status(status).body(body);
	}

	@ExceptionHandler(ModelNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleModelNotFound(ModelNotFoundException e) {
		log.warn("模型未找到: {}", e.getMessage());
		return body(HttpStatus.NOT_FOUND, "MODEL_NOT_FOUND", e);
	}

	@ExceptionHandler(AudioFormatException.class)
	public ResponseEntity<Map<String, Object>> handleAudioFormat(AudioFormatException e) {
		log.warn("音频格式错误: {}", e.getMessage());
		return body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AUDIO_FORMAT_INVALID", e);
	}

	@ExceptionHandler(EngineException.class)
	public ResponseEntity<Map<String, Object>> handleEngine(EngineException e) {
		log.error("引擎错误: {}", e.getMessage(), e);
		return body(HttpStatus.INTERNAL_SERVER_ERROR, "ENGINE_ERROR", e);
	}

	@ExceptionHandler(MicaVoiceException.class)
	public ResponseEntity<Map<String, Object>> handleMicaVoice(MicaVoiceException e) {
		log.warn("mica-voice 业务异常: {}", e.getMessage());
		return body(HttpStatus.BAD_REQUEST, "MICA_VOICE_ERROR", e);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException e) {
		log.warn("非法参数: {}", e.getMessage());
		return body(HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT", e);
	}
}
