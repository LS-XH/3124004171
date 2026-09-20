package com.exercise.arithmetic.generator;

import com.exercise.arithmetic.model.Expression;

import java.util.Objects;

public record Exercise(int number, Expression expression) {
    public Exercise {
        if (number <= 0) {
            throw new IllegalArgumentException("题目编号必须为正数");
        }
        Objects.requireNonNull(expression, "expression");
    }
}

