"""从 cProfile 数据自动生成性能对比报告和 SVG 图。"""

from __future__ import annotations

import pstats
from pathlib import Path

PROFILE_DIR = Path(__file__).resolve().parent


def cumulative_time(stats: pstats.Stats, function_name: str) -> float:
    """提取指定函数的累计耗时。"""
    # pstats 以 (文件名, 行号, 函数名) 为键，索引 3 对应累计耗时。
    matches = [
        value[3] for key, value in stats.stats.items() if key[2] == function_name
    ]
    if not matches:
        raise ValueError(f"性能数据中找不到函数: {function_name}")
    return max(matches)


def main() -> None:
    """读取优化前后数据并生成可直接放入博客的材料。"""
    before = pstats.Stats(str(PROFILE_DIR / "before.prof"))
    after = pstats.Stats(str(PROFILE_DIR / "after.prof"))
    before_core = cumulative_time(before, "calculate_similarity")
    after_core = cumulative_time(after, "calculate_similarity")
    reduction = (before_core - after_core) / before_core * 100

    report = (
        "cProfile 性能分析报告\n"
        f"优化前 calculate_similarity 累计耗时: {before_core:.4f} s\n"
        f"优化后 calculate_similarity 累计耗时: {after_core:.4f} s\n"
        f"耗时下降: {reduction:.1f}%\n"
        "优化前热点: Counter 构造与 _ngrams 生成器（重复构造四份特征）\n"
        "优化措施: 两种相似度复用同一对二元组 Counter，避免重复切分。\n"
    )
    (PROFILE_DIR / "profile_report.txt").write_text(report, encoding="utf-8")

    maximum = max(before_core, after_core)
    # 以较慢的一组为满宽度，保证两根柱子的长度可以直接比较。
    before_width = 560 * before_core / maximum
    after_width = 560 * after_core / maximum
    svg = "\n".join(
        [
            '<svg xmlns="http://www.w3.org/2000/svg" width="900" height="360" '
            'viewBox="0 0 900 360">',
            '<rect width="900" height="360" fill="#ffffff"/>',
            '<text x="40" y="48" font-size="26" '
            'font-family="Arial, Microsoft YaHei" font-weight="bold">'
            "cProfile 核心函数性能对比</text>",
            '<text x="40" y="78" font-size="15" '
            'font-family="Arial, Microsoft YaHei" fill="#555">'
            "大型中文文本，calculate_similarity 累计耗时（秒，越低越好）</text>",
            '<text x="40" y="142" font-size="18" '
            'font-family="Arial, Microsoft YaHei">优化前</text>',
            f'<rect x="150" y="112" width="{before_width:.1f}" height="45" '
            'rx="5" fill="#e76f51"/>',
            f'<text x="{165 + before_width:.1f}" y="142" font-size="18" '
            f'font-family="Arial">{before_core:.4f} s</text>',
            '<text x="40" y="222" font-size="18" '
            'font-family="Arial, Microsoft YaHei">优化后</text>',
            f'<rect x="150" y="192" width="{after_width:.1f}" height="45" '
            'rx="5" fill="#2a9d8f"/>',
            f'<text x="{165 + after_width:.1f}" y="222" font-size="18" '
            f'font-family="Arial">{after_core:.4f} s</text>',
            '<text x="40" y="270" font-size="20" '
            'font-family="Arial, Microsoft YaHei" fill="#264653">'
            f"耗时下降 {reduction:.1f}% / 特征生成次数减半</text>",
            "</svg>",
        ]
    )
    (PROFILE_DIR / "performance_comparison.svg").write_text(svg, encoding="utf-8")


if __name__ == "__main__":
    main()
