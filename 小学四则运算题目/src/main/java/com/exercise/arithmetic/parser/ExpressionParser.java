package com.exercise.arithmetic.parser;

import com.exercise.arithmetic.model.BinaryExpression;
import com.exercise.arithmetic.model.Expression;
import com.exercise.arithmetic.model.Fraction;
import com.exercise.arithmetic.model.NumberExpression;
import com.exercise.arithmetic.model.Operator;

import java.math.BigInteger;

/** 使用递归下降法解析题目中的四则运算表达式。 */
public final class ExpressionParser {
    private final String source;
    private int position;

    private ExpressionParser(String source) {
        this.source = source;
    }

    public static Expression parse(String source) {
        if (source == null || source.isBlank()) {
            throw new ExpressionParseException("表达式不能为空");
        }
        ExpressionParser parser = new ExpressionParser(source);
        Expression expression = parser.parseAddAndSubtract();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw parser.error("存在无法识别的内容");
        }
        return expression;
    }

    public static Fraction parseFraction(String source) {
        Expression expression = parse(source);
        if (expression.operatorCount() != 0) {
            throw new ExpressionParseException("答案必须是自然数、分数或带分数");
        }
        return expression.evaluate();
    }

    private Expression parseAddAndSubtract() {
        Expression expression = parseMultiplyAndDivide();
        while (true) {
            skipWhitespace();
            if (match('+')) {
                expression = new BinaryExpression(expression, Operator.ADD, parseMultiplyAndDivide());
            } else if (match('-') || match('−')) {
                expression = new BinaryExpression(expression, Operator.SUBTRACT, parseMultiplyAndDivide());
            } else {
                return expression;
            }
        }
    }

    private Expression parseMultiplyAndDivide() {
        Expression expression = parsePrimary();
        while (true) {
            skipWhitespace();
            if (match('×') || match('*')) {
                expression = new BinaryExpression(expression, Operator.MULTIPLY, parsePrimary());
            } else if (match('÷')) {
                expression = new BinaryExpression(expression, Operator.DIVIDE, parsePrimary());
            } else {
                return expression;
            }
        }
    }

    private Expression parsePrimary() {
        skipWhitespace();
        if (match('(')) {
            Expression expression = parseAddAndSubtract();
            skipWhitespace();
            if (!match(')')) {
                throw error("缺少右括号");
            }
            return expression;
        }
        return new NumberExpression(parseNumber());
    }

    private Fraction parseNumber() {
        skipWhitespace();
        BigInteger first = readUnsignedInteger("需要自然数或分数");

        if (match('\'') || match('’')) {
            BigInteger numerator = readUnsignedInteger("带分数缺少分子");
            require('/');
            BigInteger denominator = readUnsignedInteger("带分数缺少分母");
            validateDenominator(denominator);
            return new Fraction(first.multiply(denominator).add(numerator), denominator);
        }
        if (match('/')) {
            BigInteger denominator = readUnsignedInteger("分数缺少分母");
            validateDenominator(denominator);
            return new Fraction(first, denominator);
        }
        return new Fraction(first, BigInteger.ONE);
    }

    private BigInteger readUnsignedInteger(String message) {
        int start = position;
        while (!isAtEnd() && Character.isDigit(source.charAt(position))) {
            position++;
        }
        if (start == position) {
            throw error(message);
        }
        return new BigInteger(source.substring(start, position));
    }

    private void validateDenominator(BigInteger denominator) {
        if (denominator.signum() == 0) {
            throw error("分母不能为 0");
        }
    }

    private void require(char expected) {
        if (!match(expected)) {
            throw error("需要字符 '" + expected + "'");
        }
    }

    private boolean match(char expected) {
        if (!isAtEnd() && source.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }
    }

    private boolean isAtEnd() {
        return position >= source.length();
    }

    private ExpressionParseException error(String message) {
        return new ExpressionParseException(message + "（位置 " + position + "）: " + source);
    }
}
