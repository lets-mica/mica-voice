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
import net.dreamlu.mica.voice.asr.OfflineAsrService;
import net.dreamlu.mica.voice.core.MicaVoice;
import net.dreamlu.mica.voice.diarization.DiarizationService;
import net.dreamlu.mica.voice.transcribe.OfflineDiarizationTranscribeService;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * "说话人分离 + 转写"联合服务自动装配。
 *
 * <p>v1.1+。当 {@code mica.voice.asr.offline.enabled=true} 且
 * {@code mica.voice.diarization.enabled=true} 时生效。
 *
 * <p>v1.2+：依赖的 ASR 收紧为 {@link OfflineAsrService} 具体类型，避免
 * 离线/在线两个 ASR 都实现同一接口时 {@code @Inject AsrService} 的歧义。
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class)
public class TranscribeAutoConfiguration {

	@Inject(required = false)
	private OfflineAsrService micaVoiceOfflineAsrService;

	@Inject(required = false)
	private DiarizationService micaVoiceDiarizationService;

	@Bean(name = "micaVoiceDiarizationTranscribeService", typed = true)
	@Condition(onMissingBeanName = "micaVoiceDiarizationTranscribeService",
		onBeanName = "micaVoiceOfflineAsrService")
	public OfflineDiarizationTranscribeService micaVoiceDiarizationTranscribeService() {
		if (micaVoiceOfflineAsrService == null || micaVoiceDiarizationService == null) {
			log.warn("mica-voice OfflineDiarizationTranscribeService 装配失败：依赖 {} / {} 不全",
				"micaVoiceOfflineAsrService", "micaVoiceDiarizationService");
			return null;
		}
		log.info("mica-voice 装配 OfflineDiarizationTranscribeService");
		return MicaVoice.transcribe(micaVoiceDiarizationService, micaVoiceOfflineAsrService);
	}
}