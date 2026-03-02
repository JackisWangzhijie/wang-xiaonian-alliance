---
name: tushare-finance
description: Tushare A股/港股/美股数据接口 - 中国股市实时行情、历史数据、财务数据。用于获取中国股票实时报价、K线数据、财务报表、龙虎榜、资金流向等。需配置 TUSHARE_TOKEN。
homepage: https://tushare.pro
---

# Tushare Finance - 中国股票数据接口

Tushare 是中国最流行的金融数据接口，支持 A股、港股、美股数据获取。

## 核心功能

- **实时行情** - A股实时报价、涨跌幅、成交量
- **历史数据** - K线数据、历史价格、复权数据
- **财务数据** - 财务报表、业绩快报、业绩预告
- **市场数据** - 龙虎榜、资金流向、融资融券
- **基本面** - 公司信息、股东信息、股本变动

---

## 安装配置

### 1. 安装 Python 依赖
```bash
pip install tushare pandas
```

### 2. 获取 Token
- 访问 https://tushare.pro
- 注册账号并获取 Token

### 3. 配置 OpenClaw
```bash
openclaw config set skills.tushare.token "你的Tushare Token"
```

---

## 使用示例

### 初始化
```python
import tushare as ts
import pandas as pd

# 设置 Token
ts.set_token('your_tushare_token')
pro = ts.pro_api()
```

### 获取实时行情
```python
# 获取单只股票实时数据
df = pro.daily(ts_code='600519.SH', start_date='20260101', end_date='20260301')
print(df[['trade_date', 'open', 'high', 'low', 'close', 'vol']])

# 获取多只股票
codes = ['600519.SH', '000858.SZ', '002415.SZ']  # 茅台、五粮液、海康
for code in codes:
    df = pro.daily(ts_code=code, start_date='20260101', end_date='20260301')
    print(f"\n{code}:")
    print(df.head())
```

### 获取股票列表
```python
# 获取所有A股列表
stocks = pro.stock_basic(exchange='', list_status='L', 
                         fields='ts_code,symbol,name,area,industry,list_date')
print(stocks.head(20))

# 按行业筛选
industry = stocks[stocks['industry'] == '白酒']
print(f"白酒行业股票数: {len(industry)}")
print(industry[['name', 'ts_code']])
```

### 获取财务数据
```python
# 获取资产负债表
balance = pro.balancesheet(ts_code='600519.SH', period='20241231')
print(balance[['total_assets', 'total_liab', 'total_hldr_eqy_exc_min_int']])

# 获取利润表
income = pro.income(ts_code='600519.SH', period='20241231')
print(income[['total_revenue', 'operate_profit', 'net_profit']])
```

---

## 常用数据字段

### 日线数据 (daily)
| 字段 | 说明 |
|------|------|
| ts_code | 股票代码 (如 600519.SH) |
| trade_date | 交易日期 |
| open | 开盘价 |
| high | 最高价 |
| low | 最低价 |
| close | 收盘价 |
| pct_chg | 涨跌幅(%) |
| vol | 成交量(手) |

---

> 注意: 使用本技能需要 Tushare Pro 账号
