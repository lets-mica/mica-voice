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
import net.dreamlu.mica.voice.core.MicaVoice;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

import java.io.File;

/**
 * mica-voice Solon 顶层装配入口。
 *
 * <p>负责：
 * <ul>
 *     <li>把 starter 的 {@link MicaVoiceProperties} 转为 core 的
 *         {@link net.dreamlu.mica.voice.config.MicaVoiceConfig} 作为可注入 Bean
 *         （其它 Bean 注入运行时属性都从这里取）；</li>
 *     <li>各能力（ASR / TTS / Speaker / VAD / Diarization / KWS / Denoise）通过对应的自动配置类装配。</li>
 * </ul>
 *
 * @author dreamlu
 */
@Slf4j
@Configuration
@Condition(onClass = MicaVoice.class, onExpression = "${mica.voice.enabled:true} == true")
public class MicaVoiceAutoConfiguration {

	/**
	 * 把 starter 的扁平配置转换成 core 用的运行时 {@link net.dreamlu.mica.voice.config.MicaVoiceConfig}。
	 * 该 Bean 是 core 层各 Service 构造时的统一入口（命名 micaVoiceCoreProperties）。
	 *
	 * @param props yml 配置属性
	 * @return 转换后的 core 属性
	 */
	@Bean(name = "micaVoiceCoreProperties")
	public net.dreamlu.mica.voice.config.MicaVoiceConfig coreProperties(@Inject MicaVoiceProperties props) {
		net.dreamlu.mica.voice.config.MicaVoiceConfig p =
			new net.dreamlu.mica.voice.config.MicaVoiceConfig();
		p.setModelsDir(new File(props.getModelsDir()));
		p.setOutputDir(new File(props.getOutputDir()));
		if (props.getThreads() != null) {
			p.setThreads(props.getThreads());
		}
		p.setDebug(props.isDebug());
		log.info("mica-voice 运行时属性初始化: modelsDir={}, outputDir={}, threads={}, debug={}",
			p.getModelsDir(), p.getOutputDir(), p.getThreads(), p.isDebug());
		return p;
	}
}