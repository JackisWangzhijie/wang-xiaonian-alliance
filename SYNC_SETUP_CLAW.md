# 王小年 Claw 同步配置指南

## 每小时自动同步设置

### 1. 安装crontab (如果还没有)
```bash
# Ubuntu/Debian
apt-get install cron

# 启动cron服务
service cron start
```

### 2. 添加定时任务
```bash
crontab -e
```

添加以下行：
```
# 每小时整点同步
0 * * * * /root/.openclaw/workspace/wang-xiaonian-alliance-sync/scripts/sync_claw.sh >> /var/log/wxn-claw-sync.log 2>&1

# 或者每30分钟同步一次
*/30 * * * * /root/.openclaw/workspace/wang-xiaonian-alliance-sync/scripts/sync_claw.sh >> /var/log/wxn-claw-sync.log 2>&1
```

### 3. 查看定时任务
```bash
crontab -l
```

### 4. 查看同步日志
```bash
tail -f /var/log/wxn-claw-sync.log
tail -f /root/.openclaw/workspace/wang-xiaonian-alliance-sync/logs/sync-$(date +%Y%m%d).log
```

### 5. 手动测试同步
```bash
/root/.openclaw/workspace/wang-xiaonian-alliance-sync/scripts/sync_claw.sh
```

## 同步内容

每小时自动同步：
- ✅ 拉取其他成员的最新更新
- ✅ 推送我的工作成果
- ✅ 同步项目文档
- ✅ 更新状态文件
- ✅ 记录同步日志

## 手动同步命令

如果需要立即同步（不等待整点）：
```bash
cd /root/.openclaw/workspace/wang-xiaonian-alliance-sync
./scripts/sync_claw.sh
```

