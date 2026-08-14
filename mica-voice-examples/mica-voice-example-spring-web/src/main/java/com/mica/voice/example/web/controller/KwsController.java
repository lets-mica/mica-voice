package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.audio.AudioData;
import net.dreamlu.mica.voice.audio.AudioReaders;
import net.dreamlu.mica.voice.kws.KwsResult;
import net.dreamlu.mica.voice.kws.KwsService;
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
 * KWS REST 端点（v1.1）。
 *
 * <pre>
 *   POST /mica/voice/kws/spot  file=@input.wav
 *     resp: { keywords: [ { keyword, tokens, timestamps }, ... ] }
 * </pre>
 *
 * @author dreamlu
 */
@Slf4j
@RestController
@RequestMapping("/mica/voice/kws")
@RequiredArgsConstructor
public class KwsController {

	private final ObjectProvider<KwsService> kwsProvider;

	private static File toTmp(MultipartFile file) {
		try {
			File tmp = File.createTempFile("mica-voice-kws-", ".wav");
			tmp.deleteOnExit();
			file.transferTo(tmp);
			return tmp;
		} catch (Exception e) {
			throw new IllegalArgumentException("无法保存上传文件: " + e.getMessage(), e);
		}
	}

	@PostMapping(value = "/spot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, Object> spot(@RequestParam("file") MultipartFile file) {
		KwsService kws = kwsProvider.getIfAvailable();
		if (kws == null) {
			throw new IllegalStateException("KWS 服务未启用，请设置 mica.voice.kws.enabled=true");
		}
		AudioData audio = AudioReaders.read(toTmp(file));
		List<KwsResult> hits = kws.spot(audio);
		List<Map<String, Object>> list = new ArrayList<>(hits.size());
		for (KwsResult r : hits) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("keyword", r.getKeyword());
			m.put("tokens", r.getTokens());
			m.put("timestamps", r.getTimestamps());
			list.add(m);
		}
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("total", list.size());
		resp.put("keywords", list);
		return resp;
	}
}
