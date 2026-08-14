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

package net.dreamlu.mica.voice.solon;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.asr.OnlineAsrService;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.OnlineAsrConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

import java.util.Locale;

/**
 * ASR 自动装配：离线 + 在线两个独立 Bean。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class)
public class AsrAutoConfiguration {

	private static <E extends Enum<E>> E parseModelType(String raw, Class<E> type, E fallback) {
		if (raw == null || raw.isEmpty()) {
			return fallback;
		}
		try {
			return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			log.warn("无效的模型类型: {}，回退到 {}", raw, fallback);
			return fallback;
		}
	}

	/**
	 * 离线 ASR。
	 *
	 * <p>{@link OnlineAsrService} 也实现了 {@link AsrService}，
	 * 但本 Bean 名称为 {@code micaVoiceOfflineAsrService}（唯一），
	 * 与 {@code micaVoiceOnlineAsrService}（{@link OnlineAsrService} 类型唯一）互不冲突：
	 * 需要 {@link AsrService} 接口注入时按名称注入离线版本即可。
	 */
	@Bean(name = "micaVoiceOfflineAsrService")
	@Condition(onMissingBeanName = "micaVoiceOfflineAsrService",
		onBeanName = "micaVoiceCoreProperties",
		onExpression = "${mica.voice.asr.offline.enabled:true} == true")
	public AsrService micaVoiceOfflineAsrService(MicaVoiceProperties props,
	                                            MicaVoiceConfig coreProps) {
		MicaVoiceProperties.Asr.Offline cfg = props.getAsr().getOffline();
		AsrConfig asrConfig = AsrConfig.builder()
			.modelDirName(cfg.getModelDirName())
			.modelType(parseModelType(cfg.getModelType(), AsrConfig.ModelType.class, AsrConfig.ModelType.PARAFORMER))
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.language(cfg.getLanguage())
			.inverseTextNormalization(cfg.isInverseTextNormalization())
			.build();
		log.info("mica-voice 装配 OfflineAsrService: modelDir={}, type={}", cfg.getModelDirName(), cfg.getModelType());
		return MicaVoice.asr(coreProps, asrConfig);
	}

	/**
	 * 在线流式 ASR。
	 */
	@Bean(name = "micaVoiceOnlineAsrService")
	@Condition(onMissingBeanName = "micaVoiceOnlineAsrService",
		onBeanName = "micaVoiceCoreProperties",
		onExpression = "${mica.voice.asr.online.enabled:false} == true")
	public OnlineAsrService micaVoiceOnlineAsrService(@Inject MicaVoiceProperties props,
	                                                 @Inject MicaVoiceConfig coreProps) {
		MicaVoiceProperties.Asr.Online cfg = props.getAsr().getOnline();
		OnlineAsrConfig onlineConfig = OnlineAsrConfig.builder()
			.modelDirName(cfg.getModelDirName())
			.modelType(parseModelType(cfg.getModelType(), OnlineAsrConfig.ModelType.class, OnlineAsrConfig.ModelType.PARAFORMER))
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.enableEndpoint(cfg.isEnableEndpoint())
			.chunkSize(cfg.getChunkSize())
			.build();
		log.info("mica-voice 装配 OnlineAsrService: modelDir={}, type={}", cfg.getModelDirName(), cfg.getModelType());
		return MicaVoice.onlineAsrTyped(coreProps, onlineConfig);
	}
}