"""论文相似度计算模块。

算法融合子词词频余弦相似度与字符二元组 Dice 系数：前者关注局部内容和
词频分布，后者降低少量增删改对结果的影响。两部分都只需线性扫描。
"""

from __future__ import annotations

import math
import re
import unicodedata
from collections import Counter
from pathlib import Path
from typing import Hashable, Iterable

_MEANINGFUL_TEXT = re.compile(r"[\w\u3400-\u4dbf\u4e00-\u9fff]+", re.UNICODE)
_TEXT_RUNS = re.compile(r"[a-z0-9_]+|[\u3400-\u4dbf\u4e00-\u9fff]+", re.UNICODE)
_ENCODINGS = ("utf-8-sig", "gb18030")
_TOKEN_WEIGHT = 0.65
_BIGRAM_WEIGHT = 1.0 - _TOKEN_WEIGHT


def read_text(path: Path) -> str:
    """读取 UTF-8 或常见中文 GB18030 编码文本。"""
    last_error: UnicodeDecodeError | None = None
    for encoding in _ENCODINGS:
        try:
            return path.read_text(encoding=encoding)
        except UnicodeDecodeError as error:
            last_error = error
    if last_error is not None:
        raise ValueError(f"无法识别文件编码: {path}") from last_error
    raise ValueError(f"无法读取文件: {path}")


def normalize_text(text: str) -> str:
    """统一全半角与大小写，并去除空白、标点等非正文字符。"""
    normalized = unicodedata.normalize("NFKC", text).casefold()
    return "".join(_MEANINGFUL_TEXT.findall(normalized))


def tokenize(text: str) -> list[str]:
    """提取英文单词及中文二元子词，不依赖外部词典。"""
    tokens: list[str] = []
    for run in _TEXT_RUNS.findall(text):
        if run.isascii():
            tokens.append(run)
        else:
            tokens.extend(_ngrams(run))
    return tokens


def _cosine_similarity(
    left: Counter[Hashable], right: Counter[Hashable]
) -> float:
    """计算两个稀疏词频向量的余弦相似度。"""
    if not left or not right:
        return 1.0 if left == right else 0.0
    smaller, larger = (left, right) if len(left) <= len(right) else (right, left)
    dot_product = sum(value * larger.get(key, 0) for key, value in smaller.items())
    left_norm = math.sqrt(sum(value * value for value in left.values()))
    right_norm = math.sqrt(sum(value * value for value in right.values()))
    return dot_product / (left_norm * right_norm)


def _dice_similarity(left: Counter[str], right: Counter[str]) -> float:
    """计算两个多重集合的 Sørensen-Dice 系数。"""
    total = sum(left.values()) + sum(right.values())
    if total == 0:
        return 1.0 if left == right else 0.0
    overlap = sum((left & right).values())
    return 2.0 * overlap / total


def _ngrams(text: str, size: int = 2) -> Iterable[str]:
    """生成连续字符 n-gram；单字符文本自身视为一个特征。"""
    if len(text) < size:
        return (text,) if text else ()
    return (text[index : index + size] for index in range(len(text) - size + 1))


def calculate_similarity(original: str, suspect: str) -> float:
    """返回区间 [0, 1] 内的论文重复率。"""
    normalized_original = normalize_text(original)
    normalized_suspect = normalize_text(suspect)
    if not normalized_original or not normalized_suspect:
        return 1.0 if normalized_original == normalized_suspect else 0.0

    token_score = _cosine_similarity(
        Counter(tokenize(normalized_original)),
        Counter(tokenize(normalized_suspect)),
    )
    bigram_score = _dice_similarity(
        Counter(_ngrams(normalized_original)),
        Counter(_ngrams(normalized_suspect)),
    )
    score = _TOKEN_WEIGHT * token_score + _BIGRAM_WEIGHT * bigram_score
    return min(1.0, max(0.0, score))
