package com.mica.voice.example.web.ws;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SentenceBuffer} 切句规则单元测试。
 *
 * @author dreamlu
 */
class SentenceBufferTest {

	@Test
	void singleChineseSentence() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("你好世界。");
		List<String> out = buffer.pollSentences();
		assertThat(out).containsExactly("你好世界。");
		assertThat(buffer.hasPending()).isFalse();
	}

	@Test
	void multipleChineseSentencesSplitAtEachTerminator() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("第一句！第二句？第三句。");
		assertThat(buffer.pollSentences())
			.containsExactly("第一句！", "第二句？", "第三句。");
	}

	@Test
	void ellipsisAndSemicolonAreTerminators() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("等等……好；那我们继续。");
		// 连续 "……" 合并为同一个句尾，不切成孤立省略号
		assertThat(buffer.pollSentences())
			.containsExactly("等等……", "好；", "那我们继续。");
	}

	@Test
	void englishSentenceSplit() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("Hello world. Next one! What?");
		assertThat(buffer.pollSentences())
			.containsExactly("Hello world.", "Next one!", "What?");
	}

	@Test
	void decimalPointDoesNotSplit() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("圆周率是3.14。版本号v1.2.3发布了！");
		assertThat(buffer.pollSentences())
			.containsExactly("圆周率是3.14。", "版本号v1.2.3发布了！");
	}

	@Test
	void newlineIsTerminator() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("第一行\n第二行\n");
		assertThat(buffer.pollSentences()).containsExactly("第一行", "第二行");
	}

	@Test
	void trailingQuoteJoinedIntoSentence() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("他说：“你好。”然后走了。");
		assertThat(buffer.pollSentences())
			.containsExactly("他说：“你好。”", "然后走了。");
	}

	@Test
	void incompleteSentenceKeptInBuffer() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("这句话还没写");
		assertThat(buffer.pollSentences()).isEmpty();
		assertThat(buffer.hasPending()).isTrue();

		buffer.append("完，继续。下一句");
		assertThat(buffer.pollSentences()).containsExactly("这句话还没写完，继续。");
		assertThat(buffer.hasPending()).isTrue();
	}

	@Test
	void tokenStreamingAccumulatesAcrossAppends() {
		SentenceBuffer buffer = new SentenceBuffer();
		// 模拟 LLM 逐 token 输出
		for (String token : new String[]{"你", "好", "呀", "！", "这", "是", "流", "式", "合", "成", "。"}) {
			buffer.append(token);
		}
		assertThat(buffer.pollSentences()).containsExactly("你好呀！", "这是流式合成。");
		assertThat(buffer.hasPending()).isFalse();
	}

	@Test
	void flushSettlesResidualText() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("没有标点收尾的残句");
		assertThat(buffer.pollSentences()).isEmpty();

		List<String> rest = buffer.flushPending();
		assertThat(rest).containsExactly("没有标点收尾的残句");
		assertThat(buffer.hasPending()).isFalse();
	}

	@Test
	void flushWithQuotesAndParens() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("他说（未闭合");
		assertThat(buffer.flushPending()).containsExactly("他说（未闭合");
	}

	@Test
	void blankAndNullInputIgnored() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append(null);
		buffer.append("");
		buffer.append("   ");
		assertThat(buffer.pollSentences()).isEmpty();
		assertThat(buffer.hasPending()).isFalse();
	}

	@Test
	void blankFragmentsBetweenSentencesSkipped() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("甲。   \n\n乙。");
		assertThat(buffer.pollSentences()).containsExactly("甲。", "乙。");
	}

	@Test
	void whitespaceOnlyTextNotEmittedAsSentence() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append(" \n\n ");
		assertThat(buffer.pollSentences()).isEmpty();
		assertThat(buffer.hasPending()).isFalse();
	}

	@Test
	void overlongSentenceForceSplitAtLastComma() {
		SentenceBuffer buffer = new SentenceBuffer();
		// 250 个字符无终结符，中间 120 处放一个中文逗号 → 应在逗号处切断
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 120; i++) {
			sb.append('啊');
		}
		sb.append('，');
		for (int i = 0; i < 129; i++) {
			sb.append('呀');
		}
		buffer.append(sb.toString());
		List<String> out = buffer.pollSentences();
		assertThat(out).hasSize(1);
		assertThat(out.get(0)).endsWith("，");
		// 剩余残句保留在缓冲中，等待后续文本/flush
		assertThat(buffer.hasPending()).isTrue();
		assertThat(buffer.flushPending()).hasSize(1);
		assertThat(buffer.hasPending()).isFalse();
	}

	@Test
	void mixedTextKeepsOrder() {
		SentenceBuffer buffer = new SentenceBuffer();
		buffer.append("你好。I am mica. 一起加油！");
		assertThat(buffer.pollSentences())
			.containsExactly("你好。", "I am mica.", "一起加油！");
	}
}
