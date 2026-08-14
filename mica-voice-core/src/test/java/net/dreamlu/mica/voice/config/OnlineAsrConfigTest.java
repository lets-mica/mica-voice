package net.dreamlu.mica.voice.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OnlineAsrConfig} 单元测试（不需要真模型，只测 POJO + Builder + 映射逻辑）。
 *
 * @author dreamlu
 */
class OnlineAsrConfigTest {

    @Test
    void defaults() {
        OnlineAsrConfig c = new OnlineAsrConfig();
        assertEquals(OnlineAsrConfig.ModelType.PARAFORMER, c.getModelType());
        assertEquals(1600, c.getChunkSize());
        assertEquals(16000, c.getSampleRate());
        assertEquals(80, c.getFeatureDim());
        assertTrue(c.isEnableEndpoint());
        assertEquals("greedy_search", c.getDecodingMethod());
        assertEquals(0, c.getThreads() != null ? c.getThreads() : 0);
        assertFalse(c.isDebug());
        // 默认 encoder/decoder/joiner 候选名
        assertNotNull(c.getEncoderCandidates());
        assertEquals("encoder-960ms.onnx", c.getEncoderCandidates()[0]);
        assertEquals("encoder.int8.onnx", c.getEncoderCandidates()[1]);
        assertEquals("encoder.onnx", c.getEncoderCandidates()[2]);
        assertEquals("decoder-960ms.onnx", c.getDecoderCandidates()[0]);
        assertEquals("joiner-960ms.onnx", c.getJoinerCandidates()[0]);
    }

    @Test
    void constructorWithName() {
        OnlineAsrConfig c = new OnlineAsrConfig("x-asr-zh-en-chunk-960ms");
        assertEquals("x-asr-zh-en-chunk-960ms", c.getModelDirName());
    }

    @Test
    void builderBasic() {
        OnlineAsrConfig c = OnlineAsrConfig.builder()
                .modelDirName("x-asr-zh-en-chunk-960ms")
                .modelType(OnlineAsrConfig.ModelType.X_ASR)
                .threads(4)
                .debug(true)
                .enableEndpoint(false)
                .chunkSize(2400)
                .sampleRate(16000)
                .featureDim(80)
                .decodingMethod("modified_beam_search")
                .build();
        assertEquals("x-asr-zh-en-chunk-960ms", c.getModelDirName());
        assertEquals(OnlineAsrConfig.ModelType.X_ASR, c.getModelType());
        assertEquals(4, c.getThreads());
        assertTrue(c.isDebug());
        assertFalse(c.isEnableEndpoint());
        assertEquals(2400, c.getChunkSize());
        assertEquals(16000, c.getSampleRate());
        assertEquals(80, c.getFeatureDim());
        assertEquals("modified_beam_search", c.getDecodingMethod());
    }

    @Test
    void builderStringModelType() {
        // X_ASR（注意下划线 + 大小写不敏感）
        OnlineAsrConfig c1 = OnlineAsrConfig.builder().modelType("X_ASR").build();
        assertEquals(OnlineAsrConfig.ModelType.X_ASR, c1.getModelType());

        OnlineAsrConfig c2 = OnlineAsrConfig.builder().modelType("x_asr").build();
        assertEquals(OnlineAsrConfig.ModelType.X_ASR, c2.getModelType());

        OnlineAsrConfig c3 = OnlineAsrConfig.builder().modelType("paraformer").build();
        assertEquals(OnlineAsrConfig.ModelType.PARAFORMER, c3.getModelType());

        OnlineAsrConfig c4 = OnlineAsrConfig.builder().modelType("auto").build();
        assertEquals(OnlineAsrConfig.ModelType.AUTO, c4.getModelType());
    }

    @Test
    void builderStringModelType_emptyOrUnknown() {
        // 空字符串 / null → AUTO
        OnlineAsrConfig c1 = OnlineAsrConfig.builder().modelType("").build();
        assertEquals(OnlineAsrConfig.ModelType.AUTO, c1.getModelType());

        OnlineAsrConfig c2 = OnlineAsrConfig.builder().modelType((String) null).build();
        assertEquals(OnlineAsrConfig.ModelType.AUTO, c2.getModelType());

        // 无法识别的字符串 → AUTO（不抛异常）
        OnlineAsrConfig c3 = OnlineAsrConfig.builder().modelType("not_a_real_model").build();
        assertEquals(OnlineAsrConfig.ModelType.AUTO, c3.getModelType());
    }

