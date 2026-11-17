# 启动 EMP 员工管理 MIS 系统（部署到本仓库自带的 Tomcat 9）
# 用法：
#   .\scripts\run.ps1               # 打包 + 部署 + 启动
#   .\scripts\run.ps1 -SkipBuild    # 不重新打包，直接用现有 target\emp-mis.war
param(
    [switch]$SkipBuild,
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root
$tomcat = Join-Path $root '.runtime\tomcat9'
$logDir = Join-Path $tomcat 'logs'

if (-not (Test-Path (Join-Path $tomcat 'bin\catalina.bat'))) {
    throw "未找到 Tomcat：$tomcat。请把 apache-tomcat-9.x 解压为 .runtime\tomcat9。"
}

# 1) 停掉已在运行的实例，避免 8080 被占用
& (Join-Path $root 'scripts\stop.ps1') -Quiet

# 2) 打包
if (-not $SkipBuild) {
    Write-Host '[1/3] Maven 打包 ...' -ForegroundColor Cyan
    & (Join-Path $root 'mvnw.cmd') -B -DskipTests clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven 打包失败，请查看上面的编译错误。' }
}
$war = Join-Path $root 'target\emp-mis.war'
if (-not (Test-Path $war)) { throw "未找到 $war，请先执行不带 -SkipBuild 的 run.ps1。" }

# 3) 部署
Write-Host '[2/3] 部署 war ...' -ForegroundColor Cyan
Remove-Item (Join-Path $tomcat 'webapps\emp-mis') -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item $war (Join-Path $tomcat 'webapps\emp-mis.war') -Force

# 4) 启动（隐藏窗口的独立进程；日志由 Tomcat 与 Logback 写进 logs 目录）
if (-not $env:JAVA_HOME) {
    $java = (Get-Command java).Source
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)
}
$env:CATALINA_HOME = $tomcat
$env:CATALINA_BASE = $tomcat
New-Item -ItemType Directory -Force $logDir | Out-Null
New-Item -ItemType Directory -Force (Join-Path $root '.tmp') | Out-Null

Write-Host '[3/3] 启动 Tomcat ...' -ForegroundColor Cyan
$proc = Start-Process -FilePath (Join-Path $tomcat 'bin\catalina.bat') -ArgumentList 'run' `
    -WorkingDirectory (Join-Path $tomcat 'bin') -WindowStyle Hidden -PassThru
$proc.Id | Set-Content (Join-Path $root '.tmp\tomcat.pid')

# 5) 等待健康检查通过
$url = "http://127.0.0.1:$Port/emp-mis/health"
for ($i = 1; $i -le 45; $i++) {
    Start-Sleep -Seconds 2
    try {
        $r = Invoke-RestMethod -Uri $url -TimeoutSec 3
        Write-Host ''
        Write-Host "启动成功（约 $($i * 2) 秒）" -ForegroundColor Green
        Write-Host "  系统首页  : http://127.0.0.1:$Port/emp-mis/"
        Write-Host "  健康检查  : $url  ->  status=$($r.data.status), deptCount=$($r.data.deptCount)"
        Write-Host "  实验账号  : admin / 123456"
        Write-Host "  应用日志  : .runtime\tomcat9\logs\emp-mis.log（含 SQL 与 WebLog）"
        Write-Host "  容器日志  : .runtime\tomcat9\logs\catalina.<日期>.log"
        Write-Host "  停止服务  : .\scripts\stop.ps1"
        return
    } catch {
        if ($proc.HasExited) { throw "Tomcat 已退出，请查看 $logDir\catalina.<日期>.log 与 localhost.<日期>.log。" }
    }
}
throw "等待 $url 超时（90 秒），请查看 $logDir 下的日志。"