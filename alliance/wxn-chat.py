#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
王小年联盟 - 聊天室系统
联盟成员间通信的核心模块
"""

import os
import sys
import json
import time
import hashlib
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional
from dataclasses import dataclass, asdict

@dataclass
class Message:
    """联盟消息格式"""
    id: str
    timestamp: str
    sender: str
    sender_device: str
    channel: str
    content: str
    msg_type: str  # text, alert, task, knowledge
    mentions: List[str]
    reply_to: Optional[str]
    metadata: Dict

class WangXiaoNianChat:
    """王小年联盟聊天室"""
    
    CHANNELS = ['general', 'tasks', 'knowledge', 'alerts']
    
    def __init__(self, member_id: str = None):
        self.workspace = Path(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
        self.alliance_dir = self.workspace / "sync" / "alliance"
        self.messages_dir = self.alliance_dir / "messages"
        self.status_dir = self.alliance_dir / "status"
        
        # 读取配置文件获取成员ID
        self.member_id = member_id or self._get_member_id()
        self.device_type = self._detect_device()
        
        self.messages_dir.mkdir(parents=True, exist_ok=True)
        self.status_dir.mkdir(parents=True, exist_ok=True)
        
    def _get_member_id(self) -> str:
        """从配置文件读取成员ID"""
        config_file = self.workspace / "sync" / "sync.conf"
        if config_file.exists():
            with open(config_file, 'r', encoding='utf-8') as f:
                for line in f:
                    if line.strip().startswith('device_id'):
                        return line.split('=')[1].strip()
        return "wangxiaonian-unknown"
    
    def _detect_device(self) -> str:
        """检测设备类型"""
        if sys.platform == 'win32':
            return "Windows PC"
        elif 'android' in sys.platform.lower() or os.path.exists('/data/data/com.termux'):
            return "Android Termux"
        else:
            return "Unknown"
    
    def _generate_msg_id(self, content: str) -> str:
        """生成消息ID"""
        timestamp = datetime.now().isoformat()
        hash_input = f"{self.member_id}:{timestamp}:{content}"
        return hashlib.md5(hash_input.encode()).hexdigest()[:12]
    
    def send(self, content: str, channel: str = 'general', 
             msg_type: str = 'text', mentions: List[str] = None,
             reply_to: str = None) -> Message:
        """
        发送消息到聊天室
        
        Args:
            content: 消息内容
            channel: 频道 (general/tasks/knowledge/alerts)
            msg_type: 消息类型
            mentions: @提及的成员列表
            reply_to: 回复的消息ID
        """
        if channel not in self.CHANNELS:
            print(f"[ERR] 未知频道: {channel}")
            return None
        
        # 创建消息
        msg = Message(
            id=self._generate_msg_id(content),
            timestamp=datetime.now().isoformat(),
            sender=self.member_id,
            sender_device=self.device_type,
            channel=channel,
            content=content,
            msg_type=msg_type,
            mentions=mentions or [],
            reply_to=reply_to,
            metadata={
                'platform': sys.platform,
                'version': '1.0'
            }
        )
        
        # 保存消息
        self._save_message(msg)
        
        # 发送通知（如果是重要消息）
        if msg_type in ['alert', 'task'] or mentions:
            self._send_notification(msg)
        
        print(f"[OK] 消息已发送到 #{channel}")
        return msg
    
    def _save_message(self, msg: Message):
        """保存消息到文件"""
        # 按日期组织消息
        date_str = datetime.now().strftime('%Y-%m-%d')
        channel_dir = self.messages_dir / msg.channel
        channel_dir.mkdir(exist_ok=True)
        
        msg_file = channel_dir / f"{date_str}.jsonl"
        
        with open(msg_file, 'a', encoding='utf-8') as f:
            f.write(json.dumps(asdict(msg), ensure_ascii=False) + '\n')
    
    def _send_notification(self, msg: Message):
        """发送系统通知"""
        try:
            if sys.platform == 'win32':
                # Windows 通知
                notify_script = self.workspace / "tools" / "windows-auto" / "win_notify.py"
                if notify_script.exists():
                    import subprocess
                    title = f"[联盟] #{msg.channel} - {msg.sender}"
                    subprocess.run([
                        sys.executable, str(notify_script),
                        '-t', title, '-m', msg.content[:100], '-p', 'high'
                    ], capture_output=True)
            else:
                # Termux 通知
                import subprocess
                subprocess.run([
                    'termux-notification',
                    '-t', f'[联盟] #{msg.channel}',
                    '-c', f'{msg.sender}: {msg.content[:100]}'
                ], capture_output=True)
        except:
            pass
    
    def read(self, channel: str = None, limit: int = 20, 
             since: str = None) -> List[Message]:
        """
        读取消息
        
        Args:
            channel: 指定频道，None则读取所有
            limit: 返回消息数量
            since: 从此时间之后的消息 (ISO格式)
        """
        messages = []
        
        channels = [channel] if channel else self.CHANNELS
        
        for ch in channels:
            channel_dir = self.messages_dir / ch
            if not channel_dir.exists():
                continue
            
            # 读取所有消息文件
            for msg_file in sorted(channel_dir.glob('*.jsonl'), reverse=True):
                with open(msg_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        if line.strip():
                            data = json.loads(line)
                            msg = Message(**data)
                            
                            # 过滤时间
                            if since and msg.timestamp < since:
                                continue
                            
                            messages.append(msg)
        
        # 按时间排序，返回最新的
        messages.sort(key=lambda x: x.timestamp, reverse=True)
        return messages[:limit]
    
    def display(self, channel: str = None, limit: int = 20):
        """
        显示聊天室内容
        """
        messages = self.read(channel, limit)
        
        if not messages:
            print("[系统] 暂无消息")
            return
        
        # 显示标题
        print("\n" + "="*60)
        if channel:
            print(f"  # {channel} - 王小年联盟聊天室")
        else:
            print("  王小年联盟 - 所有频道")
        print("="*60)
        
        # 显示消息
        for msg in reversed(messages):  # 从旧到新显示
            self._display_message(msg)
        
        print("="*60 + "\n")
    
    def _display_message(self, msg: Message):
        """显示单条消息"""
        timestamp = msg.timestamp[11:16]  # 只显示时间
        
        # 频道标签
        ch_tag = f"[{msg.channel}]"
        
        # 消息类型标记
        type_icons = {
            'text': ' ',
            'alert': '[!]',
            'task': '[T]',
            'knowledge': '[K]'
        }
        type_tag = type_icons.get(msg.msg_type, ' ')
        
        # 高亮自己的消息
        if msg.sender == self.member_id:
            sender = f"[我] {msg.sender}"
        else:
            sender = msg.sender
        
        # 显示
        print(f"\n{ch_tag} {type_tag} {timestamp} {sender}")
        
        # 如果是回复，显示引用
        if msg.reply_to:
            print(f"  [回复 {msg.reply_to[:8]}...]")
        
        # 内容
        print(f"  > {msg.content}")
        
        # 提及
        if msg.mentions:
            mentions_str = ', '.join(msg.mentions)
            print(f"  [@ {mentions_str}]")
    
    def mention(self, member: str, content: str, channel: str = 'general'):
        """@提及某个成员"""
        return self.send(content, channel, mentions=[member])
    
    def broadcast(self, content: str, msg_type: str = 'alert'):
        """广播消息到所有成员"""
        return self.send(content, 'alerts', msg_type)
    
    def status(self):
        """显示联盟状态"""
        print("\n" + "="*60)
        print("  王小年联盟 - 成员状态")
        print("="*60)
        
        # 读取所有成员状态
        for status_file in self.status_dir.glob('*.json'):
            with open(status_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                member = data.get('member_id', 'unknown')
                device = data.get('device_type', 'unknown')
                last_seen = data.get('last_seen', 'unknown')
                status = data.get('status', 'unknown')
                
                # 计算在线状态（5分钟内为在线）
                if last_seen != 'unknown':
                    last = datetime.fromisoformat(last_seen)
                    delta = (datetime.now() - last).total_seconds()
                    online = "[在线]" if delta < 300 else "[离线]"
                else:
                    online = "[未知]"
                
                print(f"  {online} {member} ({device})")
                print(f"       最后活跃: {last_seen[:19]}")
                print(f"       状态: {status}")
        
        print("="*60 + "\n")
    
    def update_status(self, status_text: str = "工作中"):
        """更新自己的状态"""
        status_data = {
            'member_id': self.member_id,
            'device_type': self.device_type,
            'last_seen': datetime.now().isoformat(),
            'status': status_text,
            'capabilities': self._get_capabilities()
        }
        
        status_file = self.status_dir / f"{self.member_id}.json"
        with open(status_file, 'w', encoding='utf-8') as f:
            json.dump(status_data, f, indent=2, ensure_ascii=False)
    
    def _get_capabilities(self) -> List[str]:
        """获取自己的能力列表"""
        skills_dir = self.workspace / "skills"
        capabilities = []
        
        if skills_dir.exists():
            for skill_dir in skills_dir.iterdir():
                if skill_dir.is_dir():
                    capabilities.append(skill_dir.name)
        
        return capabilities

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="王小年联盟聊天室")
    parser.add_argument("action", choices=[
        "send", "read", "status", "mention", "broadcast"
    ])
    parser.add_argument("--channel", "-c", default="general",
                       choices=['general', 'tasks', 'knowledge', 'alerts'])
    parser.add_argument("--message", "-m", help="消息内容")
    parser.add_argument("--type", "-t", default="text",
                       choices=['text', 'alert', 'task', 'knowledge'])
    parser.add_argument("--limit", "-n", type=int, default=20)
    parser.add_argument("--to", help="@提及的成员")
    
    args = parser.parse_args()
    
    chat = WangXiaoNianChat()
    
    if args.action == "send":
        if not args.message:
            print("[ERR] 请提供消息内容: -m '消息'")
            return
        chat.send(args.message, args.channel, args.type)
        
    elif args.action == "read":
        chat.display(args.channel, args.limit)
        
    elif args.action == "status":
        chat.update_status()
        chat.status()
        
    elif args.action == "mention":
        if not args.to or not args.message:
            print("[ERR] 请提供成员和消息: --to member -m '消息'")
            return
        chat.mention(args.to, args.message, args.channel)
        
    elif args.action == "broadcast":
        if not args.message:
            print("[ERR] 请提供广播内容: -m '消息'")
            return
        chat.broadcast(args.message, args.type)

if __name__ == "__main__":
    main()
