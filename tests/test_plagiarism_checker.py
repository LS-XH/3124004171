"""计算模块的白盒单元测试。"""

from __future__ import annotations

from collections import Counter

import pytest

from plagiarism_checker import (
    _cosine_similarity,
    _dice_similarity,
    _ngrams,
    calculate_similarity,
    normalize_text,
    read_text,
    tokenize,
)


def test_identical_text_is_one() -> None:
    text = "今天是星期天，天气晴，今天晚上我要去看电影。"
    assert calculate_similarity(text, text) == pytest.approx(1.0)


def test_completely_different_text_is_zero() -> None:
    assert calculate_similarity("甲乙丙丁", "天地玄黄") == pytest.approx(0.0)


def test_small_edit_retains_similarity() -> None:
    original = "今天是星期天，天气晴，今天晚上我要去看电影。"
    suspect = "今天是周天，天气晴朗，我晚上要去看电影。"
    assert 0.5 < calculate_similarity(original, suspect) < 1.0


def test_similarity_is_symmetric() -> None:
    left = "软件工程需要持续测试和代码评审"
    right = "代码评审和持续测试是软件工程实践"
    assert calculate_similarity(left, right) == pytest.approx(
        calculate_similarity(right, left)
    )


@pytest.mark.parametrize(
    ("left", "right", "expected"),
    [("", "", 1.0), ("", "正文", 0.0), ("!!!", "？？", 1.0)],
)
def test_empty_content_boundaries(left: str, right: str, expected: float) -> None:
    assert calculate_similarity(left, right) == expected


def test_normalization_ignores_case_width_spaces_and_punctuation() -> None:
    assert normalize_text("Ａ B，c！") == "abc"
    assert calculate_similarity("Python 测试！", "ｐｙｔｈｏｎ，测试") == pytest.approx(
        1.0
    )


def test_mixed_language_tokenization() -> None:
    assert tokenize("python3测试工程") == ["python3", "测试", "试工", "工程"]


def test_single_character_uses_one_feature() -> None:
    assert tuple(_ngrams("甲")) == ("甲",)
    assert tuple(_ngrams("")) == ()
    assert tuple(_ngrams("甲乙丙")) == ("甲乙", "乙丙")


def test_cosine_handles_empty_and_sparse_vectors() -> None:
    assert _cosine_similarity(Counter(), Counter()) == 1.0
    assert _cosine_similarity(Counter({"a": 1}), Counter()) == 0.0
    assert _cosine_similarity(Counter({"a": 2}), Counter({"a": 1})) == 1.0


def test_dice_handles_multisets_and_empty_vectors() -> None:
    assert _dice_similarity(Counter(), Counter()) == 1.0
    assert _dice_similarity(Counter({"ab": 2}), Counter()) == 0.0
    assert _dice_similarity(Counter({"ab": 2}), Counter({"ab": 1})) == pytest.approx(
        2 / 3
    )


def test_read_text_supports_utf8_bom_and_gb18030(tmp_path) -> None:
    utf8_file = tmp_path / "utf8.txt"
    gb_file = tmp_path / "gb.txt"
    utf8_file.write_text("中文", encoding="utf-8-sig")
    gb_file.write_bytes("中文".encode("gb18030"))
    assert read_text(utf8_file) == "中文"
    assert read_text(gb_file) == "中文"


def test_read_text_rejects_unknown_encoding(tmp_path) -> None:
    invalid_file = tmp_path / "invalid.txt"
    invalid_file.write_bytes(b"\xff")
    with pytest.raises(ValueError, match="无法识别文件编码"):
        read_text(invalid_file)


def test_read_text_reports_missing_file(tmp_path) -> None:
    with pytest.raises(FileNotFoundError):
        read_text(tmp_path / "missing.txt")
