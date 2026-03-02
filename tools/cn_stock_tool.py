#!/usr/bin/env python3
"""
CN Stock Tool - A股实时行情查询工具
基于 AKShare 免费数据源，无需注册Token

功能:
- 查询单只股票实时行情
- 获取A股全市场实时数据
- 查询历史K线数据
- 查询股票列表
- 行业板块分析
"""

import sys
import json
import argparse
from datetime import datetime, timedelta
from typing import Optional, List, Dict

# 尝试导入akshare，如果失败则提示安装
try:
    import akshare as ak
except ImportError:
    print("❌ 请先安装 AKShare:")
    print("   pip install akshare -i https://pypi.tuna.tsinghua.edu.cn/simple")
    sys.exit(1)


class CNStockTool:
    """A股查询工具主类"""
    
    def __init__(self):
        self.cache = {}  # 简单缓存
        
    def normalize_code(self, code: str) -> str:
        """
        标准化股票代码
        支持格式: 600519, 600519.SH, 000001, 000001.SZ
        返回: 600519.SH 格式
        """
        code = code.upper().strip()
        
        # 如果已经有后缀，直接返回
        if '.' in code:
            return code
            
        # 根据代码规则判断交易所
        if code.startswith('6'):
            return f"{code}.SH"  # 沪市
        elif code.startswith('0') or code.startswith('3'):
            return f"{code}.SZ"  # 深市
        elif code.startswith('8') or code.startswith('4'):
            return f"{code}.BJ"  # 北交所
        else:
            return code
    
    def get_realtime_quote(self, code: str) -> Dict:
        """
        获取单只股票实时行情
        
        Args:
            code: 股票代码，如 "600519" 或 "600519.SH"
            
        Returns:
            Dict: 股票实时数据
        """
        try:
            # 标准化代码
            full_code = self.normalize_code(code)
            
            # 获取实时行情
            df = ak.stock_zh_a_spot_em()
            
            # 查找指定股票
            stock_row = df[df['代码'] == full_code.split('.')[0]]
            
            if stock_row.empty:
                return {"error": f"未找到股票: {code}"}
            
            row = stock_row.iloc[0]
            
            # 构建返回数据
            result = {
                "代码": row['代码'],
                "名称": row['名称'],
                "最新价": row['最新价'],
                "涨跌幅": f"{row['涨跌幅']}%",
                "涨跌额": row['涨跌额'],
                "成交量": f"{row['成交量']/10000:.2f}万手",
                "成交额": f"{row['成交额']/100000000:.2f}亿",
                "振幅": f"{row['振幅']}%",
                "最高": row['最高'],
                "最低": row['最低'],
                "今开": row['今开'],
                "昨收": row['昨收'],
                "换手率": f"{row['换手率']}%",
                "市盈率": row['市盈率-动态'],
                "市净率": row['市净率'],
                "总市值": f"{row['总市值']/100000000:.2f}亿",
                "流通市值": f"{row['流通市值']/100000000:.2f}亿",
                "更新时间": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            }
            
            return result
            
        except Exception as e:
            return {"error": f"查询失败: {str(e)}"}
    
    def get_kline(self, code: str, period: str = "daily", 
                  start_date: Optional[str] = None, 
                  end_date: Optional[str] = None,
                  adjust: str = "") -> List[Dict]:
        """
        获取历史K线数据
        
        Args:
            code: 股票代码
            period: 周期 daily/weekly/monthly
            start_date: 开始日期 YYYYMMDD
            end_date: 结束日期 YYYYMMDD
            adjust: 复权方式 qfq(前复权)/hfq(后复权)/空(不复权)
            
        Returns:
            List[Dict]: K线数据列表
        """
        try:
            # 默认最近30天
            if not end_date:
                end_date = datetime.now().strftime("%Y%m%d")
            if not start_date:
                start_date = (datetime.now() - timedelta(days=30)).strftime("%Y%m%d")
            
            # 获取K线数据
            df = ak.stock_zh_a_hist(
                symbol=code.split('.')[0],
                period=period,
                start_date=start_date,
                end_date=end_date,
                adjust=adjust
            )
            
            # 转换为列表
            records = df.to_dict('records')
            return records
            
        except Exception as e:
            return [{"error": f"获取K线失败: {str(e)}"}]
    
    def get_stock_list(self) -> List[Dict]:
        """
        获取A股全市场股票列表
        
        Returns:
            List[Dict]: 股票列表
        """
        try:
            df = ak.stock_zh_a_spot_em()
            
            # 只保留关键字段
            stocks = []
            for _, row in df.iterrows():
                stocks.append({
                    "代码": row['代码'],
                    "名称": row['名称'],
                    "最新价": row['最新价'],
                    "涨跌幅": row['涨跌幅'],
                    "换手率": row['换手率'],
                    "总市值": row['总市值'],
                    "市盈率": row['市盈率-动态']
                })
            
            return stocks
            
        except Exception as e:
            return [{"error": f"获取股票列表失败: {str(e)}"}]
    
    def get_top_gainers(self, limit: int = 20) -> List[Dict]:
        """
        获取涨幅榜
        
        Args:
            limit: 返回数量
            
        Returns:
            List[Dict]: 涨幅排行榜
        """
        try:
            df = ak.stock_zh_a_spot_em()
            
            # 按涨跌幅排序，取涨幅最大的
            df_sorted = df.nlargest(limit, '涨跌幅')
            
            results = []
            for _, row in df_sorted.iterrows():
                results.append({
                    "排名": len(results) + 1,
                    "代码": row['代码'],
                    "名称": row['名称'],
                    "最新价": row['最新价'],
                    "涨跌幅": f"{row['涨跌幅']}%",
                    "成交量": f"{row['成交量']/10000:.0f}万手"
                })
            
            return results
            
        except Exception as e:
            return [{"error": f"获取涨幅榜失败: {str(e)}"}]
    
    def get_top_losers(self, limit: int = 20) -> List[Dict]:
        """
        获取跌幅榜
        
        Args:
            limit: 返回数量
            
        Returns:
            List[Dict]: 跌幅排行榜
        """
        try:
            df = ak.stock_zh_a_spot_em()
            
            # 按涨跌幅排序，取跌幅最大的
            df_sorted = df.nsmallest(limit, '涨跌幅')
            
            results = []
            for _, row in df_sorted.iterrows():
                results.append({
                    "排名": len(results) + 1,
                    "代码": row['代码'],
                    "名称": row['名称'],
                    "最新价": row['最新价'],
                    "涨跌幅": f"{row['涨跌幅']}%",
                    "成交量": f"{row['成交量']/10000:.0f}万手"
                })
            
            return results
            
        except Exception as e:
            return [{"error": f"获取跌幅榜失败: {str(e)}"}]
    
    def get_sector_performance(self) -> List[Dict]:
        """
        获取行业板块表现
        
        Returns:
            List[Dict]: 板块涨幅排名
        """
        try:
            # 获取板块数据
            df = ak.stock_board_industry_name_em()
            
            results = []
            for _, row in df.head(20).iterrows():
                results.append({
                    "板块": row['板块名称'],
                    "涨跌幅": row.get('涨跌幅', 'N/A'),
                    "领涨股": row.get('领涨股', 'N/A')
                })
            
            return results
            
        except Exception as e:
            return [{"error": f"获取板块数据失败: {str(e)}"}]
    
    def search_stock(self, keyword: str) -> List[Dict]:
        """
        搜索股票
        
        Args:
            keyword: 搜索关键词(代码或名称)
            
        Returns:
            List[Dict]: 匹配的股票列表
        """
        try:
            df = ak.stock_zh_a_spot_em()
            
            # 按代码或名称搜索
            keyword = keyword.strip()
            mask = df['代码'].str.contains(keyword) | df['名称'].str.contains(keyword)
            matched = df[mask]
            
            results = []
            for _, row in matched.head(10).iterrows():
                results.append({
                    "代码": row['代码'],
                    "名称": row['名称'],
                    "最新价": row['最新价'],
                    "涨跌幅": f"{row['涨跌幅']}%"
                })
            
            return results
            
        except Exception as e:
            return [{"error": f"搜索失败: {str(e)}"}]


