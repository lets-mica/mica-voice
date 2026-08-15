package net.dreamlu.mica.voice.tts;

import com.k2fsa.sherpa.onnx.*;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.config.ModelSelector;
import net.dreamlu.mica.voice.config.TtsConfig;
import net.dreamlu.mica.voice.exception.EngineException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * sherpa-onnx {@code OfflineTts} 的 mica-voice 适配（VITS）。
 *
 * <p>自动识别模型家族结构：
 * <ul>
 *     <li>HF 系（如 vits-zh-hf-fanchen-C）：model/lexicon/tokens 三个文件</li>
 *     <li>icefall 系（如 vits-icefall-zh-aishell3）：额外带 dict/ 子目录 + rule FST / FAR</li>
 *     <li>Melo 系（如 vits-melo-tts-zh_en）：dict/（jieba），自动走 MeloTtsLexicon 前端支持中英混合</li>
 * </ul>
 *
 * <p>还会自动发现并启用模型目录下的 rule FST（{@code phone.fst / date.fst / number.fst / new_heteronym.fst}）
 * 与 FAR 归档（{@code rule.far}），解决 icefall 系合成时数字 / 日期 / 部分英文被当 OOV 丢弃的问题
 * （典型日志：{@code ConvertTextToTokenIdsChinese ... OOV ... Ignore it!}）。
 *
 * <p><b>关于英文合成</b>：Melo 系模型（{@code vits-melo-tts-zh_en}）自带 jieba 词典且 lexicon.txt 收录了常用英文词，
 * 是当前 mica-voice 默认推荐的中英混合 TTS 模型；纯中文场景下体积更小的 {@code vits-icefall-zh-aishell3} 即可。
 *
 * @author dreamlu
 */
@Slf4j
public class OfflineTtsService implements TtsService {

	private static final String[] MODEL_CANDIDATES = {
		"vits-zh-hf-fanchen-C.onnx", "model.onnx", "model.int8.onnx"
	};

	/**
	 * 中文 TTS 模型（vits-icefall-zh-* / vits-zh-hf-fanchen-C / vits-melo-tts-zh_en 等）随包自带的文本归一化 FST。
	 * 缺失任意一个都可能导致英文/数字/日期读音失败（表现为日志中大量 OOV 警告）。
	 */
	private static final String[] RULE_FST_CANDIDATES = {
		"phone.fst", "date.fst", "number.fst", "new_heteronym.fst"
	};

	/**
	 * 中文 TTS 模型随包自带的 FAR 归档规则集（如 icefall-zh-aishell3 下的 rule.far），
	 * 里面打包了更多 itn / 标点等规则。
	 */
	private static final String[] RULE_FAR_CANDIDATES = {
		"rule.far"
	};

