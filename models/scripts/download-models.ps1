<#
.SYNOPSIS
    并行分段下载 sherpa-onnx 测试模型（SenseVoice / X-ASR / ASR / TTS / 声纹 / VAD / KWS / Diarization / Denoise）。

.DESCRIPTION
    Windows PowerShell 下载脚本：
      - 内置多段并行下载（默认 4 段），比单线程快数倍
      - 支持断点续传（已完成的分段自动跳过）
      - 单段 600 秒超时 + 低速检测，避免连接卡死
      - 下载源：SenseVoice / X-ASR 走 HuggingFace（hf-mirror.com 镜像），其余走 GitHub
      - .tar.bz2 走 Windows 自带 tar.exe 解压
      - 解压后会把 KWS / Diarization 实际文件名软链/复制到 mica-voice 配置默认期望的命名

.PARAMETER Target
    下载目标：sensevoice / x-asr / asr / asr-online / tts / tts-fanchen / tts-zh-en /
              speaker / vad / denoise / denoise-dpdfnet / kws / diarization /
              all（默认 all）

.PARAMETER Parts
    并行分段数（默认 4）

.PARAMETER Mirror
    HuggingFace 镜像源：hf-mirror（默认，国内快）或 huggingface（官方）

.EXAMPLE
    .\models\scripts\download-models.ps1 -Target sensevoice
    .\models\scripts\download-models.ps1 -Target x-asr -Parts 6
    .\models\scripts\download-models.ps1 -Target vad
#>
param(
    [string]$Target = "all",
    [int]$Parts = 4,
    [string]$Mirror = "hf-mirror"
)

$ErrorActionPreference = 'Stop'
# 脚本位于 models/scripts/ 下，模型目录 = scripts 的父目录（即 models/）
$ModelsDir = Split-Path -Parent $PSScriptRoot
New-Item -ItemType Directory -Force -Path $ModelsDir | Out-Null

# HuggingFace 镜像基址
if ($Mirror -eq "huggingface") {
    $HF = "https://huggingface.co"
} else {
    $HF = "https://hf-mirror.com"
}

