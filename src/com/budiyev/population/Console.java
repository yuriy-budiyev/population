/*
 * Population
 * Copyright (C) 2016 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.budiyev.population;

import com.budiyev.population.model.Calculator;
import com.budiyev.population.model.Result;
import com.budiyev.population.model.Task;
import com.budiyev.population.model.Transition;
import com.budiyev.population.util.PopulationThreadFactory;
import com.budiyev.population.util.TaskParser;
import com.budiyev.population.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public final class Console {
    private static final String KEY_HELP = "-help";
    private static final String KEY_TASK = "-task";
    private static final String KEY_TASKS = "-tasks";
    private static final String KEY_INTERVAL = "-interval";
    private static final String KEY_PARALLEL = "-parallel";

    public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
            (thread, throwable) -> {
                System.out.println("Error");
                System.out.println(Utils.buildErrorText(throwable));
            };

    private static final ThreadFactory THREAD_FACTORY =
            new PopulationThreadFactory(UNCAUGHT_EXCEPTION_HANDLER);

    private Console() {
    }

    private static File buildResultFile(File inputFile) {
        return new File(inputFile.getAbsolutePath() + ".result.csv");
    }

    private static File buildResultFile(String inputFilePath, int number) {
        return new File(inputFilePath + ".result_" + number + ".csv");
    }

    private static void printInitialization(int tasks, int processors, boolean parallel) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Initialized");
        if (tasks > 1) {
            stringBuilder.append(", tasks: ").append(tasks);
        }
        stringBuilder.append(", processors: ").append(processors);
        if (parallel) {
            stringBuilder.append(", parallel");
        }
        System.out.println(stringBuilder.toString());
    }

    private static void calculateTask(File inputFile, File resultFile,
            ResourceBundle resources) throws IOException {
        System.out.println("Calculating: " + inputFile.getName());
        Task task = TaskParser.parse(inputFile);
        if (task == null) {
            System.out.println("Can't load: " + inputFile.getName());
            return;
        }
        Result result = Calculator.calculateSync(task, true, false, THREAD_FACTORY);
        Utils.exportResults(resultFile, result, task.getColumnSeparator(),
                task.getDecimalSeparator(), task.getLineSeparator(), task.getEncoding(), resources);
        System.out.println("Done: " + resultFile.getName());
    }

    private static void calculateTask(Task task, ResourceBundle resources) throws IOException {
        int taskId = task.getId();
        System.out.println("Calculating: " + taskId);
        Result result = Calculator.calculateSync(task, true, false, THREAD_FACTORY);
        Utils.exportResults(buildResultFile(task.getName(), taskId), result,
                task.getColumnSeparator(), task.getDecimalSeparator(), task.getLineSeparator(),
                task.getEncoding(), resources);
        System.out.println("Done: " + taskId);
    }

    private static void calculateTasks(List<File> tasks, ResourceBundle resources, int processors,
            boolean parallel) throws ExecutionException, InterruptedException, IOException {
        int tasksCount = tasks.size();
        if (parallel) {
            ExecutorService executor = Utils.newExecutor(THREAD_FACTORY);
            List<Future<?>> futures = new ArrayList<>(tasksCount);
            printInitialization(tasksCount, processors, true);
            for (File task : tasks) {
                futures.add(executor.submit(new CalculateFileAction(task, resources)));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } else {
            printInitialization(tasksCount, processors, false);
            for (File task : tasks) {
                calculateTask(task, buildResultFile(task), resources);
            }
        }
        System.out.println("Done all.");
    }

    private static Task buildTask(Task start, Task end, List<Double> shifts, int position,
            int size) {
        if (position == 0) {
            return start;
        }
        if (position == size - 1) {
            return end;
        }
        Task result = new Task();
        result.setId(position + 1);
        result.setName(start.getName());
        result.setStates(start.getStates());
        List<Transition> startTransitions = start.getTransitions();
        List<Transition> resultTransitions = new ArrayList<>(startTransitions.size());
        for (int i = 0; i < startTransitions.size(); i++) {
            Transition startTransition = startTransitions.get(i);
            Transition resultTransition = new Transition(startTransition.getSourceState(),
                    startTransition.getSourceCoefficient(), startTransition.getSourceDelay(),
                    startTransition.getOperandState(), startTransition.getOperandCoefficient(),
                    startTransition.getOperandDelay(), startTransition.getResultState(),
                    startTransition.getResultCoefficient(),
                    startTransition.getProbability() + position * shifts.get(i),
                    startTransition.getType(), startTransition.getMode(),
                    startTransition.getDescription());
            resultTransitions.add(resultTransition);
        }
        result.setTransitions(resultTransitions);
        result.setStartPoint(start.getStartPoint());
        result.setStepsCount(start.getStepsCount());
        result.setParallel(start.isParallel());
        result.setHigherAccuracy(start.isHigherAccuracy());
        result.setAllowNegative(start.isAllowNegative());
        result.setColumnSeparator(start.getColumnSeparator());
        result.setDecimalSeparator(start.getDecimalSeparator());
        result.setLineSeparator(start.getLineSeparator());
        result.setEncoding(start.getEncoding());
        return result;
    }

    private static List<Double> calculateShifts(Task start, Task end, int size) {
        List<Double> result = new ArrayList<>(start.getTransitions().size());
        List<Transition> startTransitions = start.getTransitions();
        List<Transition> endTransitions = end.getTransitions();
        for (int i = 0; i < startTransitions.size(); i++) {
            result.add((endTransitions.get(i).getProbability() -
                    startTransitions.get(i).getProbability()) / size);
        }
        return result;
    }

    private static void calculateTasks(File startFile, File endFile, int size,
            ResourceBundle resources, boolean parallel) throws Exception {
        Task startTask = TaskParser.parse(startFile);
        Task endTask = TaskParser.parse(endFile);
        if (startTask == null || endTask == null ||
                startTask.getTransitions().size() != endTask.getTransitions().size()) {
            System.out.println("Can't perform calculations. Invalid or missing data.");
            return;
        }
        startTask.setId(1);
        endTask.setId(size);
        endTask.setName(startTask.getName());
        List<Double> shifts = calculateShifts(startTask, endTask, size);
        if (parallel) {
            ExecutorService executor = Utils.newExecutor(THREAD_FACTORY);
            List<Future<?>> futures = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                futures.add(executor.submit(
                        new CalculateTaskAction(i, size, startTask, endTask, shifts, resources)));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } else {
            for (int i = 0; i < size; i++) {
                calculateTask(buildTask(startTask, endTask, shifts, i, size), resources);
            }
        }
        System.out.println("Done all.");
    }

    public static void main(String[] args) {
        try {
            System.out.println("Population [version " + Launcher.VERSION + "].");
            System.out.println("Copyright (C) 2016 Yuriy Budiyev [yuriy.budiyev@yandex.ru].");
            System.out.println("This program comes with ABSOLUTELY NO WARRANTY.");
            System.out.println("This is free software, and you are welcome to");
            System.out.println("redistribute it under certain conditions.");
            System.out.println("To get help, use \"-help\".");
            System.out.println("Initializing...");
            int processors = Runtime.getRuntime().availableProcessors();
            ResourceBundle resources = ResourceBundle
                    .getBundle("com.budiyev.population.resource.strings", Locale.getDefault());
            if (KEY_HELP.equalsIgnoreCase(args[0])) {
                printInitialization(0, processors, false);
                System.out.println("Usage:");
                System.out.println("-task task_file [result_file]");
                System.out.println("-tasks [-parallel] task_file1 ... task_fileN");
                System.out.println("-interval [-parallel] start_task end_task interval_count");
                System.out.println("License info:");
                System.out.println(
                        "This program is free software: you can redistribute it and/or modify");
                System.out.println(
                        "it under the terms of the GNU General Public License as published by");
                System.out.println(
                        "the Free Software Foundation, either version 3 of the License, or");
                System.out.println("any later version.");
                System.out
                        .println("This program is distributed in the hope that it will be useful,");
                System.out
                        .println("but WITHOUT ANY WARRANTY; without even the implied warranty of");
                System.out.println("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the");
                System.out.println("GNU General Public License for more details.");
                System.out.println(
                        "You should have received a copy of the GNU General Public License");
                System.out.println(
                        "along with this program. If not, see http://www.gnu.org/licenses/.");
            } else if (KEY_TASK.equalsIgnoreCase(args[0])) {
                File inputFile = new File(args[1]);
                File resultFile;
                if (args.length < 3) {
                    resultFile = buildResultFile(inputFile);
                } else {
                    resultFile = new File(args[2]);
                }
                printInitialization(1, processors, false);
                calculateTask(inputFile, resultFile, resources);
            } else if (KEY_TASKS.equalsIgnoreCase(args[0])) {
                String secondArgument = args[1];
                boolean parallel = Objects.equals(secondArgument, KEY_PARALLEL);
                int shift = parallel ? 2 : 1;
                List<File> tasks = new ArrayList<>(args.length - shift);
                for (int i = shift; i < args.length; i++) {
                    tasks.add(new File(args[i]));
                }
                calculateTasks(tasks, resources, processors, parallel);
            } else if (KEY_INTERVAL.equalsIgnoreCase(args[0])) {
                String secondArgument = args[1];
                boolean parallel = Objects.equals(secondArgument, KEY_PARALLEL);
                int shift = parallel ? 2 : 1;
                File startFile = new File(args[shift]);
                File endFile = new File(args[shift + 1]);
                int size = Integer.parseInt(args[shift + 2]);
                calculateTasks(startFile, endFile, size, resources, parallel);
            } else {
                System.out.println("Invalid arguments.");
            }
            System.exit(0);
        } catch (Throwable t) {
            System.out.println("Error");
            System.out.println(Utils.buildErrorText(t));
            System.exit(1);
        }
    }

    private static class CalculateFileAction implements Callable<Void> {
        private final File mInputFile;
        private final ResourceBundle mResources;

        public CalculateFileAction(File inputFile, ResourceBundle resources) {
            mInputFile = inputFile;
            mResources = resources;
        }

        @Override
        public Void call() throws Exception {
            calculateTask(mInputFile, buildResultFile(mInputFile), mResources);
            return null;
        }
    }

    private static class CalculateTaskAction implements Callable<Void> {
        private final int mPosition;
        private final int mSize;
        private final Task mStartTask;
        private final Task mEndTask;
        private final List<Double> mShifts;
        private final ResourceBundle mResources;

        private CalculateTaskAction(int position, int size, Task startTask, Task endTask,
                List<Double> shifts, ResourceBundle resources) {
            mPosition = position;
            mSize = size;
            mStartTask = startTask;
            mEndTask = endTask;
            mShifts = shifts;
            mResources = resources;
        }

        @Override
        public Void call() throws Exception {
            calculateTask(buildTask(mStartTask, mEndTask, mShifts, mPosition, mSize), mResources);
            return null;
        }
    }
}
