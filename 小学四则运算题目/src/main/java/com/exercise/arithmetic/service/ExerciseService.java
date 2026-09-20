package com.exercise.arithmetic.service;

import com.exercise.arithmetic.generator.Exercise;
import com.exercise.arithmetic.generator.ExerciseGenerator;
import com.exercise.arithmetic.io.ExerciseFileRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ExerciseService {
    private final ExerciseGenerator generator;
    private final ExerciseFileRepository repository;

    public ExerciseService(ExerciseGenerator generator, ExerciseFileRepository repository) {
        this.generator = generator;
        this.repository = repository;
    }

    public void generateAndWrite(int count, int range, Path outputDirectory) throws IOException {
        List<Exercise> exercises = generator.generate(count, range);
        repository.writeGeneratedFiles(exercises, outputDirectory);
    }
}

