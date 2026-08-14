package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.diarization.DiarizationSegment;
import net.dreamlu.mica.voice.diarization.DiarizationService;
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
 * Diarization REST 端点（v1.1）。
 *
 * <pre>
 *   POST /mica/voice/diarization/run  file=@input.wav
 *     resp: { segments: [ { startSec, endSec, speaker, durationSec }, ... ] }
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
@RestController
@RequestMapping("/mica/voice/diarization")
@RequiredArgsConstructor
public class DiarizationController {

	private final ObjectProvider<DiarizationService> diarizationProvider;

	private static File toTmp(MultipartFile file) {
		try {
			File tmp = File.createTempFile("mica-voice-diar-", ".wav");
			tmp.deleteOnExit();
			file.transferTo(tmp);
			return tmp;
		} catch (Exception e) {
			throw new IllegalArgumentException("无法保存上传文件: " + e.getMessage(), e);
		}
	}

	@PostMapping(value = "/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, Object> run(@RequestParam("file") MultipartFile file) {
		DiarizationService diar = diarizationProvider.getIfAvailable();
		if (diar == null) {
			throw new IllegalStateException("Diarization 服务未启用，请设置 mica.voice.diarization.enabled=true");
		}
		AudioData audio = AudioReaders.read(toTmp(file));
		List<DiarizationSegment> segments = diar.diarize(audio);
		List<Map<String, Object>> segList = new ArrayList<>(segments.size());
		for (DiarizationSegment s : segments) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("startSec", s.getStartSec());
			m.put("endSec", s.getEndSec());
			m.put("durationSec", s.getEndSec() - s.getStartSec());
			m.put("speaker", s.getSpeaker());
			segList.add(m);
		}
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("total", segList.size());
		resp.put("segments", segList);
		return resp;
	}
}
