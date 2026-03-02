# 王小年联盟 - 自动推送脚本
# 在网络恢复后自动推送到 GitHub

$maxRetries = 10
$retryInterval = 30  # 秒

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  王小年联盟 - 自动同步工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

for ($i = 1; $i -le $maxRetries; $i++) {
    Write-Host "尝试 $i/$maxRetries: 推送到 GitHub..."
    
    $result = git push origin master 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "[OK] 推送成功！" -ForegroundColor Green
        Write-Host ""
        
        # 发送通知
        $notifyScript = "..\tools\windows-auto\win_notify.py"
        if (Test-Path $notifyScript) {
            python $notifyScript -t "王小年联盟" -m "已成功同步到 GitHub！" -p high
        }
        
        # 创建同步完成标记
        $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        "Synced at $timestamp" | Out-File -FilePath ".last_sync" -Encoding UTF8
        
        exit 0
    } else {
        Write-Host "[ERR] 推送失败: $result" -ForegroundColor Red
        
        if ($i -lt $maxRetries) {
            Write-Host "等待 ${retryInterval}秒后重试..."
            Start-Sleep -Seconds $retryInterval
        }
    }
}

Write-Host ""
Write-Host "[ERR] 达到最大重试次数，推送失败" -ForegroundColor Red
Write-Host ""
Write-Host "可能的解决方案:" -ForegroundColor Yellow
Write-Host "1. 检查网络连接"
Write-Host "2. 配置代理: git config --global http.proxy http://proxy:port"
Write-Host "3. 使用 SSH 方式连接"
Write-Host "4. 手动复制文件到 Mobile 分身"
Write-Host ""

exit 1
