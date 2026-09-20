package com.exercise.arithmetic.model;

import java.util.Objects;

public final class BinaryExpression implements Expression {
    private final Expression left;
    private final Operator operator;
    private final Expression right;
    private final Fraction value;

    public BinaryExpression(Expression left, Operator operator, Expression right) {
        this.left = Objects.requireNonNull(left, "left");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.right = Objects.requireNonNull(right, "right");
        this.value = operator.apply(left.evaluate(), right.evaluate());
    }

    public Expression left() {
        return left;
    }

    public Operator operator() {
        return operator;
    }

    public Expression right() {
        return right;
    }

    @Override
    public Fraction evaluate() {
        return value;
    }

    @Override
    public int operatorCount() {
        return left.operatorCount() + right.operatorCount() + 1;
    }

    @Override
    public int precedence() {
        return operator.precedence();
    }

    @Override
    public String canonicalKey() {
        String leftKey = left.canonicalKey();
        String rightKey = right.canonicalKey();

        // 只在当前 + 或 × 节点交换左右子树，不展平表达式树；这与题目规定的判重规则一致。
        if (operator.isCommutative() && leftKey.compareTo(rightKey) > 0) {
            String temporary = leftKey;
            leftKey = rightKey;
            rightKey = temporary;
        }
        return operator.name() + "(" + leftKey + "," + rightKey + ")";
    }

    @Override
    public String format() {
        return formatChild(left, false) + " " + operator.symbol() + " " + formatChild(right, true);
    }

    private String formatChild(Expression child, boolean rightChild) {
        boolean needsParentheses = child.precedence() < precedence();
        if (child instanceof BinaryExpression && child.precedence() == precedence()) {
            needsParentheses = needsParenthesesForEqualPrecedence(rightChild);
        }
        String text = child.format();
        return needsParentheses ? "(" + text + ")" : text;
    }

    private boolean needsParenthesesForEqualPrecedence(boolean rightChild) {
        // 同优先级右子树必须保留括号。即使加法数值上满足结合律，题目的判重规则仍区分
        // (a + b) + c 与 a + (b + c) 的树结构，省略括号会改变题目身份。
        return rightChild;
    }
}
