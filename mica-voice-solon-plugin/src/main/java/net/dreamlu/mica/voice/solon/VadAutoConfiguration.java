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
import net.dreamlu.mica.voice.config.VadConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.vad.VadService;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import java.util.Locale;

/**
 * VAD 自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class)
public class VadAutoConfiguration {

	@Bean(name = "micaVoiceVadService")
	@Condition(onMissingBeanName = "micaVoiceVadService",
		onBeanName = "micaVoiceCoreProperties",
		onExpression = "${mica.voice.vad.enabled:false} == true")
	public VadService micaVoiceVadService(@Inject MicaVoiceProperties props,
	                                      @Inject net.dreamlu.mica.voice.config.MicaVoiceConfig coreProps) {
		MicaVoiceProperties.Vad cfg = props.getVad();
		VadConfig.ModelType type;
		try {
			type = Enum.valueOf(VadConfig.ModelType.class, cfg.getModelType().toUpperCase(Locale.ROOT));
		} catch (Exception ex) {
			type = VadConfig.ModelType.SILERO;
		}
		VadConfig vadConfig = VadConfig.builder()
			.modelFileName(cfg.getModelFileName())
			.modelType(type)
			.sampleRate(cfg.getSampleRate())
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.threshold(cfg.getThreshold())
			.minSilenceDuration(cfg.getMinSilenceDuration())
			.minSpeechDuration(cfg.getMinSpeechDuration())
			.maxSpeechDuration(cfg.getMaxSpeechDuration())
			.windowSize(cfg.getWindowSize())
			.build();
		log.info("mica-voice 装配 VadService: model={}, type={}", cfg.getModelFileName(), cfg.getModelType());
		return MicaVoice.vad(coreProps, vadConfig);
	}
}