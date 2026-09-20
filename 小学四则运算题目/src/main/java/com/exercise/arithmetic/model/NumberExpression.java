package com.exercise.arithmetic.model;

import java.util.Objects;

public final class NumberExpression implements Expression {
    private final Fraction value;

    public NumberExpression(Fraction value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public Fraction evaluate() {
        return value;
    }

    @Override
    public int operatorCount() {
        return 0;
    }

    @Override
    public int precedence() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String canonicalKey() {
        return "N(" + value.canonicalKey() + ")";
    }

    @Override
    public String format() {
        return value.toDisplayString();
    }
}