def print_dict(data: Dict, indent: int = 0):
    """美观打印字典"""
    for key, value in data.items():
        if isinstance(value, dict):
            print("  " * indent + f"{key}:")
            print_dict(value, indent + 1)
        else:
            print("  " * indent + f"{key}: {value}")


def main():
    """命令行入口"""
    parser = argparse.ArgumentParser(
        description="A股实时行情查询工具 - 基于AKShare免费数据源",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python3 cn_stock_tool.py quote 600519         # 查询茅台实时行情
  python3 cn_stock_tool.py quote 000001         # 查询平安银行
  python3 cn_stock_tool.py kline 600519 -d 60   # 查询60日K线
  python3 cn_stock_tool.py gainers              # 查看涨幅榜
  python3 cn_stock_tool.py losers               # 查看跌幅榜
  python3 cn_stock_tool.py search 茅台          # 搜索股票
  python3 cn_stock_tool.py sectors              # 查看板块
        """
    )
    
    subparsers = parser.add_subparsers(dest='command', help='可用命令')
    
    # quote 命令
    quote_parser = subparsers.add_parser('quote', help='查询单只股票实时行情')
    quote_parser.add_argument('code', help='股票代码，如600519')
    
    # kline 命令
    kline_parser = subparsers.add_parser('kline', help='查询历史K线')
    kline_parser.add_argument('code', help='股票代码')
    kline_parser.add_argument('-d', '--days', type=int, default=30, help='查询天数')
    kline_parser.add_argument('-a', '--adjust', choices=['', 'qfq', 'hfq'], 
                             default='', help='复权方式')
    
    # list 命令
    list_parser = subparsers.add_parser('list', help='获取全市场股票列表')
    list_parser.add_argument('-n', '--limit', type=int, default=50, help='返回数量')
    
    # gainers 命令
    gainers_parser = subparsers.add_parser('gainers', help='查看涨幅榜')
    gainers_parser.add_argument('-n', '--limit', type=int, default=20, help='返回数量')
    
    # losers 命令
    losers_parser = subparsers.add_parser('losers', help='查看跌幅榜')
    losers_parser.add_argument('-n', '--limit', type=int, default=20, help='返回数量')
    
    # search 命令
    search_parser = subparsers.add_parser('search', help='搜索股票')
    search_parser.add_argument('keyword', help='搜索关键词')
    
    # sectors 命令
    subparsers.add_parser('sectors', help='查看行业板块表现')
    
    args = parser.parse_args()
    
    if not args.command:
        parser.print_help()
        return
    
    # 初始化工具
    tool = CNStockTool()
    
    # 执行命令
    if args.command == 'quote':
        result = tool.get_realtime_quote(args.code)
        if 'error' in result:
            print(f"❌ {result['error']}")
        else:
            print("\n📈 股票实时行情")
            print("=" * 40)
            print_dict(result)
    
    elif args.command == 'kline':
        start_date = (datetime.now() - timedelta(days=args.days)).strftime("%Y%m%d")
        end_date = datetime.now().strftime("%Y%m%d")
        results = tool.get_kline(args.code, start_date=start_date, 
                                end_date=end_date, adjust=args.adjust)
        if results and 'error' in results[0]:
            print(f"❌ {results[0]['error']}")
        else:
            print(f"\n📊 {args.code} 最近{args.days}日K线数据")
            print("=" * 60)
            for record in results[-10:]:  # 只显示最近10条
                print(f"{record['日期']} 收:{record['收盘']} 涨:{record['涨跌幅']}%")
    
    elif args.command == 'list':
        results = tool.get_stock_list()
        print(f"\n📋 A股全市场股票列表 (前{args.limit}只)")
        print("=" * 80)
        print(f"{'代码':<10} {'名称':<10} {'最新价':<10} {'涨跌幅':<10} {'换手率':<10}")
        print("-" * 80)
        for stock in results[:args.limit]:
            print(f"{stock['代码']:<10} {stock['名称']:<10} {stock['最新价']:<10.2f} "
                  f"{stock['涨跌幅']:<10.2f}% {stock['换手率']:<10.2f}%")
    
    elif args.command == 'gainers':
        results = tool.get_top_gainers(args.limit)
        print(f"\n🚀 A股涨幅榜 Top {args.limit}")
        print("=" * 60)
        print(f"{'排名':<6} {'代码':<10} {'名称':<10} {'最新价':<10} {'涨跌幅':<10}")
        print("-" * 60)
        for stock in results:
            print(f"{stock['排名']:<6} {stock['代码']:<10} {stock['名称']:<10} "
                  f"{stock['最新价']:<10.2f} {stock['涨跌幅']:<10}")
    
    elif args.command == 'losers':
        results = tool.get_top_losers(args.limit)
        print(f"\n📉 A股跌幅榜 Top {args.limit}")
        print("=" * 60)
        print(f"{'排名':<6} {'代码':<10} {'名称':<10} {'最新价':<10} {'涨跌幅':<10}")
        print("-" * 60)
        for stock in results:
            print(f"{stock['排名']:<6} {stock['代码']:<10} {stock['名称']:<10} "
                  f"{stock['最新价']:<10.2f} {stock['涨跌幅']:<10}")
    
    elif args.command == 'search':
        results = tool.search_stock(args.keyword)
        if results and 'error' in results[0]:
            print(f"❌ {results[0]['error']}")
        else:
            print(f"\n🔍 搜索 '{args.keyword}' 结果")
            print("=" * 60)
            for stock in results:
                print(f"{stock['代码']} {stock['名称']} 最新价:{stock['最新价']} {stock['涨跌幅']}")
    
    elif args.command == 'sectors':
        results = tool.get_sector_performance()
        if results and 'error' in results[0]:
            print(f"❌ {results[0]['error']}")
        else:
            print("\n🏭 行业板块表现")
            print("=" * 40)
            for sector in results:
                print(f"{sector['板块']}: {sector['涨跌幅']}")


if __name__ == "__main__":
    main()
