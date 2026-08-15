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
import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.OfflineAsrService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.UploadedFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 离线 ASR REST 端点。
 *
 * <pre>
 *   POST /mica/voice/asr/recognize
 *     multipart: file=@input.wav
 *     resp: { "text": "...", "language": "...", "emotion": "...", "event": "...", "costMs": 123, "tokens": [...] }
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
@Controller
@Mapping("/mica/voice/asr")
public class AsrController {

	@Inject
	private OfflineAsrService offlineAsrService;

	private static File toTempFile(UploadedFile file) {
		try {
			File tmp = File.createTempFile("mica-voice-asr-", ".wav");
			tmp.deleteOnExit();
			if (file.getContentSize() > 0) {
				Files.write(tmp.toPath(), file.getContentAsBytes());
			}
			return tmp;
		} catch (IOException e) {
			throw new IllegalArgumentException("无法保存上传文件: " + e.getMessage(), e);
		}
	}

	@Post
	@Mapping("/recognize")
	public Map<String, Object> recognize(Context ctx, UploadedFile file) {
		AsrResult r = offlineAsrService.recognize(toTempFile(file));
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("text", r.getText());
		resp.put("language", r.getLanguage());
		resp.put("emotion", r.getEmotion());
		resp.put("event", r.getEvent());
		resp.put("costMs", r.getCostMs());
		resp.put("tokens", r.getTokens());
		return resp;
	}
}