---
name: economic-calendar-fetcher
description: 财经日历 - 获取中国经济数据发布日历、重要财经事件、央行政策会议等。包括GDP、CPI、PMI、利率决议等数据。
homepage: https://github.com/openclaw/skills/economic-calendar-fetcher
---

# Economic Calendar Fetcher - 财经日历

获取中国及全球重要财经数据和事件日历。

## 核心功能

- **中国数据** - GDP、CPI、PPI、PMI、社融等
- **央行政策** - 利率决议、货币政策报告
- **重要会议** - 两会、政治局会议、经济工作会议
- **全球数据** - 美联储、欧央行重要数据

---

## 使用示例

### 获取财经日历
```python
# 获取本周中国财经数据
calendar = get_economic_calendar(country='CN', week='current')
for event in calendar:
    print(f"{event['date']} {event['time']}")
    print(f"  {event['name']}: 预期{event['forecast']}, 前值{event['previous']}")
    print(f"  重要度: {'★' * event['importance']}")
```

### 关注重要数据
```python
# 重点关注数据
important_events = [
    '中国GDP',
    'CPI',
    'PPI',
    'PMI',
    'LPR',
    'MLF'
]

# 设置提醒
set_event_reminder(events=important_events, minutes_before=30)
```

---

## 数据类型

- GDP - 国内生产总值
- CPI - 消费者物价指数
- PPI - 生产者物价指数
- PMI - 采购经理指数
- LPR - 贷款市场报价利率
