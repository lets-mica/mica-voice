# mica-voice

![Java](https://img.shields.io/badge/Java-8%2B-orange)

> Java 生态的声音 AI 全家桶：ASR / TTS / 声纹 / VAD / 说话人分离 / 降噪 / KWS。
>
> 基于 [`mica-sherpa-onnx`](https://github.com/lets-mica/mica-sherpa-onnx)（已发布 Maven Central 的 sherpa-onnx 全平台 fat jar）。

## 模块一览

| 模块 | 作用 |
| ---- | ---- |
| `mica-voice-core` | **核心 SDK**（纯 Java）。统一 ASR / TTS / 声纹 / VAD 等 API，强依赖 `mica-sherpa-onnx` |
| `mica-voice-spring-boot-starter` | **Spring Boot 自动装配**。`mica.voice.*` 配置即开箱即用，注入 `AsrService / TtsService / SpeakerService` Bean |
| `mica-voice-examples` | **集成示例聚合**，展示 mica-voice 在真实业务场景下怎么用 |

### `mica-voice-examples` 子模块（按需展开）

| 子模块 | 作用 |
| ------ | ---- |
| `mica-voice-example-console` | 纯 Java main 示例：直接用 `mica-voice-core` 门面 + `models/` 模型，命令行测 ASR / SenseVoice / X-ASR / TTS / 声纹 |
| `mica-voice-example-spring-web` | Spring Boot Web 示例：REST 上传 wav 做 ASR、GET 合成 wav、声纹注册/验证、**浏览器麦克风 → WebSocket → 流式 ASR**（`/mica/voice/ws/online-asr`） |

## 快速开始（Maven 依赖集成）

mica-voice 通过 Maven 依赖引入到你的项目，直接写代码即可，无需下载本仓库源码。

### 1. 添加依赖

**纯 Java**（非 Spring 项目）引入核心 SDK：

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-voice-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Spring Boot** 项目引入 starter（自动装配，开箱即用）：

```xml
<dependency>
    <groupId>net.dreamlu</groupId>
    <artifactId>mica-voice-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> **发布状态**：`mica-voice-core` / `mica-voice-spring-boot-starter` 当前为 `1.0.0-SNAPSHOT`，尚未发布 Maven Central。正式版发布前需先从源码本地安装（底层 native 依赖 [`mica-sherpa-onnx`](https://github.com/lets-mica/mica-sherpa-onnx) 1.13.5 已发布 Central，无需额外处理）：
>
> ```bash
> git clone https://github.com/lets-mica/mica-voice.git
> cd mica-voice
> mvn -pl mica-voice-core,mica-voice-spring-boot-starter -am install -DskipTests
> ```

### 2. 准备模型

模型不随 jar 分发（体积大），按需下载到 `models/` 目录（模型默认从当前目录 `models/` 加载，也可用 `-Dmica.voice.models-dir=E:/.../models` 指定绝对路径）：

```bash
# Linux / macOS
bash models/scripts/download-models.sh asr          # 按需换 tts / speaker / vad / denoise / kws / diarization / all
bash models/scripts/parallel-download.sh all        # 并行下载，适合 all 全量时加速

# Windows（任选其一）
models\scripts\download-models.bat asr              # CMD
powershell -ExecutionPolicy Bypass -File models\scripts\download-models.ps1 asr   # PowerShell
```

### 3. 纯 Java：门面一行调用

```java
import net.dreamlu.mica.voice.asr.AsrResult;
import net.dreamlu.mica.voice.asr.AsrService;
import net.dreamlu.mica.voice.config.AsrConfig;
import net.dreamlu.mica.voice.config.MicaVoiceProperties;
import net.dreamlu.mica.voice.core.MicaVoice;

import java.io.File;

public class AsrDemo {

    public static void main(String[] args) {
        MicaVoiceProperties props = MicaVoiceProperties.builder()
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

`application.yml` 里配置模型，无需手动 new 服务：

```yaml
mica:
  voice:
    models-dir: models
    asr:
      offline:
        model-dir-name: sherpa-onnx-paraformer-zh-small-2024-03-09
        model-type: PARAFORMER
```

代码里直接注入 `AsrService` 即可（starter 自动装配 8 个 Service Bean，容器关闭时统一释放 native 资源）：

| Bean | 能力 | 对应 starter 类 |
| ---- | ---- | --------------- |
| `AsrService` | 离线 ASR | `AsrAutoConfiguration` |
| `OnlineAsrService` | 在线流式 ASR | `AsrAutoConfiguration` |
| `TtsService` | 语音合成 | `TtsAutoConfiguration` |
| `SpeakerService` | 声纹识别 | `SpeakerAutoConfiguration` |
| `VadService` | 语音活动检测 | `VadAutoConfiguration` |
| `DiarizationService` | 说话人分离 | `DiarizationAutoConfiguration` |
| `KwsService` | 关键词唤醒 | `KwsAutoConfiguration` |
| `DenoiseService` | 音频降噪 | `DenoiseAutoConfiguration` |

```java
@RestController
public class AsrController {

    private final AsrService asrService;

    public AsrController(AsrService asrService) {
        this.asrService = asrService;
    }

    @PostMapping("/asr")
    public String asr(@RequestParam("file") MultipartFile file) throws IOException {
        File tmp = File.createTempFile("asr", ".wav");
        file.transferTo(tmp);
        try {
            return asrService.recognize(tmp).getText();
        } finally {
            tmp.delete();
        }
    }
}
```

## 源码示例

想直接跑现成 demo，可克隆仓库运行：

```bash
git clone https://github.com/lets-mica/mica-voice.git
cd mica-voice

# 控制台示例（纯 Java main，测 ASR / SenseVoice / X-ASR / TTS / 声纹）
mvn -pl mica-voice-examples/mica-voice-example-console -am package -DskipTests
java -jar mica-voice-examples/mica-voice-example-console/target/mica-voice-example-console-1.0.0-SNAPSHOT.jar all

# Spring Web 示例（REST + WebSocket 流式 ASR）
mvn -pl mica-voice-examples/mica-voice-example-spring-web -am package -DskipTests
java -jar mica-voice-examples/mica-voice-example-spring-web/target/mica-voice-example-spring-web-1.0.0-SNAPSHOT.jar
# 或开发时热启：cd mica-voice-examples/mica-voice-example-spring-web && mvn spring-boot:run
# 默认监听 http://localhost:8080，模型目录用 -Dmica.voice.models-dir=E:/.../models 指定绝对路径
```

各功能的完整调用示例见 `mica-voice-examples/mica-voice-example-console/src/main/java/com/mica/voice/example/console/` 下的 `AsrExample` / `SenseVoiceExample` / `OnlineAsrExample` / `TtsExample` / `SpeakerExample`。

## 关联项目

- [lets-mica/mica-sherpa-onnx](https://github.com/lets-mica/mica-sherpa-onnx) — sherpa-onnx 全平台 fat jar，本项目的 native 依赖来源
- [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) — 上游 ONNX 推理引擎与模型

## License

Apache License 2.0 — 详见 [LICENSE](LICENSE)。