package com.exercise.arithmetic.model;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 不可变的精确分数。分母始终为正数，分子和分母始终互质。
 */
public final class Fraction implements Comparable<Fraction> {
    public static final Fraction ZERO = new Fraction(BigInteger.ZERO, BigInteger.ONE);
    public static final Fraction ONE = new Fraction(BigInteger.ONE, BigInteger.ONE);

    private static final char MIXED_NUMBER_SEPARATOR = '\u2019';

    private final BigInteger numerator;
    private final BigInteger denominator;

    public Fraction(long numerator, long denominator) {
        this(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
    }

    public Fraction(BigInteger numerator, BigInteger denominator) {
        Objects.requireNonNull(numerator, "numerator");
        Objects.requireNonNull(denominator, "denominator");
        if (denominator.signum() == 0) {
            throw new ArithmeticException("分母不能为 0");
        }

        // 统一符号并立即约分，使数值相等的分数拥有相同的内部表示。
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }
        BigInteger gcd = numerator.gcd(denominator);
        this.numerator = numerator.divide(gcd);
        this.denominator = denominator.divide(gcd);
    }

    public static Fraction of(long value) {
        return new Fraction(value, 1);
    }

    public BigInteger numerator() {
        return numerator;
    }

    public BigInteger denominator() {
        return denominator;
    }

    public Fraction add(Fraction other) {
        return new Fraction(
                numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public Fraction subtract(Fraction other) {
        return new Fraction(
                numerator.multiply(other.denominator).subtract(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public Fraction multiply(Fraction other) {
        return new Fraction(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    public Fraction divide(Fraction other) {
        if (other.isZero()) {
            throw new ArithmeticException("除数不能为 0");
        }
        return new Fraction(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }

    public boolean isZero() {
        return numerator.signum() == 0;
    }

    public boolean isProperFraction() {
        return numerator.signum() > 0 && numerator.compareTo(denominator) < 0;
    }

    public String canonicalKey() {
        return numerator + "/" + denominator;
    }

    /**
     * 按作业格式输出自然数、真分数或带分数。
     */
    public String toDisplayString() {
        BigInteger[] quotientAndRemainder = numerator.divideAndRemainder(denominator);
        BigInteger whole = quotientAndRemainder[0];
        BigInteger remainder = quotientAndRemainder[1].abs();

        if (remainder.signum() == 0) {
            return whole.toString();
        }
        if (whole.signum() == 0) {
            return numerator + "/" + denominator;
        }
        return whole + String.valueOf(MIXED_NUMBER_SEPARATOR) + remainder + "/" + denominator;
    }

    @Override
    public int compareTo(Fraction other) {
        return numerator.multiply(other.denominator)
                .compareTo(other.numerator.multiply(denominator));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Fraction other)) {
            return false;
        }
        return numerator.equals(other.numerator) && denominator.equals(other.denominator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numerator, denominator);
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}

