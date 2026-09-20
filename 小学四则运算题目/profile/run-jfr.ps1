param(
    [string]$RecordingName = "recording",
    [int]$Warmups = 10,
    [int]$Iterations = 30,
    [int]$Count = 10000,
    [int]$Range = 10
)

$ErrorActionPreference = "Stop"
$projectDirectory = Split-Path -Parent $PSScriptRoot
$profileClasses = Join-Path $projectDirectory "build\profile"
$workDirectory = Join-Path $PSScriptRoot "work"
$recordingPath = Join-Path $PSScriptRoot "$RecordingName.jfr"
$resultPath = Join-Path $PSScriptRoot "$RecordingName-results.txt"

& (Join-Path $projectDirectory "build.ps1")
New-Item -ItemType Directory -Force -Path $profileClasses, $workDirectory | Out-Null
$profileSources = Get-ChildItem -Path (Join-Path $projectDirectory "src\profile\java") `
    -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

& javac -encoding UTF-8 `
    -cp (Join-Path $projectDirectory "build\classes") `
    -d $profileClasses `
    $profileSources
if ($LASTEXITCODE -ne 0) {
    throw "性能基准编译失败。"
}

$classPath = (Join-Path $projectDirectory "build\classes") + ";" + $profileClasses
$jfrOption = "-XX:StartFlightRecording=filename=$recordingPath,settings=profile,dumponexit=true,disk=true"
& java $jfrOption -cp $classPath `
    com.exercise.arithmetic.profile.PerformanceBenchmark `
    $Warmups $Iterations $Count $Range $workDirectory |
    Tee-Object -FilePath $resultPath

if ($LASTEXITCODE -ne 0) {
    throw "JFR 性能基准运行失败。"
}

Write-Host "JFR 录制: $recordingPath"
Write-Host "测量结果: $resultPath"