    @Test
    void setModelType_nullBecomesAuto() {
        OnlineAsrConfig c = new OnlineAsrConfig();
        c.setModelType(null);
        assertEquals(OnlineAsrConfig.ModelType.AUTO, c.getModelType());
    }

    @Test
    void toSherpaModelType_xAsr() {
        OnlineAsrConfig c = OnlineAsrConfig.builder()
                .modelType(OnlineAsrConfig.ModelType.X_ASR).build();
        assertEquals("zipformer2", c.toSherpaModelType());
    }

    @Test
    void toSherpaModelType_zipformer2Ctc() {
        OnlineAsrConfig c = OnlineAsrConfig.builder()
                .modelType(OnlineAsrConfig.ModelType.ZIPFORMER2_CTC).build();
        assertEquals("zipformer2", c.toSherpaModelType());
    }

    @Test
    void toSherpaModelType_zipformer() {
        OnlineAsrConfig c = OnlineAsrConfig.builder()
                .modelType(OnlineAsrConfig.ModelType.ZIPFORMER).build();
        assertEquals("zipformer", c.toSherpaModelType());
    }

    @Test
    void toSherpaModelType_paraformer() {
        // Paraformer 由 setParaformer() 体现，setModelType 不必指定 → 返回 null
        OnlineAsrConfig c = OnlineAsrConfig.builder()
                .modelType(OnlineAsrConfig.ModelType.PARAFORMER).build();
        assertNull(c.toSherpaModelType());
    }

    @Test
    void toSherpaModelType_transducerAutoNemo() {
        for (OnlineAsrConfig.ModelType t : Arrays.asList(
                OnlineAsrConfig.ModelType.TRANSDUCER,
                OnlineAsrConfig.ModelType.AUTO,
                OnlineAsrConfig.ModelType.NEMO_CTC)) {
            OnlineAsrConfig c = OnlineAsrConfig.builder().modelType(t).build();
            assertNull(c.toSherpaModelType(), "modelType=" + t + " 应返回 null（不调用 setModelType）");
        }
    }

    @Test
    void setEncoderCandidates_nullBecomesEmpty() {
        OnlineAsrConfig c = new OnlineAsrConfig();
        c.setEncoderCandidates(null);
        assertEquals(0, c.getEncoderCandidates().length);
    }

    @Test
    void setCandidates_clonesArray() {
        String[] arr = {"a.onnx", "b.onnx"};
        OnlineAsrConfig c = new OnlineAsrConfig();
        c.setEncoderCandidates(arr);
        // 修改原数组不应影响 Config 内部数组
        arr[0] = "hacked.onnx";
        assertEquals("a.onnx", c.getEncoderCandidates()[0]);
    }

    @Test
    void builderCustomCandidates() {
        String[] enc = {"custom-encoder.onnx"};
        String[] dec = {"custom-decoder.onnx"};
        String[] join = {"custom-joiner.onnx"};
        OnlineAsrConfig c = OnlineAsrConfig.builder()
                .encoderCandidates(enc)
                .decoderCandidates(dec)
                .joinerCandidates(join)
                .build();
        assertArrayEquals(new String[]{"custom-encoder.onnx"}, c.getEncoderCandidates());
        assertArrayEquals(new String[]{"custom-decoder.onnx"}, c.getDecoderCandidates());
        assertArrayEquals(new String[]{"custom-joiner.onnx"}, c.getJoinerCandidates());
    }

    @Test
    void builderEmptyVarargs() {
        // 给空 varargs（不会 NPE）
        OnlineAsrConfig c = OnlineAsrConfig.builder()
                .encoderCandidates(new String[0])
                .decoderCandidates(new String[0])
                .joinerCandidates(new String[0])
                .build();
        assertEquals(0, c.getEncoderCandidates().length);
        assertEquals(0, c.getDecoderCandidates().length);
        assertEquals(0, c.getJoinerCandidates().length);
    }

    @Test
    void allModelTypeEnumValues() {
        // 确保所有枚举值都能设置/读取，且 toSherpaModelType 不抛
        for (OnlineAsrConfig.ModelType t : OnlineAsrConfig.ModelType.values()) {
            OnlineAsrConfig c = OnlineAsrConfig.builder().modelType(t).build();
            assertEquals(t, c.getModelType());
            // 不抛异常即可
            c.toSherpaModelType();
        }
    }

    @Test
    void chunkSizeZeroIsNegativeWhenSet() {
        // chunkSize=0 是非法值，但不抛——由 Service 层兜底；这里只验证 setter
        OnlineAsrConfig c = OnlineAsrConfig.builder().chunkSize(0).build();
        assertEquals(0, c.getChunkSize());
    }
}