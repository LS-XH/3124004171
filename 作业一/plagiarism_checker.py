"""论文相似度计算模块。

算法在同一份字符二元组特征上融合词频余弦相似度与 Dice 系数：前者关注
频率分布，后者关注重合数量。复用特征避免重复切分，两部分都只需线性扫描。
"""

from __future__ import annotations

import math
import re
import unicodedata
from collections import Counter
from collections.abc import Hashable, Iterable
from pathlib import Path

# 只保留字母、数字、下划线和中日韩统一表意文字，排除排版符号的干扰。
_MEANINGFUL_TEXT = re.compile(r"[\w\u3400-\u4dbf\u4e00-\u9fff]+", re.UNICODE)
_TEXT_RUNS = re.compile(r"[a-z0-9_]+|[\u3400-\u4dbf\u4e00-\u9fff]+", re.UNICODE)
# utf-8-sig 同时兼容有 BOM 和无 BOM 的 UTF-8 文件。
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
            # 仅解码失败时尝试下一编码；不存在、无权限等 I/O 错误应直接上抛。
            last_error = error
    if last_error is not None:
        raise ValueError(f"无法识别文件编码: {path}") from last_error
    raise ValueError(f"无法读取文件: {path}")


def normalize_text(text: str) -> str:
    """统一全半角与大小写，并去除空白、标点等非正文字符。"""
    # NFKC 将全角字母等兼容字符转换为统一形式，casefold 比 lower 更通用。
    normalized = unicodedata.normalize("NFKC", text).casefold()
    return "".join(_MEANINGFUL_TEXT.findall(normalized))


def tokenize(text: str) -> list[str]:
    """提取英文单词及中文二元子词，不依赖外部词典。"""
    tokens: list[str] = []
    for run in _TEXT_RUNS.findall(text):
        if run.isascii():
            # 英文和数字保留完整词，避免将一个单词拆成大量无意义字符。
            tokens.append(run)
        else:
            # 中文没有天然空格，使用相邻二元子词避免依赖外部分词词典。
            tokens.extend(_ngrams(run))
    return tokens


def _cosine_similarity(left: Counter[Hashable], right: Counter[Hashable]) -> float:
    """计算两个稀疏词频向量的余弦相似度。"""
    if not left or not right:
        return 1.0 if left == right else 0.0
    # 点积只遍历特征种类较少的一侧，大词典输入时可减少哈希查找次数。
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
    # Counter 的交集保留每个特征在两侧出现次数的较小值。
    overlap = sum((left & right).values())
    return 2.0 * overlap / total


def _ngrams(text: str, size: int = 2) -> Iterable[str]:
    """生成连续字符 n-gram；单字符文本自身视为一个特征。"""
    if len(text) < size:
        return (text,) if text else ()
    # 返回生成器而不是列表，使 Counter 可以边迭代边计数，降低峰值内存。
    return (text[index : index + size] for index in range(len(text) - size + 1))


def calculate_similarity(original: str, suspect: str) -> float:
    """返回区间 [0, 1] 内的论文重复率。"""
    normalized_original = normalize_text(original)
    normalized_suspect = normalize_text(suspect)
    # 提前定义空文本结果，同时避免后续相似度公式出现除零。
    if not normalized_original or not normalized_suspect:
        return 1.0 if normalized_original == normalized_suspect else 0.0

    # 两种指标复用同一对特征计数，这是性能分析后保留的关键优化。
    original_features = Counter(_ngrams(normalized_original))
    suspect_features = Counter(_ngrams(normalized_suspect))
    token_score = _cosine_similarity(original_features, suspect_features)
    bigram_score = _dice_similarity(original_features, suspect_features)
    score = _TOKEN_WEIGHT * token_score + _BIGRAM_WEIGHT * bigram_score
    # 浮点运算可能产生极小的越界误差，最终结果始终限制在合法区间。
    return min(1.0, max(0.0, score))
