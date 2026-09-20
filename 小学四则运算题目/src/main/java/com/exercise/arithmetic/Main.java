package com.exercise.arithmetic;

import com.exercise.arithmetic.cli.CommandLineOptions;
import com.exercise.arithmetic.cli.UserInputException;
import com.exercise.arithmetic.generator.ExerciseGenerationException;
import com.exercise.arithmetic.generator.ExerciseGenerator;
import com.exercise.arithmetic.io.ExerciseFileRepository;
import com.exercise.arithmetic.service.ExerciseService;
import com.exercise.arithmetic.service.GradeResult;
import com.exercise.arithmetic.service.GradingService;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

public final class Main {
    private static final String USAGE = """
            用法:
              生成题目: java -jar Myapp.jar [-n 题目数量] -r 数值范围
              批改答案: java -jar Myapp.jar -e <题目文件> -a <答案文件>
              查看帮助: java -jar Myapp.jar --help

            示例:
              java -jar Myapp.jar -n 10 -r 10
              java -jar Myapp.jar -e Exercises.txt -a Answers.txt
            """;

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, Path.of(".").toAbsolutePath().normalize(), System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args, Path workingDirectory, PrintStream out, PrintStream err) {
        try {
            CommandLineOptions options = CommandLineOptions.parse(args);
            if (options.mode() == CommandLineOptions.Mode.HELP) {
                out.print(USAGE);
                return 0;
            }

            ExerciseFileRepository repository = new ExerciseFileRepository();
            if (options.mode() == CommandLineOptions.Mode.GENERATE) {
                ExerciseService service = new ExerciseService(new ExerciseGenerator(), repository);
                service.generateAndWrite(options.exerciseCount(), options.range(), workingDirectory);
                out.println("已生成 " + options.exerciseCount() + " 道题目。");
                out.println("题目文件: " + workingDirectory.resolve(ExerciseFileRepository.EXERCISES_FILE_NAME));
                out.println("答案文件: " + workingDirectory.resolve(ExerciseFileRepository.ANSWERS_FILE_NAME));
            } else {
                GradingService service = new GradingService(repository);
                GradeResult result = service.grade(
                        options.exerciseFile(), options.answerFile(), workingDirectory);
                out.print(result.format());
                out.println("批改文件: " + workingDirectory.resolve(ExerciseFileRepository.GRADE_FILE_NAME));
            }
            return 0;
        } catch (UserInputException | ExerciseGenerationException exception) {
            err.println("错误: " + exception.getMessage());
            err.print(USAGE);
            return 2;
        } catch (IOException exception) {
            err.println("文件读写失败: " + exception.getMessage());
            return 3;
        } catch (ArithmeticException exception) {
            err.println("计算失败: " + exception.getMessage());
            return 4;
        }
    }
}
