package com.exercise.arithmetic;

import com.exercise.arithmetic.generator.Exercise;
import com.exercise.arithmetic.generator.ExerciseGenerator;
import com.exercise.arithmetic.io.ExerciseFileRepository;
import com.exercise.arithmetic.model.BinaryExpression;
import com.exercise.arithmetic.model.Expression;
import com.exercise.arithmetic.model.Fraction;
import com.exercise.arithmetic.model.NumberExpression;
import com.exercise.arithmetic.model.Operator;
import com.exercise.arithmetic.parser.ExpressionParseException;
import com.exercise.arithmetic.parser.ExpressionParser;
import com.exercise.arithmetic.service.ExerciseService;
import com.exercise.arithmetic.service.GradeResult;
import com.exercise.arithmetic.service.GradingService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 无第三方依赖的回归测试入口，便于在仅安装 JDK 的环境中执行。 */
public final class TestRunner {
    private final List<TestCase> tests = new ArrayList<>();
    private Path workDirectory;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("需要提供测试工作目录");
        }
        TestRunner runner = new TestRunner();
        runner.workDirectory = Files.createTempDirectory(Path.of(args[0]), "run-");
        try {
            runner.registerTests();
            runner.runAll();
        } finally {
            runner.deleteRecursively(runner.workDirectory);
        }
    }

    private void registerTests() {
        add("T01 分数精确运算与约分", this::testFractionArithmetic);
        add("T02 真分数和带分数格式", this::testFractionFormatting);
        add("T03 表达式优先级与括号", this::testParserPrecedence);
        add("T04 加法乘法交换等价判重", this::testCommutativeDuplicateKeys);
        add("T05 题目示例中的嵌套判重", this::testNestedDuplicateRule);
        add("T06 随机题目的全部生成约束", this::testGeneratedExerciseRules);
        add("T07 默认题目数量和输出格式", this::testDefaultCountAndFileFormat);
        add("T08 缺少必填范围参数", this::testMissingRangeParameter);
        add("T09 生成文件与标准答案批改", this::testEndToEndCorrectGrading);
        add("T10 错答、非法答案和缺失答案", this::testMixedGradingResult);
        add("T11 最小范围 r=1", this::testMinimumRange);
        add("T12 一次生成一万道题", this::testTenThousandExercises);
        add("T13 非法表达式输入", this::testMalformedExpression);
        add("T14 帮助参数", this::testHelpOption);
    }

    private void runAll() throws Exception {
        int passed = 0;
        List<String> failures = new ArrayList<>();
        long suiteStart = System.nanoTime();
        for (TestCase test : tests) {
            long start = System.nanoTime();
            try {
                test.body().run();
                passed++;
                System.out.printf("PASS %-28s %8.3f ms%n", test.name(), elapsedMillis(start));
            } catch (Throwable throwable) {
                failures.add(test.name() + ": " + throwable.getMessage());
                System.out.printf("FAIL %-28s %s%n", test.name(), throwable.getMessage());
            }
        }

        System.out.printf(
                "SUMMARY passed=%d failed=%d total=%d elapsed_ms=%.3f%n",
                passed,
                failures.size(),
                tests.size(),
                elapsedMillis(suiteStart));
        if (!failures.isEmpty()) {
            failures.forEach(failure -> System.out.println("  - " + failure));
            System.exit(1);
        }
    }

    private void testFractionArithmetic() {
        Fraction result = new Fraction(1, 6).add(new Fraction(1, 8));
        assertEquals(new Fraction(7, 24), result, "1/6 + 1/8 应等于 7/24");
        assertEquals(new Fraction(1, 2), new Fraction(6, 12), "分数应自动约分");
    }

    private void testFractionFormatting() {
        assertEquals("3/5", new Fraction(3, 5).toDisplayString(), "真分数格式错误");
        assertEquals("2’3/8", new Fraction(19, 8).toDisplayString(), "带分数格式错误");
        assertEquals("4", new Fraction(12, 3).toDisplayString(), "整数格式错误");
    }

    private void testParserPrecedence() {
        Expression expression = ExpressionParser.parse("1/2 + 3 × (2 − 1/3)");
        assertEquals(new Fraction(11, 2), expression.evaluate(), "运算优先级或括号解析错误");
        assertEquals(new Fraction(2, 3), ExpressionParser.parse("1/2 ÷ 3/4").evaluate(), "分数除法错误");
    }

    private void testCommutativeDuplicateKeys() {
        assertEquals(
                ExpressionParser.parse("23 + 45").canonicalKey(),
                ExpressionParser.parse("45 + 23").canonicalKey(),
                "交换加法左右两侧后应判为重复");
        assertEquals(
                ExpressionParser.parse("6 × 8").canonicalKey(),
                ExpressionParser.parse("8 × 6").canonicalKey(),
                "交换乘法左右两侧后应判为重复");
    }

    private void testNestedDuplicateRule() {
        Expression first = ExpressionParser.parse("3 + (2 + 1)");
        Expression second = ExpressionParser.parse("1 + 2 + 3");
        Expression third = ExpressionParser.parse("3 + 2 + 1");
        assertEquals(first.canonicalKey(), second.canonicalKey(), "题目指定的两个表达式应重复");
        assertNotEquals(first.canonicalKey(), third.canonicalKey(), "不同树结构不应错误判重");
        assertEquals(
                first.canonicalKey(),
                ExpressionParser.parse(first.format()).canonicalKey(),
                "格式化输出必须保留表达式树结构");
    }

    private void testGeneratedExerciseRules() {
        int range = 10;
        List<Exercise> exercises = new ExerciseGenerator(4171L).generate(1_000, range);
        Set<String> keys = new HashSet<>();
        for (Exercise exercise : exercises) {
            Expression expression = exercise.expression();
            assertTrue(expression.operatorCount() >= 1 && expression.operatorCount() <= 3,
                    "运算符数量应为 1～3");
            assertTrue(keys.add(expression.canonicalKey()), "生成了交换等价的重复题目");
            validateExpression(expression, range);
        }
    }

    private void validateExpression(Expression expression, int range) {
        assertTrue(expression.evaluate().compareTo(Fraction.ZERO) >= 0, "表达式中出现负数结果");
        if (expression instanceof NumberExpression) {
            Fraction value = expression.evaluate();
            assertTrue(value.compareTo(Fraction.of(range)) < 0, "运算数达到或超过 -r 上限");
            assertTrue(value.denominator().compareTo(BigInteger.valueOf(range)) < 0 || value.denominator().equals(BigInteger.ONE),
                    "分母达到或超过 -r 上限");
            return;
        }

        BinaryExpression binary = (BinaryExpression) expression;
        validateExpression(binary.left(), range);
        validateExpression(binary.right(), range);
        if (binary.operator() == Operator.SUBTRACT) {
            assertTrue(binary.left().evaluate().compareTo(binary.right().evaluate()) >= 0,
                    "减法子表达式产生负数");
        }
        if (binary.operator() == Operator.DIVIDE) {
            assertTrue(!binary.right().evaluate().isZero(), "除数为 0");
            assertTrue(binary.evaluate().isProperFraction(), "除法结果不是真分数");
        }
    }

    private void testDefaultCountAndFileFormat() throws Exception {
        Path directory = caseDirectory("default-count");
        CapturedRun run = runMain(directory, "-r", "10");
        assertEquals(0, run.exitCode(), "合法生成命令应成功");
        List<String> exercises = Files.readAllLines(directory.resolve("Exercises.txt"), StandardCharsets.UTF_8);
        List<String> answers = Files.readAllLines(directory.resolve("Answers.txt"), StandardCharsets.UTF_8);
        assertEquals(10, exercises.size(), "未提供 -n 时应生成 10 道题");
        assertEquals(10, answers.size(), "答案数量应与题目一致");
        for (int index = 0; index < exercises.size(); index++) {
            assertTrue(exercises.get(index).matches((index + 1) + "\\. .+ ="), "题目行格式错误");
            assertTrue(answers.get(index).matches((index + 1) + "\\. .+"), "答案行格式错误");
        }
    }

    private void testMissingRangeParameter() {
        CapturedRun run = runMain(workDirectory, "-n", "10");
        assertEquals(2, run.exitCode(), "缺少 -r 时应以参数错误结束");
        assertTrue(run.error().contains("-r"), "错误信息应指出缺少 -r");
        assertTrue(run.error().contains("用法"), "参数错误时应输出帮助信息");
    }

    private void testEndToEndCorrectGrading() throws Exception {
        Path directory = caseDirectory("end-to-end");
        ExerciseFileRepository repository = new ExerciseFileRepository();
        new ExerciseService(new ExerciseGenerator(2026L), repository).generateAndWrite(100, 10, directory);
        CapturedRun run = runMain(
                directory,
                "-e", directory.resolve("Exercises.txt").toString(),
                "-a", directory.resolve("Answers.txt").toString());
        assertEquals(0, run.exitCode(), "合法批改命令应成功");
        String grade = Files.readString(directory.resolve("Grade.txt"), StandardCharsets.UTF_8);
        assertTrue(grade.contains("Correct: 100 ("), "标准答案应有 100 道正确题");
        assertTrue(grade.contains("Wrong: 0 ()"), "标准答案不应包含错题");
    }

    private void testMixedGradingResult() throws Exception {
        Path directory = caseDirectory("mixed-grade");
        Path exercises = directory.resolve("Exercises.txt");
        Path answers = directory.resolve("Submitted.txt");
        Files.writeString(exercises, """
                1. 1/6 + 1/8 =
                2. 2 × 3 =
                3. 1 + 1 =
                4. 1/2 ÷ 2 =
                """, StandardCharsets.UTF_8);
        Files.writeString(answers, """
                1. 7/24
                2. 5
                3. abc
                """, StandardCharsets.UTF_8);

        GradeResult result = new GradingService(new ExerciseFileRepository())
                .grade(exercises, answers, directory);
        assertEquals(List.of(1), result.correctNumbers(), "正确题号统计错误");
        assertEquals(List.of(2, 3, 4), result.wrongNumbers(), "错误题号统计错误");
    }

    private void testMinimumRange() {
        List<Exercise> exercises = new ExerciseGenerator(1L).generate(10, 1);
        assertEquals(10, exercises.size(), "-r 1 应能生成默认数量的题目");
        for (Exercise exercise : exercises) {
            validateExpression(exercise.expression(), 1);
        }
    }

    private void testTenThousandExercises() {
        List<Exercise> exercises = new ExerciseGenerator(20260920L).generate(10_000, 10);
        assertEquals(10_000, exercises.size(), "未生成 10,000 道题");
        Set<String> keys = new HashSet<>();
        for (Exercise exercise : exercises) {
            assertTrue(keys.add(exercise.expression().canonicalKey()), "10,000 道题中存在重复");
            assertTrue(exercise.expression().operatorCount() <= 3, "10,000 道题中存在运算符超限");
        }
    }

    private void testMalformedExpression() {
        assertThrows(ExpressionParseException.class, () -> ExpressionParser.parse("1 + (2 × 3"),
                "缺失右括号应解析失败");
        assertThrows(ExpressionParseException.class, () -> ExpressionParser.parse("1/0"),
                "分母为 0 应解析失败");
    }

    private void testHelpOption() {
        CapturedRun run = runMain(workDirectory, "--help");
        assertEquals(0, run.exitCode(), "帮助命令应成功结束");
        assertTrue(run.output().contains("生成题目") && run.output().contains("批改答案"),
                "帮助信息不完整");
    }

    private CapturedRun runMain(Path workingDirectory, String... arguments) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(error, true, StandardCharsets.UTF_8)) {
            exitCode = Main.run(arguments, workingDirectory, out, err);
        }
        return new CapturedRun(
                exitCode,
                output.toString(StandardCharsets.UTF_8),
                error.toString(StandardCharsets.UTF_8));
    }

    private Path caseDirectory(String name) throws Exception {
        return Files.createDirectories(workDirectory.resolve(name));
    }

    private void add(String name, ThrowingRunnable body) {
        tests.add(new TestCase(name, body));
    }

    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }

    private void deleteRecursively(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual);
        }
    }

    private static void assertNotEquals(Object first, Object second, String message) {
        if (java.util.Objects.equals(first, second)) {
            throw new AssertionError(message + "，两者均为=" + first);
        }
    }

    private static void assertThrows(
            Class<? extends Throwable> expectedType,
            ThrowingRunnable action,
            String message) {
        try {
            action.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(message + "，异常类型=" + throwable.getClass().getName());
        }
        throw new AssertionError(message + "，但没有抛出异常");
    }

    private record TestCase(String name, ThrowingRunnable body) {
    }

    private record CapturedRun(int exitCode, String output, String error) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
