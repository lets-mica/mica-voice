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

package com.mica.voice.example.solon;

import org.noear.solon.Solon;

/**
 * mica-voice Solon Web 示例入口。
 *
 * <p>启动后按 {@code app.yml} 装配 mica-voice-solon-plugin：
 * <ul>
 *     <li>{@code mica.voice.asr.offline.enabled=true} → {@code AsrService} Bean</li>
 *     <li>{@code mica.voice.tts.enabled=true} → {@code TtsService} Bean</li>
 *     <li>{@code mica.voice.speaker.enabled=true} → {@code SpeakerService} Bean</li>
 * </ul>
 *
 * <p>提供与 spring-web 示例同款的 REST 端点（{@code /mica/voice/*}、{@code /demo/*}、{@code /api/*}）。
 *
 * @author dreamlu
 */
public class Application {

	public static void main(String[] args) {
		Solon.start(Application.class, args);
	}
}