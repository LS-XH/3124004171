"""可复现的性能基准；在仓库根目录执行本文件。"""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from plagiarism_checker import calculate_similarity  # noqa: E402


def build_documents(repetitions: int = 12_000) -> tuple[str, str]:
    """构造包含局部增删改的两篇大型中文测试论文。"""
    original_paragraph = "软件工程强调需求分析、设计、编码、测试和持续改进。"
    suspect_paragraph = "软件工程重视需求分析、系统设计、编码测试与持续改进。"
    return original_paragraph * repetitions, suspect_paragraph * repetitions


def benchmark() -> float:
    """执行一次大文本相似度计算。"""
    original, suspect = build_documents()
    return calculate_similarity(original, suspect)


if __name__ == "__main__":
    print(f"similarity={benchmark():.4f}")
