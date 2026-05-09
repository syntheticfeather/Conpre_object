# smart-customer-service/utils/file_upload_utils.py
import os
import uuid
import logging
from datetime import datetime
from typing import Tuple, List, Dict

# 支持的文件类型
ALLOWED_EXTENSIONS = {'.json', '.md'}

# 默认文件大小限制（5MB）
MAX_FILE_SIZE = 5 * 1024 * 1024

logger = logging.getLogger(__name__)

def allowed_file(filename: str) -> bool:
    """检查文件扩展名是否允许"""
    if not filename:
        return False
    ext = os.path.splitext(filename)[1].lower()
    return ext in ALLOWED_EXTENSIONS

def generate_file_name(original_filename: str) -> str:
    """生成带时间戳和UUID的文件名
    
    格式: 时间戳_UUID_原文件名
    
    Args:
        original_filename: 原始文件名
        
    Returns:
        新生成的文件名
    """
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    unique_id = str(uuid.uuid4())[:8]
    name, ext = os.path.splitext(original_filename)
    return f"{timestamp}_{unique_id}_{name}{ext}"

def validate_file(file_content: bytes, filename: str) -> Tuple[bool, str]:
    """校验上传文件
    
    Args:
        file_content: 文件内容
        filename: 文件名
        
    Returns:
        (是否通过校验, 错误信息)
    """
    # 检查文件名
    if not filename:
        return False, "文件名不能为空"
    
    # 检查文件扩展名
    if not allowed_file(filename):
        return False, f"不支持的文件格式，仅支持: {', '.join(ALLOWED_EXTENSIONS)}"
    
    # 检查文件大小
    file_size = len(file_content)
    if file_size == 0:
        return False, "文件内容为空"
    if file_size > MAX_FILE_SIZE:
        return False, f"文件大小超过限制（最大 {MAX_FILE_SIZE // (1024 * 1024)}MB）"
    
    return True, ""

def save_uploaded_file(file_content: bytes, filename: str, upload_dir: str) -> Tuple[bool, str, str]:
    """保存上传的文件
    
    Args:
        file_content: 文件内容
        filename: 原始文件名
        upload_dir: 上传目录
        
    Returns:
        (是否成功, 保存的文件路径, 错误信息)
    """
    try:
        # 确保上传目录存在
        os.makedirs(upload_dir, exist_ok=True)
        
        # 生成保存文件名
        saved_filename = generate_file_name(filename)
        saved_path = os.path.join(upload_dir, saved_filename)
        
        # 保存文件
        with open(saved_path, 'wb') as f:
            f.write(file_content)
        
        return True, saved_path, ""
    except Exception as e:
        logger.error(f"保存文件失败: {str(e)}")
        return False, "", f"文件保存失败: {str(e)}"

def process_uploaded_file(file_path: str, initializer) -> Dict[str, int]:
    """异步处理上传的文件，加载到知识库（追加模式）
    
    Args:
        file_path: 文件路径
        initializer: 知识库初始化器实例
        
    Returns:
        处理结果统计
    """
    try:
        # 直接加载文件，不检查是否需要初始化（追加模式）
        chunks, qa_pairs = initializer._load_file(file_path)
        
        total_loaded = len(chunks) + len(qa_pairs)
        added_count = 0
        
        if chunks:
            added_count += initializer._add_document_chunks(chunks)
        
        if qa_pairs:
            added_count += initializer._add_qa_pairs(qa_pairs)
        
        logger.info(f"文件处理完成 [{file_path}]: 加载 {total_loaded} 条，成功添加 {added_count} 条")
        
        return {
            "loaded": total_loaded,
            "added": added_count,
            "status": "completed" if added_count > 0 else "no_data"
        }
    except Exception as e:
        logger.error(f"文件处理失败 [{file_path}]: {str(e)}")
        return {
            "loaded": 0,
            "added": 0,
            "status": "error",
            "error": str(e)
        }