# ---------------------------------------------------------------------------
# 分段下载一个文件
# ---------------------------------------------------------------------------
function Download-File {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$OutFile,
        [int]$Parts = 4
    )
    # 先把要下载的地址打出来，方便用户确认（特别是 hf-mirror 走代理或被劫持时一眼能看出）
    Write-Host "  [url] $Url"
    $file = Get-Item -LiteralPath $OutFile -ErrorAction SilentlyContinue
    $need = $true

    # 获取总大小（跟随重定向）
    $total = 0
    for ($try = 0; $try -lt 3; $try++) {
        try {
            $head = Invoke-WebRequest -Uri $Url -Method Head -UseBasicParsing -TimeoutSec 60 -MaximumRedirection 5
            $cl = $head.Headers['Content-Length']
            if ($cl -is [array]) { $cl = $cl[0] }   # PS 5.1 中 Header 值可能是数组
            $total = [long]$cl
            break
        } catch {
            Write-Host "  [warn] HEAD 失败($($_.Exception.Message))，重试 $try/3"
            Start-Sleep -Seconds 2
        }
    }

    # 已完整下载则跳过（HEAD 拿到了大小，本地文件大小一致）
    # 这个判断必须在「小文件/HEAD 拿不到大小」分支之前，否则 50MB 以下或 HEAD 被 CDN 拒绝的
    # 文件每次都会重下。
    if ($total -gt 0 -and $file -and $file.Length -eq $total) {
        Write-Host "[skip] $OutFile 已完整下载 ($([math]::Round($total/1MB,1)) MB)"
        return
    }
    if ($total -gt 0 -and $file -and $file.Length -gt $total) {
        Write-Host "  [warn] 本地文件大于远端大小，删除后重下"
        Remove-Item -LiteralPath $OutFile -Force
        $file = $null
    }

    if ($total -le 0) {
        # 拿不到大小（部分 CDN 对 HEAD 不返回 Content-Length）。
        # 本地已有非空文件就先跳过——宁可少下一次也不要覆盖用户已经下好的东西。
        if ($file -and $file.Length -gt 0) {
            Write-Host "  [skip] 无法 HEAD 验证大小，但本地文件已存在 ($([math]::Round($file.Length/1KB,1)) KB)，跳过"
            return
        }
        Write-Host "  [info] 无法获取大小，直接下载..."
        Invoke-WebRequest -Uri $Url -OutFile $OutFile -UseBasicParsing -TimeoutSec 600 -MaximumRedirection 5
        if (Test-Path -LiteralPath $OutFile) {
            Write-Host "[ok] $OutFile ($([math]::Round((Get-Item -LiteralPath $OutFile).Length/1KB,1)) KB)"
        }
        return
    }

    # 小文件（< 50MB）直接整体下载，不分段
    if ($total -lt 50MB) {
        Write-Host "  [info] 小文件 $([math]::Round($total/1MB,1)) MB，直接下载..."
        Invoke-WebRequest -Uri $Url -OutFile $OutFile -UseBasicParsing -TimeoutSec 600 -MaximumRedirection 5
        Write-Host "[ok] $OutFile ($([math]::Round((Get-Item -LiteralPath $OutFile).Length/1MB,1)) MB)"
        return
    }

    $partSize = [long][math]::Ceiling($total / $Parts)
    $numParts = [int][math]::Ceiling($total / $partSize)
    Write-Host "[down] $([math]::Round($total/1MB,1)) MB -> $numParts 段 x $([math]::Round($partSize/1MB,1)) MB"

    # 并发下载各分段（Start-Job 隔离，互不影响）
    $jobs = @()
    for ($i = 0; $i -lt $numParts; $i++) {
        $start = $i * $partSize
        $end = [math]::Min($start + $partSize - 1, $total - 1)
        $partFile = "$OutFile.part$i"
        $partLen = $end - $start + 1

        # 断点续传：已完整下载的分段跳过，未完成的从断点继续
        $have = 0
        if (Test-Path -LiteralPath $partFile) {
            $have = (Get-Item -LiteralPath $partFile).Length
        }
        if ($have -ge $partLen) {
            Write-Host "  [skip] 分段 $i 已完成"
            continue
        }
        $range = "bytes=$($start + $have)-$end"
        $job = Start-Job -ScriptBlock {
            param($u, $o, $r)
            $ErrorActionPreference = 'Stop'
            for ($try = 0; $try -lt 3; $try++) {
                try {
                    Add-Type -AssemblyName System.Net.Http
                    $handler = New-Object System.Net.Http.HttpClientHandler
                    $handler.AllowAutoRedirect = $true
                    $client = New-Object System.Net.Http.HttpClient($handler)
                    $client.Timeout = [TimeSpan]::FromSeconds(600)
                    $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Get, $u)
                    $req.Headers.Range = [System.Net.Http.Headers.RangeHeaderValue]::Parse($r)
                    $resp = $client.SendAsync($req, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).Result
                    $resp.EnsureSuccessStatusCode()
                    $in = $resp.Content.ReadAsStreamAsync().Result
                    $fs = [System.IO.File]::Open($o, [System.IO.FileMode]::Append)
                    try { $in.CopyTo($fs) } finally { $fs.Dispose(); $in.Dispose(); $resp.Dispose(); $client.Dispose() }
                    return 0
                } catch {
                    Write-Host "  [warn] 分段重试 $try/3: $($_.Exception.Message)"
                    Start-Sleep -Seconds 3
                }
            }
            return 1
        } -ArgumentList $Url, $partFile, $range
        $jobs += $job
    }

    $fail = 0
    foreach ($job in $jobs) {
        Wait-Job $job -Timeout 700 | Out-Null
        $code = Receive-Job $job
        if ($code -ne 0) { $fail++ }
        Remove-Job $job -Force
    }
    if ($fail -gt 0) {
        throw "有 $fail 个分段下载失败: $OutFile（重跑本脚本可断点续传）"
    }

    # 校验各分段并合并
    $fs = [System.IO.File]::Create($OutFile)
    try {
        for ($i = 0; $i -lt $numParts; $i++) {
            $partFile = "$OutFile.part$i"
            $expected = [math]::Min(($i + 1) * $partSize, $total) - $i * $partSize
            $actual = (Get-Item -LiteralPath $partFile).Length
            if ($actual -ne $expected) {
                throw "分段 $i 大小不符: $actual != $expected"
            }
            $buf = New-Object byte[] (4 * 1024 * 1024)
            $in = [System.IO.File]::OpenRead($partFile)
            try {
                while (($n = $in.Read($buf, 0, $buf.Length)) -gt 0) {
                    $fs.Write($buf, 0, $n)
                }
            } finally {
                $in.Dispose()
            }
            Remove-Item -LiteralPath $partFile -Force
        }
    } finally {
        $fs.Dispose()
    }
    Write-Host "[ok] $OutFile ($([math]::Round((Get-Item -LiteralPath $OutFile).Length/1MB,1)) MB)"
}

