---
name: stock-analysis
description: 投资组合、热门扫描 - 股票投资组合分析、风险评估、热门股票扫描。支持持仓分析、收益率计算、风险指标、热门股筛选等。
homepage: https://github.com/openclaw/skills/stock-analysis
---

# Stock Analysis - 投资组合与热门扫描

提供投资组合分析和热门股票扫描功能。

## 核心功能

- **投资组合** - 持仓分析、收益率计算
- **风险评估** - 波动率、夏普比率、最大回撤
- **热门扫描** - 涨停股、异动股、资金流向
- **收益分析** - 累计收益、年化收益、对比基准

---

## 使用示例

### 投资组合分析
```python
# 定义持仓
portfolio = {
    '600519.SH': {'shares': 100, 'cost': 1500},
    '000858.SZ': {'shares': 200, 'cost': 150},
    '002415.SZ': {'shares': 500, 'cost': 30}
}

# 分析组合
analysis = analyze_portfolio(portfolio)
print(f"总市值: {analysis['total_value']}元")
print(f"总收益: {analysis['total_gain']}元 ({analysis['total_return']}%)")
print(f"波动率: {analysis['volatility']}")
```

### 热门股扫描
```python
# 扫描涨停股
limit_up_stocks = scan_limit_up()
print(f"今日涨停: {len(limit_up_stocks)}只")
for stock in limit_up_stocks[:10]:
    print(f"  {stock['name']}: {stock['reason']}")

# 扫描异动股
abnormal = scan_abnormal(volume_threshold=3, price_threshold=0.05)
```

---

## 风险指标

- **波动率 (Volatility)** - 价格变动程度
- **夏普比率 (Sharpe)** - 风险调整后收益
- **最大回撤 (Max Drawdown)** - 最大亏损幅度
- **Beta** - 相对市场波动
