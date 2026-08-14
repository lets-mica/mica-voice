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
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.TtsConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.tts.TtsService;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;

import java.util.Locale;

/**
 * TTS 自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class)
public class TtsAutoConfiguration {

	@Bean(name = "micaVoiceTtsService")
	@Condition(onMissingBeanName = "micaVoiceTtsService",
		onBeanName = "micaVoiceCoreProperties",
		onExpression = "${mica.voice.tts.enabled:true} == true")
	public TtsService micaVoiceTtsService(MicaVoiceProperties props,
	                                      MicaVoiceConfig coreProps) {
		MicaVoiceProperties.Tts cfg = props.getTts();
		TtsConfig.ModelType type;
		try {
			type = Enum.valueOf(TtsConfig.ModelType.class, cfg.getModelType().toUpperCase(Locale.ROOT));
		} catch (Exception ex) {
			type = TtsConfig.ModelType.VITS;
		}
		TtsConfig ttsConfig = TtsConfig.builder()
			.modelDirName(cfg.getModelDirName())
			.modelType(type)
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.defaultSpeakerId(cfg.getDefaultSpeakerId())
			.defaultSpeed(cfg.getDefaultSpeed())
			.callbackSampleStep(cfg.getCallbackSampleStep())
			.build();
		log.info("mica-voice 装配 TtsService: modelDir={}, type={}", cfg.getModelDirName(), cfg.getModelType());
		return MicaVoice.tts(coreProps, ttsConfig);
	}
}