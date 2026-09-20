package com.exercise.arithmetic.profile;

import com.exercise.arithmetic.generator.ExerciseGenerator;
import com.exercise.arithmetic.io.ExerciseFileRepository;
import com.exercise.arithmetic.service.ExerciseService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * 固定工作负载的性能基准入口，仅用于 JFR 分析，不参与用户程序的打包。
 */
public final class PerformanceBenchmark {
    private static final long RANDOM_SEED = 20260920L;

    private PerformanceBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        int warmups = readArgument(args, 0, 5);
        int iterations = readArgument(args, 1, 20);
        int count = readArgument(args, 2, 10_000);
        int range = readArgument(args, 3, 10);
        Path outputDirectory = args.length > 4 ? Path.of(args[4]) : Path.of("profile", "work");
        Files.createDirectories(outputDirectory);

        for (int index = 0; index < warmups; index++) {
            runOnce(count, range, outputDirectory);
        }

        long[] elapsedNanos = new long[iterations];
        for (int index = 0; index < iterations; index++) {
            long start = System.nanoTime();
            runOnce(count, range, outputDirectory);
            elapsedNanos[index] = System.nanoTime() - start;
            System.out.printf("iteration=%d elapsed_ms=%.3f%n", index + 1, toMillis(elapsedNanos[index]));
        }

        long[] sorted = elapsedNanos.clone();
        Arrays.sort(sorted);
        double median = iterations % 2 == 0
                ? (toMillis(sorted[iterations / 2 - 1]) + toMillis(sorted[iterations / 2])) / 2.0
                : toMillis(sorted[iterations / 2]);
        double average = Arrays.stream(elapsedNanos).average().orElseThrow() / 1_000_000.0;
        System.out.printf(
                "summary warmups=%d iterations=%d count=%d range=%d median_ms=%.3f average_ms=%.3f min_ms=%.3f max_ms=%.3f%n",
                warmups,
                iterations,
                count,
                range,
                median,
                average,
                toMillis(sorted[0]),
                toMillis(sorted[sorted.length - 1]));
    }

    private static void runOnce(int count, int range, Path outputDirectory) throws Exception {
        ExerciseService service = new ExerciseService(
                new ExerciseGenerator(RANDOM_SEED),
                new ExerciseFileRepository());
        service.generateAndWrite(count, range, outputDirectory);
    }

    private static int readArgument(String[] args, int index, int defaultValue) {
        return args.length > index ? Integer.parseInt(args[index]) : defaultValue;
    }

    private static double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
