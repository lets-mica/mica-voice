package net.dreamlu.mica.voice.exception;

import lombok.Getter;

import java.util.Arrays;

/**
 * 模型文件/目录缺失异常。
 *
 * <p>当根据配置在 {@code models/} 目录下找不到对应模型目录，或模型目录里缺少必需的
 * 关键文件（如 model.onnx / tokens.txt / lexicon.txt 等）时抛出。
 *
 * @author dreamlu
 */
@Getter
public class ModelNotFoundException extends MicaVoiceException {

	private static final long serialVersionUID = 1L;

	/**
	 * 模型目录名称（如 sherpa-onnx-paraformer-zh-small-2024-03-09）
	 */
	private final String modelDirName;

	/**
	 * 期望的模型文件候选名（按优先级），用于在错误信息里提示用户
	 */
	private final String[] candidates;

	/**
	 * @param modelDirName 期望的模型目录名
	 * @param candidates   按优先级排列的候选模型文件名（可为 null）
	 */
	public ModelNotFoundException(String modelDirName, String[] candidates) {
		super(buildMessage(modelDirName, candidates));
		this.modelDirName = modelDirName;
		this.candidates = candidates == null ? new String[0] : candidates.clone();
	}

	/**
	 * 构建人类可读的错误信息，包含目录名、候选文件名以及下载提示。
	 */
	private static String buildMessage(String modelDirName, String[] candidates) {
		StringBuilder sb = new StringBuilder("找不到模型目录: ").append(modelDirName);
		if (candidates != null && candidates.length > 0) {
			sb.append("（按优先级依次查找: ").append(Arrays.toString(candidates)).append("）");
		}
		sb.append("，请先运行 models/scripts/download-models.sh 下载，或检查 mica.voice.models-dir 配置");
		return sb.toString();
	}

	/**
	 * 获取候选文件名的防御性拷贝。
	 *
	 * @return 候选文件名数组
	 */
	public String[] getCandidates() {
		return candidates.clone();
	}

	@Override
	public String getMessage() {
		return buildMessage(modelDirName, candidates);
	}
}
