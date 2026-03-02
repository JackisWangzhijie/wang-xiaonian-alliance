#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
智能 Memory 合并器
处理多个 Kimi 分身的记忆合并
"""

import re
import json
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Set
from dataclasses import dataclass, asdict

@dataclass
class MemoryEntry:
    """记忆条目"""
    timestamp: str
    source: str  # 来源设备
    category: str  # 类别: skill, task, preference, etc.
    content: str
    tags: List[str]
    priority: int  # 1-5, 5 为最高

class MemoryMerger:
    """记忆合并器"""
    
    def __init__(self):
        self.entries: List[MemoryEntry] = []
        
    def parse_memory_file(self, file_path: Path, device_id: str) -> List[MemoryEntry]:
        """解析 memory 文件，提取结构化条目"""
        entries = []
        content = file_path.read_text(encoding='utf-8')
        
        # 提取技能学习记录
        skill_pattern = r'\*\*新技能[:：](.+?)\*\*.*?-\s*\*\*掌握时间\*\*[:\uff1a](\d{4}-\d{2}-\d{2})'
        for match in re.finditer(skill_pattern, content, re.DOTALL):
            skill_name = match.group(1).strip()
            date = match.group(2)
            entries.append(MemoryEntry(
                timestamp=f"{date}T00:00:00",
                source=device_id,
                category="skill",
                content=f"学习技能: {skill_name}",
                tags=["skill", "learning"],
                priority=4
            ))
        
        # 提取任务记录
        task_pattern = r'\*\*([^*]+?)\*\*[:\uff1a]\s*(\S+)\s*-\s*(.+?)(?=\n|$)'
        for match in re.finditer(task_pattern, content):
            task_name = match.group(1).strip()
            task_status = match.group(2).strip()
            task_detail = match.group(3).strip()
            entries.append(MemoryEntry(
                timestamp=datetime.now().isoformat(),
                source=device_id,
                category="task",
                content=f"{task_name}: {task_status} - {task_detail}",
                tags=["task", task_status.lower()],
                priority=3
            ))
        
        # 提取用户偏好
        pref_pattern = r'\*\*([^*]+?)\*\*[:\uff1a]\s*(.+?)(?=\n|$)'
        for match in re.finditer(pref_pattern, content):
            key = match.group(1).strip()
            value = match.group(2).strip()
            if any(k in key.lower() for k in ['偏好', '喜欢', '语言', '风格', '环境']):
                entries.append(MemoryEntry(
                    timestamp=datetime.now().isoformat(),
                    source=device_id,
                    category="preference",
                    content=f"{key}: {value}",
                    tags=["preference"],
                    priority=5
                ))
        
        # 提取会话摘要
        session_pattern = r'#{3,}\s*(.+?)会话.*?-\s*\*\*时间\*\*[:\uff1a]\s*(\d{4}-\d{2}-\d{2})'
        for match in re.finditer(session_pattern, content, re.DOTALL):
            session_name = match.group(1).strip()
            date = match.group(2)
            entries.append(MemoryEntry(
                timestamp=f"{date}T00:00:00",
                source=device_id,
                category="session",
                content=f"会话: {session_name}",
                tags=["session", "history"],
                priority=2
            ))
        
        return entries
    
    def deduplicate(self, entries: List[MemoryEntry]) -> List[MemoryEntry]:
        """去重：相同内容的条目只保留最新的"""
        seen: Dict[str, MemoryEntry] = {}
        
        for entry in entries:
            # 使用内容和类别作为唯一键
            key = f"{entry.category}:{entry.content[:100]}"
            
            if key in seen:
                # 保留时间更新的
                if entry.timestamp > seen[key].timestamp:
                    seen[key] = entry
            else:
                seen[key] = entry
        
        return list(seen.values())
    
    def merge_entries(self, all_entries: List[MemoryEntry]) -> List[MemoryEntry]:
        """合并多个来源的条目"""
        # 按时间排序
        sorted_entries = sorted(all_entries, key=lambda x: x.timestamp, reverse=True)
        
        # 去重
        unique_entries = self.deduplicate(sorted_entries)
        
        # 按优先级和时间排序
        return sorted(unique_entries, key=lambda x: (-x.priority, x.timestamp), reverse=True)
    
    def generate_merged_memory(self, entries: List[MemoryEntry]) -> str:
        """生成合并后的 memory.md 内容"""
        sections = {
            'preference': [],
            'skill': [],
            'task': [],
            'session': [],
            'other': []
        }
        
        for entry in entries:
            section = sections.get(entry.category, sections['other'])
            section.append(entry)
        
        output = []
        output.append("# Kimi 对话记忆档案（合并版）")
        output.append("")
        output.append("> 本文件由多个 Kimi 分身的记忆自动合并生成")
        output.append("> 最后更新: " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        output.append("")
        output.append("---")
        output.append("")
        
        # 用户偏好（高优先级，去重）
        if sections['preference']:
            output.append("## 用户偏好（汇总）")
            output.append("")
            seen_prefs = set()
            for entry in sections['preference']:
                pref_key = entry.content.split(':')[0] if ':' in entry.content else entry.content
                if pref_key not in seen_prefs:
                    seen_prefs.add(pref_key)
                    output.append(f"- {entry.content}")
            output.append("")
        
        # 技能学习（按时间倒序）
        if sections['skill']:
            output.append("## 技能学习记录")
            output.append("")
            output.append(f"**总计掌握: {len(sections['skill'])} 个技能**")
            output.append("")
            for entry in sections['skill'][:20]:  # 只显示最近20个
                output.append(f"- [{entry.source}] {entry.content}")
            output.append("")
        
        # 进行中的任务
        active_tasks = [e for e in sections['task'] if '进行中' in e.content or 'pending' in e.tags]
        if active_tasks:
            output.append("## 进行中的任务")
            output.append("")
            for entry in active_tasks[:10]:
                output.append(f"- [{entry.source}] {entry.content}")
            output.append("")
        
        # 历史会话
        if sections['session']:
            output.append("## 历史会话摘要")
            output.append("")
            for entry in sections['session'][:10]:
                output.append(f"- {entry.timestamp[:10]}: {entry.content}")
            output.append("")
        
        # 数据来源
        output.append("---")
        output.append("")
        output.append("## 数据来源")
        output.append("")
        sources = set(e.source for e in entries)
        for source in sources:
            count = len([e for e in entries if e.source == source])
            output.append(f"- {source}: {count} 条记录")
        output.append("")
        output.append(f"*合并时间: {datetime.now().isoformat()}*")
        
        return '\n'.join(output)
    
    def merge_memory_files(self, memory_files: Dict[str, Path]) -> str:
        """
        合并多个设备的 memory 文件
        
        Args:
            memory_files: {device_id: file_path}
        
        Returns:
            合并后的 memory 内容
        """
        all_entries = []
        
        for device_id, file_path in memory_files.items():
            if file_path.exists():
                entries = self.parse_memory_file(file_path, device_id)
                all_entries.extend(entries)
        
        merged = self.merge_entries(all_entries)
        return self.generate_merged_memory(merged)
    
    def save_merge_state(self, output_dir: Path):
        """保存合并状态（JSON 格式，便于追踪）"""
        state = {
            'last_merge': datetime.now().isoformat(),
            'entry_count': len(self.entries),
            'entries': [asdict(e) for e in self.entries[:100]]  # 只保存前100条
        }
        
        state_file = output_dir / '.merge_state.json'
        state_file.write_text(json.dumps(state, indent=2, ensure_ascii=False), encoding='utf-8')

def main():
    """命令行接口"""
    import argparse
    
    parser = argparse.ArgumentParser(description="智能 Memory 合并器")
    parser.add_argument("--input", "-i", nargs='+', required=True, help="输入的 memory 文件")
    parser.add_argument("--output", "-o", required=True, help="输出文件")
    parser.add_argument("--device", "-d", nargs='+', help="设备标识（与输入文件对应）")
    
    args = parser.parse_args()
    
    merger = MemoryMerger()
    
    # 构建文件映射
    memory_files = {}
    for i, input_file in enumerate(args.input):
        device_id = args.device[i] if args.device and i < len(args.device) else f"device_{i}"
        memory_files[device_id] = Path(input_file)
    
    # 合并
    merged_content = merger.merge_memory_files(memory_files)
    
    # 输出
    output_path = Path(args.output)
    output_path.write_text(merged_content, encoding='utf-8')
    
    print(f"合并完成！输出到: {output_path}")
    print(f"共处理 {len(merger.entries)} 条记忆")

if __name__ == "__main__":
    main()
