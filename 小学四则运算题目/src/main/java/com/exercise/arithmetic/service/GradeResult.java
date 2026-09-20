package com.exercise.arithmetic.service;

import java.util.List;

public record GradeResult(List<Integer> correctNumbers, List<Integer> wrongNumbers) {
    public GradeResult {
        correctNumbers = List.copyOf(correctNumbers);
        wrongNumbers = List.copyOf(wrongNumbers);
    }

    public String format() {
        return "Correct: " + correctNumbers.size() + " (" + join(correctNumbers) + ")"
                + System.lineSeparator()
                + "Wrong: " + wrongNumbers.size() + " (" + join(wrongNumbers) + ")"
                + System.lineSeparator();
    }

    private String join(List<Integer> numbers) {
        return numbers.stream().map(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("");
    }
}

