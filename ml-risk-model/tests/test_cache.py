"""文件缓存测试：键隔离、持久化、损坏容错。"""
import json

from llm_features.cache import FileCache, make_cache_key


def test_set_and_get_roundtrip(tmp_path):
    cache = FileCache(tmp_path / "cache.json")
    cache.set("k1", {"value": 1})
    assert cache.get("k1") == {"value": 1}
    assert cache.get("missing") is None


def test_entries_persist_across_instances(tmp_path):
    path = tmp_path / "cache.json"
    FileCache(path).set("k1", {"value": 1})
    assert FileCache(path).get("k1") == {"value": 1}


def test_key_isolated_by_model_prompt_and_payload():
    base = make_cache_key("m1", "v1", {"text": "abc"})
    assert base != make_cache_key("m2", "v1", {"text": "abc"})
    assert base != make_cache_key("m1", "v2", {"text": "abc"})
    assert base != make_cache_key("m1", "v1", {"text": "abd"})
    assert base == make_cache_key("m1", "v1", {"text": "abc"})


def test_key_is_stable_for_dict_order():
    a = make_cache_key("m", "v", {"x": 1, "y": 2})
    b = make_cache_key("m", "v", {"y": 2, "x": 1})
    assert a == b


def test_corrupted_cache_file_is_tolerated(tmp_path):
    path = tmp_path / "cache.json"
    path.write_text("{not valid json", encoding="utf-8")
    cache = FileCache(path)
    assert cache.get("anything") is None
    cache.set("k", {"ok": True})
    assert json.loads(path.read_text(encoding="utf-8"))["k"] == {"ok": True}


def test_deferred_set_is_persisted_only_after_save(tmp_path):
    path = tmp_path / "cache.json"
    cache = FileCache(path)
    cache.set("k", {"value": 1}, persist=False)
    assert FileCache(path).get("k") is None

    cache.save()
    assert FileCache(path).get("k") == {"value": 1}


def test_unicode_payload_is_preserved(tmp_path):
    path = tmp_path / "cache.json"
    payload = {"reasoning": "收入证明一致"}
    FileCache(path).set("k", payload)
    assert FileCache(path).get("k") == payload
