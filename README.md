# mica-voice

[![Java CI](https://github.com/lets-mica/mica-voice/actions/workflows/test-and-build.yml/badge.svg)](https://github.com/lets-mica/mica-voice/actions/workflows/test-and-build.yml)
![JDK 8/17/21/25](https://img.shields.io/badge/JDK-8%20%7C%2017%20%7C%2021%20%7C%2025-informational)
[![Maven release](https://img.shields.io/maven-central/v/net.dreamlu/mica-voice-core.svg?style=flat-square)](https://central.sonatype.com/artifact/net.dreamlu/mica-voice-core/versions)
![Maven SNAPSHOT](https://img.shields.io/maven-metadata/v?metadataUrl=https://central.sonatype.com/repository/maven-snapshots/net/dreamlu/mica-voice-core/maven-metadata.xml)

> Java 生态的声音 AI 全家桶：ASR / TTS / 声纹 / VAD / 说话人分离 / 降噪 / KWS。
>
> 基于 [`mica-sherpa-onnx`](https://github.com/lets-mica/mica-sherpa-onnx)（已发布 Maven Central 的 sherpa-onnx 全平台 fat jar）。

[✨✨✨推广：**BladeX 物联网平台**✨✨✨iot.bladex.cn](https://iot.bladex.cn?from=mica-mqtt)

## 模块一览

| 模块 | 作用 |
| ---- | ---- |
| `mica-voice-core` | **核心 SDK**（纯 Java）。统一 ASR / TTS / 声纹 / VAD 等 API，强依赖 `mica-sherpa-onnx` |
| `mica-voice-spring-boot-starter` | **Spring Boot 自动装配**。`mica.voice.*` 配置即开箱即用，注入 `OfflineAsrService / OnlineAsrService / TtsService / SpeakerService` 等 9 个 Service Bean |
| `mica-voice-solon-plugin` | **Solon 自动装配插件**。同样的 `mica.voice.*` 配置树，一行接入 Solon 应用（JDK 8 ~ 26） |
| `mica-voice-examples` | **集成示例聚合**，展示 mica-voice 在真实业务场景下怎么用 |

### `mica-voice-examples` 子模块（使用示例）

| 子模块 | 作用 |
| ------ | ---- |
| `mica-voice-example-console` | 纯 Java main 示例：直接用 `mica-voice-core` 门面 + `models/` 模型，命令行测 ASR / SenseVoice / X-ASR / TTS / 声纹 / VAD / 分离 / KWS / 降噪 |
| `mica-voice-example-spring-web` | Spring Boot Web 示例：REST 上传 wav 做 ASR、GET 合成 wav、声纹注册/验证、**浏览器麦克风 → WebSocket → 流式 ASR**（`/mica/voice/ws/online-asr`） |
| `mica-voice-example-solon-web` | Solon Web 示例：REST 上传 wav 做 ASR、GET 合成 wav、声纹注册/验证（默认端口 8081） |

## 快速开始（Maven 依赖集成）

mica-voice 通过 Maven 依赖引入到你的项目，直接写代码即可，无需下载本仓库源码。

### 1. 添加依赖

**纯 Java**（非 Spring / Solon 项目）引入核心 SDK：

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-voice-core</artifactId>
    <version>${mica-voice.version}</version>
</dependency>
```

**Spring Boot** 项目引入 starter（自动装配，开箱即用）：

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-voice-spring-boot-starter</artifactId>
    <version>${mica-voice.version}</version>
</dependency>
```

**Solon** 项目引入 plugin（自动装配，开箱即用）：

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-voice-solon-plugin</artifactId>
    <version>${mica-voice.version}</version>
</dependency>
```

### 2. 准备模型

模型不随 jar 分发（体积大），按需下载到 `models/` 目录（模型默认从当前目录 `models/` 加载，也可用 `-Dmica.voice.models-dir=E:/.../models` 指定绝对路径）：

```bash
# Linux / macOS
bash models/scripts/download-models.sh asr          # 按需换 tts / speaker / asr-online / x-asr / all
bash models/scripts/parallel-download.sh all        # 并行下载，适合 all 全量时加速

# Windows（任选其一）
models\scripts\download-models.bat asr              # CMD
powershell -ExecutionPolicy Bypass -File models\scripts\download-models.ps1 asr   # PowerShell
```

> 完整 target 清单与下载说明见 [`models/README.md`](models/README.md)。VAD / 说话人分离 / KWS / 降噪模型暂无下载脚本 target，需自行从 sherpa-onnx release 下载到 `models/` 根目录。

### 3. 纯 Java：门面一行调用

```java
import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.MicaVoiceConfig;
import net.dreamlu.mica.voice.core.MicaVoice;

import java.io.File;

public class AsrDemo {

    public static void main(String[] args) {
        MicaVoiceConfig props = MicaVoiceConfig.builder()
            .modelsDir("models")   // 或 -Dmica.voice.models-dir 指定绝对路径
            .threads(2)
            .build();
        AsrConfig config = AsrConfig.builder()
            .modelDirName("sherpa-onnx-paraformer-zh-small-2024-03-09")
            .modelType(AsrConfig.ModelType.PARAFORMER)
            .build();

        try (AsrService svc = MicaVoice.asr(props, config)) {
            AsrResult result = svc.recognize(new File("test.wav"));
            System.out.println("识别结果: " + result.getText());
        }
    }
}
```

其他能力同理，**`MicaVoice` 门面** 一行拿到对应服务（均实现 `AutoCloseable`，强烈建议 try-with-resources）：

| 能力 | 门面方法 | 配置文件 |
| ---- | -------- | -------- |
| 离线 ASR | `MicaVoice.asr(props, AsrConfig)` | `AsrConfig` |
| 在线流式 ASR | `MicaVoice.onlineAsr(props, OnlineAsrConfig)` / `onlineAsrTyped(...)` | `OnlineAsrConfig` |
| 语音合成 TTS | `MicaVoice.tts(props, TtsConfig)` | `TtsConfig` |
| 声纹识别 | `MicaVoice.speaker(props, SpeakerConfig)` | `SpeakerConfig` |
| 语音活动检测 VAD | `MicaVoice.vad(props, VadConfig)` | `VadConfig` |
| 说话人分离 | `MicaVoice.diarization(props, DiarizationConfig)` | `DiarizationConfig` |
| 关键词唤醒 KWS | `MicaVoice.kws(props, KwsConfig)` | `KwsConfig` |
| 音频降噪 | `MicaVoice.denoise(props, DenoiseConfig)` | `DenoiseConfig` |
| 分离 + 转写 | `MicaVoice.transcribe(diarizationService, asrService)` | 组合 `DiarizationConfig` + `AsrConfig` |

### 4. Spring Boot：注入即用

`application.yml` 里配置模型，无需手动 new 服务。**全部可配置字段**如下（不常用的字段已注释并附说明，按需打开）：

```yaml
mica:
  voice:
    # ===================== 全局配置 =====================
    enabled: true                        # 全局总开关；false 时所有能力都不装配（默认 true）
    models-dir: ./models                 # 模型根目录（默认 ./models）
    output-dir: ./output                 # 输出目录（默认 ./output，不存在自动创建）
    threads: 2                           # 全局线程数（默认 2，各能力可单独覆盖）
    debug: false                         # 全局 debug 开关（默认 false，各能力可单独覆盖）

    # ===================== ASR =====================
    asr:
      # ---- 离线 ASR ----
      offline:
        enabled: true                    # 离线 ASR 开关（默认 true）
        model-dir-name: sherpa-onnx-paraformer-zh-small-2024-03-09  # 模型目录名（位于 models-dir 下）
        model-type: PARAFORMER           # 模型家族：PARAFORMER / SENSE_VOICE / WHISPER / MOONSHINE / ZIPFORMER / NEMO_CTC / AUTO
        # threads: 2                     # 覆盖全局线程数（不配置则用全局 threads）
        # debug: false                   # 覆盖全局 debug
        # language: auto                 # 语言（SenseVoice / Whisper 专用）：auto/zh/en/ja/ko/yue
        # inverse-text-normalization: true  # 逆文本规范化（SenseVoice 专用，还原数字/标点）
      # ---- 在线流式 ASR ----
      online:
        enabled: false                   # 在线流式 ASR 开关（默认 false，需下载流式模型）
        model-dir-name: x-asr-zh-en-chunk-960ms   # 流式模型目录名（X-ASR 或 Streaming Paraformer）
        model-type: X_ASR                # 模型家族：PARAFORMER / X_ASR / ZIPFORMER / ZIPFORMER2_CTC / NEMO_CTC / TRANSDUCER / AUTO
        # threads: 2                     # 覆盖全局线程数
        # debug: false                   # 覆盖全局 debug
        enable-endpoint: true            # 是否启用句尾静音自动结束（默认 true）
        # endpoint-rule1-min-trailing-silence: 2.4  # 端点规则 1：最短尾部静音（秒），调小可更快识别"说完了"
        # endpoint-rule2-min-trailing-silence: 1.2  # 端点规则 2：最短尾部静音（秒），兜底规则
        # endpoint-rule3-min-utterance-length: 0.2  # 端点规则 3：最短语音长度（秒），过短视为噪音不触发
        chunk-size: 1600                 # 流式分块大小（采样点数，默认 1600 ≈ 100ms @ 16kHz）

    # ===================== TTS =====================
    tts:
      enabled: true                      # TTS 开关（默认 true）
      model-dir-name: vits-icefall-zh-aishell3      # 纯中文模型；中英混合用 vits-melo-tts-zh_en
      model-type: VITS                   # 模型家族：VITS / MATCHA / KOKORO / AUTO
      default-speaker-id: 0              # 默认说话人 id（多说话人模型有效）
      default-speed: 1.0                 # 默认语速
      # threads: 2                       # 覆盖全局线程数
      # debug: false                     # 覆盖全局 debug
      # callback-sample-step: 1600       # 回调式合成时每多少采样回调一次（默认 1600 ≈ 100ms @ 16kHz）

    # ===================== 声纹识别 =====================
    speaker:
      enabled: true                      # 声纹识别开关（默认 true）
      threshold: 0.5                     # 相似度阈值（cosine），超过判定为同一人
      # threads: 2                       # 覆盖全局线程数
      # debug: false                     # 覆盖全局 debug
      # model-candidates:                # 候选模型名（按优先级查找，默认 3 个 eres2net / campplus 候选）
      #   - 3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx
      #   - 3dspeaker_speech_eres2net_base_sv_zh_en.onnx
      #   - 3dspeaker_speech_campplus_sv_zh_en.onnx
      # embedding-timeout-ms: 30000      # 提取嵌入时等待特征就绪的最大超时（毫秒）

    # ===================== VAD（需 silero_vad.onnx） =====================
    vad:
      enabled: false                     # VAD 开关（默认 false，开启后才装配 VadService）
      model-file-name: silero_vad.onnx   # VAD 模型文件名（silero_vad.onnx / ten_vad.onnx）
      model-type: SILERO                 # 模型家族：SILERO / TEN
      sample-rate: 16000                 # 模型要求的采样率
      threshold: 0.5                     # 触发语音的阈值（越高越严格）
      min-silence-duration: 0.5          # 最小静音时长（秒），超过判定为一句话结束
      min-speech-duration: 0.25          # 最小语音时长（秒），短于此视为噪声
      max-speech-duration: 20.0          # 语音最大时长（秒），超过则强制切分
      # threads: 2                       # 覆盖全局线程数
      # debug: false                     # 覆盖全局 debug
      # window-size: 512                 # SILERO 窗口大小（512 / 1024 / 1536 samples）

    # ===================== 说话人分离（需 segmentation + embedding 模型） =====================
    diarization:
      enabled: false                     # 分离开关（默认 false）
      segmentation-model-file-name: sherpa-onnx-pyannote-segmentation-3-0.onnx   # segmentation 模型
      embedding-model-file-name: 3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx  # embedding 模型
      # threads: 2                       # 覆盖全局线程数
      # debug: false                     # 覆盖全局 debug
      # num-clusters: 0                  # 期望说话人数（0 = 由 clustering 自动推断）
      # cluster-threshold: 0.5           # fast clustering 阈值
      # min-duration-off: 0.5            # 最小关闭时长（秒）：相邻两段合并的最大间隔
      # min-duration-on: 0.3             # 最小开启时长（秒）：短于此视为噪声丢弃

    # ===================== 关键词唤醒 KWS（需 kws 模型 + keywords.txt） =====================
    kws:
      enabled: false                     # KWS 开关（默认 false）
      model-dir-name: sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-23  # KWS 模型目录名
      # threads: 2                       # 覆盖全局线程数
      # debug: false                     # 覆盖全局 debug
      # sample-rate: 16000               # 特征采样率（推荐 16000）
      # feature-dim: 80                  # 特征维度
      # keywords-score: 2.0              # 关键词分数阈值
      # keywords-threshold: 0.25         # 关键词触发阈值
      # max-active-paths: 4              # 最大激活路径数
      # keywords-file: keywords.txt      # 关键词文件名（位于 model-dir-name 内）

    # ===================== 音频降噪（需 gtcrn / dpdfnet 模型） =====================
    denoise:
      enabled: false                     # 降噪开关（默认 false）
      model-file-name: sherpa-onnx-gtcrn.onnx   # 降噪模型文件名（gtcrn / dfpdfnet）
      model-type: GTCRN                  # 模型家族：GTCRN（轻量流式）/ DPDFNet（高质量离线）
      # threads: 2                       # 覆盖全局线程数
      # debug: false                     # 覆盖全局 debug
      # attenuation-limit-db: 12.0       # 仅 DPDFNet：衰减限制（dB），控制降噪强度
```

代码里直接注入 `OfflineAsrService` 即可（starter 自动装配 9 个 Service Bean，容器关闭时统一释放 native 资源）：

| Bean | 能力 |
| ---- | ---- |
| `OfflineAsrService` | 离线 ASR |
| `OnlineAsrService` | 在线流式 ASR |
| `TtsService` | 语音合成 |
| `SpeakerService` | 声纹识别 |
| `VadService` | 语音活动检测 |
| `DiarizationService` | 说话人分离 |
| `KwsService` | 关键词唤醒 |
| `DenoiseService` | 音频降噪 |
| `OfflineDiarizationTranscribeService` | 分离+转写 |

```java
@RestController
public class AsrController {

    private final OfflineAsrService offlineAsrService;

    public AsrController(OfflineAsrService offlineAsrService) {
        this.offlineAsrService = offlineAsrService;
    }

    @PostMapping("/asr")
    public String asr(@RequestParam("file") MultipartFile file) throws IOException {
        File tmp = File.createTempFile("asr", ".wav");
        file.transferTo(tmp);
        try {
            return offlineAsrService.recognize(tmp).getText();
        } finally {
            tmp.delete();
        }
    }
}
```

> 当前 ASR 两个 Bean 不再注册为统一的 `AsrService` 接口类型，
> 而是按各自的具体类型 `OfflineAsrService` / `OnlineAsrService` 暴露，
> 避免 Spring 容器出现同接口多 Bean 时的注入歧义。

### 5. Solon：注入即用

Solon 项目引入 `mica-voice-solon-plugin` 后，在 `app.yml`（或 `application.yml`）里使用**与 Spring Boot 完全相同的 `mica.voice.*` 配置树**，自动装配同款 9 个 Service Bean，用法与第 4 节一致：

```yaml
mica:
  voice:
    models-dir: ./models
    asr:
      offline:
        model-dir-name: sherpa-onnx-paraformer-zh-small-2024-03-09
        model-type: PARAFORMER
```

```java
@Controller
public class AsrController {

    @Inject
    private OfflineAsrService offlineAsrService;

    @PostMapping("/asr")
    public String asr(@Body MultipartFile file) throws IOException {
        File tmp = File.createTempFile("asr", ".wav");
        file.transferTo(tmp);
        try {
            return offlineAsrService.recognize(tmp).getText();
        } finally {
            tmp.delete();
        }
    }
}
```

> 完整 Solon 示例见 `mica-voice-examples/mica-voice-example-solon-web`。

## 关联项目

- [lets-mica/mica-sherpa-onnx](https://github.com/lets-mica/mica-sherpa-onnx) — sherpa-onnx 全平台 fat jar，本项目的 native 依赖来源
- [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) — 上游 ONNX 推理引擎与模型

## License

Apache License 2.0 — 详见 [LICENSE](LICENSE)。

## 微信

![如梦技术](docs/images/dreamlu-weixin.jpg)

**JAVA架构日记**，精彩内容每日推荐！