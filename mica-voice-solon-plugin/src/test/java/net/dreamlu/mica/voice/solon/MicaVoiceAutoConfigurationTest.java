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

import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.SimpleSolonApp;
import org.noear.solon.Utils;
import org.noear.solon.core.AppContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link MicaVoiceAutoConfiguration} 自动装配单元测试。
 *
 * <p>本测试只验证 starter 的装配条件 + 配置树绑定，避免触发任何 native 装配。
 * mica.voice.enabled=false → 装配类不激活；=true（默认）→ micaVoiceCoreProperties Bean 创建。
 */
class MicaVoiceAutoConfigurationTest {

	@Test
	void shouldNotCreateCorePropertiesWhenDisabled() throws Throwable {
		SimpleSolonApp app = new SimpleSolonApp(MicaVoiceAutoConfigurationTest.class);
		app.cfg().put("mica.voice.enabled", "false");
		app.start(null);

		assertNull(app.context().getBean("micaVoiceCoreProperties"));
		app.stop();
	}

	@Test
	void shouldCreateCorePropertiesBeanByDefault() throws Throwable {
		SimpleSolonApp app = new SimpleSolonApp(MicaVoiceAutoConfigurationTest.class);
		app.cfg().put("mica.voice.enabled", "true");
		app.start(null);

		MicaVoiceConfig coreProps =
			app.context().getBean(MicaVoiceConfig.class);
		assertNotNull(coreProps, "micaVoiceCoreProperties 应由 MicaVoiceAutoConfiguration 自动装配");
		assertNotNull(coreProps.getModelsDir());
		assertNotNull(coreProps.getOutputDir());

		app.stop();
	}

	@Test
	void shouldBindNestedConfig() throws Throwable {
		SimpleSolonApp app = new SimpleSolonApp(MicaVoiceAutoConfigurationTest.class);
		app.cfg().put("mica.voice.enabled", "true");
		// 本测试只验证配置树绑定，避免触发任何 native 装配（模型文件不存在会抛异常）
		app.cfg().put("mica.voice.asr.offline.enabled", "false");
		app.cfg().put("mica.voice.asr.online.enabled", "false");
		app.cfg().put("mica.voice.tts.enabled", "false");
		app.cfg().put("mica.voice.speaker.enabled", "false");
		app.cfg().put("mica.voice.models-dir", "/tmp/test-models");
		app.cfg().put("mica.voice.output-dir", "/tmp/test-output");
		app.cfg().put("mica.voice.threads", "8");
		app.cfg().put("mica.voice.debug", "true");
		app.cfg().put("mica.voice.asr.offline.model-dir-name", "custom-asr");
		app.cfg().put("mica.voice.asr.offline.model-type", "SENSE_VOICE");
		app.cfg().put("mica.voice.tts.model-dir-name", "custom-tts");
		app.cfg().put("mica.voice.tts.default-speaker-id", "3");
		app.cfg().put("mica.voice.speaker.threshold", "0.7");
		app.cfg().put("mica.voice.vad.threshold", "0.6");
		app.start(null);

		// starter 的 MicaVoiceProperties 绑定正确
		MicaVoiceProperties props = app.context().getBean(MicaVoiceProperties.class);
		assertNotNull(props);
		Assertions.assertEquals("/tmp/test-models", props.getModelsDir());
		Assertions.assertEquals("/tmp/test-output", props.getOutputDir());
		Assertions.assertEquals(Integer.valueOf(8), props.getThreads());
		Assertions.assertTrue(props.isDebug());
		Assertions.assertEquals("custom-asr", props.getAsr().getOffline().getModelDirName());
		Assertions.assertEquals("SENSE_VOICE", props.getAsr().getOffline().getModelType());
		Assertions.assertEquals("custom-tts", props.getTts().getModelDirName());
		Assertions.assertEquals(3, props.getTts().getDefaultSpeakerId());
		Assertions.assertEquals(0.7f, props.getSpeaker().getThreshold());
		Assertions.assertEquals(0.6f, props.getVad().getThreshold());

		// core 派生属性正确
		MicaVoiceConfig coreProps =
			app.context().getBean(MicaVoiceConfig.class);
		assertNotNull(coreProps);
		Assertions.assertEquals(new java.io.File("/tmp/test-models"), coreProps.getModelsDir());
		Assertions.assertEquals(new java.io.File("/tmp/test-output"), coreProps.getOutputDir());
		Assertions.assertEquals(8, coreProps.getThreads());
		Assertions.assertTrue(coreProps.isDebug());

		app.stop();
	}

	@Test
	void shouldNotCreateAsrServiceBeanWhenOfflineDisabled() throws Exception {
		SimpleSolonApp app = new SimpleSolonApp(MicaVoiceAutoConfigurationTest.class);
		app.cfg().put("mica.voice.enabled", "true");
		app.cfg().put("mica.voice.asr.offline.enabled", "false");
		app.cfg().put("mica.voice.asr.online.enabled", "false");
		AtomicReference<Throwable> reference = new AtomicReference<>();
		try {
			app.start(null);
		} catch (Throwable e) {
			e = Utils.throwableUnwrap(e.getCause());
			while (e.getCause() != null) {
				e = e.getCause();
			}
			reference.set(e);
		}

		// micaVoiceOfflineAsrService 没创建；其他能力同理
		assertNull(app.context().getBean("micaVoiceOfflineAsrService"));
		assertNull(app.context().getBean("micaVoiceOnlineAsrService"));
		assertNull(app.context().getBean("micaVoiceVadService"));
		assertNull(app.context().getBean("micaVoiceDiarizationService"));
		assertNull(app.context().getBean("micaVoiceKwsService"));
		assertNull(app.context().getBean("micaVoiceDenoiseService"));
		// micaVoiceTtsService / micaVoiceSpeakerService 默认启用
		assertNotNull(app.context().getBean("micaVoiceTtsService"));
		assertNotNull(app.context().getBean("micaVoiceSpeakerService"));

		// 联合服务不应装配（缺 offline-asr + diarization）
		assertNull(app.context().getBean("micaVoiceDiarizationTranscribeService"));

		app.stop();
	}

	@Test
	void shouldBindCorePropertiesType() throws Throwable {
		SimpleSolonApp app = new SimpleSolonApp(MicaVoiceAutoConfigurationTest.class);
		app.cfg().put("mica.voice.enabled", "true");
		app.start(null);

		// core 属性类型应为 net.dreamlu.mica.voice.config.MicaVoiceConfig
		AppContext ctx = app.context();
		MicaVoiceConfig core =
			ctx.getBean(MicaVoiceConfig.class);
		assertNotNull(core);

		app.stop();
	}
}
