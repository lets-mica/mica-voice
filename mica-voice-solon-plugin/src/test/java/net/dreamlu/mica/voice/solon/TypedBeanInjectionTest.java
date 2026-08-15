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

import net.dreamlu.mica.voice.asr.OfflineAsrService;
import net.dreamlu.mica.voice.asr.OnlineAsrService;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import net.dreamlu.mica.voice.tts.TtsService;
import org.junit.jupiter.api.Test;
import org.noear.solon.SimpleSolonApp;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 plugin 装配的 Bean 既能按名字也能按类型被注入。
 *
 * <p>v1.2+：ASR 不再以 {@code AsrService} 接口暴露（避免同接口多 Bean 的歧义），
 * 而是按 {@link OfflineAsrService} / {@link OnlineAsrService} 具体类型注入。
 *
 * <p>业务 controller 风格的消费者只需要按字段类型注入即可。
 * 为避免污染同包内其他单元测试，本测试的 {@link TypedBeanConsumer} 不使用 {@code @Component}
 * 注解，而是通过 {@code AppContext#wrapAndPut} 在启动完成后手动注册。
 */
class TypedBeanInjectionTest {

	/**
	 * 业务 controller 风格的消费者：仅按字段类型注入。
	 */
	public static class TypedBeanConsumer {

		@Inject
		private SpeakerService speakerService;

		@Inject
		private OfflineAsrService offlineAsrService;

		@Inject
		private TtsService ttsService;

		public SpeakerService getSpeakerService() {
			return speakerService;
		}

		public OfflineAsrService getOfflineAsrService() {
			return offlineAsrService;
		}

		public TtsService getTtsService() {
			return ttsService;
		}
	}

	private static String resolveModelsDir() {
		File dir = new File(System.getProperty("user.dir"));
		File models = new File(dir, "models");
		if (models.isDirectory()) {
			return models.getAbsolutePath();
		}
		File parentModels = new File(dir.getParentFile(), "models");
		if (parentModels.isDirectory()) {
			return parentModels.getAbsolutePath();
		}
		return "models";
	}

	@Test
	void beansAreResolvableByType() throws Throwable {
		SimpleSolonApp app = new SimpleSolonApp(TypedBeanInjectionTest.class);
		app.cfg().put("mica.voice.enabled", "true");
		app.cfg().put("mica.voice.models-dir", resolveModelsDir());
		app.cfg().put("mica.voice.asr.offline.enabled", "true");
		app.cfg().put("mica.voice.asr.online.enabled", "false");
		app.cfg().put("mica.voice.tts.enabled", "true");
		app.cfg().put("mica.voice.speaker.enabled", "true");
		app.start(null);

		AppContext ctx = app.context();
		// 按类型查找（这是 consumer 字段注入的最终路径）
		assertNotNull(ctx.getBean(SpeakerService.class), "SpeakerService 应可按类型查找");
		assertNotNull(ctx.getBean(OfflineAsrService.class), "OfflineAsrService 应可按类型查找");
		assertNotNull(ctx.getBean(TtsService.class), "TtsService 应可按类型查找");

		// 同时按名字查找（用于支持 byName 的消费者）
		assertNotNull(ctx.getBean("micaVoiceSpeakerService"));
		assertNotNull(ctx.getBean("micaVoiceOfflineAsrService"));
		assertNotNull(ctx.getBean("micaVoiceTtsService"));

		// mica.voice.asr.online.enabled=false 时 OnlineAsrService 不应被装配
		assertNull(ctx.getBean(OnlineAsrService.class), "OnlineAsrService 在 disabled 时不应装配");
		assertNull(ctx.getBean("micaVoiceOnlineAsrService"));

		// 消费者字段注入也必须成功（@Inject by type）—— 通过 wrapAndPut 手动注册，
		// 确保不会干扰其他同包单元测试的扫描范围。
		ctx.wrapAndPut(TypedBeanInjectionTest.TypedBeanConsumer.class);
		TypedBeanConsumer consumer = ctx.getBean(TypedBeanInjectionTest.TypedBeanConsumer.class);
		assertNotNull(consumer);
		assertNotNull(consumer.getSpeakerService(), "SpeakerService 字段注入应成功");
		assertNotNull(consumer.getOfflineAsrService(), "OfflineAsrService 字段注入应成功");
		assertNotNull(consumer.getTtsService(), "TtsService 字段注入应成功");

		app.stop();
	}
}