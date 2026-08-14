package com.mica.voice.example.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.speaker.SearchResult;
import net.dreamlu.mica.voice.speaker.SpeakerService;
import net.dreamlu.mica.voice.speaker.VerificationResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
@RestController
@RequestMapping("/mica/voice/speaker")
@RequiredArgsConstructor
public class SpeakerController {

	private final SpeakerService speakerService;

	private static File toTempFile(MultipartFile file) {
		try {
			File tmp = File.createTempFile("mica-voice-spk-", ".wav");
			tmp.deleteOnExit();
			file.transferTo(tmp);
			return tmp;
		} catch (Exception e) {
			throw new IllegalArgumentException("无法保存上传文件: " + e.getMessage(), e);
		}
	}

	@PostMapping("/enroll")
	public Map<String, Object> enroll(@RequestParam("name") String name,
									  @RequestParam("file") MultipartFile file) {
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

	@PostMapping("/verify")
	public VerificationResult verify(@RequestParam("name") String name,
									 @RequestParam("file") MultipartFile file) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("name 不能为空");
		}
		return speakerService.verify(name, toTempFile(file));
	}

	@PostMapping("/search")
	public SearchResult search(@RequestParam("file") MultipartFile file) {
		return speakerService.search(toTempFile(file));
	}

	@GetMapping("/names")
	public List<String> names() {
		return speakerService.names();
	}

	@DeleteMapping("/{name}")
	public Map<String, Object> remove(@PathVariable("name") String name) {
		boolean removed = speakerService.remove(name);
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("name", name);
		resp.put("removed", removed);
		resp.put("total", speakerService.size());
		return resp;
	}
}
