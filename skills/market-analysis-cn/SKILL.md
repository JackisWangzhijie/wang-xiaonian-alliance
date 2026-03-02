---
name: market-analysis-cn
description: 中文市场分析 - 专门针对中国股市的技术分析、市场情绪、板块轮动分析。支持A股热点板块、个股技术分析、市场资金流向、情绪指标等。
homepage: https://github.com/openclaw/skills/market-analysis-cn
---

# Market Analysis CN - 中文市场分析

专门针对中国股市的市场分析技能，提供技术分析、市场情绪、板块轮动等功能。

## 核心功能

- **热点板块** - 实时追踪A股热点板块和龙头股
- **技术分析** - K线形态、技术指标分析
- **市场情绪** - 涨跌家数、涨停跌停、恐慌指数
- **资金流向** - 北向资金、主力资金流向
- **龙虎榜** - 营业部买卖分析

---

## 使用场景

### 场景1: 查看今日热点
```
用户: 今天A股哪些板块最热？
分析: 获取当日行业涨幅排名，找出领涨板块和龙头股
```

### 场景2: 技术分析
```
用户: 分析茅台的技术面
分析: 获取K线数据，计算MA、MACD、RSI等指标，判断趋势
```

---

## 分析方法

### 热点板块追踪
```python
# 获取行业板块涨幅
sectors = get_sector_performance(sort_by='pct_chg', limit=10)

# 分析领涨板块
for sector in sectors:
    print(f"{sector['name']}: {sector['pct_chg']}%")
    print(f"  龙头股: {sector['leaders']}")
```

### 个股技术分析
```python
def analyze_technical(stock_code):
    # 获取数据
    df = get_kline_data(stock_code, period='daily', count=60)
    
    # 计算指标
    df['ma5'] = df['close'].rolling(5).mean()
    df['ma10'] = df['close'].rolling(10).mean()
    df['ma20'] = df['close'].rolling(20).mean()
    
    # 判断趋势
    current = df.iloc[-1]
    signal = []
    
    if current['close'] > current['ma5'] > current['ma10']:
        signal.append("多头排列")
    
    return {
        'trend': '上涨' if current['close'] > current['ma20'] else '下跌',
        'signals': signal
    }
```

---

## 注意事项

- 数据延迟约15分钟
- 交易时间: 9:30-11:30, 13:00-15:00
- 节假日休市
