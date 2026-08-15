@echo off
rem =============================================================
rem 下载 mica-sherpa-onnx 测试所需的模型与测试音频（Windows 版）
rem 内部调用同目录下的 download-models.ps1 做多段并行下载
rem 用法: models\scripts\download-models.bat [TARGET]
rem   Target 一览：
rem     asr             离线 ASR（Paraformer zh small int8, ~79MB）
rem     asr-online      在线流式 ASR（Streaming Paraformer, ~230MB）
rem     asr-sensevoice  SenseVoice 多语言离线 ASR（中英日韩粤 + 情感/事件, ~228MB）
rem     x-asr           X-ASR 在线流式 ASR（Zipformer Transducer 960ms, ~586MB）
rem     tts             TTS（VITS icefall-aishell3 单说话人, ~30MB）
rem     tts-fanchen     TTS 大模型（VITS fanchen-C 187 说话人, ~290MB）
rem     tts-zh-en       TTS 中英混合（VITS Melo + jieba + espeak, ~310MB）
rem     speaker         声纹模型 + 测试音频（~95MB）
rem     vad             SILERO VAD（~1MB，单文件）
rem     denoise         GTCRN 降噪（~523KB，单文件）
rem     denoise-dpdfnet DPDFNet 降噪（高质量离线）
rem     kws             关键词唤醒（~30MB）
rem     diarization     说话人分离（segmentation ~5.7MB）
rem     all             全部
rem   默认 all
rem 支持断点续传与多段并行，已完整下载的文件自动跳过。
rem =============================================================
setlocal
set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=all"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0download-models.ps1" -Target "%TARGET%"
endlocal