# ---------------------------------------------------------------------------
# 解压后把 KWS / Diarization 文件复制到 mica-voice 默认期望的命名
# （兼容 Windows 无管理员权限时无法创建符号链接的情况，统一用 Copy-Item）
# ---------------------------------------------------------------------------
function Ensure-Like-File {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Target
    )
    if (Test-Path -LiteralPath $Source) {
        if (-not (Test-Path -LiteralPath $Target)) {
            Copy-Item -LiteralPath $Source -Destination $Target -Force
            Write-Host "  [link] $Target <- $Source"
        } else {
            Write-Host "  [skip] $Target 已存在"
        }
    } else {
        Write-Host "  [warn] 源文件不存在，无法生成 $Target : $Source"
    }
}

# ---------------------------------------------------------------------------
# 下载定义
# ---------------------------------------------------------------------------
$GITHUB = "https://github.com/k2-fsa/sherpa-onnx/releases/download"

$targets = @{
    # SenseVoice 多语言离线 ASR（中英日韩粤 + 情感/事件），HF 官方镜像
    sensevoice = @(
        @{ url = "$HF/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/model.int8.onnx";   out = "$ModelsDir/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/model.int8.onnx" },
        @{ url = "$HF/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/tokens.txt";        out = "$ModelsDir/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/tokens.txt" },
        @{ url = "$HF/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/test_wavs/zh.wav";  out = "$ModelsDir/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/test_wavs/zh.wav" },
        @{ url = "$HF/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/test_wavs/yue.wav"; out = "$ModelsDir/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/test_wavs/yue.wav" },
        @{ url = "$HF/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/test_wavs/en.wav";  out = "$ModelsDir/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/test_wavs/en.wav" },
        @{ url = "$HF/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/test_wavs/ja.wav";  out = "$ModelsDir/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/test_wavs/ja.wav" },
        @{ url = "$HF/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main/test_wavs/ko.wav";  out = "$ModelsDir/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/test_wavs/ko.wav" }
    )
    # X-ASR 中英流式 ASR（Zipformer transducer，960ms 分块），HF 官方仓库
    "x-asr" = @(
        @{ url = "$HF/GilgameshWind/X-ASR-zh-en/resolve/main/deployment/models/chunk-960ms-model/encoder-960ms.onnx"; out = "$ModelsDir/x-asr-zh-en-chunk-960ms/encoder-960ms.onnx" },
        @{ url = "$HF/GilgameshWind/X-ASR-zh-en/resolve/main/deployment/models/chunk-960ms-model/decoder-960ms.onnx"; out = "$ModelsDir/x-asr-zh-en-chunk-960ms/decoder-960ms.onnx" },
        @{ url = "$HF/GilgameshWind/X-ASR-zh-en/resolve/main/deployment/models/chunk-960ms-model/joiner-960ms.onnx";  out = "$ModelsDir/x-asr-zh-en-chunk-960ms/joiner-960ms.onnx" },
        @{ url = "$HF/GilgameshWind/X-ASR-zh-en/resolve/main/deployment/models/chunk-960ms-model/tokens.txt";        out = "$ModelsDir/x-asr-zh-en-chunk-960ms/tokens.txt" }
    )
    # 离线 ASR（GitHub）
    asr = @(
        @{ url = "$GITHUB/asr-models/sherpa-onnx-paraformer-zh-small-2024-03-09.tar.bz2"; out = "$ModelsDir/sherpa-onnx-paraformer-zh-small-2024-03-09.tar.bz2" }
    )
    # 在线流式 ASR（GitHub）
    "asr-online" = @(
        @{ url = "$GITHUB/asr-models/sherpa-onnx-streaming-paraformer-bilingual-zh-en.tar.bz2"; out = "$ModelsDir/sherpa-onnx-streaming-paraformer-bilingual-zh-en.tar.bz2" }
    )
    # TTS（GitHub）
    tts = @(
        @{ url = "$GITHUB/tts-models/vits-icefall-zh-aishell3.tar.bz2"; out = "$ModelsDir/vits-icefall-zh-aishell3.tar.bz2" }
    )
    # TTS 大模型（GitHub，187 说话人）
    "tts-fanchen" = @(
        @{ url = "$GITHUB/tts-models/vits-zh-hf-fanchen-C.tar.bz2"; out = "$ModelsDir/vits-zh-hf-fanchen-C.tar.bz2" }
    )
    # TTS 中英混合模型（GitHub，melo-tts，jieba + espeak-ng-data）
    "tts-zh-en" = @(
        @{ url = "$GITHUB/tts-models/vits-melo-tts-zh_en.tar.bz2"; out = "$ModelsDir/vits-melo-tts-zh_en.tar.bz2" }
    )
    # 声纹（GitHub）
    speaker = @(
        @{ url = "$GITHUB/speaker-recongition-models/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx"; out = "$ModelsDir/3dspeaker_speech_eres2net_base_sv_zh-cn_3dspeaker_16k.onnx" },
        @{ url = "$GITHUB/speaker-segmentation-models/0-four-speakers-zh.wav"; out = "$ModelsDir/0-four-speakers-zh.wav" }
    )
    # VAD：SILERO VAD 单文件，放 models/ 根
    vad = @(
        @{ url = "$GITHUB/asr-models/silero_vad.onnx"; out = "$ModelsDir/silero_vad.onnx" }
    )
    # Denoise GTCRN：speech-enhancement-models 单文件，保存为 mica-voice 默认名
    denoise = @(
        @{ url = "$GITHUB/speech-enhancement-models/gtcrn_simple.onnx"; out = "$ModelsDir/sherpa-onnx-gtcrn.onnx" }
    )
    # Denoise DPDFNet：高质量离线降噪，16kHz baseline
    "denoise-dpdfnet" = @(
        @{ url = "$GITHUB/speech-enhancement-models/dpdfnet_baseline.onnx"; out = "$ModelsDir/sherpa-onnx-dpdfnet.onnx" }
    )
    # KWS：下载 tarball 解压；解压后会把 epoch/chunk 后缀的文件复制成 mica-voice 默认期望的 encoder/decoder/joiner/tokens.txt
    #   mica-voice KwsConfig 默认 modelDirName = sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01
    #   KwsConfig 默认 keywordsFile   = keywords.txt（位于 modelDirName 内）
    kws = @(
        @{ url = "$GITHUB/kws-models/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01.tar.bz2"; out = "$ModelsDir/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01.tar.bz2" }
    )
    # 说话人分离：下载 tarball，解压后把 model.onnx 复制成 mica-voice 默认期望的单文件命名
    #   mica-voice DiarizationConfig 默认 segmentationModelFileName = sherpa-onnx-pyannote-segmentation-3-0.onnx（位于 models/ 根）
    #   embedding 模型由 `speaker` target 负责
    diarization = @(
        @{ url = "$GITHUB/speaker-segmentation-models/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2"; out = "$ModelsDir/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2" }
    )
}

