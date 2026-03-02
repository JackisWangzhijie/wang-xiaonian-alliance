#!/usr/bin/env python3
"""
ClawWork Mini - Agent 主程序
AI 经济压力测试框架简化版
"""

import sys
import time
import random
from typing import Dict, Optional
from datetime import datetime

from economic_tracker import EconomicTracker
from task_manager import TaskManager, SimpleEvaluator


class ClawWorkAgent:
    """
    ClawWork Agent - 必须在经济压力下生存的 AI
    
    核心理念:
    1. 初始资金 $10
    2. 每次行动消耗 tokens (扣钱)
    3. 完成任务获得报酬
    4. 余额归零 = 破产淘汰
    """
    
    def __init__(self, name: str = "ClawWorker"):
        self.name = name
        self.tracker = EconomicTracker(save_path=f"{name}_state.json")
        self.task_manager = TaskManager()
        self.evaluator = SimpleEvaluator()
        self.memory: list = []  # 学习记忆
        self.day = 0
        
    def think(self, message: str) -> str:
        """
        思考过程 - 每次思考消耗 tokens
        
        模拟: 每 100 字符 ≈ 25 tokens
        """
        token_count = len(message) // 4
        success = self.tracker.charge_tokens(token_count, "thinking")
        
        if not success:
            return "💀 破产！无法继续思考。"
        
        return f"💭 思考完成 (消耗 {token_count} tokens)"
    
    def decide_activity(self) -> str:
        """
        决策: 工作 vs 学习
        
        策略:
        - 余额充足 (> $5): 优先工作
        - 余额紧张 (< $3): 必须工作
        - 余额充裕 (> $20): 可以投资学习
        """
        balance = self.tracker.state.balance
        
        self.think(f"当前余额 ${balance:.2f}，决策工作还是学习")
        
        if balance < 3:
            decision = "work"  # 必须工作赚钱
            reason = "余额不足，必须工作赚钱"
        elif balance > 20:
            decision = "learn"  # 可以学习提升
            reason = "余额充裕，投资学习提升能力"
        else:
            decision = "work"  # 默认工作
            reason = "稳健策略，先赚钱"
        
        print(f"🤔 决策: {reason}")
        return decision
    
    def work(self, task_id: Optional[str] = None) -> Dict:
        """
        执行工作任务
        """
        # 获取任务
        task = self.task_manager.get_task(task_id)
        if not task:
            return {"success": False, "error": "没有可用任务"}
        
        print(f"\n🎯 开始任务: {task.title}")
        print(f"   预期收入: ${task.max_payment:.2f}")
        
        # 思考任务策略
        self.think(f"如何完成: {task.description}")
        
        # 模拟工作过程
        work_result = self._simulate_work(task)
        
        # 提交成果评估
        print("📤 提交成果...")
        eval_result = self.evaluator.evaluate(task, work_result)
        
        # 获得报酬
        payment = eval_result["payment"]
        quality = eval_result["quality_score"]
        
        self.tracker.earn(payment, task.id, quality)
        self.task_manager.complete_task(task.id)
        
        print(f"📊 质量评分: {quality}/1.0")
        print(f"💰 获得报酬: ${payment:.2f}")
        
        return {
            "success": True,
            "task": task,
            "quality": quality,
            "payment": payment
        }
    
    def _simulate_work(self, task) -> str:
        """
        模拟工作过程
        
        真实场景: 这里应该调用 LLM 生成实际工作成果
        简化版: 根据任务类型返回模拟结果
        """
        # 模拟思考过程
        for i in range(3):
            self.think(f"工作步骤 {i+1}: 分析{task.category}相关数据")
            time.sleep(0.1)
        
        # 模拟成果 (真实场景应该调用 LLM)
        templates = {
            "finance": """
# 财务分析报告

## 执行摘要
经过详细分析，目标公司财务状况良好。

## 关键指标
- 营收增长率: 15.2%
- 净利润率: 18.5%
- ROE: 22.3%

## 结论
建议持有，目标价位 $125。
""",
            "technology": """
# 代码审查报告

## 发现的问题
1. 内存泄漏风险 (第45行)
2. 异常处理不完善
3. 性能优化建议

## 建议
- 使用上下文管理器
- 添加日志记录
- 优化数据库查询
""",
            "default": """
# 工作成果报告

## 任务概述
已完成 {title} 任务的所有要求。

## 交付内容
1. 数据收集与整理
2. 分析与总结
3. 最终报告

## 质量检查
✅ 符合所有要求
✅ 按时交付
""".format(title=task.title)
        }
        
        return templates.get(task.category, templates["default"])
    
    def learn(self) -> Dict:
        """
        学习新知识
        
        投资未来: 消耗当前 tokens，提升未来能力
        """
        print("\n📚 进入学习模式...")
        
        # 学习需要投入时间和 tokens
        learning_topics = [
            "提升财务分析能力",
            "学习代码优化技巧",
            "研究市场趋势分析方法",
            "掌握文档写作规范",
            "学习数据可视化"
        ]
        
        topic = random.choice(learning_topics)
        print(f"   学习主题: {topic}")
        
        # 学习消耗
        self.think(f"深入学习: {topic}")
        self.think("整理笔记，构建知识体系")
        
        # 保存到记忆
        knowledge = f"[{datetime.now().isoformat()}] 学习了: {topic}"
        self.memory.append(knowledge)
        
        print(f"   ✅ 学习完成，知识已保存")
        print(f"   📖 当前记忆: {len(self.memory)} 条")
        
        return {
            "success": True,
            "topic": topic,
            "memory_count": len(self.memory)
        }
    
    def run_day(self):
        """
        运行一天的工作循环
        """
        self.day += 1
        print(f"\n{'='*50}")
        print(f"📅 第 {self.day} 天 - {self.name}")
        print(f"{'='*50}")
        
        # 显示当前状态
        self.tracker.display_dashboard()
        
        # 检查是否破产
        if self.tracker.state.status == "bankrupt":
            print("💀 已经破产，无法继续")
            return False
        
        # 决策
        decision = self.decide_activity()
        
        # 执行
        if decision == "work":
            result = self.work()
            if not result["success"]:
                print(f"❌ 工作失败: {result.get('error')}")
        else:
            self.learn()
        
        # 显示更新后状态
        self.tracker.display_dashboard()
        
        return True
    
    def run_simulation(self, days: int = 5):
        """
        运行完整模拟
        
        Args:
            days: 模拟天数
        """
        print(f"\n🚀 启动 ClawWork Mini 模拟")
        print(f"   Agent: {self.name}")
        print(f"   初始资金: $10.00")
        print(f"   目标: 生存 {days} 天\n")
        
        for i in range(days):
            if not self.run_day():
                print(f"\n💀 模拟结束 - {self.name} 在第 {i+1} 天破产")
                break
            
            if i < days - 1:
                print("\n" + "-"*50)
                time.sleep(0.5)
        
        # 最终报告
        self._final_report()
    
    def _final_report(self):
        """生成最终报告"""
        status = self.tracker.get_status()
        
        print("\n" + "="*50)
        print("📊 最终报告")
        print("="*50)
        print(f"Agent: {self.name}")
        print(f"生存天数: {self.day}")
        print(f"最终余额: ${status['balance']}")
        print(f"累计收入: ${status['total_income']}")
        print(f"累计成本: ${status['total_cost']}")
        print(f"净利润: ${status['profit']}")
        print(f"利润率: {status['profit_margin']}")
        print(f"效率: {status['efficiency']}")
        print(f"完成任务: {status['tasks_completed']}")
        print(f"记忆条目: {len(self.memory)}")
        
        if status['status'] == 'bankrupt':
            print("\n💀 结局: 破产淘汰")
        elif float(status['balance']) > 100:
            print("\n🏆 结局: 商业大亨")
        elif float(status['balance']) > 50:
            print("\n⭐ 结局: 成功企业家")
        elif float(status['balance']) > 20:
            print("\n✅ 结局: 稳定盈利")
        else:
            print("\n⚠️  结局: 勉强生存")
        
        print("="*50)


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='ClawWork Mini - AI 经济压力测试')
    parser.add_argument('--name', '-n', default='ClawWorker', help='Agent 名称')
    parser.add_argument('--days', '-d', type=int, default=5, help='模拟天数')
    parser.add_argument('--reset', '-r', action='store_true', help='重置状态')
    
    args = parser.parse_args()
    
    # 如果重置，删除状态文件
    if args.reset:
        import os
        state_file = f"{args.name}_state.json"
        if os.path.exists(state_file):
            os.remove(state_file)
            print(f"🗑️  已重置 {args.name} 的状态")
    
    # 创建并运行 Agent
    agent = ClawWorkAgent(name=args.name)
    agent.run_simulation(days=args.days)


if __name__ == "__main__":
    main()
