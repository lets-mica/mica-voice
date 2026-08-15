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
import net.dreamlu.mica.voice.config.KwsConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.kws.KwsService;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
/**
 * 关键词唤醒（KWS）自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class)
public class KwsAutoConfiguration {

	@Bean(name = "micaVoiceKwsService", typed = true)
	@Condition(onMissingBeanName = "micaVoiceKwsService",
		onBeanName = "micaVoiceCoreProperties",
		onExpression = "${mica.voice.kws.enabled:false} == true")
	public KwsService micaVoiceKwsService(MicaVoiceProperties props,
	                                      MicaVoiceConfig coreProps) {
		MicaVoiceProperties.Kws cfg = props.getKws();
		KwsConfig kwsConfig = KwsConfig.builder()
			.modelDirName(cfg.getModelDirName())
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.sampleRate(cfg.getSampleRate())
			.featureDim(cfg.getFeatureDim())
			.keywordsScore(cfg.getKeywordsScore())
			.keywordsThreshold(cfg.getKeywordsThreshold())
			.maxActivePaths(cfg.getMaxActivePaths())
			.keywordsFile(cfg.getKeywordsFile())
			.build();
		log.info("mica-voice 装配 KwsService: modelDir={}", cfg.getModelDirName());
		return MicaVoice.kws(coreProps, kwsConfig);
	}
}