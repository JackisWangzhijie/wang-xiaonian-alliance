#!/usr/bin/env python3
"""
ClawWork Mini - 任务管理器
管理模拟的工作任务
"""

import json
import random
from typing import Dict, List, Optional
from dataclasses import dataclass
from pathlib import Path


@dataclass
class Task:
    """工作任务"""
    id: str
    title: str
    category: str           # 行业分类
    description: str        # 任务描述
    deliverable: str        # 交付物要求
    estimated_hours: float  # 预计工时
    hourly_wage: float      # 时薪 (BLS标准)
    max_payment: float      # 最高报酬
    difficulty: int         # 难度 1-5


class TaskManager:
    """任务管理器 - 提供工作任务"""
    
    # 模拟的 BLS 时薪数据 (美元)
    BLS_WAGES = {
        "manufacturing": 35.50,      # 制造业
        "finance": 52.80,            # 金融
        "healthcare": 42.30,         # 医疗
        "technology": 58.90,         # 科技
        "retail": 18.50,             # 零售
        "government": 38.20,         # 政府
        "education": 32.10,          # 教育
        "legal": 65.40,              # 法律
    }
    
    # 模拟任务库
    SAMPLE_TASKS = [
        {
            "title": "市场分析报告",
            "category": "finance",
            "description": "分析某上市公司的财务状况，撰写投资分析报告",
            "deliverable": "PDF报告 (3-5页)",
            "estimated_hours": 4,
            "difficulty": 3
        },
        {
            "title": "采购清单整理",
            "category": "manufacturing",
            "description": "整理供应商报价，制作采购对比表",
            "deliverable": "Excel表格",
            "estimated_hours": 2,
            "difficulty": 2
        },
        {
            "title": "客户数据处理",
            "category": "retail",
            "description": "清洗和分类客户数据，生成统计图表",
            "deliverable": "Excel + 图表",
            "estimated_hours": 3,
            "difficulty": 2
        },
        {
            "title": "政策合规审查",
            "category": "legal",
            "description": "审查公司政策文件，标注合规风险点",
            "deliverable": "标注文档",
            "estimated_hours": 5,
            "difficulty": 4
        },
        {
            "title": "医疗数据录入",
            "category": "healthcare",
            "description": "将手写病历录入电子系统",
            "deliverable": "结构化数据文件",
            "estimated_hours": 6,
            "difficulty": 2
        },
        {
            "title": "代码审查报告",
            "category": "technology",
            "description": "审查Python项目代码，找出潜在bug和优化点",
            "deliverable": "Markdown报告",
            "estimated_hours": 3,
            "difficulty": 4
        },
        {
            "title": "培训材料制作",
            "category": "education",
            "description": "为新员工制作入职培训PPT",
            "deliverable": "PPT文件 (10-15页)",
            "estimated_hours": 5,
            "difficulty": 3
        },
        {
            "title": "行政文档整理",
            "category": "government",
            "description": "整理会议记录，撰写会议纪要",
            "deliverable": "Word文档",
            "estimated_hours": 2,
            "difficulty": 2
        },
    ]
    
    def __init__(self):
        self.tasks: List[Task] = []
        self.completed_tasks: List[str] = []
        self._init_tasks()
    
    def _init_tasks(self):
        """初始化任务列表"""
        for i, task_data in enumerate(self.SAMPLE_TASKS):
            category = task_data["category"]
            hourly_wage = self.BLS_WAGES.get(category, 30.0)
            estimated_hours = task_data["estimated_hours"]
            
            task = Task(
                id=f"task_{i:03d}",
                title=task_data["title"],
                category=category,
                description=task_data["description"],
                deliverable=task_data["deliverable"],
                estimated_hours=estimated_hours,
                hourly_wage=hourly_wage,
                max_payment=estimated_hours * hourly_wage,
                difficulty=task_data["difficulty"]
            )
            self.tasks.append(task)
    
    def get_task(self, task_id: Optional[str] = None) -> Optional[Task]:
        """
        获取任务
        
        Args:
            task_id: 特定任务ID，None则随机分配
            
        Returns:
            Task 对象或 None
        """
        if task_id:
            for task in self.tasks:
                if task.id == task_id:
                    return task
            return None
        
        # 随机分配未完成的任务
        available = [t for t in self.tasks if t.id not in self.completed_tasks]
        if not available:
            return None
        
        return random.choice(available)
    
    def complete_task(self, task_id: str):
        """标记任务完成"""
        if task_id not in self.completed_tasks:
            self.completed_tasks.append(task_id)
    
    def list_tasks(self) -> List[Dict]:
        """列出所有任务"""
        return [
            {
                "id": t.id,
                "title": t.title,
                "category": t.category,
                "max_payment": f"${t.max_payment:.2f}",
                "difficulty": "⭐" * t.difficulty,
                "status": "✅" if t.id in self.completed_tasks else "⏳"
            }
            for t in self.tasks
        ]
    
    def display_task(self, task: Task):
        """显示任务详情"""
        print(f"\n📋 任务: {task.title}")
        print(f"   ID: {task.id}")
        print(f"   行业: {task.category}")
        print(f"   难度: {'⭐' * task.difficulty}")
        print(f"   预计工时: {task.estimated_hours} 小时")
        print(f"   时薪: ${task.hourly_wage}/小时")
        print(f"   最高报酬: ${task.max_payment:.2f}")
        print(f"\n📝 任务描述:")
        print(f"   {task.description}")
        print(f"\n📤 交付物:")
        print(f"   {task.deliverable}")
        print()


