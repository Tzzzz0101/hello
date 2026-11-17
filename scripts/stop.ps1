# 停止 EMP 员工管理 MIS 系统（scripts\run.ps1 启动的 Tomcat 实例）
param([switch]$Quiet)

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$pidFile = Join-Path $root '.tmp\tomcat.pid'
$tomcat = Join-Path $root '.runtime\tomcat9'
$stopped = $false

if (Test-Path $pidFile) {
    $procId = Get-Content $pidFile | Select-Object -First 1
    if ($procId -and (Get-Process -Id $procId -ErrorAction SilentlyContinue)) {
        & taskkill /PID $procId /T /F | Out-Null
        $stopped = $true
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

# 兜底：命令行里带本仓库 Tomcat 路径的 java 进程（java.exe 本身在 JDK 目录，不能按进程路径判断）
$javaProcs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue
foreach ($jp in $javaProcs) {
    if ($jp.CommandLine -and $jp.CommandLine.Contains($tomcat)) {
        & taskkill /PID $jp.ProcessId /T /F | Out-Null
        $stopped = $true
    }
}

if (-not $Quiet) {
    if ($stopped) { Write-Host 'EMP MIS 已停止。' -ForegroundColor Yellow }
    else { Write-Host '没有找到正在运行的实例（8080 未被本项目的 Tomcat 占用）。' }
}