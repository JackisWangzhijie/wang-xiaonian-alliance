#!/usr/bin/env python3
"""
CN Stock Simple - A股实时行情查询工具 (轻量版)
使用新浪财经API，无需安装额外库

功能:
- 查询单只股票实时行情
- 获取A股全市场实时数据 (前50)
- 查询股票列表
"""

import sys
import json
import urllib.request
import urllib.parse
import argparse
from typing import Dict, List, Optional


class CNStockSimple:
    """轻量级A股查询工具"""
    
    def __init__(self):
        self.base_url = "http://hq.sinajs.cn"
        
    def normalize_code(self, code: str) -> str:
        """标准化股票代码"""
        code = code.strip()
        
        if code.startswith('6'):
            return f"sh{code}"  # 沪市
        elif code.startswith('0') or code.startswith('3') or code.startswith('2'):
            return f"sz{code}"  # 深市
        elif code.startswith('8') or code.startswith('4'):
            return f"bj{code}"  # 北交所
        return code
    
    def parse_sina_data(self, data: str, code: str) -> Dict:
        """解析新浪返回的数据"""
        try:
            # 新浪返回格式: var hq_str_sh600519="贵州茅台,1500.00,...";
            if '"' in data:
                content = data.split('"')[1]
                fields = content.split(',')
                
                if len(fields) < 33:
                    return {"error": "数据格式错误"}
                
                # 计算涨跌幅
                prev_close = float(fields[2])
                current = float(fields[3])
                change_pct = ((current - prev_close) / prev_close * 100) if prev_close > 0 else 0
                
                return {
                    "代码": code.upper().replace('SH', '').replace('SZ', '').replace('BJ', ''),
                    "名称": fields[0],
                    "最新价": current,
                    "昨收": prev_close,
                    "今开": float(fields[1]),
                    "最高": float(fields[4]),
                    "最低": float(fields[5]),
                    "涨跌幅": round(change_pct, 2),
                    "涨跌额": round(current - prev_close, 2),
                    "成交量": f"{float(fields[8])/10000:.2f}万手",
                    "成交额": f"{float(fields[9])/10000:.0f}万",
                    "买一": float(fields[11]),
                    "卖一": float(fields[21]),
                    "更新时间": f"{fields[30]} {fields[31]}"
                }
            else:
                return {"error": "股票代码不存在或暂无数据"}
                
        except Exception as e:
            return {"error": f"解析数据失败: {str(e)}"}
    
    def get_realtime_quote(self, code: str) -> Dict:
        """获取单只股票实时行情"""
        try:
            full_code = self.normalize_code(code)
            url = f"{self.base_url}/list={full_code}"
            
            # 发送请求
            req = urllib.request.Request(
                url,
                headers={
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Referer': 'http://finance.sina.com.cn'
                }
            )
            
            with urllib.request.urlopen(req, timeout=10) as response:
                data = response.read().decode('gbk')
                return self.parse_sina_data(data, code)
                
        except Exception as e:
            return {"error": f"查询失败: {str(e)}"}
    
    def get_batch_quotes(self, codes: List[str]) -> List[Dict]:
        """批量获取股票行情"""
        results = []
        
        # 新浪API一次最多支持查询多个股票
        code_str = ','.join([self.normalize_code(c) for c in codes])
        
        try:
            url = f"{self.base_url}/list={code_str}"
            req = urllib.request.Request(
                url,
                headers={
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
                    'Referer': 'http://finance.sina.com.cn'
                }
            )
            
            with urllib.request.urlopen(req, timeout=10) as response:
                data = response.read().decode('gbk')
                
                # 解析多行数据
                lines = data.strip().split(';')
                for i, line in enumerate(lines):
                    if line.strip() and i < len(codes):
                        result = self.parse_sina_data(line + ';', codes[i])
                        results.append(result)
                        
        except Exception as e:
            results.append({"error": f"批量查询失败: {str(e)}"})
            
        return results
    
    def get_top_stocks(self, type: str = "gainers") -> List[Dict]:
        """
        获取热门股票 (使用预定义的热门股票列表)
        注意: 新浪财经API不直接提供排行榜，这里使用一些热门股票
        """
        # 热门股票代码列表
        hot_stocks = [
            '600519',  # 茅台
            '000001',  # 平安银行
            '000858',  # 五粮液
            '002415',  # 海康威视
            '002594',  # 比亚迪
            '300750',  # 宁德时代
            '601012',  # 隆基绿能
            '601888',  # 中国中免
            '600276',  # 恒瑞医药
            '000333',  # 美的集团
            '600036',  # 招商银行
            '000002',  # 万科A
            '601318',  # 中国平安
            '600887',  # 伊利股份
            '002475',  # 立讯精密
            '300059',  # 东方财富
            '600030',  # 中信证券
            '000568',  # 泸州老窖
            '600809',  # 山西汾酒
            '603288',  # 海天味业
        ]
        
        return self.get_batch_quotes(hot_stocks)


