package com.exercise.arithmetic.io;

import com.exercise.arithmetic.generator.Exercise;

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

        String lineSeparator = System.lineSeparator();
        StringBuilder exerciseContent = new StringBuilder(exercises.size() * 32);
        StringBuilder answerContent = new StringBuilder(exercises.size() * 16);
        for (Exercise exercise : exercises) {
            exerciseContent.append(exercise.number())
                    .append(". ")
                    .append(exercise.expression().format())
                    .append(" =")
                    .append(lineSeparator);
            answerContent.append(exercise.number())
                    .append(". ")
                    .append(exercise.expression().evaluate().toDisplayString())
                    .append(lineSeparator);
        }
        // 聚合后各写入一次，减少 10,000 道题场景中的重复 writer 调用。
        Files.writeString(exercisePath, exerciseContent, StandardCharsets.UTF_8);
        Files.writeString(answerPath, answerContent, StandardCharsets.UTF_8);
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
