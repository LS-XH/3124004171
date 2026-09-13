"""论文查重程序的命令行入口。"""

from __future__ import annotations

import sys


def main() -> int:
    """运行命令行程序。核心功能将在下一阶段实现。"""
    if len(sys.argv) != 4:
        print("用法: python main.py <原文绝对路径> <抄袭文绝对路径> <答案绝对路径>", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
