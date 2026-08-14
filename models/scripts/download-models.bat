@echo off
rem =============================================================
rem 下载 mica-sherpa-onnx 测试所需的模型与测试音频（Windows 版）
rem 内部调用同目录下的 download-models.ps1 做多段并行下载
rem 用法: models\scripts\download-models.bat [asr|asr-online|asr-sensevoice|x-asr|tts|tts-fanchen|speaker|all]
rem   默认 all: 下载全部
rem   asr:            仅离线 ASR 模型（Paraformer zh small int8, ~79MB）
rem   asr-online:     仅在线流式 ASR 模型（Streaming Paraformer, ~230MB）
rem   asr-sensevoice: SenseVoice 多语言离线 ASR（中英日韩粤 + 情感/事件, ~228MB）
rem   x-asr:          X-ASR 在线流式 ASR（Zipformer Transducer 960ms, ~586MB）
rem   tts:            仅 TTS 模型（VITS icefall-aishell3, ~30MB）
rem   tts-fanchen:    TTS 大模型（VITS fanchen-C 187 说话人, ~290MB）
rem   speaker:        仅声纹模型 + 测试音频（~95MB）
rem 支持断点续传与多段并行，已完整下载的文件自动跳过。
rem =============================================================
setlocal
set "TARGET=%~1"
if "%TARGET%"=="" set "TARGET=all"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0download-models.ps1" -Target "%TARGET%"
endlocal
