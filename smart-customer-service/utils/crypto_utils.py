from cryptography.fernet import Fernet
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt
from cryptography.hazmat.backends import default_backend
import base64
import os
from dotenv import load_dotenv

load_dotenv()

class CryptoUtils:
    """加密工具类，用于API Key的加密和解密"""
    
    _instance = None
    _fernet = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialize()
        return cls._instance
    
    def _initialize(self):
        # 从环境变量获取密钥，不存在则生成新密钥
        secret_key = os.getenv("ENCRYPTION_KEY")
        if secret_key:
            self._fernet = Fernet(secret_key)
        else:
            # 生成新密钥并打印警告
            new_key = Fernet.generate_key()
            print(f"WARNING: ENCRYPTION_KEY not found in environment. Generated new key: {new_key.decode()}")
            print("Please set this key in your .env file as ENCRYPTION_KEY")
            self._fernet = Fernet(new_key)
    
    def encrypt(self, plaintext: str) -> str:
        """加密字符串"""
        return self._fernet.encrypt(plaintext.encode()).decode()
    
    def decrypt(self, ciphertext: str) -> str:
        """解密字符串"""
        return self._fernet.decrypt(ciphertext.encode()).decode()

crypto_utils = CryptoUtils()