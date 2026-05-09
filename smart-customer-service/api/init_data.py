# init_data.py
import os
import json
import logging
from typing import List, Dict, Optional
from dataclasses import dataclass
from knowledge.vector_store import VectorStore
from knowledge.models import KnowledgeItem
from utils.markdown_processor import MarkdownProcessor, DocumentChunk

logger = logging.getLogger(__name__)

class KnowledgeInitializer:
    """知识库初始化器"""
    
    def __init__(self):
        self.vector_store = VectorStore()
        self.md_processor = MarkdownProcessor()
    
    def check_if_needs_init(self) -> bool:
        """检查知识库是否需要初始化"""
        try:
            all_items = self.vector_store.get_all()
            return len(all_items) == 0
        except Exception as e:
            logger.error(f"检查知识库状态失败: {e}")
            return True
    
    def initialize(self, 
                   file_path: Optional[str] = None, 
                   directory: Optional[str] = None) -> Dict[str, int]:
        """执行初始化
        
        Args:
            file_path: 单个文件路径（支持.md和.json）
            directory: 目录路径（批量处理）
        
        Returns:
            初始化结果统计
        """
        # 1. 检查是否需要初始化
        if not self.check_if_needs_init():
            current_count = len(self.vector_store.get_all())
            logger.info(f"知识库已初始化，跳过（当前条目: {current_count}）")
            return {"loaded": 0, "added": 0, "status": "skipped"}
        
        # 2. 加载数据源 两种：分块和QA对
        chunks = []
        qa_pairs = []
        
        # 从文件加载
        if file_path:
            loaded_chunks, loaded_qa = self._load_file(file_path)
            chunks.extend(loaded_chunks)
            qa_pairs.extend(loaded_qa)
        
        # 从目录加载
        if directory:
            dir_chunks = self._load_directory(directory)
            chunks.extend(dir_chunks)
        
        # 3. 如果都没提供，尝试环境变量
        if not file_path and not directory:
            env_file = os.getenv('KNOWLEDGE_INIT_FILE', '')
            env_dir = os.getenv('KNOWLEDGE_INIT_DIRECTORY', '')
            
            if env_file:
                loaded_chunks, loaded_qa = self._load_file(env_file)
                chunks.extend(loaded_chunks)
                qa_pairs.extend(loaded_qa)
            
            if env_dir:
                dir_chunks = self._load_directory(env_dir)
                chunks.extend(dir_chunks)
        
        # 4. 添加到向量库
        total_loaded = len(chunks) + len(qa_pairs)
        added_count = 0
        
        if chunks:
            added_count += self._add_document_chunks(chunks)
        
        if qa_pairs:
            added_count += self._add_qa_pairs(qa_pairs)
        
        # 5. 返回结果
        logger.info(f"初始化完成：加载 {total_loaded} 条，成功添加 {added_count} 条")
        return {
            "loaded": total_loaded, 
            "added": added_count,
            "status": "completed" if added_count > 0 else "no_data"
        }
    
    # ============ 数据加载方法 ============
    
    def _load_file(self, file_path: str) -> tuple[List[DocumentChunk], List[Dict]]:
        """加载单个文件，自动识别格式"""
        if not os.path.exists(file_path):
            logger.warning(f"文件不存在: {file_path}")
            return [], []
        
        ext = os.path.splitext(file_path)[1].lower()
        
        if ext == '.md':
            return self._load_markdown(file_path), []
        elif ext == '.json':
            return self._load_json(file_path)
        else:
            logger.warning(f"不支持的文件格式: {ext}")
            return [], []
    
    def _load_markdown(self, file_path: str) -> List[DocumentChunk]:
        """加载Markdown文件"""
        try:
            chunks = self.md_processor.parse(file_path)
            logger.info(f"Markdown文件解析完成: {file_path} -> {len(chunks)} 个块")
            return chunks
        except Exception as e:
            logger.error(f"解析Markdown失败: {e}")
            return []
    
    def _load_json(self, file_path: str) -> tuple[List[DocumentChunk], List[Dict]]:
        """加载JSON文件，问答格式"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            # 统一处理为列表
            items = data if isinstance(data, list) else data.get('items', [])
            
            chunks = []
            qa_pairs = []
            
            for item in items:
                if 'question' in item and 'answer' in item:
                    # 问答格式
                    qa_pairs.append(item)
                elif 'content' in item:
                    # 文档格式 - 转换为DocumentChunk
                    chunks.append(self._dict_to_chunk(item, file_path))
                else:
                    logger.warning(f"跳过无法识别的JSON条目: {item}")
            
            logger.info(f"JSON文件解析完成: {file_path} -> {len(chunks)} 文档块, {len(qa_pairs)} 问答对")
            return chunks, qa_pairs
            
        except Exception as e:
            logger.error(f"解析JSON失败: {e}")
            return [], []
    
    def _load_directory(self, directory: str) -> List[DocumentChunk]:
        """加载目录中的所有Markdown文件"""
        if not os.path.isdir(directory):
            logger.warning(f"目录不存在: {directory}")
            return []
        
        all_chunks = []
        md_files = [f for f in os.listdir(directory) if f.endswith('.md')]
        
        for filename in md_files:
            file_path = os.path.join(directory, filename)
            chunks = self._load_markdown(file_path)
            all_chunks.extend(chunks)
        
        logger.info(f"目录加载完成: {directory} -> {len(md_files)} 个文件, {len(all_chunks)} 个块")
        return all_chunks
    
    # ============ 添加到向量库的方法 ============
    
    def _add_document_chunks(self, chunks: List[DocumentChunk]) -> int:
        """批量添加文档分块到向量库"""
        if not chunks:
            return 0
        
        success_count = 0
        for chunk in chunks:
            try:
                # 直接使用DocumentChunk的属性构建向量库条目
                metadata = {
                    "document_name": chunk.document_name,
                    "section": chunk.section,
                    "section_level": chunk.section_level,
                    "section_path": chunk.section_path,
                    "chunk_index": chunk.chunk_index,
                    "source_path": chunk.source_path,
                    "source_type": "document",
                    "content": chunk.content
                }
                
                self.vector_store.client.add_item(
                    chunk.chunk_id,
                    chunk.combined,
                    metadata
                )
                success_count += 1
                
            except Exception as e:
                logger.error(f"添加文档块失败 [{chunk.chunk_id}]: {e}")
        
        logger.info(f"文档块添加完成: {success_count}/{len(chunks)}")
        return success_count
    
    def _add_qa_pairs(self, qa_pairs: List[Dict]) -> int:
        """批量添加问答对到向量库"""
        if not qa_pairs:
            return 0
        
        success_count = 0
        for qa in qa_pairs:
            try:
                question = qa.get('question', '')
                answer = qa.get('answer', '')
                
                if not question or not answer:
                    logger.warning("跳过不完整的问答对")
                    continue
                
                kb_item = KnowledgeItem(
                    question=question,
                    answer=answer,
                    category=qa.get('category', '通用')
                )
                self.vector_store.add_item(kb_item)
                success_count += 1
                
            except Exception as e:
                logger.error(f"添加问答对失败: {e}")
        
        logger.info(f"问答对添加完成: {success_count}/{len(qa_pairs)}")
        return success_count
    
    # ============ 辅助方法 ============
    
    def _dict_to_chunk(self, data: Dict, source_path: str) -> DocumentChunk:
        """将字典转换为DocumentChunk对象"""
        return DocumentChunk(
            chunk_id=data.get('chunk_id', data.get('document_id', 'unknown')),
            document_name=data.get('document_name', ''),
            section=data.get('section', ''),
            section_level=data.get('section_level', 0),
            section_path=data.get('section_path', ''),
            title=data.get('title', ''),
            content=data.get('content', ''),
            combined=data.get('combined', ''),
            chunk_index=data.get('chunk_index', 0),
            source_path=source_path
        )


# ============ 便捷函数 ============

# 全局实例
knowledge_initializer = KnowledgeInitializer()

def init_knowledge_base():
    """使用环境变量初始化知识库"""
    return knowledge_initializer.initialize()

def init_knowledge_base_with_file(file_path: str):
    """从单个文件初始化"""
    return knowledge_initializer.initialize(file_path=file_path)

def init_knowledge_base_with_directory(directory: str):
    """从目录初始化"""
    return knowledge_initializer.initialize(directory=directory)