package net.dreamlu.mica.voice.config;

import lombok.experimental.UtilityClass;
import net.dreamlu.mica.voice.exception.ModelNotFoundException;

import java.io.File;

/**
 * 模型路径解析工具。
 *
 * <p>核心规则：
 * <ol>
 *     <li>在 {@code modelsDir} 下找到名为 {@code dirName} 的子目录；找不到则抛 {@link ModelNotFoundException}</li>
 *     <li>在子目录内按优先级依次查找 {@code candidates}，返回第一个存在的文件</li>
 *     <li>没有候选时返回子目录路径本身（用于 TTS 整目录场景）</li>
 * </ol>
 *
 * <p>典型 int8-优先-fp32 兜底：{@code resolveModelFile(modelsDir, "sherpa-onnx-paraformer-zh-small-2024-03-09", "model.int8.onnx", "model.onnx")}。
 *
 * @author dreamlu
 */
@UtilityClass
public class ModelSelector {

	/**
	 * 解析子目录绝对路径。找不到返回 null。
	 *
	 * @param modelsDir 模型根目录
	 * @param dirName   子目录名
	 * @return 子目录绝对路径；找不到返回 null
	 */
	public String resolveModelDir(File modelsDir, String dirName) {
		if (modelsDir == null || !modelsDir.isDirectory() || dirName == null) {
			return null;
		}
		File dir = new File(modelsDir, dirName);
		return dir.isDirectory() ? dir.getAbsolutePath() : null;
	}

	/**
	 * 解析模型目录 + 在目录内按候选名查找第一个存在的文件，返回绝对路径；找不到抛 {@link ModelNotFoundException}。
	 *
	 * @param modelsDir  模型根目录
	 * @param dirName    子目录名
	 * @param candidates 按优先级排列的文件候选名
	 * @return 第一个存在的文件绝对路径
	 */
	public String resolveModelFile(File modelsDir, String dirName, String... candidates) {
		String dir = resolveModelDir(modelsDir, dirName);
		if (dir == null) {
			throw new ModelNotFoundException(dirName, candidates);
		}
		if (candidates == null || candidates.length == 0) {
			return dir;
		}
		for (String name : candidates) {
			File f = new File(dir, name);
			if (f.isFile()) {
				return f.getAbsolutePath();
			}
		}
		throw new ModelNotFoundException(dirName, candidates);
	}

	/**
	 * 同 {@link #resolveModelFile(File, String, String...)}，但找不到时不抛异常，直接返回 null。
	 *
	 * @param modelsDir  模型根目录
	 * @param dirName    子目录名
	 * @param candidates 按优先级排列的文件候选名
	 * @return 第一个存在的文件绝对路径；不存在返回 null
	 */
	public String tryResolveModelFile(File modelsDir, String dirName, String... candidates) {
		String dir = resolveModelDir(modelsDir, dirName);
		if (dir == null) {
			return null;
		}
		if (candidates == null || candidates.length == 0) {
			return dir;
		}
		for (String name : candidates) {
			File f = new File(dir, name);
			if (f.isFile()) {
				return f.getAbsolutePath();
			}
		}
		return null;
	}

	/**
	 * 在指定目录里按候选名查找第一个存在的文件。
	 *
	 * @param dir        目录绝对路径
	 * @param candidates 按优先级排列的文件候选名
	 * @return 第一个存在的文件绝对路径；都不存在返回 null
	 */
	public String resolveInDir(String dir, String... candidates) {
		if (dir == null || candidates == null) {
			return null;
		}
		for (String name : candidates) {
			File f = new File(dir, name);
			if (f.isFile()) {
				return f.getAbsolutePath();
			}
		}
		return null;
	}
}
