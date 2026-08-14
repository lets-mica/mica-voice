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

import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.solon.MicaVoiceProperties;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Get;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当前 mica-voice 装配信息（用于调试与 HTML 控制台展示）。
 *
 * <p>{@code GET /api/info}、{@code GET /api/models}
 *
 * @author dreamlu
 */
@Controller
@Mapping("/api")
public class ModelInfoController {

	@Inject
	private MicaVoiceProperties starterProps;

	@Inject("micaVoiceCoreProperties")
	private MicaVoiceConfig coreProps;

	@Get
	@Mapping("/info")
	public Map<String, Object> info() {
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("modelsDir", coreProps.getModelsDir() == null ? null : coreProps.getModelsDir().getAbsolutePath());
		resp.put("modelsDirExists", coreProps.getModelsDir() != null && coreProps.getModelsDir().isDirectory());
		resp.put("outputDir", coreProps.getOutputDir() == null ? null : coreProps.getOutputDir().getAbsolutePath());
		resp.put("threads", coreProps.getThreads());
		resp.put("debug", coreProps.isDebug());

		Map<String, Object> asr = new LinkedHashMap<>();
		asr.put("offlineEnabled", starterProps.getAsr().getOffline().isEnabled());
		asr.put("offlineModelDirName", starterProps.getAsr().getOffline().getModelDirName());
		asr.put("offlineModelType", starterProps.getAsr().getOffline().getModelType());
		asr.put("onlineEnabled", starterProps.getAsr().getOnline().isEnabled());
		resp.put("asr", asr);

		Map<String, Object> tts = new LinkedHashMap<>();
		tts.put("enabled", starterProps.getTts().isEnabled());
		tts.put("modelDirName", starterProps.getTts().getModelDirName());
		tts.put("defaultSpeakerId", starterProps.getTts().getDefaultSpeakerId());
		tts.put("defaultSpeed", starterProps.getTts().getDefaultSpeed());
		resp.put("tts", tts);

		Map<String, Object> speaker = new LinkedHashMap<>();
		speaker.put("enabled", starterProps.getSpeaker().isEnabled());
		speaker.put("threshold", starterProps.getSpeaker().getThreshold());
		speaker.put("modelCandidates", Arrays.asList(starterProps.getSpeaker().getModelCandidates()));
		resp.put("speaker", speaker);

		return resp;
	}

	/**
	 * 探测 {@code models-dir} 下的子目录列表（用于调试）。
	 */
	@Get
	@Mapping("/models")
	public Map<String, Object> models() {
		Map<String, Object> resp = new LinkedHashMap<>();
		File dir = coreProps.getModelsDir();
		resp.put("modelsDir", dir == null ? null : dir.getAbsolutePath());
		if (dir != null && dir.isDirectory()) {
			String[] subdirs = dir.list((d, n) -> new File(d, n).isDirectory());
			resp.put("subdirs", subdirs == null ? Collections.emptyList() : Arrays.asList(subdirs));
		} else {
			resp.put("subdirs", Collections.emptyList());
		}
		return resp;
	}
}