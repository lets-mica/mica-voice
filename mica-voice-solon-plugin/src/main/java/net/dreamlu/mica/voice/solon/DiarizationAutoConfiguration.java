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
import net.dreamlu.mica.voice.config.DiarizationConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.diarization.DiarizationService;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
/**
 * 说话人分离自动装配。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class)
public class DiarizationAutoConfiguration {

	@Bean(name = "micaVoiceDiarizationService")
	@Condition(onMissingBeanName = "micaVoiceDiarizationService",
		onBeanName = "micaVoiceCoreProperties",
		onExpression = "${mica.voice.diarization.enabled:false} == true")
	public DiarizationService micaVoiceDiarizationService(@Inject MicaVoiceProperties props,
	                                                      @Inject MicaVoiceConfig coreProps) {
		MicaVoiceProperties.Diarization cfg = props.getDiarization();
		DiarizationConfig diarConfig = DiarizationConfig.builder()
			.segmentationModelFileName(cfg.getSegmentationModelFileName())
			.embeddingModelFileName(cfg.getEmbeddingModelFileName())
			.threads(cfg.getThreads())
			.debug(cfg.isDebug())
			.numClusters(cfg.getNumClusters())
			.clusterThreshold(cfg.getClusterThreshold())
			.minDurationOff(cfg.getMinDurationOff())
			.minDurationOn(cfg.getMinDurationOn())
			.build();
		log.info("mica-voice 装配 DiarizationService: seg={}, emb={}",
			cfg.getSegmentationModelFileName(), cfg.getEmbeddingModelFileName());
		return MicaVoice.diarization(coreProps, diarConfig);
	}
}