def print_stock_info(stock: Dict):
    """美观打印股票信息"""
    if 'error' in stock:
        print(f"❌ {stock['error']}")
        return
    
    print("\n" + "=" * 50)
    print(f"📈 {stock['名称']} ({stock['代码']})")
    print("=" * 50)
    
    # 根据涨跌幅显示颜色符号
    change = stock.get('涨跌幅', 0)
    if change > 0:
        trend = "📈"
    elif change < 0:
        trend = "📉"
    else:
        trend = "➖"
    
    print(f"最新价: {stock['最新价']:.2f} 元 {trend}")
    print(f"涨跌幅: {change:+.2f}%")
    print(f"涨跌额: {stock['涨跌额']:+.2f} 元")
    print(f"成交量: {stock['成交量']}")
    print(f"成交额: {stock['成交额']}")
    print("-" * 50)
    print(f"今开: {stock['今开']:.2f}    昨收: {stock['昨收']:.2f}")
    print(f"最高: {stock['最高']:.2f}    最低: {stock['最低']:.2f}")
    print(f"买一: {stock['买一']:.2f}    卖一: {stock['卖一']:.2f}")
    print("-" * 50)
    print(f"更新时间: {stock['更新时间']}")
    print("=" * 50)


def main():
    """命令行入口"""
    parser = argparse.ArgumentParser(
        description="A股实时行情查询工具 - 轻量版 (无需额外安装)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python3 cn_stock_simple.py quote 600519      # 查询茅台
  python3 cn_stock_simple.py quote 000001      # 查询平安银行
  python3 cn_stock_simple.py hot               # 查看热门股票
  python3 cn_stock_simple.py batch 600519,000858,002415  # 批量查询
        """
    )
    
    subparsers = parser.add_subparsers(dest='command', help='可用命令')
    
    # quote 命令
    quote_parser = subparsers.add_parser('quote', help='查询单只股票')
    quote_parser.add_argument('code', help='股票代码，如600519')
    
    # hot 命令
    subparsers.add_parser('hot', help='查看热门股票')
    
    # batch 命令
    batch_parser = subparsers.add_parser('batch', help='批量查询')
    batch_parser.add_argument('codes', help='股票代码，用逗号分隔')
    
    args = parser.parse_args()
    
    if not args.command:
        parser.print_help()
        return
    
    # 初始化工具
    tool = CNStockSimple()
    
    # 执行命令
    if args.command == 'quote':
        result = tool.get_realtime_quote(args.code)
        print_stock_info(result)
    
    elif args.command == 'hot':
        print("\n🔥 热门股票行情")
        results = tool.get_top_stocks()
        
        # 按涨跌幅排序
        sorted_results = sorted(
            [r for r in results if 'error' not in r],
            key=lambda x: x.get('涨跌幅', 0),
            reverse=True
        )
        
        print(f"\n{'排名':<6} {'代码':<10} {'名称':<12} {'最新价':<10} {'涨跌幅':<10}")
        print("-" * 60)
        for i, stock in enumerate(sorted_results[:20], 1):
            print(f"{i:<6} {stock['代码']:<10} {stock['名称']:<12} "
                  f"{stock['最新价']:<10.2f} {stock['涨跌幅']:+.2f}%")
    
    elif args.command == 'batch':
        codes = args.codes.split(',')
        print(f"\n📊 批量查询 {len(codes)} 只股票")
        results = tool.get_batch_quotes(codes)
        for result in results:
            print_stock_info(result)


if __name__ == "__main__":
    main()
