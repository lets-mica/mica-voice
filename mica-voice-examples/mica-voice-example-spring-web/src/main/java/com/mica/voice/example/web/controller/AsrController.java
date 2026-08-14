package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.AsrService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
@RestController
@RequestMapping("/mica/voice/asr")
@RequiredArgsConstructor
public class AsrController {

	private final AsrService asrService;

	private static File toTempFile(MultipartFile file) {
		try {
			File tmp = File.createTempFile("mica-voice-asr-", ".wav");
			tmp.deleteOnExit();
			file.transferTo(tmp);
			return tmp;
		} catch (Exception e) {
			throw new IllegalArgumentException("无法保存上传文件: " + e.getMessage(), e);
		}
	}

	@PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, Object> recognize(@RequestParam("file") MultipartFile file) {
		AsrResult r = asrService.recognize(toTempFile(file));
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
