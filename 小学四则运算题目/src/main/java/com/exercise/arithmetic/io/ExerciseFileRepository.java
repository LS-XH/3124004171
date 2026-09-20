package com.exercise.arithmetic.io;

import com.exercise.arithmetic.generator.Exercise;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ExerciseFileRepository {
    public static final String EXERCISES_FILE_NAME = "Exercises.txt";
    public static final String ANSWERS_FILE_NAME = "Answers.txt";
    public static final String GRADE_FILE_NAME = "Grade.txt";

    public void writeGeneratedFiles(List<Exercise> exercises, Path outputDirectory) throws IOException {
        Path exercisePath = outputDirectory.resolve(EXERCISES_FILE_NAME);
        Path answerPath = outputDirectory.resolve(ANSWERS_FILE_NAME);

        try (BufferedWriter exerciseWriter = Files.newBufferedWriter(exercisePath, StandardCharsets.UTF_8);
             BufferedWriter answerWriter = Files.newBufferedWriter(answerPath, StandardCharsets.UTF_8)) {
            for (Exercise exercise : exercises) {
                exerciseWriter.write(exercise.number() + ". " + exercise.expression().format() + " =");
                exerciseWriter.newLine();
                answerWriter.write(exercise.number() + ". "
                        + exercise.expression().evaluate().toDisplayString());
                answerWriter.newLine();
            }
        }
    }

    public List<String> readLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    public void writeGrade(String content, Path outputDirectory) throws IOException {
        Files.writeString(
                outputDirectory.resolve(GRADE_FILE_NAME),
                content,
                StandardCharsets.UTF_8);
    }
}

