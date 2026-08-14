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
import net.dreamlu.mica.voice.config.SpeakerConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

import java.util.Arrays;

/**
 * 声纹识别自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class)
public class SpeakerAutoConfiguration {

	@Bean(name = "micaVoiceSpeakerService")
	@Condition(onMissingBeanName = "micaVoiceSpeakerService",
		onBeanName = "micaVoiceCoreProperties",
		onExpression = "${mica.voice.speaker.enabled:true} == true")
	public SpeakerService micaVoiceSpeakerService(@Inject MicaVoiceProperties props,
	                                              @Inject MicaVoiceConfig coreProps) {
		MicaVoiceProperties.Speaker cfg = props.getSpeaker();
		SpeakerConfig speakerConfig = SpeakerConfig.builder()
			.modelCandidates(cfg.getModelCandidates() == null
				? SpeakerConfig.DEFAULT_MODEL_CANDIDATES
				: cfg.getModelCandidates())
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.threshold(cfg.getThreshold())
			.embeddingTimeoutMs(cfg.getEmbeddingTimeoutMs())
			.build();
		log.info("mica-voice 装配 SpeakerService: threshold={}, candidates={}",
			cfg.getThreshold(), Arrays.toString(cfg.getModelCandidates()));
		return MicaVoice.speaker(coreProps, speakerConfig);
	}
}
