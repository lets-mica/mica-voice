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
import net.dreamlu.mica.voice.config.DenoiseConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.denoise.DenoiseService;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import java.util.Locale;

/**
 * 音频降噪自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class)
public class DenoiseAutoConfiguration {

	@Bean(name = "micaVoiceDenoiseService", typed = true)
	@Condition(onMissingBeanName = "micaVoiceDenoiseService",
		onBeanName = "micaVoiceCoreProperties",
		onExpression = "${mica.voice.denoise.enabled:false} == true")
	public DenoiseService micaVoiceDenoiseService(@Inject MicaVoiceProperties props,
	                                              @Inject MicaVoiceConfig coreProps) {
		MicaVoiceProperties.Denoise cfg = props.getDenoise();
		DenoiseConfig.ModelType type;
		try {
			type = Enum.valueOf(DenoiseConfig.ModelType.class, cfg.getModelType().toUpperCase(Locale.ROOT));
		} catch (Exception ex) {
			type = DenoiseConfig.ModelType.GTCRN;
		}
		DenoiseConfig denoiseConfig = DenoiseConfig.builder()
			.modelFileName(cfg.getModelFileName())
			.modelType(type)
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.attenuationLimitDb(cfg.getAttenuationLimitDb())
			.build();
		log.info("mica-voice 装配 DenoiseService: model={}, type={}", cfg.getModelFileName(), cfg.getModelType());
		return MicaVoice.denoise(coreProps, denoiseConfig);
	}
}