	private final MicaVoiceConfig props;
	private final TtsConfig config;
	private final OfflineTts tts;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/**
	 * 构造离线 TTS 服务。
	 *
	 * @param props  全局 mica-voice 配置
	 * @param config TTS 配置
	 */
	public OfflineTtsService(MicaVoiceConfig props, TtsConfig config) {
		this.props = props;
		this.config = config;

		if (config.getModelDirName() == null) {
			throw new IllegalArgumentException("TtsConfig.modelDirName 不能为空");
		}
		String modelDir = ModelSelector.resolveModelDir(props.getModelsDir(), config.getModelDirName());
		if (modelDir == null) {
			// 兜底抛错：找不到目录
			ModelSelector.resolveModelFile(props.getModelsDir(), config.getModelDirName(), MODEL_CANDIDATES);
			// 理论上走不到这里
			throw new IllegalStateException("模型目录不存在: " + config.getModelDirName());
		}
		String modelPath = ModelSelector.resolveInDir(modelDir, MODEL_CANDIDATES);
		String lexicon = ModelSelector.resolveInDir(modelDir, "lexicon.txt");
		String tokens = ModelSelector.resolveInDir(modelDir, "tokens.txt");
		if (modelPath == null || lexicon == null || tokens == null) {
			throw new EngineException("TTS 模型目录缺少必要文件 (model/lexicon/tokens): " + modelDir);
		}

		int threads = config.getThreads() != null ? config.getThreads() : props.getThreads();
		boolean debug = config.isDebug() || props.isDebug();

		OfflineTtsVitsModelConfig.Builder vitsBuilder = OfflineTtsVitsModelConfig.builder()
			.setModel(modelPath)
			.setLexicon(lexicon)
			.setTokens(tokens);

		// icefall 系模型自带 dict/ 子目录，需额外设置 dictDir。
		// 注意：sherpa-onnx 1.12+ 的 Validate() 在 data_dir 设置时会要求 data_dir/phontab 等
		// piper 必备文件都存在，而 icefall/melo 模型不带这些文件，会直接 return false 抛
		// "Invalid OfflineTtsConfig"。因此这里只设 dictDir（dict_dir 在 1.12+ 已废弃，
		// 设了 sherpa-onnx 会打 "you don't need to provide dict_dir" 警告，但不会报错）；
		// dataDir 只在确认包含 phontab 时才设置（piper / coqui / inflect 模型）。
		File dictDir = new File(modelDir, "dict");
		if (dictDir.isDirectory()) {
			vitsBuilder.setDictDir(dictDir.getAbsolutePath());
			File phontab = new File(dictDir, "phontab");
			if (phontab.isFile()) {
				vitsBuilder.setDataDir(dictDir.getAbsolutePath());
				log.info("TTS 检测到 piper 系 dict（含 phontab），启用 dataDir: {}", dictDir.getAbsolutePath());
			} else {
				log.info("TTS 检测到 dict 目录（jieba / icefall）：{}", dictDir.getAbsolutePath());
			}
		}

		OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
			.setVits(vitsBuilder.build())
			.setNumThreads(threads)
			.setDebug(debug)
			.build();

		OfflineTtsConfig.Builder cfgBuilder = OfflineTtsConfig.builder()
			.setModel(modelConfig);

		// 自动发现中文文本归一化 FST（phone/date/number/new_heteronym）。
		// 缺少这些规则时，sherpa-onnx 走纯 lexicon 查表，英文/数字会全部 OOV，
		// 现象就是日志里 "ConvertTextToTokenIdsChinese ... OOV ... Ignore it!" 连环刷屏、合成出来的英文段缺失。
		List<String> ruleFsts = resolveRuleFiles(modelDir, RULE_FST_CANDIDATES);
		if (!ruleFsts.isEmpty()) {
			String joined = String.join(",", ruleFsts);
			cfgBuilder.setRuleFsts(joined);
			log.info("TTS 启用 ruleFsts (modelDir={}): {}", modelDir, joined);
		} else {
			log.warn("TTS 模型目录缺少 rule FST（phone/date/number 等），英文/数字可能无法合成。目录: {}", modelDir);
		}

		// 自动发现 FAR 归档规则集（如 rule.far），里面通常包含更多 itn / 标点等规则。
		List<String> ruleFars = resolveRuleFiles(modelDir, RULE_FAR_CANDIDATES);
		if (!ruleFars.isEmpty()) {
			String joined = String.join(",", ruleFars);
			cfgBuilder.setRuleFars(joined);
			log.info("TTS 启用 ruleFars (modelDir={}): {}", modelDir, joined);
		}

		OfflineTtsConfig cfg = cfgBuilder.build();

		try {
			this.tts = new OfflineTts(cfg);
		} catch (Throwable t) {
			throw new EngineException("初始化 OfflineTts 失败（model=" + modelPath + "）", t);
		}
		log.info("OfflineTtsService 初始化完成: model={}, sampleRate={}Hz, numSpeakers={}",
			modelPath, tts.getSampleRate(), tts.getNumSpeakers());
	}

