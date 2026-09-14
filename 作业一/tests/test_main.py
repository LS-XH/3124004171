"""文件接口和异常处理测试。"""

from __future__ import annotations

from pathlib import Path

from main import main, run


def test_run_writes_two_decimal_answer(tmp_path: Path) -> None:
    original = tmp_path / "original.txt"
    suspect = tmp_path / "suspect.txt"
    answer = tmp_path / "answer.txt"
    original.write_text("相同的论文内容", encoding="utf-8")
    suspect.write_text("相同的论文内容", encoding="utf-8")

    assert run(original, suspect, answer) == 1.0
    assert answer.read_text(encoding="utf-8") == "1.00"


def test_main_accepts_three_absolute_paths(tmp_path: Path) -> None:
    original = tmp_path / "original.txt"
    suspect = tmp_path / "suspect.txt"
    answer = tmp_path / "answer.txt"
    original.write_text("甲乙", encoding="utf-8")
    suspect.write_text("甲乙", encoding="utf-8")
    assert main([str(original), str(suspect), str(answer)]) == 0
    assert answer.read_text(encoding="utf-8") == "1.00"


def test_main_rejects_wrong_argument_count(capsys) -> None:
    assert main([]) == 2
    assert "用法" in capsys.readouterr().err


def test_main_rejects_relative_paths(capsys) -> None:
    assert main(["original.txt", "suspect.txt", "answer.txt"]) == 2
    assert "绝对路径" in capsys.readouterr().err


def test_main_handles_missing_input_file(tmp_path: Path, capsys) -> None:
    missing = tmp_path / "missing.txt"
    answer = tmp_path / "answer.txt"
    assert main([str(missing), str(missing), str(answer)]) == 1
    assert "错误" in capsys.readouterr().err
    assert not answer.exists()


def test_main_handles_unwritable_output_location(tmp_path: Path, capsys) -> None:
    original = tmp_path / "original.txt"
    suspect = tmp_path / "suspect.txt"
    original.write_text("正文", encoding="utf-8")
    suspect.write_text("正文", encoding="utf-8")
    invalid_answer = tmp_path / "missing-directory" / "answer.txt"
    assert main([str(original), str(suspect), str(invalid_answer)]) == 1
    assert "错误" in capsys.readouterr().err
