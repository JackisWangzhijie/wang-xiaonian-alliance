#!/usr/bin/env python3
"""多股票实时监控 - 每5分钟推送"""
import urllib.request
import json
import sys

# 监控股票列表
STOCKS = {
    "600884": "杉杉股份",
    "002195": "岩山科技",
    "002050": "三花智控",
    "600150": "中国船舶",
    "600089": "特变电工",
    "603166": "福达股份",
    "002580": "圣阳股份",
    "300346": "南大光电",
    "002648": "卫星化学"
}

def get_quote(stock_code):
    """获取单只股票行情"""
    try:
        market = "sh" if stock_code.startswith('6') else "sz"
        url = f"https://hq.sinajs.cn/list={market}{stock_code}"
        headers = {
            "Referer": "https://finance.sina.com.cn",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.0"
        }
        
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=10) as response:
            data = response.read().decode('gbk', errors='ignore')
            
            import re
            match = re.search(r'"([^"]+)"', data)
            if match:
                fields = match.group(1).split(',')
                price = float(fields[3])
                prev_close = float(fields[2])
                change_pct = ((price - prev_close) / prev_close) * 100
                
                return {
                    'name': fields[0],
                    'price': price,
                    'change_pct': change_pct,
                    'high': float(fields[4]),
                    'low': float(fields[5]),
                }
    except Exception as e:
        print(f"获取 {stock_code} 失败: {e}", file=sys.stderr)
    
    return None

def get_all_quotes():
    """获取所有股票行情"""
    results = {}
    for code, name in STOCKS.items():
        quote = get_quote(code)
        if quote:
            results[code] = quote
    return results

def format_notification(quotes):
    """格式化通知内容"""
    lines = []
    total_change = 0
    up_count = 0
    down_count = 0
    
    for code, q in quotes.items():
        name = STOCKS.get(code, "未知")
        price = q['price']
        change = q['change_pct']
        total_change += change
        
        if change >= 0:
            symbol = "📈"
            up_count += 1
        else:
            symbol = "📉"
            down_count += 1
        
        lines.append(f"{symbol} {name[:4]}: ¥{price:.2f} ({change:+.2f}%)")
    
    # 统计行
    avg_change = total_change / len(quotes) if quotes else 0
    trend = "🔴" if avg_change >= 0 else "🟢"
    
    header = f"{trend} 实时监控 ({len(quotes)}只) 平均: {avg_change:+.2f}%"
    summary = f"📊 涨: {up_count}只 | 跌: {down_count}只 | 平: {len(quotes)-up_count-down_count}只"
    
    content = header + "\n" + "="*20 + "\n" + "\n".join(lines[:6])  # 最多显示6只
    if len(lines) > 6:
        content += f"\n...等共{len(lines)}只股票"
    
    content += f"\n" + "="*20 + "\n" + summary
    
    return content

def send_notification(content):
    """发送 Termux 通知"""
    import subprocess
    import shlex
    
    try:
        cmd = [
            "termux-notification",
            "--title", "📈 股票实时监控",
            "--content", content,
            "--priority", "high",
            "--vibrate", "200,100,200",
            "--button1", "查看行情",
            "--button1-action", "termux-open-url 'https://quote.eastmoney.com/center/gridlist.html#hs_a_board'"
        ]
        subprocess.run(cmd, check=True)
        return True
    except Exception as e:
        print(f"发送通知失败: {e}", file=sys.stderr)
        return False

if __name__ == "__main__":
    import datetime
    
    print(f"[{datetime.datetime.now()}] 开始获取行情...")
    
    quotes = get_all_quotes()
    if quotes:
        content = format_notification(quotes)
        print(content)
        print("-" * 40)
        
        if send_notification(content):
            print(f"✅ 通知已发送 ({len(quotes)}只股票)")
            # 记录日志
            with open("/data/data/com.termux/files/home/kimi-workspace/logs/stock_monitor.log", "a") as f:
                f.write(f"[{datetime.datetime.now()}] 推送成功: {len(quotes)}只股票\n")
        else:
            print("❌ 通知发送失败")
    else:
        print("❌ 未能获取任何行情数据")
