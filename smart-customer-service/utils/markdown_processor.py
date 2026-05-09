# markdown_processor.py
import re
import os
import uuid
from typing import List
from dataclasses import dataclass

@dataclass
class DocumentChunk:
    """文档分块"""
    chunk_id: str         # 分块ID
    document_name: str    # 文档名称

    section: str          # 章节名
    section_level: int    # 章节层级
    section_path: str     # 章节路径 例如："第一章 > 1.1节 > 1.1.1小节

    title: str            # 标题
    content: str          # 内容
    combined: str         # 合并内容

    chunk_index: int     # 分块索引，从0开始计数
    source_path: str     # 源文件路径

class MarkdownProcessor:
    """Markdown处理器"""
    
    def parse(self, file_path: str) -> List[DocumentChunk]:
        """解析MD文件，返回分块"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        doc_name = os.path.basename(file_path).replace('.md', '')
        doc_id = str(uuid.uuid5(uuid.NAMESPACE_URL, file_path))
        
        # 找所有标题
        headings = list(re.finditer(r'(#{1,4})\s+(.+?)(?=\n|$)', content))
        
        if not headings:
            return [DocumentChunk(
                chunk_id=f"{doc_id}_full",
                document_name=doc_name,
                section="全文",
                section_level=0,
                section_path=doc_name,
                title=doc_name,
                content=content.strip(),
                combined=content.strip(),
                chunk_index=0,
                source_path=file_path
            )]
        
        chunks = []
        for i, h in enumerate(headings):
            level = len(h.group(1))
            title = h.group(2).strip()
            end = headings[i+1].start() if i+1 < len(headings) else len(content)
            chunk_content = content[h.end():end].strip()
            
            chunks.append(DocumentChunk(
                chunk_id=f"{doc_id}_{i}",
                document_name=doc_name,
                section=title,
                section_level=level,
                section_path=self._build_path(headings, i),
                title=title,
                content=chunk_content,
                combined=f"{title}\n\n{chunk_content}",
                chunk_index=i,
                source_path=file_path
            ))
        
        return chunks
    
    def _build_path(self, headings, current_idx):
        """构建章节路径：如 '第一章 > 1.1节'"""
        current_level = len(headings[current_idx].group(1))
        path = [headings[current_idx].group(2).strip()]
        
        # 向前找父级标题
        for i in range(current_idx-1, -1, -1):
            level = len(headings[i].group(1))
            if level < current_level:
                path.insert(0, headings[i].group(2).strip())
                current_level = level
        
        return " > ".join(path)