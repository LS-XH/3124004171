package com.exercise.arithmetic.cli;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CommandLineOptions {
    public enum Mode {
        GENERATE,
        GRADE,
        HELP
    }

    private static final int DEFAULT_EXERCISE_COUNT = 10;
    private static final Set<String> VALUE_OPTIONS = Set.of("-n", "-r", "-e", "-a");

    private final Mode mode;
    private final int exerciseCount;
    private final int range;
    private final Path exerciseFile;
    private final Path answerFile;

    private CommandLineOptions(
            Mode mode,
            int exerciseCount,
            int range,
            Path exerciseFile,
            Path answerFile) {
        this.mode = mode;
        this.exerciseCount = exerciseCount;
        this.range = range;
        this.exerciseFile = exerciseFile;
        this.answerFile = answerFile;
    }

    public static CommandLineOptions parse(String[] args) {
        if (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            return new CommandLineOptions(Mode.HELP, 0, 0, null, null);
        }
        if (args.length == 0) {
            throw new UserInputException("缺少参数");
        }

        Map<String, String> values = new HashMap<>();
        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if (!VALUE_OPTIONS.contains(option)) {
                throw new UserInputException("未知参数: " + option);
            }
            if (values.containsKey(option)) {
                throw new UserInputException("参数不能重复: " + option);
            }
            if (index + 1 >= args.length || VALUE_OPTIONS.contains(args[index + 1])) {
                throw new UserInputException("参数 " + option + " 缺少值");
            }
            values.put(option, args[++index]);
        }

        boolean hasGenerateOptions = values.containsKey("-n") || values.containsKey("-r");
        boolean hasGradeOptions = values.containsKey("-e") || values.containsKey("-a");
        if (hasGenerateOptions && hasGradeOptions) {
            throw new UserInputException("生成模式与批改模式的参数不能混用");
        }
        if (hasGenerateOptions) {
            if (!values.containsKey("-r")) {
                throw new UserInputException("生成题目时必须提供 -r 参数");
            }
            int count = parsePositiveInteger(values.getOrDefault("-n", String.valueOf(DEFAULT_EXERCISE_COUNT)), "-n");
            int range = parsePositiveInteger(values.get("-r"), "-r");
            return new CommandLineOptions(Mode.GENERATE, count, range, null, null);
        }
        if (hasGradeOptions) {
            if (!values.containsKey("-e") || !values.containsKey("-a")) {
                throw new UserInputException("批改答案时必须同时提供 -e 和 -a 参数");
            }
            return new CommandLineOptions(
                    Mode.GRADE,
                    0,
                    0,
                    parsePath(values.get("-e"), "-e"),
                    parsePath(values.get("-a"), "-a"));
        }
        throw new UserInputException("未指定运行模式");
    }

    private static int parsePositiveInteger(String value, String option) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new UserInputException(option + " 必须是正整数: " + value);
        }
    }

    private static Path parsePath(String value, String option) {
        try {
            return Path.of(value);
        } catch (InvalidPathException exception) {
            throw new UserInputException(option + " 的文件路径不合法: " + value);
        }
    }

    public Mode mode() {
        return mode;
    }

    public int exerciseCount() {
        return exerciseCount;
    }

    public int range() {
        return range;
    }

    public Path exerciseFile() {
        return exerciseFile;
    }

    public Path answerFile() {
        return answerFile;
    }
}