# 选择目标（兼容 asr-sensevoice 别名）
$selected = @()
if ($Target -eq "asr-sensevoice") { $Target = "sensevoice" }
if ($Target -eq "all") {
    foreach ($k in @("sensevoice", "x-asr", "asr", "asr-online", "tts", "tts-fanchen", "tts-zh-en", "speaker", "vad", "denoise", "kws", "diarization")) {
        $selected += $targets[$k]
    }
} elseif ($targets.ContainsKey($Target)) {
    $selected = $targets[$Target]
} else {
    Write-Host "未知目标: $Target"
    Write-Host "可选: asr-sensevoice / sensevoice / x-asr / asr / asr-online / tts / tts-fanchen / tts-zh-en /"
    Write-Host "      speaker / vad / denoise / denoise-dpdfnet / kws / diarization / all"
    exit 1
}

# 解压后需额外处理的 target（先下载/解压，再生成 mica-voice 默认期望的软链/副本）
$postExtractTargets = @{
    # KWS：把 epoch/chunk 后缀文件复制成 encoder/decoder/joiner.onnx + keywords.txt
    kws = {
        $dir = Join-Path $ModelsDir "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01"
        Ensure-Like-File -Source (Join-Path $dir "encoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx") -Target (Join-Path $dir "encoder.int8.onnx")
        Ensure-Like-File -Source (Join-Path $dir "encoder-epoch-12-avg-2-chunk-16-left-64.onnx")      -Target (Join-Path $dir "encoder.onnx")
        Ensure-Like-File -Source (Join-Path $dir "decoder-epoch-12-avg-2-chunk-16-left-64.onnx")      -Target (Join-Path $dir "decoder.onnx")
        Ensure-Like-File -Source (Join-Path $dir "joiner-epoch-12-avg-2-chunk-16-left-64.int8.onnx")  -Target (Join-Path $dir "joiner.int8.onnx")
        Ensure-Like-File -Source (Join-Path $dir "joiner-epoch-12-avg-2-chunk-16-left-64.onnx")       -Target (Join-Path $dir "joiner.onnx")
        # keywords.txt（默认位于 modelDirName 内；原包内是 test_wavs/test_keywords.txt）
        Ensure-Like-File -Source (Join-Path $dir "test_wavs/test_keywords.txt") -Target (Join-Path $dir "keywords.txt")
    }
    # Diarization：把 tarball 解压目录里的 model.onnx 复制成 mica-voice 默认期望的根目录单文件
    diarization = {
        $dir = Join-Path $ModelsDir "sherpa-onnx-pyannote-segmentation-3-0"
        Ensure-Like-File -Source (Join-Path $dir "model.onnx")     -Target (Join-Path $ModelsDir "sherpa-onnx-pyannote-segmentation-3-0.onnx")
        Ensure-Like-File -Source (Join-Path $dir "model.int8.onnx") -Target (Join-Path $ModelsDir "sherpa-onnx-pyannote-segmentation-3-0.int8.onnx")
    }
}

