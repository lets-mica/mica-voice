package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import net.dreamlu.mica.voice.tts.TtsAudio;
import net.dreamlu.mica.voice.tts.TtsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 极简示例 Controller（{@code /demo/*}）。
 *
 * <p>和 {@code /mica/voice/*} 完整端点对照演示怎么直接注入 starter装配好的 Service Bean。
 *
 * @author dreamlu
 */
@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

	private final AsrService asrService;
	private final TtsService ttsService;
	private final SpeakerService speakerService;

	private static File toTmp(MultipartFile file) throws Exception {
		File tmp = File.createTempFile("demo-", ".wav");
		tmp.deleteOnExit();
		file.transferTo(tmp);
		return tmp;
	}

	@PostMapping("/asr")
	public Map<String, Object> asr(@RequestParam("file") MultipartFile file) throws Exception {
		AsrResult r = asrService.recognize(toTmp(file));
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("text", r.getText());
		resp.put("costMs", r.getCostMs());
		return resp;
	}

	@GetMapping("/tts")
	public Map<String, Object> tts(@RequestParam("text") String text) {
		TtsAudio audio = ttsService.synthesize(text);
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("samples", audio.getSamples().length);
		resp.put("sampleRate", audio.getSampleRate());
		resp.put("durationSec", audio.durationSeconds());
		resp.put("costMs", audio.getCostMs());
		return resp;
	}

	@PostMapping("/speaker/enroll")
	public Map<String, Object> enroll(@RequestParam("name") String name,
									  @RequestParam("file") MultipartFile file) throws Exception {
		speakerService.enroll(name, toTmp(file));
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("name", name);
		resp.put("ok", true);
		return resp;
	}
}
