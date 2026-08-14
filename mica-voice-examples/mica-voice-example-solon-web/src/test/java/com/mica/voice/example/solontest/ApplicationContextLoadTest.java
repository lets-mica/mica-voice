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

package com.mica.voice.example.solontest;

import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.solon.MicaVoiceProperties;
import org.junit.jupiter.api.Test;
import org.noear.solon.SimpleSolonApp;
import org.noear.solon.core.AppContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 mica-voice-solon-plugin 的装配链路通畅。
 *
 * <p>本测试刻意放在 {@code com.mica.voice.example.solontest} 子包，
 * 避免扫描到 {@code com.mica.voice.example.solon.controller} 下的业务 controller
 * （这些 controller 会注入 Service Bean，依赖 native 模型加载）。
 *
 * <p>只启动 Solon + plugin，检查 starter 顶层装配的两个核心 Bean：
 * <ul>
 *     <li>{@link MicaVoiceProperties}（yml 绑定）</li>
 *     <li>{@code micaVoiceCoreProperties}（core 属性派生）</li>
 * </ul>
 *
 * @author dreamlu
 */
class ApplicationContextLoadTest {

	@Test
	void starterBeanWired() throws Throwable {
		// 仅开启 mica.voice 顶层装配，避免任何 native 模型加载
		SimpleSolonApp app = new SimpleSolonApp(ApplicationContextLoadTest.class);
		app.cfg().put("mica.voice.enabled", "true");
		// 关闭会触发 native 初始化的能力
		app.cfg().put("mica.voice.asr.offline.enabled", "false");
		app.cfg().put("mica.voice.asr.online.enabled", "false");
		app.cfg().put("mica.voice.tts.enabled", "false");
		app.cfg().put("mica.voice.speaker.enabled", "false");
		app.start(null);

		AppContext ctx = app.context();
		assertNotNull(ctx.getBean(MicaVoiceProperties.class), "MicaVoiceProperties 应已绑定");
		assertNotNull(ctx.getBean("micaVoiceCoreProperties"), "micaVoiceCoreProperties 应已派生");
		Object coreProps = ctx.getBean("micaVoiceCoreProperties");
		assertNotNull(coreProps);
		assertTrue(coreProps instanceof MicaVoiceConfig, "micaVoiceCoreProperties 应是 MicaVoiceConfig 类型");

		app.stop();
	}
}