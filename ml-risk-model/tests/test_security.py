"""PII 脱敏测试。"""
from llm_features.security import redact_pii


def test_redacts_chinese_id_card():
    result = redact_pii("身份证号 110101199001011234，已核验。")
    assert "110101199001011234" not in result.text
    assert "[身份证]" in result.text
    assert result.counts["id_card"] == 1


def test_redacts_phone_number():
    result = redact_pii("联系电话 13800138000。")
    assert "13800138000" not in result.text
    assert "[手机号]" in result.text
    assert result.counts["phone"] == 1


def test_redacts_bank_card():
    result = redact_pii("银行卡 6222020200112233445。")
    assert "6222020200112233445" not in result.text
    assert "[银行卡]" in result.text
    assert result.counts["bank_card"] == 1


def test_redacts_multiple_occurrences():
    result = redact_pii(
        "甲 110101199001011234，乙 110101199002021234；"
        "电话 13800138000，备用 13900139000。"
    )
    assert result.counts["id_card"] == 2
    assert result.counts["phone"] == 2
    assert "110101199001011234" not in result.text
    assert "13800138000" not in result.text


def test_short_numbers_are_not_redacted():
    text = "金额 10000 元，期限 12 个月，日期 20261027。"
    assert redact_pii(text).text == text


def test_text_without_pii_is_unchanged():
    text = "收入证明与申报收入一致，工作稳定。"
    result = redact_pii(text)
    assert result.text == text
    assert result.counts == {"id_card": 0, "phone": 0, "bank_card": 0}
