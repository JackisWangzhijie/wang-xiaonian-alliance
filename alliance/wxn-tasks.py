#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
王小年联盟 - 共享任务板
联盟成员间任务协作和分配
"""

import os
import sys
import json
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional
from dataclasses import dataclass, asdict
from enum import Enum

class TaskStatus(Enum):
    PENDING = "待处理"
    ASSIGNED = "已分配"
    IN_PROGRESS = "进行中"
    REVIEW = "待审核"
    COMPLETED = "已完成"
    BLOCKED = "阻塞"

class TaskPriority(Enum):
    LOW = 1
    NORMAL = 2
    HIGH = 3
    URGENT = 4

@dataclass
class Task:
    """联盟任务格式"""
    id: str
    title: str
    description: str
    creator: str
    assignee: Optional[str]
    status: str
    priority: int
    created_at: str
    updated_at: str
    deadline: Optional[str]
    tags: List[str]
    comments: List[Dict]
    progress: int  # 0-100

def generate_task_id() -> str:
    """生成任务ID"""
    timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
    import random
    rand = random.randint(1000, 9999)
    return f"TASK-{timestamp}-{rand}"

class AllianceTaskBoard:
    """联盟任务板"""
    
    def __init__(self):
        self.workspace = Path(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
        self.tasks_dir = self.workspace / "sync" / "alliance" / "shared-tasks"
        self.tasks_file = self.tasks_dir / "tasks.json"
        
        self.member_id = self._get_member_id()
        
        self.tasks_dir.mkdir(parents=True, exist_ok=True)
        self.tasks = self._load_tasks()
    
    def _get_member_id(self) -> str:
        """获取成员ID"""
        config_file = self.workspace / "sync" / "sync.conf"
        if config_file.exists():
            with open(config_file, 'r', encoding='utf-8') as f:
                for line in f:
                    if line.strip().startswith('device_id'):
                        return line.split('=')[1].strip()
        return "wangxiaonian-unknown"
    
    def _load_tasks(self) -> List[Task]:
        """加载任务列表"""
        if not self.tasks_file.exists():
            return []
        
        try:
            with open(self.tasks_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                return [Task(**task) for task in data]
        except:
            return []
    
    def _save_tasks(self):
        """保存任务列表"""
        data = [asdict(task) for task in self.tasks]
        with open(self.tasks_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
    
    def create(self, title: str, description: str = "",
               assignee: str = None, priority: int = 2,
               deadline: str = None, tags: List[str] = None) -> Task:
        """
        创建新任务
        
        Args:
            title: 任务标题
            description: 任务描述
            assignee: 分配给谁，None表示待分配
            priority: 优先级 1-4
            deadline: 截止日期 (ISO格式)
            tags: 标签列表
        """
        task = Task(
            id=generate_task_id(),
            title=title,
            description=description,
            creator=self.member_id,
            assignee=assignee,
            status=TaskStatus.PENDING.value if not assignee else TaskStatus.ASSIGNED.value,
            priority=priority,
            created_at=datetime.now().isoformat(),
            updated_at=datetime.now().isoformat(),
            deadline=deadline,
            tags=tags or [],
            comments=[],
            progress=0
        )
        
        self.tasks.append(task)
        self._save_tasks()
        
        print(f"[OK] 任务已创建: {task.id}")
        print(f"     标题: {title}")
        if assignee:
            print(f"     分配给: {assignee}")
        
        return task
    
    def assign(self, task_id: str, assignee: str):
        """分配任务"""
        task = self._find_task(task_id)
        if not task:
            print(f"[ERR] 任务不存在: {task_id}")
            return False
        
        task.assignee = assignee
        task.status = TaskStatus.ASSIGNED.value
        task.updated_at = datetime.now().isoformat()
        
        # 添加评论记录
        task.comments.append({
            'author': self.member_id,
            'content': f'任务分配给 {assignee}',
            'timestamp': datetime.now().isoformat()
        })
        
        self._save_tasks()
        print(f"[OK] 任务 {task_id} 已分配给 {assignee}")
        return True
    
    def start(self, task_id: str):
        """开始处理任务"""
        task = self._find_task(task_id)
        if not task:
            print(f"[ERR] 任务不存在: {task_id}")
            return False
        
        if task.assignee != self.member_id:
            print(f"[WARN] 任务未分配给你，当前分配给: {task.assignee}")
            return False
        
        task.status = TaskStatus.IN_PROGRESS.value
        task.updated_at = datetime.now().isoformat()
        task.comments.append({
            'author': self.member_id,
            'content': '开始处理任务',
            'timestamp': datetime.now().isoformat()
        })
        
        self._save_tasks()
        print(f"[OK] 任务 {task_id} 状态更新为: 进行中")
        return True
    
    def update_progress(self, task_id: str, progress: int, comment: str = None):
        """更新任务进度"""
        task = self._find_task(task_id)
        if not task:
            print(f"[ERR] 任务不存在: {task_id}")
            return False
        
        task.progress = max(0, min(100, progress))
        task.updated_at = datetime.now().isoformat()
        
        if comment:
            task.comments.append({
                'author': self.member_id,
                'content': f'进度更新 {progress}%: {comment}',
                'timestamp': datetime.now().isoformat()
            })
        
        self._save_tasks()
        print(f"[OK] 任务 {task_id} 进度: {progress}%")
        return True
    
    def complete(self, task_id: str, comment: str = None):
        """完成任务"""
        task = self._find_task(task_id)
        if not task:
            print(f"[ERR] 任务不存在: {task_id}")
            return False
        
        task.status = TaskStatus.COMPLETED.value
        task.progress = 100
        task.updated_at = datetime.now().isoformat()
        
        task.comments.append({
            'author': self.member_id,
            'content': f'任务完成{f": {comment}" if comment else ""}',
            'timestamp': datetime.now().isoformat()
        })
        
        self._save_tasks()
        print(f"[OK] 任务 {task_id} 已完成！")
        return True
    
    def list(self, status: str = None, assignee: str = None, 
             my_tasks: bool = False):
        """
        列出任务
        
        Args:
            status: 按状态筛选
            assignee: 按负责人筛选
            my_tasks: 只显示我的任务
        """
        tasks = self.tasks
        
        if status:
            tasks = [t for t in tasks if t.status == status]
        if assignee:
            tasks = [t for t in tasks if t.assignee == assignee]
        if my_tasks:
            tasks = [t for t in tasks if t.assignee == self.member_id]
        
        # 按优先级和时间排序
        tasks.sort(key=lambda x: (-x.priority, x.created_at), reverse=True)
        
        print("\n" + "="*70)
        print(f"  王小年联盟 - 任务板 ({len(tasks)} 个任务)")
        print("="*70)
        
        if not tasks:
            print("  暂无任务")
            print("="*70 + "\n")
            return
        
        # 优先级图标
        priority_map = {4: "[!]", 3: "[H]", 2: "[N]", 1: "[L]"}
        
        for task in tasks:
            p_icon = priority_map.get(task.priority, "[?]")
            status_short = task.status[:4]
            assignee_short = task.assignee[:10] if task.assignee else "未分配"
            progress_bar = self._progress_bar(task.progress)
            
            print(f"\n  {p_icon} [{task.id}] {task.title}")
            print(f"     状态: {status_short} | 负责人: {assignee_short}")
            print(f"     进度: {progress_bar} {task.progress}%")
            if task.tags:
                print(f"     标签: {', '.join(task.tags)}")
        
        print("\n" + "="*70)
        print(f"  图例: [!]=紧急 [H]=高 [N]=普通 [L]=低")
        print("="*70 + "\n")
    
    def _progress_bar(self, progress: int, width: int = 20) -> str:
        """生成进度条"""
        filled = int(width * progress / 100)
        return "[" + "=" * filled + " " * (width - filled) + "]"
    
    def view(self, task_id: str):
        """查看任务详情"""
        task = self._find_task(task_id)
        if not task:
            print(f"[ERR] 任务不存在: {task_id}")
            return
        
        print("\n" + "="*70)
        print(f"  任务详情: {task.id}")
        print("="*70)
        print(f"  标题: {task.title}")
        print(f"  描述: {task.description or '无'}")
        print(f"  创建者: {task.creator}")
        print(f"  负责人: {task.assignee or '待分配'}")
        print(f"  状态: {task.status}")
        print(f"  优先级: {task.priority}/4")
        print(f"  进度: {self._progress_bar(task.progress)} {task.progress}%")
        print(f"  创建时间: {task.created_at[:19]}")
        print(f"  更新时间: {task.updated_at[:19]}")
        if task.deadline:
            print(f"  截止日期: {task.deadline}")
        if task.tags:
            print(f"  标签: {', '.join(task.tags)}")
        
        if task.comments:
            print("\n  评论记录:")
            for comment in task.comments[-5:]:  # 显示最近5条
                print(f"    [{comment['timestamp'][11:16]}] {comment['author']}: {comment['content']}")
        
        print("="*70 + "\n")
    
    def _find_task(self, task_id: str) -> Optional[Task]:
        """查找任务"""
        for task in self.tasks:
            if task.id == task_id or task.id.startswith(task_id):
                return task
        return None
    
    def delete(self, task_id: str):
        """删除任务"""
        task = self._find_task(task_id)
        if not task:
            print(f"[ERR] 任务不存在: {task_id}")
            return False
        
        self.tasks.remove(task)
        self._save_tasks()
        print(f"[OK] 任务 {task_id} 已删除")
        return True
    
    def get_stats(self):
        """获取任务统计"""
        total = len(self.tasks)
        by_status = {}
        by_assignee = {}
        
        for task in self.tasks:
            by_status[task.status] = by_status.get(task.status, 0) + 1
            assignee = task.assignee or "未分配"
            by_assignee[assignee] = by_assignee.get(assignee, 0) + 1
        
        print("\n" + "="*50)
        print("  任务统计")
        print("="*50)
        print(f"  总计: {total} 个任务")
        print("\n  按状态:")
        for status, count in sorted(by_status.items()):
            print(f"    {status}: {count}")
        print("\n  按负责人:")
        for assignee, count in sorted(by_assignee.items()):
            print(f"    {assignee}: {count}")
        print("="*50 + "\n")

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="王小年联盟任务板")
    parser.add_argument("action", choices=[
        "create", "list", "view", "assign", "start", 
        "progress", "complete", "delete", "stats", "my"
    ])
    parser.add_argument("--id", help="任务ID")
    parser.add_argument("--title", "-t", help="任务标题")
    parser.add_argument("--desc", "-d", help="任务描述")
    parser.add_argument("--assignee", "-a", help="负责人")
    parser.add_argument("--priority", "-p", type=int, default=2,
                       choices=[1, 2, 3, 4])
    parser.add_argument("--tags", help="标签（逗号分隔）")
    parser.add_argument("--progress", type=int, help="进度 0-100")
    parser.add_argument("--comment", "-c", help="评论")
    parser.add_argument("--status", "-s", help="状态筛选")
    
    args = parser.parse_args()
    
    board = AllianceTaskBoard()
    
    if args.action == "create":
        if not args.title:
            print("[ERR] 请提供任务标题: -t '标题'")
            return
        tags = args.tags.split(',') if args.tags else None
        board.create(args.title, args.desc, args.assignee, 
                    args.priority, tags=tags)
    
    elif args.action == "list":
        board.list(args.status, args.assignee)
    
    elif args.action == "my":
        board.list(my_tasks=True)
    
    elif args.action == "view":
        if not args.id:
            print("[ERR] 请提供任务ID: --id TASK-xxx")
            return
        board.view(args.id)
    
    elif args.action == "assign":
        if not args.id or not args.assignee:
            print("[ERR] 请提供任务ID和负责人: --id xxx -a member")
            return
        board.assign(args.id, args.assignee)
    
    elif args.action == "start":
        if not args.id:
            print("[ERR] 请提供任务ID: --id TASK-xxx")
            return
        board.start(args.id)
    
    elif args.action == "progress":
        if not args.id or args.progress is None:
            print("[ERR] 请提供任务ID和进度: --id xxx --progress 50")
            return
        board.update_progress(args.id, args.progress, args.comment)
    
    elif args.action == "complete":
        if not args.id:
            print("[ERR] 请提供任务ID: --id TASK-xxx")
            return
        board.complete(args.id, args.comment)
    
    elif args.action == "delete":
        if not args.id:
            print("[ERR] 请提供任务ID: --id TASK-xxx")
            return
        board.delete(args.id)
    
    elif args.action == "stats":
        board.get_stats()

if __name__ == "__main__":
    main()
