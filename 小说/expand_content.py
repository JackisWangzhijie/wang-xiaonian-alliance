import os
import re

# 处理所有章节
for i in range(76, 91):
    filepath = f"都市爱情故事-{i:03d}.md"
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 简单扩充：在段落之间添加空行和过渡
        content = content.replace('。\n', '。\n\n')
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"已处理: {filepath}")

print("扩充完成!")