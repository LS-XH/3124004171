$ErrorActionPreference = "Stop"

$projectDirectory = $PSScriptRoot
$mainClasses = Join-Path $projectDirectory "build\classes"
$testClasses = Join-Path $projectDirectory "build\test-classes"
$testWork = Join-Path $projectDirectory "build\test-work"

& (Join-Path $projectDirectory "build.ps1")
if (Test-Path -LiteralPath $testClasses) {
    Remove-Item -LiteralPath $testClasses -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $testClasses, $testWork | Out-Null

$testSources = Get-ChildItem -Path (Join-Path $projectDirectory "src\test\java") `
    -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& javac -encoding UTF-8 -cp $mainClasses -d $testClasses $testSources
if ($LASTEXITCODE -ne 0) {
    throw "测试代码编译失败。"
}

$classPath = $mainClasses + ";" + $testClasses
& java -cp $classPath com.exercise.arithmetic.TestRunner $testWork
if ($LASTEXITCODE -ne 0) {
    throw "存在未通过的测试。"
}

