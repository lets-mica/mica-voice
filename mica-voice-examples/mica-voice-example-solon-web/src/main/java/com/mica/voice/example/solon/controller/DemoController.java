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

import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import net.dreamlu.mica.voice.tts.TtsAudio;
import net.dreamlu.mica.voice.tts.TtsService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.UploadedFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 极简示例 Controller（{@code /demo/*}）。
 *
 * <p>和 {@code /mica/voice/*} 完整端点对照演示怎么直接注入 plugin 装配好的 Service Bean。
 *
 * @author dreamlu
 */
@Controller
@Mapping("/demo")
public class DemoController {

	@Inject
	private AsrService asrService;

	@Inject
	private TtsService ttsService;

	@Inject
	private SpeakerService speakerService;

	private static File toTmp(UploadedFile file) throws IOException {
		File tmp = File.createTempFile("demo-", ".wav");
		tmp.deleteOnExit();
		if (file.getContentSize() > 0) {
			Files.write(tmp.toPath(), file.getContentAsBytes());
		}
		return tmp;
	}

	@Post
	@Mapping("/asr")
	public Map<String, Object> asr(UploadedFile file) throws IOException {
		AsrResult r = asrService.recognize(toTmp(file));
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("text", r.getText());
		resp.put("costMs", r.getCostMs());
		return resp;
	}

	@Get
	@Mapping("/tts")
	public Map<String, Object> tts(@Param("text") String text) {
		TtsAudio audio = ttsService.synthesize(text);
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("samples", audio.getSamples().length);
		resp.put("sampleRate", audio.getSampleRate());
		resp.put("durationSec", audio.durationSeconds());
		resp.put("costMs", audio.getCostMs());
		return resp;
	}

	@Post
	@Mapping("/speaker/enroll")
	public Map<String, Object> enroll(@Param("name") String name, UploadedFile file) throws IOException {
		speakerService.enroll(name, toTmp(file));
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("name", name);
		resp.put("ok", true);
		return resp;
	}

	@Get
	@Mapping("/speaker/names")
	public java.util.List<String> speakerNames() {
		return speakerService.names();
	}
}