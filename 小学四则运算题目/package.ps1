$ErrorActionPreference = "Stop"

$projectDirectory = $PSScriptRoot
$buildDirectory = Join-Path $projectDirectory "build"
$inputDirectory = Join-Path $buildDirectory "package-input"
$runtimeDirectory = Join-Path $buildDirectory "runtime"
$temporaryDirectory = Join-Path $buildDirectory "jpackage-temp"
$distributionDirectory = Join-Path $projectDirectory "dist"
$applicationDirectory = Join-Path $distributionDirectory "Myapp"
$zipPath = Join-Path $distributionDirectory "Myapp-windows-x64.zip"
$jarPath = Join-Path $projectDirectory "Myapp.jar"

& (Join-Path $projectDirectory "build.ps1")

foreach ($path in @($inputDirectory, $runtimeDirectory, $temporaryDirectory, $applicationDirectory)) {
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Recurse -Force
    }
}
if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}

New-Item -ItemType Directory -Force -Path $inputDirectory, $distributionDirectory | Out-Null
Copy-Item -LiteralPath $jarPath -Destination (Join-Path $inputDirectory "Myapp.jar")

$requiredModules = (& jdeps --ignore-missing-deps --print-module-deps $jarPath).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($requiredModules)) {
    throw "无法分析程序所需的 Java 模块。"
}

& jlink `
    --add-modules $requiredModules `
    --strip-debug `
    --no-header-files `
    --no-man-pages `
    --compress=2 `
    --output $runtimeDirectory
if ($LASTEXITCODE -ne 0) {
    throw "裁剪 JRE 失败。"
}

& jpackage `
    --type app-image `
    --name Myapp `
    --app-version 1.0.0 `
    --vendor "3124004171" `
    --description "小学四则运算题目生成与批改程序" `
    --input $inputDirectory `
    --main-jar "Myapp.jar" `
    --main-class "com.exercise.arithmetic.Main" `
    --runtime-image $runtimeDirectory `
    --dest $distributionDirectory `
    --temp $temporaryDirectory `
    --win-console
if ($LASTEXITCODE -ne 0) {
    throw "Windows EXE 打包失败。"
}

Compress-Archive -Path (Join-Path $applicationDirectory "*") -DestinationPath $zipPath -CompressionLevel Optimal

$exePath = Join-Path $applicationDirectory "Myapp.exe"
if (-not (Test-Path -LiteralPath $exePath)) {
    throw "打包结束但未找到 Myapp.exe。"
}

Write-Host "便携版目录: $applicationDirectory"
Write-Host "可执行文件: $exePath"
Write-Host "分发压缩包: $zipPath"
Write-Host "注意: Myapp.exe 需要同目录中的 app 和 runtime，分发时请使用整个目录或 ZIP。"

