#!/usr/bin/env python3
"""
ClawWork Mini - 经济追踪器
简化版 AI 经济压力测试框架
"""

import json
import time
from datetime import datetime
from typing import Dict, List, Optional
from dataclasses import dataclass, asdict
from pathlib import Path


@dataclass
class EconomicState:
    """经济状态"""
    balance: float = 10.0           # 当前余额
    total_income: float = 0.0       # 累计收入
    total_cost: float = 0.0         # 累计成本
    tasks_completed: int = 0        # 完成任务数
    tasks_failed: int = 0           # 失败任务数
    days_survived: int = 0          # 生存天数
    start_time: str = ""
    status: str = "active"          # active/bankrupt
    
    @property
    def profit_margin(self) -> float:
        """利润率"""
        if self.total_cost == 0:
            return 0.0
        return (self.total_income - self.total_cost) / self.total_cost
    
    @property
    def efficiency(self) -> float:
        """Token 效率: 每美元产生的收入"""
        if self.total_cost == 0:
            return 0.0
        return self.total_income / self.total_cost


class EconomicTracker:
    """经济追踪器 - 追踪 AI 的财务状况"""
    
    # Token 成本 (每 1K tokens)
    TOKEN_COST_PER_1K = 0.01  # GPT-4o-mini 价格
    
    def __init__(self, save_path: Optional[str] = None):
        self.state = EconomicState()
        self.state.start_time = datetime.now().isoformat()
        self.save_path = save_path or "clawwork_state.json"
        self.history: List[Dict] = []
        self._load_state()
    
    def _load_state(self):
        """加载状态"""
        if Path(self.save_path).exists():
            try:
                with open(self.save_path, 'r') as f:
                    data = json.load(f)
                    self.state = EconomicState(**data['state'])
                    self.history = data.get('history', [])
            except Exception as e:
                print(f"加载状态失败: {e}")
    
    def save_state(self):
        """保存状态"""
        data = {
            'state': asdict(self.state),
            'history': self.history
        }
        with open(self.save_path, 'w') as f:
            json.dump(data, f, indent=2)
    
    def charge_tokens(self, token_count: int, operation: str = "api_call") -> bool:
        """
        扣除 Token 成本
        
        Args:
            token_count: 使用的 token 数量
            operation: 操作描述
            
        Returns:
            是否成功扣除 (False 表示破产)
        """
        cost = (token_count / 1000) * self.TOKEN_COST_PER_1K
        
        if self.state.balance < cost:
            self.state.status = "bankrupt"
            self._log_event("bankrupt", {"reason": "insufficient_funds", "cost": cost})
            self.save_state()
            return False
        
        self.state.balance -= cost
        self.state.total_cost += cost
        
        self._log_event("charge", {
            "operation": operation,
            "tokens": token_count,
            "cost": round(cost, 4),
            "balance": round(self.state.balance, 2)
        })
        
        return True
    
    def earn(self, amount: float, task_id: str, quality_score: float):
        """
        赚取收入
        
        Args:
            amount: 收入金额
            task_id: 任务ID
            quality_score: 质量评分 (0-1)
        """
        self.state.balance += amount
        self.state.total_income += amount
        self.state.tasks_completed += 1
        
        self._log_event("earn", {
            "task_id": task_id,
            "amount": round(amount, 2),
            "quality": round(quality_score, 2),
            "balance": round(self.state.balance, 2)
        })
    
    def _log_event(self, event_type: str, data: Dict):
        """记录事件"""
        self.history.append({
            "time": datetime.now().isoformat(),
            "type": event_type,
            "data": data
        })
        self.save_state()
    
    def get_status(self) -> Dict:
        """获取当前状态"""
        return {
            "balance": round(self.state.balance, 2),
            "total_income": round(self.state.total_income, 2),
            "total_cost": round(self.state.total_cost, 2),
            "profit": round(self.state.total_income - self.state.total_cost, 2),
            "profit_margin": f"{self.state.profit_margin*100:.1f}%",
            "efficiency": f"{self.state.efficiency:.2f}x",
            "tasks_completed": self.state.tasks_completed,
            "status": self.state.status,
            "survival_time": self._get_survival_time()
        }
    
    def _get_survival_time(self) -> str:
        """计算生存时间"""
        try:
            start = datetime.fromisoformat(self.state.start_time)
            elapsed = datetime.now() - start
            hours = elapsed.total_seconds() / 3600
            return f"{hours:.1f} hours"
        except:
            return "unknown"
    
    def display_dashboard(self):
        """显示仪表盘"""
        status = self.get_status()
        
        print("\n" + "="*50)
        print("🎯 CLAWWORK MINI - 经济仪表盘")
        print("="*50)
        print(f"💰 当前余额: ${status['balance']}")
        print(f"📈 累计收入: ${status['total_income']}")
        print(f"📉 累计成本: ${status['total_cost']}")
        print(f"💵 净利润: ${status['profit']}")
        print(f"📊 利润率: {status['profit_margin']}")
        print(f"⚡ 效率: {status['efficiency']}")
        print(f"✅ 完成任务: {status['tasks_completed']}")
        print(f"⏱️  生存时间: {status['survival_time']}")
        print(f"🎮 状态: {'🟢 活跃' if status['status'] == 'active' else '🔴 破产'}")
        print("="*50 + "\n")


if __name__ == "__main__":
    # 测试
    tracker = EconomicTracker()
    
    # 模拟一些操作
    print("🚀 启动 ClawWork Mini 经济追踪器")
    print("初始资金: $10.00\n")
    
    # 模拟 API 调用
    for i in range(5):
        success = tracker.charge_tokens(500, f"api_call_{i}")
        if not success:
            print("💀 破产！")
            break
        time.sleep(0.1)
    
    # 模拟完成任务
    tracker.earn(50.0, "task_001", 0.85)
    tracker.earn(30.0, "task_002", 0.72)
    
    # 显示状态
    tracker.display_dashboard()
