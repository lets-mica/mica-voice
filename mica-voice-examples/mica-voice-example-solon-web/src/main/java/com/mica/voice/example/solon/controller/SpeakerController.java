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
import net.dreamlu.mica.voice.speaker.SearchResult;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import net.dreamlu.mica.voice.speaker.VerificationResult;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.UploadedFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 声纹 REST 端点。
 *
 * <pre>
 *   POST /mica/voice/speaker/enroll?name=alice  file=@a.wav
 *   POST /mica/voice/speaker/verify?name=alice  file=@b.wav
 *   POST /mica/voice/speaker/search            file=@test.wav
 *   GET  /mica/voice/speaker/names
 *   DELETE /mica/voice/speaker/{name}
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
@Controller
@Mapping("/mica/voice/speaker")
public class SpeakerController {

	@Inject
	private SpeakerService speakerService;

	private static File toTempFile(UploadedFile file) {
		try {
			File tmp = File.createTempFile("mica-voice-spk-", ".wav");
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
	@Mapping("/enroll")
	public Map<String, Object> enroll(@Param("name") String name, UploadedFile file) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("name 不能为空");
		}
		speakerService.enroll(name, toTempFile(file));
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("name", name);
		resp.put("ok", true);
		resp.put("total", speakerService.size());
		return resp;
	}

	@Post
	@Mapping("/verify")
	public VerificationResult verify(@Param("name") String name, UploadedFile file) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("name 不能为空");
		}
		return speakerService.verify(name, toTempFile(file));
	}

	@Post
	@Mapping("/search")
	public SearchResult search(UploadedFile file) {
		return speakerService.search(toTempFile(file));
	}

	@Get
	@Mapping("/names")
	public List<String> names() {
		return speakerService.names();
	}

	@Delete
	@Mapping("/{name}")
	public Map<String, Object> remove(@Path("name") String name) {
		boolean removed = speakerService.remove(name);
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("name", name);
		resp.put("removed", removed);
		resp.put("total", speakerService.size());
		return resp;
	}
}