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

package net.dreamlu.mica.voice.solon.integration;

import net.dreamlu.mica.voice.solon.AsrAutoConfiguration;
import net.dreamlu.mica.voice.solon.DenoiseAutoConfiguration;
import net.dreamlu.mica.voice.solon.DiarizationAutoConfiguration;
import net.dreamlu.mica.voice.solon.KwsAutoConfiguration;
import net.dreamlu.mica.voice.solon.MicaVoiceAutoConfiguration;
import net.dreamlu.mica.voice.solon.MicaVoiceProperties;
import net.dreamlu.mica.voice.solon.SpeakerAutoConfiguration;
import net.dreamlu.mica.voice.solon.TranscribeAutoConfiguration;
import net.dreamlu.mica.voice.solon.TtsAutoConfiguration;
import net.dreamlu.mica.voice.solon.VadAutoConfiguration;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;

/**
 * mica-voice Solon 插件入口。
 *
 * <p>由 {@code META-INF/solon/solon.mica.voice.properties} 通过 SPI 注册，
 * 启动时按声明顺序把 {@link MicaVoiceProperties} + 8 个自动配置类注入到 Solon 容器。
 *
 * @author dreamlu
 */
public class MicaVoicePlugin implements Plugin {

	@Override
	public void start(AppContext context) throws Throwable {
		// 1. yml 属性绑定
		context.beanMake(MicaVoiceProperties.class);
		// 2. core 属性派生（依赖 yml 属性）
		context.beanMake(MicaVoiceAutoConfiguration.class);
		// 3. 各能力装配（按字母序；Solon 内部按依赖解析，无需额外排序）
		context.beanMake(AsrAutoConfiguration.class);
		context.beanMake(DenoiseAutoConfiguration.class);
		context.beanMake(DiarizationAutoConfiguration.class);
		context.beanMake(KwsAutoConfiguration.class);
		context.beanMake(SpeakerAutoConfiguration.class);
		context.beanMake(TtsAutoConfiguration.class);
		context.beanMake(VadAutoConfiguration.class);
		// 4. 联合服务（依赖 offline-asr + diarization）
		context.beanMake(TranscribeAutoConfiguration.class);
	}
}