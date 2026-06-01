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
    """Markdown处理器 — 一级标题切分 + 二级句子切分 + chunk重叠"""

    # 句子边界正则：中英文句号、问号、感叹号、分号、换行
    SENTENCE_BOUNDARY = re.compile(r'(?<=[。！？；.!?;\n])\s*')

    def __init__(self, overlap_chars: int = 200, max_chunk_chars: int = 800):
        """
        Args:
            overlap_chars:  相邻chunk之间的重叠字符数，默认200字符。设为0禁用重叠
            max_chunk_chars: 单个chunk最大字符数，超过则按句子边界进一步拆分子chunk。
                             设为0禁用二级切分。默认800字符
        """
        self.overlap_chars = overlap_chars
        self.max_chunk_chars = max_chunk_chars

    # ============ 主入口 ============

    def parse(self, file_path: str) -> List[DocumentChunk]:
        """解析MD文件，返回分块"""
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        content = content.strip()

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
                content=content,
                combined=content,
                chunk_index=0,
                source_path=file_path
            )]

        chunks = []
        for i, h in enumerate(headings):
            level = len(h.group(1))
            title = h.group(2).strip()
            # 当前chunk的结束位置：下一个标题的起始位置（或文件末尾）
            chunk_end = headings[i+1].start() if i+1 < len(headings) else len(content)
            chunk_content = content[h.end():chunk_end]

            # ---- 重叠：把下一个区域的前 overlap_chars 个字符追加到当前chunk末尾 ----
            if self.overlap_chars > 0 and i + 1 < len(headings):
                next_start = headings[i+1].start()
                next_end = headings[i+2].start() if i+2 < len(headings) else len(content)
                next_content = content[next_start:next_end]
                overlap_text = next_content[:self.overlap_chars]
                if overlap_text:
                    chunk_content = chunk_content + "\n\n" + overlap_text

            # ---- 二级句子切分：如果chunk超过阈值，按句子边界再拆 ----
            section_path = self._build_path(headings, i)
            if self.max_chunk_chars > 0 and len(chunk_content) > self.max_chunk_chars:
                sub_contents = self._split_long_section(chunk_content, self.max_chunk_chars)
                for j, sub_content in enumerate(sub_contents):
                    chunks.append(DocumentChunk(
                        chunk_id=f"{doc_id}_{i}_{j}",
                        document_name=doc_name,
                        section=title,
                        section_level=level,
                        section_path=section_path,
                        title=title,
                        content=sub_content,
                        combined=f"{title}\n\n{sub_content}",
                        chunk_index=i,
                        source_path=file_path
                    ))
            else:
                chunks.append(DocumentChunk(
                    chunk_id=f"{doc_id}_{i}",
                    document_name=doc_name,
                    section=title,
                    section_level=level,
                    section_path=section_path,
                    title=title,
                    content=chunk_content,
                    combined=f"{title}\n\n{chunk_content}",
                    chunk_index=i,
                    source_path=file_path
                ))

        return chunks

    # ============ 辅助方法 ============

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

    def _split_long_section(self, content: str, max_chars: int) -> List[str]:
        """
        按句子边界将长文本拆成多个子块。
        规则：逐句累积，当累积长度接近 max_chars 时切一刀。
        保证每个子块都是完整句子的组合，不在句子中间截断。
        """
        # 1. 按句子边界切分
        sentences = self.SENTENCE_BOUNDARY.split(content)
        sentences = [s.strip() for s in sentences if s.strip()]

        chunks = []
        current = ""

        for sentence in sentences:
            # 如果加上这句话会超长，先保存当前chunk
            if len(current) + len(sentence) > max_chars and current:
                # 重叠：当前的 chunk 末尾带上新 chunk 的第一句
                if self.overlap_chars > 0 and sentence:
                    overlap = sentence[:min(len(sentence), self.overlap_chars)]
                    current = current + "\n" + overlap
                chunks.append(current)
                current = sentence
            else:
                current = (current + sentence) if current else sentence

        # 剩余部分
        if current:
            chunks.append(current)

        return chunks
