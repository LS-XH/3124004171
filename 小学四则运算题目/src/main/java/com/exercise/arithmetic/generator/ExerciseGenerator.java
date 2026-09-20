package com.exercise.arithmetic.generator;

import com.exercise.arithmetic.model.BinaryExpression;
import com.exercise.arithmetic.model.Expression;
import com.exercise.arithmetic.model.Fraction;
import com.exercise.arithmetic.model.NumberExpression;
import com.exercise.arithmetic.model.Operator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

public final class ExerciseGenerator {
    private static final int MAX_OPERATORS = 3;
    private static final int ATTEMPTS_PER_EXERCISE = 500;
    private static final Operator[] OPERATORS = Operator.values();

    private final SplittableRandom random;

    public ExerciseGenerator() {
        this(new SplittableRandom());
    }

    public ExerciseGenerator(long seed) {
        this(new SplittableRandom(seed));
    }

    private ExerciseGenerator(SplittableRandom random) {
        this.random = random;
    }

    public List<Exercise> generate(int count, int range) {
        if (count <= 0) {
            throw new IllegalArgumentException("题目数量必须大于 0");
        }
        if (range <= 0) {
            throw new IllegalArgumentException("数值范围必须大于 0");
        }

        List<Exercise> exercises = new ArrayList<>(count);
        Set<String> canonicalKeys = new HashSet<>(hashSetCapacity(count));
        long maximumAttempts = Math.max(10_000L, (long) count * ATTEMPTS_PER_EXERCISE);

        for (long attempt = 0; exercises.size() < count && attempt < maximumAttempts; attempt++) {
            int operatorCount = random.nextInt(1, MAX_OPERATORS + 1);
            Expression expression = generateExpression(operatorCount, range);
            if (expression == null) {
                continue;
            }

            String key = expression.canonicalKey();
            if (canonicalKeys.add(key)) {
                exercises.add(new Exercise(exercises.size() + 1, expression));
            }
        }

        if (exercises.size() != count) {
            throw new ExerciseGenerationException(
                    "在范围 " + range + " 内只能生成 " + exercises.size()
                            + " 道不重复题目，请增大 -r 或减小 -n");
        }
        return exercises;
    }

    private Expression generateExpression(int operatorCount, int range) {
        if (operatorCount == 0) {
            return generateNumber(range);
        }

        int leftOperatorCount = random.nextInt(operatorCount);
        int rightOperatorCount = operatorCount - leftOperatorCount - 1;
        Expression left = generateExpression(leftOperatorCount, range);
        Expression right = generateExpression(rightOperatorCount, range);
        // 任一子树违反约束时，本次候选整体作废，交由外层重新生成。
        if (left == null || right == null) {
            return null;
        }
        Operator operator = OPERATORS[random.nextInt(OPERATORS.length)];

        Fraction leftValue = left.evaluate();
        Fraction rightValue = right.evaluate();

        // 减法左右值不满足约束时直接交换，比丢弃整棵已生成的子树更节省分配和重试。
        if (operator == Operator.SUBTRACT && leftValue.compareTo(rightValue) < 0) {
            Expression temporaryExpression = left;
            left = right;
            right = temporaryExpression;
            Fraction temporaryValue = leftValue;
            leftValue = rightValue;
            rightValue = temporaryValue;
        }
        if (operator == Operator.DIVIDE) {
            if (leftValue.isZero() || rightValue.isZero() || leftValue.equals(rightValue)) {
                return null;
            }
            // 将较小值放在左侧，使商天然位于 (0, 1)，避免计算后再丢弃候选。
            if (leftValue.compareTo(rightValue) > 0) {
                Expression temporaryExpression = left;
                left = right;
                right = temporaryExpression;
                Fraction temporaryValue = leftValue;
                leftValue = rightValue;
                rightValue = temporaryValue;
            }
        }
        Fraction result = operator.apply(leftValue, rightValue);
        return BinaryExpression.withPrecomputedValue(left, operator, right, result);
    }

    private NumberExpression generateNumber(int range) {
        // r=1 或 r=2 时不存在分母位于 [2, r) 的真分数，只生成自然数。
        boolean generateFraction = range >= 3 && random.nextInt(100) < 45;
        if (!generateFraction) {
            return new NumberExpression(Fraction.of(random.nextInt(range)));
        }

        int denominator = random.nextInt(2, range);
        int numerator = random.nextInt(1, denominator);
        int whole = random.nextInt(100) < 30 ? random.nextInt(range) : 0;
        long improperNumerator = (long) whole * denominator + numerator;
        return new NumberExpression(new Fraction(improperNumerator, denominator));
    }

    private int hashSetCapacity(int count) {
        long desired = (long) Math.ceil(count / 0.75d) + 1L;
        return (int) Math.min(desired, 1 << 30);
    }
}
