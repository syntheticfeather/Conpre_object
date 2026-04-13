# smart-customer-service/tests/test_vector_store.py
import pytest
import shutil
from knowledge.vector_store import VectorStore
from knowledge.models import KnowledgeItem

@pytest.fixture
def vector_store():
    store = VectorStore(persist_directory="./test_chroma_db")
    yield store
    shutil.rmtree("./test_chroma_db", ignore_errors=True)

def test_add_and_search(vector_store):
    item = KnowledgeItem(
        question="贷款需要什么材料？",
        answer="需要身份证、收入证明",
        category="申请流程"
    )
    vector_store.add_item(item)
    results = vector_store.search("贷款材料", top_k=3)
    assert len(results) > 0
    assert any("身份证" in res["answer"] for res in results)