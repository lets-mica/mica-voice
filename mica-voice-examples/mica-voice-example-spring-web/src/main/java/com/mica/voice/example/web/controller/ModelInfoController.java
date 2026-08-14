package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import net.dreamlu.mica.voice.autoconfigure.MicaVoiceProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当前 mica-voice 装配信息（用于调试与 HTML 控制台展示）。
 *
 * <p>{@code GET /api/info}
 *
 * @author dreamlu
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ModelInfoController {

	private final MicaVoiceProperties starterProps;

	@Qualifier("micaVoiceCoreProperties")
	private final net.dreamlu.mica.voice.config.MicaVoiceProperties coreProps;

	@GetMapping("/info")
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
	@GetMapping("/models")
	public Map<String, Object> models() {
		Map<String, Object> resp = new LinkedHashMap<>();
		File dir = coreProps.getModelsDir();
		resp.put("modelsDir", dir == null ? null : dir.getAbsolutePath());
		if (dir != null && dir.isDirectory()) {
			String[] subdirs = dir.list((d, n) -> new File(d, n).isDirectory());
			resp.put("subdirs", subdirs == null ? java.util.Collections.emptyList() : Arrays.asList(subdirs));
		} else {
			resp.put("subdirs", java.util.Collections.emptyList());
		}
		return resp;
	}
}
