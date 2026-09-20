package com.exercise.arithmetic.service;

import com.exercise.arithmetic.cli.UserInputException;
import com.exercise.arithmetic.io.ExerciseFileRepository;
import com.exercise.arithmetic.model.Fraction;
import com.exercise.arithmetic.parser.ExpressionParseException;
import com.exercise.arithmetic.parser.ExpressionParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GradingService {
    private static final Pattern EXERCISE_LINE = Pattern.compile("^\\s*(\\d+)\\.\\s*(.*?)\\s*=\\s*$");
    private static final Pattern ANSWER_LINE = Pattern.compile("^\\s*(\\d+)\\.\\s*(.*?)\\s*$");

    private final ExerciseFileRepository repository;

    public GradingService(ExerciseFileRepository repository) {
        this.repository = repository;
    }

    public GradeResult grade(Path exerciseFile, Path answerFile, Path outputDirectory) throws IOException {
        List<ExpectedAnswer> expectedAnswers = parseExercises(repository.readLines(exerciseFile));
        SubmittedAnswers submittedAnswers = parseAnswers(repository.readLines(answerFile));

        List<Integer> correct = new ArrayList<>();
        List<Integer> wrong = new ArrayList<>();
        for (ExpectedAnswer expected : expectedAnswers) {
            Fraction submitted = submittedAnswers.values().get(expected.number());
            if (!submittedAnswers.invalidNumbers().contains(expected.number())
                    && expected.value().equals(submitted)) {
                correct.add(expected.number());
            } else {
                wrong.add(expected.number());
            }
        }

        GradeResult result = new GradeResult(correct, wrong);
        repository.writeGrade(result.format(), outputDirectory);
        return result;
    }

    private List<ExpectedAnswer> parseExercises(List<String> lines) {
        List<ExpectedAnswer> answers = new ArrayList<>(lines.size());
        Set<Integer> seenNumbers = new HashSet<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            Matcher matcher = EXERCISE_LINE.matcher(line);
            if (!matcher.matches()) {
                throw new UserInputException("题目文件第 " + (index + 1) + " 行格式错误");
            }
            int number = parseLineNumber(matcher.group(1), "题目", index);
            if (!seenNumbers.add(number)) {
                throw new UserInputException("题目编号重复: " + number);
            }
            try {
                Fraction value = ExpressionParser.parse(matcher.group(2)).evaluate();
                answers.add(new ExpectedAnswer(number, value));
            } catch (ExpressionParseException | ArithmeticException exception) {
                throw new UserInputException(
                        "题目文件第 " + (index + 1) + " 行无法计算: " + exception.getMessage());
            }
        }
        return answers;
    }

    private SubmittedAnswers parseAnswers(List<String> lines) {
        Map<Integer, Fraction> values = new HashMap<>();
        Set<Integer> invalidNumbers = new HashSet<>();
        Set<Integer> seenNumbers = new HashSet<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            Matcher matcher = ANSWER_LINE.matcher(line);
            if (!matcher.matches()) {
                throw new UserInputException("答案文件第 " + (index + 1) + " 行格式错误");
            }
            int number = parseLineNumber(matcher.group(1), "答案", index);
            if (!seenNumbers.add(number)) {
                throw new UserInputException("答案编号重复: " + number);
            }
            try {
                values.put(number, ExpressionParser.parseFraction(matcher.group(2)));
            } catch (ExpressionParseException | ArithmeticException exception) {
                // 某一答案内容非法时只判该题错误，不阻止其他题目的统计。
                invalidNumbers.add(number);
            }
        }
        return new SubmittedAnswers(values, invalidNumbers);
    }

    private int parseLineNumber(String source, String type, int lineIndex) {
        try {
            int number = Integer.parseInt(source);
            if (number <= 0) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new UserInputException(type + "文件第 " + (lineIndex + 1) + " 行编号不合法");
        }
    }

    private record ExpectedAnswer(int number, Fraction value) {
    }

    private record SubmittedAnswers(Map<Integer, Fraction> values, Set<Integer> invalidNumbers) {
    }
}

