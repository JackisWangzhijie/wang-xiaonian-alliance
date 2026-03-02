# Termux 自动化技能

王小年掌握的 Termux 后台自动化能力，实现定时任务、系统通知、后台脚本执行、手机闹钟集成等功能。

## 核心能力

### 1. 定时任务 (Cron)
```bash
# 查看当前定时任务
crontab -l

# 编辑定时任务
crontab -e

# 启动 cron 服务
crond

# 管理服务
termux-auto status   # 查看状态
termux-auto start    # 启动服务
termux-auto stop     # 停止服务
```

### 2. 系统通知 (Termux API)
```bash
# 发送通知
termux-notification --title "标题" --content "内容"

# 震动提醒
termux-vibrate -d 500

# 播放铃声
termux-media-player play sound.mp3

# 智能提醒工具
smart-reminder notify "标题" "内容"   # 发送通知
smart-reminder timer 25 "休息"         # 25分钟倒计时
smart-reminder alarm 09:00 "早会"      # 设置闹钟
```

### 3. 手机闹钟集成
```bash
# 一键设置股票交易提醒 (打开时钟应用)
stock-alarm-setup

# 或使用 termux-clock (需安装)
termux-clock alarm --hours 9 --minutes 25 --label "开盘提醒"
termux-clock timer --minutes 25 --label "番茄钟"
```

### 4. 后台运行
```bash
# nohup 方式
nohup python3 script.py > output.log 2>&1 &

# 使用 screen
screen -dmS mysession python3 script.py

# 使用 tmux
tmux new-session -d -s trading 'python3 trading_bot.py'
```

## 使用场景

### 场景1: 股票定时交易提醒
```bash
# 每天 9:25 提醒开盘选股
echo "25 9 * * 1-5 /data/data/com.termux/files/home/kimi-workspace/trading-sim/notify_open.sh" | crontab -

# 每天 14:50 提醒收盘前操作
echo "50 14 * * 1-5 /data/data/com.termux/files/home/kimi-workspace/trading-sim/notify_close.sh" | crontab -
```

### 场景2: 后台数据监控
```bash
# 后台运行价格监控脚本
nohup python3 ~/kimi-workspace/trading-sim/price_monitor.py > ~/kimi-workspace/logs/monitor.log 2>&1 &
```

### 场景3: 定时备份
```bash
# 每天凌晨备份数据
0 2 * * * /data/data/com.termux/files/home/kimi-workspace/tools/backup.sh
```

## 重要文件

| 文件 | 用途 |
|------|------|
| `~/.termux/tasker/` | Termux:Tasker 脚本目录 |
| `~/.termux/boot/` | 开机自启动脚本 |
| `/data/data/com.termux/files/usr/var/spool/cron/` | cron 任务存储 |
| `~/kimi-workspace/cron/` | 自定义定时任务脚本 |

## 开机自启动

1. 创建启动目录: `mkdir -p ~/.termux/boot`
2. 添加启动脚本:
```bash
#!/data/data/com.termux/files/usr/bin/sh
crond  # 启动 cron 服务
termux-wake-lock  # 保持唤醒
```
3. 设置权限: `chmod +x ~/.termux/boot/startup.sh`

## 注意事项

1. **电池优化**: Android 可能会杀死后台进程，需要设置电池白名单
2. **Termux:Boot**: 重启后自动启动 Termux
3. **Termux:Widget**: 桌面小部件快速执行脚本
4. **唤醒锁**: 使用 `termux-wake-lock` 防止休眠

## 王小年的承诺

> 我将主动学习并利用 Termux 的一切能力，弥补与 nanobot 的差距。
> 定时任务、后台运行、系统通知——我都能做到！
