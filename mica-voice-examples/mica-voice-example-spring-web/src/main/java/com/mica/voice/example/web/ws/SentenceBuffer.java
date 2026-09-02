package com.mica.voice.example.web.ws;

import java.util.ArrayList;
import java.util.List;

/**
 * 流式 TTS 的文本切句缓冲器。
 *
 * <p>职责：把 LLM 逐 token 吐出的文本增量累积到 {@code pending}，一旦凑出一个完整句子
 * （以句末标点/换行收尾）就切出来返回，实现"收到一个完整句子就立即合成出声"。
 *
 * <p>切句规则（中英混排友好）：
 * <ul>
 *     <li>句子终结符：{@code 。！？…；} + 英文 {@code . ! ? ;} + 换行 {@code \n}，终结符保留在句尾随句合成</li>
 *     <li>英文小数点 / 版本号（{@code 3.14}、{@code config.yml}）不切句：{@code .} 后面紧跟数字或字母时不当作终结符</li>
 *     <li>句尾引号/括号（{@code 。”}、{@code ！'}）并入前一句，不单独成句</li>
 *     <li>空白片段跳过不入队</li>
 *     <li>单句长度上限 {@link #MAX_SENTENCE_LEN} 字符，防止无标点长文本占死队列：超限后强制在最后一个逗号类标点处切断</li>
 * </ul>
 *
 * <p>线程模型：每个 WS 连接一个实例，只在收文本的 IO 线程访问，无并发、无需加锁。
 *
 * @author dreamlu
 */
public class SentenceBuffer {

	/**
	 * 中文句子终结符。'…' 连续出现（如"……"）会合并成同一个句尾，见 {@link #pollSentences()}。
	 */
	private static final String CN_TERMINATORS = "。！？…；";
	/**
	 * 英文句子终结符（'.' 有小数点/缩写保护，见 {@link #isSentenceEnd}）。
	 */
	private static final String EN_TERMINATORS = ".!?;";
	/**
	 * 句尾可并入前一句的收尾符号（成对出现的右引号/右括号等）。
	 */
	private static final String TAIL_CHARS = "”’』」）】〉》\"')]}>";
	/**
	 * 逗号类次级切分点：单句超长时优先在此处切断（比硬切更自然）。
	 */
	private static final String COMMA_CHARS = "，,、；;";
	/**
	 * 单句最大长度（字符）。超出且无终结符时强制切分，避免"一个超长句占死整条流水线"。
	 */
	static final int MAX_SENTENCE_LEN = 200;

	/**
	 * 待切分文本缓冲（仅 IO 线程访问）。
	 */
	private final StringBuilder pending = new StringBuilder();

	/**
	 * 追加一段文本增量（LLM 输出的一个 token 或一小段），不立即返回任何句子；
	 * 由 {@link #pollSentences()} 统一结算。
	 *
	 * @param text 文本增量
	 */
	public void append(String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		pending.append(text);
	}

	/**
	 * 是否还有未结算的文本。
	 *
	 * @return true 表示缓冲非空
	 */
	public boolean hasPending() {
		return pending.length() > 0;
	}

	/**
	 * 把当前缓冲中所有"完整句子"切出来并清出缓冲；末尾不完整的残句保留在缓冲内，
	 * 等下一次增量或 {@link #flushPending()} 时再处理。
	 *
	 * @return 完整句子列表（可能为空），不含换行/首尾空白
	 */
	public List<String> pollSentences() {
		List<String> sentences = new ArrayList<>();
		int from = 0;
		int i = from;
		while (i < pending.length()) {
			char c = pending.charAt(i);
			if (isSentenceEnd(i, c)) {
				// 连续同字符终结符（"……"、"！！"）合并成同一个句尾，避免切出孤立残片
				int end = i + 1;
				while (end < pending.length() && pending.charAt(end) == c) {
					end++;
				}
				// 句尾引号/括号（。” 等）并入前一句
				while (end < pending.length() && isTailChar(pending.charAt(end))) {
					end++;
				}
				String sentence = clean(pending.substring(from, end));
				if (!sentence.isEmpty()) {
					sentences.add(sentence);
				}
				from = end;
				i = end;
				continue;
			}
			if (i - from >= MAX_SENTENCE_LEN) {
				// 超长无标点文本：在最后一个逗号类标点处强制切断，避免占死队列
				int cut = lastComma(from, i);
				String sentence = clean(pending.substring(from, cut + 1));
				if (!sentence.isEmpty()) {
					sentences.add(sentence);
				}
				from = cut + 1;
				i = from;
				continue;
			}
			i++;
		}
		if (from > 0) {
			pending.delete(0, from);
		}
		dropIfBlankOnly();
		return sentences;
	}

	/**
	 * flush 结算：把缓冲内残余文本（含无标点收尾、引号未闭合等）作为最后一句强制切出并清空缓冲。
	 * 之后本缓冲即可接收新一轮文本。
	 *
	 * @return 残余文本组成的句子列表（可能为空）
	 */
	public List<String> flushPending() {
		List<String> sentences = new ArrayList<>();
		if (pending.length() > 0) {
			String sentence = clean(pending.toString());
			if (!sentence.isEmpty()) {
				sentences.add(sentence);
			}
			pending.setLength(0);
		}
		return sentences;
	}

	/**
	 * 判断位置 {@code i} 的字符是否为句子终结符。
	 *
	 * <p>特殊规则：{@code '.'} 仅在后面紧跟空白/引号/结尾时才作终结符，
	 * 避免把 {@code 3.14}、{@code config.yml}、{@code v1.2} 切开。
	 *
	 * @param i 字符位置
	 * @param c 字符
	 * @return true 表示此处句子结束
	 */
	private boolean isSentenceEnd(int i, char c) {
		if (CN_TERMINATORS.indexOf(c) >= 0 || EN_TERMINATORS.indexOf(c) >= 0 || c == '\n') {
			if (c == '.') {
				// '.' 后面紧跟数字/字母（3.14、Mr.、config.yml 等）不切
				char next = i + 1 < pending.length() ? pending.charAt(i + 1) : ' ';
				if (Character.isLetterOrDigit(next)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * 是否句尾收尾符号（右引号/右括号），可并入前一句。
	 *
	 * @param c 字符
	 * @return true 表示收尾符号
	 */
	private boolean isTailChar(char c) {
		return TAIL_CHARS.indexOf(c) >= 0;
	}

	/**
	 * 从 {@code from}（含）到 {@code to}（含）范围内找最后一个逗号类标点。
	 *
	 * @param from 起点
	 * @param to   终点（含）
	 * @return 位置；找不到返回 {@code to}（原样硬切）
	 */
	private int lastComma(int from, int to) {
		for (int i = to; i >= from; i--) {
			if (COMMA_CHARS.indexOf(pending.charAt(i)) >= 0) {
				return i;
			}
		}
		return to;
	}

	/**
	 * 清理成"可入队句子"：去掉首尾空白与换行。
	 *
	 * @param raw 原始片段
	 * @return 清理后的句子
	 */
	private String clean(String raw) {
		return raw.trim();
	}

	/**
	 * 若缓冲只剩空白/换行（无实际内容），直接清空，避免空壳句子滞留。
	 */
	private void dropIfBlankOnly() {
		for (int i = 0; i < pending.length(); i++) {
			if (!Character.isWhitespace(pending.charAt(i))) {
				return;
			}
		}
		pending.setLength(0);
	}
}
