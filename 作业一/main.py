"""论文查重程序的命令行入口。"""

from __future__ import annotations

import sys
from collections.abc import Sequence
from pathlib import Path

from plagiarism_checker import calculate_similarity, read_text

USAGE = "用法: python main.py <原文绝对路径> <抄袭文绝对路径> <答案绝对路径>"


def run(original_path: Path, suspect_path: Path, answer_path: Path) -> float:
    """读取两篇论文、计算相似度，并将结果写入指定答案文件。"""
    original = read_text(original_path)
    suspect = read_text(suspect_path)
    similarity = calculate_similarity(original, suspect)
    answer_path.write_text(f"{similarity:.2f}", encoding="utf-8")
    return similarity


def main(argv: Sequence[str] | None = None) -> int:
    """运行命令行程序并返回进程退出码。"""
    arguments = list(sys.argv[1:] if argv is None else argv)
    if len(arguments) != 3:
        print(USAGE, file=sys.stderr)
        return 2

    paths = [Path(argument) for argument in arguments]
    if not all(path.is_absolute() for path in paths):
        print(f"错误: 三个文件路径都必须是绝对路径。\n{USAGE}", file=sys.stderr)
        return 2

    try:
        run(*paths)
    except (OSError, UnicodeError, ValueError) as error:
        print(f"错误: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