class SimpleEvaluator:
    """简化版评估器"""
    
    def evaluate(self, task: Task, work_result: str) -> Dict:
        """
        评估工作成果
        
        简化版：基于结果长度和内容质量进行模拟评分
        真实版应使用 GPT-4 进行评估
        """
        # 基础分数
        base_score = 0.6
        
        # 长度加分 (假设更详细的回答质量更高)
        length_bonus = min(len(work_result) / 1000, 0.2)
        
        # 结构加分 (检查是否有标题、列表等)
        structure_bonus = 0.0
        if "#" in work_result or "##" in work_result:  # Markdown 标题
            structure_bonus += 0.05
        if "- " in work_result or "1." in work_result:  # 列表
            structure_bonus += 0.05
        if "```" in work_result:  # 代码块
            structure_bonus += 0.05
        
        # 随机波动 (模拟真实评估的不确定性)
        random_factor = random.uniform(-0.05, 0.05)
        
        quality_score = min(base_score + length_bonus + structure_bonus + random_factor, 1.0)
        
        # 计算报酬
        payment = quality_score * task.max_payment
        
        return {
            "quality_score": round(quality_score, 2),
            "payment": round(payment, 2),
            "breakdown": {
                "base": base_score,
                "length_bonus": round(length_bonus, 3),
                "structure_bonus": structure_bonus,
                "max_payment": task.max_payment
            }
        }


if __name__ == "__main__":
    # 测试
    manager = TaskManager()
    evaluator = SimpleEvaluator()
    
    print("📋 ClawWork Mini 任务管理器")
    print("="*50)
    
    # 显示任务列表
    print("\n可用任务:")
    for task_info in manager.list_tasks():
        print(f"  {task_info['status']} {task_info['id']}: {task_info['title']}")
        print(f"     报酬: {task_info['max_payment']} | 难度: {task_info['difficulty']}")
    
    # 获取一个任务
    task = manager.get_task()
    if task:
        manager.display_task(task)
        
        # 模拟工作成果
        sample_work = """
# 市场分析报告

## 1. 执行摘要
本报告分析了某上市公司的财务状况...

## 2. 财务数据
- 营收增长: 15%
- 利润率: 22%
- 现金流: 健康

## 3. 投资建议
建议买入，目标价 $150
"""
        result = evaluator.evaluate(task, sample_work)
        print("📊 评估结果:")
        print(f"   质量评分: {result['quality_score']}/1.0")
        print(f"   获得报酬: ${result['payment']}")
