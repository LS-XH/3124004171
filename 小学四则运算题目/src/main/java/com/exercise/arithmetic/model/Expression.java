package com.exercise.arithmetic.model;

public interface Expression {
    Fraction evaluate();

    int operatorCount();

    int precedence();

    String canonicalKey();

    String format();
}

