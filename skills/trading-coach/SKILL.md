---
name: trading-coach
description: 交易复盘教练 - 交易记录、复盘分析、策略回测。帮助投资者记录交易、分析盈亏原因、改进交易策略。
homepage: https://github.com/openclaw/skills/trading-coach
---

# Trading Coach - 交易复盘教练

交易复盘和策略分析工具，帮助投资者持续改进交易能力。

## 核心功能

- **交易记录** - 记录每笔交易的详细信息
- **复盘分析** - 分析盈亏原因、交易习惯
- **策略回测** - 测试交易策略的历史表现
- **改进建议** - 基于数据的交易改进建议

---

## 使用示例

### 记录交易
```python
# 记录买入
trade = {
    'date': '2026-03-01',
    'code': '600519.SH',
    'name': '贵州茅台',
    'action': 'buy',
    'price': 1500.0,
    'shares': 100,
    'reason': '突破前期高点，放量上涨'
}
record_trade(trade)

# 记录卖出
close_trade(code='600519.SH', price=1600, reason='达到目标价位')
```

### 复盘分析
```python
# 月度复盘
report = generate_monthly_report(month='2026-02')
print(f"月度收益: {report['return']}%")
print(f"胜率: {report['win_rate']}")
print(f"最大单笔盈利: {report['max_profit']}")
print(f"最大单笔亏损: {report['max_loss']}")
```

---

## 复盘维度

- 买卖点分析
- 持仓时间管理
- 盈亏原因统计
- 情绪影响分析