foreach ($item in $selected) {
    $dir = Split-Path -Parent $item.out
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Write-Host ""
    Write-Host "==> 下载: $(Split-Path -Leaf $item.out)"
    Write-Host "    URL : $($item.url)"
    Write-Host "    目标: $($item.out)"

    # .tar.bz2 已经被解压过（脚本解压完会删源 tarball），就直接跳过整段下载。
    # 否则每次重跑都会重新拉一遍大文件。
    if ($item.out.EndsWith(".tar.bz2")) {
        $stem = [System.IO.Path]::GetFileNameWithoutExtension([System.IO.Path]::GetFileNameWithoutExtension($item.out))
        $extractDir = Join-Path $ModelsDir $stem
        if ((Test-Path -LiteralPath $extractDir) -and `
            ((Get-ChildItem -LiteralPath $extractDir -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count -gt 0)) {
            # 清掉上次未跑完留下来的 .part* 残片，避免下次误判或占空间
            $partPattern = [System.IO.Path]::GetFileName($item.out) + ".part*"
            Get-ChildItem -LiteralPath $ModelsDir -Filter $partPattern -ErrorAction SilentlyContinue | ForEach-Object {
                Write-Host "  [clean] 清理残留分片: $($_.Name)"
                Remove-Item -LiteralPath $_.FullName -Force
            }
            Write-Host "  [skip] 解压目录已存在且非空: $extractDir，跳过下载"
            continue
        }
    }

    Download-File -Url $item.url -OutFile $item.out -Parts $Parts
}

# 处理需要先下载/解压 .tar.bz2 的 target
$bz2Targets = @("asr", "asr-online", "tts", "tts-fanchen", "tts-zh-en", "kws", "diarization")
if ($bz2Targets -contains $Target) {
    foreach ($item in $selected) {
        if ($item.out.EndsWith(".tar.bz2")) {
            $archiveFile = $item.out
            $extractDir = Join-Path $ModelsDir ([System.IO.Path]::GetFileNameWithoutExtension([System.IO.Path]::GetFileNameWithoutExtension($archiveFile)))
            if (Test-Path -LiteralPath $archiveFile) {
                Write-Host ""
                Write-Host "[unpack] $archiveFile -> $extractDir"
                & tar -xjf "$archiveFile" -C "$ModelsDir"
                Remove-Item -LiteralPath $archiveFile -Force
            } elseif (Test-Path -LiteralPath $extractDir) {
                Write-Host "[skip] $extractDir 已存在，跳过解压"
            }
        }
    }
    # 执行 target 特定的解压后处理
    if ($postExtractTargets.ContainsKey($Target)) {
        Write-Host ""
        Write-Host "[post] 解压后处理 ($Target)"
        & $postExtractTargets[$Target]
    }
}

Write-Host ""
Write-Host "全部下载完成！模型位于: $ModelsDir"