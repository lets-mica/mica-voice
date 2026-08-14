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

package com.mica.voice.example.solon.controller;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.tts.TtsAudio;
import net.dreamlu.mica.voice.tts.TtsService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;
import org.noear.solon.core.handle.DownloadedFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TTS REST 端点。
 *
 * <pre>
 *   GET  /mica/voice/tts/synthesize?text=...&speakerId=0&speed=1.0  -> audio/wav
 *   GET  /mica/voice/tts/info -> { sampleRate, numSpeakers, defaultSpeakerId, defaultSpeed }
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
@Controller
@Mapping("/mica/voice/tts")
public class TtsController {

	@Inject
	private TtsService ttsService;

	/**
	 * 把 float[] [-1,1] 写成单声道 16-bit PCM WAV。
	 */
	static byte[] wrapAsWav(float[] samples, int sampleRate) {
		int byteRate = sampleRate * 2;
		int dataLen = samples.length * 2;
		int totalLen = 36 + dataLen;
		ByteBuffer buf = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) 'R').put((byte) 'I').put((byte) 'F').put((byte) 'F');
		buf.putInt(totalLen);
		buf.put((byte) 'W').put((byte) 'A').put((byte) 'V').put((byte) 'E');
		buf.put((byte) 'f').put((byte) 'm').put((byte) 't').put((byte) ' ');
		buf.putInt(16);
		buf.putShort((short) 1);
		buf.putShort((short) 1);
		buf.putInt(sampleRate);
		buf.putInt(byteRate);
		buf.putShort((short) 2);
		buf.putShort((short) 16);
		buf.put((byte) 'd').put((byte) 'a').put((byte) 't').put((byte) 'a');
		buf.putInt(dataLen);
		for (float s : samples) {
			float c = Math.max(-1f, Math.min(1f, s));
			buf.putShort((short) (c * 32767f));
		}
		return buf.array();
	}

	@Get
	@Mapping("/synthesize")
	public DownloadedFile synthesize(
		@Param("text") String text,
		@Param(value = "speakerId", defaultValue = "0") int speakerId,
		@Param(value = "speed", defaultValue = "1.0") float speed) throws IOException {
		if (text == null || text.isEmpty()) {
			throw new IllegalArgumentException("text 不能为空");
		}
		TtsAudio audio = ttsService.synthesize(text, speakerId, speed);
		byte[] wav = wrapAsWav(audio.getSamples(), audio.getSampleRate());
		return new DownloadedFile("audio/wav", new ByteArrayInputStream(wav), "tts-" + speakerId + ".wav");
	}

	/**
	 * 模型信息（用于调试与 HTML 控制台）。
	 */
	@Get
	@Mapping("/info")
	public Map<String, Object> info() {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("sampleRate", ttsService.getSampleRate());
		info.put("numSpeakers", ttsService.getNumSpeakers());
		return info;
	}
}