	/**
	 * 在模型目录里按优先级查找存在的 rule FST 文件，返回按 {@link #RULE_FST_CANDIDATES} 顺序拼接好的列表。
	 *
	 * @param modelDir   模型目录绝对路径
	 * @param candidates 候选文件名数组（按优先级）
	 * @return 找到的文件绝对路径列表（顺序与 candidates 一致）
	 */
	private static List<String> resolveRuleFiles(String modelDir, String[] candidates) {
		List<String> found = new ArrayList<>(candidates.length);
		File dir = new File(modelDir);
		for (String name : candidates) {
			File f = new File(dir, name);
			if (f.isFile()) {
				found.add(f.getAbsolutePath());
			}
		}
		return found;
	}

	@Override
	public TtsAudio synthesize(String text) {
		return synthesize(text, config.getDefaultSpeakerId(), config.getDefaultSpeed());
	}

	@Override
	public TtsAudio synthesize(String text, int speakerId) {
		return synthesize(text, speakerId, config.getDefaultSpeed());
	}

	@Override
	public TtsAudio synthesize(String text, int speakerId, float speed) {
		ensureOpen();
		long start = System.currentTimeMillis();
		try {
			GeneratedAudio a = tts.generate(text, speakerId, speed);
			long cost = System.currentTimeMillis() - start;
			return new TtsAudio(a.getSamples(), tts.getSampleRate(), speakerId, speed, cost);
		} catch (Throwable t) {
			throw new EngineException("TTS 合成失败: " + t.getMessage(), t);
		}
	}

	@Override
	public TtsAudio synthesizeWithCallback(String text, Consumer<float[]> callback) {
		ensureOpen();
		long start = System.currentTimeMillis();
		int step = config.getCallbackSampleStep() <= 0 ? 1600 : config.getCallbackSampleStep();
		try {
			AtomicInteger acc = new AtomicInteger(0);
			GeneratedAudio a = tts.generateWithCallback(text, config.getDefaultSpeakerId(),
				config.getDefaultSpeed(), samples -> {
					int total = acc.addAndGet(samples.length);
					// 只在累计达到 step 时回调一次（避免微回调刷屏）
					if (total / step > (total - samples.length) / step && callback != null) {
						callback.accept(samples);
					}
				});
			long cost = System.currentTimeMillis() - start;
			return new TtsAudio(a.getSamples(), tts.getSampleRate(),
				config.getDefaultSpeakerId(), config.getDefaultSpeed(), cost);
		} catch (Throwable t) {
			throw new EngineException("TTS 回调式合成失败: " + t.getMessage(), t);
		}
	}

	@Override
	public int getSampleRate() {
		ensureOpen();
		return tts.getSampleRate();
	}

	@Override
	public int getNumSpeakers() {
		ensureOpen();
		return tts.getNumSpeakers();
	}

	/**
	 * 把 TtsAudio 写成 wav 文件。
	 *
	 * @param audio   TTS 合成结果
	 * @param outFile 目标 wav 文件
	 * @return 是否成功保存
	 */
	public boolean saveWav(TtsAudio audio, File outFile) {
		ensureOpen();
		try {
			GeneratedAudio generated = new GeneratedAudio(audio.getSamples(), audio.getSampleRate());
			return generated.save(outFile.getAbsolutePath());
		} catch (Throwable t) {
			throw new EngineException("保存 wav 失败: " + outFile.getAbsolutePath(), t);
		}
	}

	/**
	 * 确保服务未被关闭，否则抛出 IllegalStateException。
	 */
	private void ensureOpen() {
		if (closed.get()) {
			throw new IllegalStateException("OfflineTtsService 已关闭");
		}
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				tts.release();
			} catch (Throwable t) {
				log.warn("关闭 OfflineTts 失败: {}", t.getMessage());
			}
		}
	}
}
