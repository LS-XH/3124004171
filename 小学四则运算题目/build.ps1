$ErrorActionPreference = "Stop"

$projectDirectory = $PSScriptRoot
$sourceDirectory = Join-Path $projectDirectory "src\main\java"
$classesDirectory = Join-Path $projectDirectory "build\classes"
$jarPath = Join-Path $projectDirectory "Myapp.jar"

if (Test-Path -LiteralPath $classesDirectory) {
    Remove-Item -LiteralPath $classesDirectory -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classesDirectory | Out-Null
$sourceFiles = Get-ChildItem -Path $sourceDirectory -Filter "*.java" -Recurse |
    Select-Object -ExpandProperty FullName

if ($sourceFiles.Count -eq 0) {
    throw "没有找到 Java 源文件。"
}

& javac -encoding UTF-8 -d $classesDirectory $sourceFiles
if ($LASTEXITCODE -ne 0) {
    throw "Java 编译失败。"
}

& jar --create --file $jarPath --main-class com.exercise.arithmetic.Main -C $classesDirectory .
if ($LASTEXITCODE -ne 0) {
    throw "JAR 打包失败。"
}

Write-Host "构建完成: $jarPath"
