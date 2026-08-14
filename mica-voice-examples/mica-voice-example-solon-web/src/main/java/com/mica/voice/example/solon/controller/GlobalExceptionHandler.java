/*
 * Copyright (c) 2019-2026, dreamlu.net All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mica.voice.example.solon.controller;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.exception.AudioFormatException;
import net.dreamlu.mica.voice.exception.EngineException;
import net.dreamlu.mica.voice.exception.MicaVoiceException;
import net.dreamlu.mica.voice.exception.ModelNotFoundException;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.exception.StatusException;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * mica-voice 全局异常处理器（Solon Filter）。
 *
 * <p>把 mica-voice 的异常族映射成结构化的 JSON 响应：
 * <ul>
 *     <li>{@link ModelNotFoundException} → 404 NOT_FOUND</li>
 *     <li>{@link AudioFormatException} → 415 UNSUPPORTED_MEDIA_TYPE</li>
 *     <li>{@link EngineException} → 500 INTERNAL_SERVER_ERROR</li>
 *     <li>其他 {@link MicaVoiceException} → 400 BAD_REQUEST</li>
 *     <li>{@link IllegalArgumentException} → 400 BAD_REQUEST</li>
 * </ul>
 *
 * <p>Solon 的标准做法是用 {@link Filter}（或者 RouterInterceptor）做统一异常处理，
 * 它是最外层，可覆盖所有抛出的异常（包括 MVC 之外的）。
 *
 * @author dreamlu
 */
@Slf4j
@Component
public class GlobalExceptionHandler implements Filter {

	private static void writeBody(Context ctx, int status, String code, Throwable e) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("code", code);
		body.put("message", e.getMessage());
		body.put("status", status);
		body.put("timestamp", System.currentTimeMillis());
		if (e instanceof ModelNotFoundException) {
			ModelNotFoundException ex = (ModelNotFoundException) e;
			body.put("modelDirName", ex.getModelDirName());
			body.put("candidates", ex.getCandidates());
		}
		ctx.status(status);
		try {
			ctx.render(body);
		} catch (Throwable renderError) {
			// 渲染失败兜底：直接以 JSON 字符串输出
			ctx.output("{\"code\":\"" + code + "\",\"message\":\""
				+ (e.getMessage() == null ? "" : e.getMessage().replace("\"", "\\\""))
				+ "\",\"status\":" + status + "}");
		}
	}

	@Override
	public void doFilter(Context ctx, FilterChain chain) throws Throwable {
		try {
			chain.doFilter(ctx);
		} catch (ModelNotFoundException e) {
			log.warn("模型未找到: {}", e.getMessage());
			writeBody(ctx, 404, "MODEL_NOT_FOUND", e);
		} catch (AudioFormatException e) {
			log.warn("音频格式错误: {}", e.getMessage());
			writeBody(ctx, 415, "AUDIO_FORMAT_INVALID", e);
		} catch (EngineException e) {
			log.error("引擎错误: {}", e.getMessage(), e);
			writeBody(ctx, 500, "ENGINE_ERROR", e);
		} catch (MicaVoiceException e) {
			log.warn("mica-voice 业务异常: {}", e.getMessage());
			writeBody(ctx, 400, "MICA_VOICE_ERROR", e);
		} catch (IllegalArgumentException e) {
			log.warn("非法参数: {}", e.getMessage());
			writeBody(ctx, 400, "ILLEGAL_ARGUMENT", e);
		} catch (StatusException e) {
			log.warn("Solon 状态异常: status={}, msg={}", e.getCode(), e.getMessage());
			ctx.status(e.getCode());
			ctx.output(e.getMessage());
		}
	}
}