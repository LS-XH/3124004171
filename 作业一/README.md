# 论文查重程序（3124004171）

给定原文、疑似抄袭文本和答案文件的绝对路径，程序计算区间 `[0, 1]` 内的重复率，答案保留两位小数。运行时仅使用 Python 标准库，不联网，也不会读写命令行指定范围外的文件。

## 环境与运行

- Python 3.10 或更高版本
- 测试/质量工具：`python -m pip install -r requirements.txt`

```powershell
python main.py C:\tests\orig.txt C:\tests\orig_add.txt C:\tests\ans.txt
```

例如，完全相同的文本会在 `ans.txt` 中写入 `1.00`。程序支持 UTF-8（含 BOM）和 GB18030 编码。

## 验证

```powershell
python -m pytest
python -m ruff check .
python -m ruff format --check .
```

当前共有 21 个自动化测试，语句与分支综合覆盖率为 96%，Ruff 检查为零警告。

## 性能分析复现

```powershell
python -m cProfile -o profile/before.prof profile/profile_benchmark.py
# 切换到优化后的代码，再生成 after.prof
python -m cProfile -o profile/after.prof profile/profile_benchmark.py
python profile/generate_profile_report.py
```

详细设计、异常说明、测试截图、性能改进和 PSP 记录见 [BLOG.md](BLOG.md) 与 [PSP.md](PSP.md)。

