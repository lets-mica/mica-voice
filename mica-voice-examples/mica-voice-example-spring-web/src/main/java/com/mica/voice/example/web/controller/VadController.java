package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.vad.VadSegment;
import net.dreamlu.mica.voice.vad.VadService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * VAD REST 端点（v1.1）。
 *
 * <pre>
 *   POST /mica/voice/vad/detect  file=@input.wav
 *     resp: { segments: [ { startSample, sampleRate, durationSec, samples }, ... ] }
 * </pre>
 *
 * <p>vad 服务为可选装配（{@code mica.voice.vad.enabled=true}），本 Controller 通过
 * {@link ObjectProvider} 懒注入，启动时不强制依赖。
 *
 * @author dreamlu
 */
@Slf4j
@RestController
@RequestMapping("/mica/voice/vad")
@RequiredArgsConstructor
public class VadController {

	private final ObjectProvider<VadService> vadProvider;

	private static File toTmp(MultipartFile file) {
		try {
			File tmp = File.createTempFile("mica-voice-vad-", ".wav");
			tmp.deleteOnExit();
			file.transferTo(tmp);
			return tmp;
		} catch (Exception e) {
			throw new IllegalArgumentException("无法保存上传文件: " + e.getMessage(), e);
		}
	}

	@PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, Object> detect(@RequestParam("file") MultipartFile file) {
		VadService vad = vadProvider.getIfAvailable();
		if (vad == null) {
			throw new IllegalStateException("VAD 服务未启用，请在 application.yml 设置 mica.voice.vad.enabled=true");
		}
		AudioData audio = AudioReaders.read(toTmp(file));
		List<VadSegment> segments = vad.detect(audio);
		List<Map<String, Object>> segList = new ArrayList<>(segments.size());
		for (VadSegment s : segments) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("startSample", s.getStartSample());
			m.put("sampleRate", s.getSampleRate());
			m.put("durationSec", s.durationSeconds());
			m.put("samplesLength", s.getSamples().length);
			segList.add(m);
		}
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("total", segList.size());
		resp.put("segments", segList);
		return resp;
	}
}
