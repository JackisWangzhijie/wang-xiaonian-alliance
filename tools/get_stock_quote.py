#!/usr/bin/env python3
"""股票实时行情获取 - 稳定版"""
import urllib.request
import sys

def get_quote(stock_code="601888"):
    """获取股票实时行情"""
    try:
        # 判断市场
        market = "sh" if stock_code.startswith('6') else "sz"
        
        url = f"https://hq.sinajs.cn/list={market}{stock_code}"
        headers = {
            "Referer": "https://finance.sina.com.cn",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.0"
        }
        
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=10) as response:
            data = response.read().decode('gbk', errors='ignore')
            
            # 解析数据
            import re
            match = re.search(r'"([^"]+)"', data)
            if match:
                fields = match.group(1).split(',')
                return {
                    'name': fields[0],
                    'open': fields[1],
                    'prev_close': fields[2],
                    'price': fields[3],
                    'high': fields[4],
                    'low': fields[5],
                    'buy1': fields[6],
                    'sell1': fields[7],
                    'volume': fields[8],
                    'turnover': fields[9],
                    'date': fields[30],
                    'time': fields[31]
                }
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
    
    return None

if __name__ == "__main__":
    import json
    code = sys.argv[1] if len(sys.argv) > 1 else "601888"
    result = get_quote(code)
    if result:
        print(json.dumps(result, ensure_ascii=False))
    else:
        print("{}")
