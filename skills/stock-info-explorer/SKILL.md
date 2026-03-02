---
name: stock-info-explorer
description: 股票实时报价、图表 - 获取股票实时行情、历史K线图、技术指标图表。支持A股、港股、美股实时报价和图表展示。
homepage: https://github.com/openclaw/skills/stock-info-explorer
---

# Stock Info Explorer - 股票实时报价与图表

提供股票实时行情查询和图表展示功能。

## 核心功能

- **实时报价** - 股票实时价格、涨跌幅
- **K线图** - 历史K线数据展示
- **技术指标** - MA、MACD、RSI等技术指标图表
- **多市场** - 支持A股、港股、美股

---

## 使用示例

### 查询实时报价
```python
# 获取A股实时报价
quote = get_stock_quote('600519.SH')
print(f"茅台: {quote['price']}元, 涨跌: {quote['change']}%")

# 获取港股
quote = get_stock_quote('00700.HK')
print(f"腾讯: {quote['price']}港币")
```

### 绘制K线图
```python
import matplotlib.pyplot as plt
import mplfinance as mpf

# 获取K线数据
df = get_kline_data('600519.SH', period='daily', count=30)

# 绘制K线图
mpf.plot(df, type='candle', title='茅台K线图', 
         ylabel='价格', volume=True)
```

---

## 支持的图表类型

- K线图 (Candlestick)
- 折线图 (Line)
- 柱状图 (Bar)
- 成交量图 (Volume)
