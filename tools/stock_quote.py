#!/usr/bin/env python3
# 股票实时行情查询 (AKShare 版)
import akshare as ak
import sys

def get_quote(stock_code="601888"):
    """获取股票实时行情"""
    try:
        # 判断是沪市还是深市
        if stock_code.startswith('6'):
            full_code = f"sh{stock_code}"
        else:
            full_code = f"sz{stock_code}"
        
        # 获取行情
        df = ak.stock_zh_a_spot_em()
        stock = df[df['代码'] == stock_code]
        
        if stock.empty:
            return None
        
        row = stock.iloc[0]
        return {
            'name': row['名称'],
            'price': row['最新价'],
            'change': row['涨跌幅'],
            'change_amount': row['涨跌额'],
            'open': row['今开'],
            'high': row['最高'],
            'low': row['最低'],
            'prev_close': row['昨收'],
            'volume': row['成交量'],
            'turnover': row['成交额']
        }
    except Exception as e:
        return {'error': str(e)}

if __name__ == "__main__":
    code = sys.argv[1] if len(sys.argv) > 1 else "601888"
    result = get_quote(code)
    if result:
        if 'error' in result:
            print(f"Error: {result['error']}")
        else:
            print(f"{result['name']}({code}): ¥{result['price']} ({result['change']}%)")
    else:
        print("无法获取数据")
