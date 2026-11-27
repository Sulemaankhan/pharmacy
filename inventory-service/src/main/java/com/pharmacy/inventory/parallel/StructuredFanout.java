package com.pharmacy.inventory.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Gatherers;

public final class StructuredFanout {
    private static final int LIMIT = 4;

    private StructuredFanout() {}

    public static void each(List<Runnable> tasks) {
        all(tasks.stream().map(task -> (Callable<Void>) () -> {
            task.run();
            return null;
        }).toList());
    }

    public static <A, B> Pair<A, B> both(Callable<A> left, Callable<B> right) {
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<A> leftTask = exec.submit(left);
            Future<B> rightTask = exec.submit(right);
            return new Pair<>(get(leftTask), get(rightTask));
        }
    }

    public static <T> List<T> all(List<Callable<T>> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        if (tasks.size() == 1) {
            List<T> result = new ArrayList<>(1);
            result.add(call(tasks.getFirst()));
            return result;
        }
        List<T> results = new ArrayList<>();
        for (List<Callable<T>> batch : tasks.stream().gather(Gatherers.windowFixed(LIMIT)).toList()) {
            results.addAll(run(batch));
        }
        return results;
    }

    private static <T> List<T> run(List<Callable<T>> batch) {
        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<T>> futures = new ArrayList<>(batch.size());
            for (Callable<T> task : batch) {
                futures.add(exec.submit(task));
            }
            List<T> results = new ArrayList<>(futures.size());
            RuntimeException failure = null;
            for (Future<T> future : futures) {
                try {
                    results.add(get(future));
                } catch (RuntimeException ex) {
                    if (failure == null) {
                        failure = ex;
                    }
                    future.cancel(true);
                }
            }
            if (failure != null) {
                throw failure;
            }
            return results;
        }
    }

    private static <T> T get(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        } catch (ExecutionException ex) {
            throw unchecked(ex.getCause() == null ? ex : ex.getCause());
        }
    }

    private static <T> T call(Callable<T> task) {
        try {
            return task.call();
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static RuntimeException unchecked(Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(cause);
    }

    public record Pair<A, B>(A left, B right) {}
}
