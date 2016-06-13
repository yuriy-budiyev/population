/**
 * Population
 * Copyright (C) 2016 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.budiyev.population.util;

import com.budiyev.population.Launcher;
import com.budiyev.population.model.Calculator;
import com.budiyev.population.model.State;
import com.budiyev.population.model.Transition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Console {
    private static final String KEY_TASKS = "-TASKS";
    private static final String KEY_INTERVAL = "-INTERVAL";
    private static final String KEY_PARALLEL = "-PARALLEL";

    private Console() {
    }

    private static File buildResultFile(File inputFile) {
        return new File(inputFile.getAbsolutePath() + ".result.csv");
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

    private static void calculateTask(File inputFile, File outputFile,
            ResourceBundle resources) throws Exception {
        System.out.println("Calculating: " + inputFile.getName());
        ArrayList<State> initialStates = new ArrayList<>();
        ArrayList<Transition> transitions = new ArrayList<>();
        HashMap<String, String> settings = new HashMap<>();
        TaskParser.parse(inputFile, initialStates, transitions, settings);
        int startPoint = Integer.valueOf(settings.get(TaskParser.Settings.START_POINT));
        int stepsCount = Integer.valueOf(settings.get(TaskParser.Settings.STEPS_COUNT));
        boolean higherAccuracy = Boolean.valueOf(settings.get(TaskParser.Settings.HIGHER_ACCURACY));
        boolean allowNegative = Boolean.valueOf(settings.get(TaskParser.Settings.ALLOW_NEGATIVE));
        boolean parallel = Boolean.valueOf(settings.get(TaskParser.Settings.PARALLEL));
        char columnSeparator = settings.get(TaskParser.Settings.COLUMN_SEPARATOR).charAt(0);
        char decimalSeparator = settings.get(TaskParser.Settings.DECIMAL_SEPARATOR).charAt(0);
        String lineSeparator = settings.get(TaskParser.Settings.LINE_SEPARATOR);
        String encoding = settings.get(TaskParser.Settings.ENCODING);
        Calculator.Results results = Calculator
                .calculateSync(initialStates, transitions, startPoint, stepsCount, higherAccuracy,
                        allowNegative, parallel, true, false);
        Utils.exportResults(outputFile, results, columnSeparator, decimalSeparator, lineSeparator,
                encoding, resources);
        System.out.println("Done: " + outputFile.getName());
    }

    private static void calculateTasks(File[] tasks, ResourceBundle resources, int processors,
            boolean parallel) throws Exception {
        if (parallel) {
            ExecutorService executor =
                    Executors.newFixedThreadPool(processors, Utils.THREAD_FACTORY);
            Future<?>[] futures = new Future<?>[tasks.length];
            printInitialization(tasks.length, processors, true);
            for (int i = 0; i < tasks.length; i++) {
                futures[i] = executor.submit(new CalculationAction(tasks[i], resources));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } else {
            printInitialization(tasks.length, processors, false);
            for (File task : tasks) {
                calculateTask(task, buildResultFile(task), resources);
            }
        }
        System.out.println("Done all.");
    }

    private static void calculateInterval(File startFile, File endFile, int size,
            ResourceBundle resources, int processors, boolean parallel) {

    }

    public static void launch(String[] args) {
        try {
            System.out.println("Population version " + Launcher.VERSION +
                               ", Copyright (C) 2016 Yuriy Budiyev [yuriy.budiyev@yandex.ru]." +
                               System.lineSeparator() +
                               "This program comes with ABSOLUTELY NO WARRANTY." +
                               System.lineSeparator() + "This is free software, and you are " +
                               "welcome to redistribute it under certain conditions.");
            System.out.println("Initializing...");
            int processors = Runtime.getRuntime().availableProcessors();
            ResourceBundle resources = ResourceBundle
                    .getBundle("com.budiyev.population.resource.strings", Locale.getDefault());
            String firstArgument = args[0].toUpperCase();
            String secondArgument = args[1].toUpperCase();
            if (Objects.equals(firstArgument, KEY_TASKS)) {
                boolean parallel = Objects.equals(secondArgument, KEY_PARALLEL);
                int shift = parallel ? 2 : 1;
                File[] tasks = new File[args.length - shift];
                for (int i = shift; i < args.length; i++) {
                    tasks[i - shift] = new File(args[i]);
                }
                calculateTasks(tasks, resources, processors, parallel);
            } else if (Objects.equals(firstArgument, KEY_INTERVAL)) {
                boolean parallel = Objects.equals(secondArgument, KEY_PARALLEL);
                int shift = parallel ? 2 : 1;
                File startFile = new File(args[shift]);
                File endFile = new File(args[shift + 1]);
                int size = Integer.valueOf(args[shift + 2]);
                calculateInterval(startFile, endFile, size, resources, processors, parallel);
            } else if (args.length == 2) {
                File inputFile = new File(args[0]);
                File outputFile = new File(args[1]);
                printInitialization(1, processors, false);
                calculateTask(inputFile, outputFile, resources);
            } else {
                System.out.println("Invalid arguments.");
            }
            System.exit(0);
        } catch (Throwable t) {
            System.out.println("Error");
            System.out.println(Utils.buildErrorText(t, Integer.MAX_VALUE));
            System.exit(1);
        }
    }

    private static class CalculationAction implements Callable<Void> {
        private final File mInputFile;
        private final ResourceBundle mResources;

        public CalculationAction(File inputFile, ResourceBundle resources) {
            mInputFile = inputFile;
            mResources = resources;
        }

        @Override
        public Void call() throws Exception {
            calculateTask(mInputFile, buildResultFile(mInputFile), mResources);
            return null;
        }
    }

    private static class Task {
        public final List<State> initialStates;
        public final List<Transition> transitions;
        public final int startPoint;
        public final int stepsCount;
        public final boolean allowNegative;
        public final boolean higherAccuracy;
        public final boolean parallel;
        public final char columnSeparator;
        public final char decimalSeparator;
        public final String lineSeparator;
        public final String encoding;

        public Task(File file) {
            initialStates = new ArrayList<>();
            transitions = new ArrayList<>();
            HashMap<String, String> settings = new HashMap<>();
            TaskParser.parse(file, initialStates, transitions, settings);
            startPoint = Integer.valueOf(settings.get(TaskParser.Settings.START_POINT));
            stepsCount = Integer.valueOf(settings.get(TaskParser.Settings.STEPS_COUNT));
            higherAccuracy = Boolean.valueOf(settings.get(TaskParser.Settings.HIGHER_ACCURACY));
            allowNegative = Boolean.valueOf(settings.get(TaskParser.Settings.ALLOW_NEGATIVE));
            parallel = Boolean.valueOf(settings.get(TaskParser.Settings.PARALLEL));
            columnSeparator = settings.get(TaskParser.Settings.COLUMN_SEPARATOR).charAt(0);
            decimalSeparator = settings.get(TaskParser.Settings.DECIMAL_SEPARATOR).charAt(0);
            lineSeparator = settings.get(TaskParser.Settings.LINE_SEPARATOR);
            encoding = settings.get(TaskParser.Settings.ENCODING);
        }

        public Task(Task task, double probabilityShift) {
            initialStates = task.initialStates;
            transitions = new ArrayList<>(task.transitions.size());
            for (Transition transition : task.transitions) {
                Transition shifted = new Transition(transition, probabilityShift);
                transitions.add(shifted);
            }
            startPoint = task.startPoint;
            stepsCount = task.stepsCount;
            allowNegative = task.allowNegative;
            higherAccuracy = task.higherAccuracy;
            parallel = task.parallel;
            columnSeparator = task.columnSeparator;
            decimalSeparator = task.decimalSeparator;
            lineSeparator = task.lineSeparator;
            encoding = task.encoding;
        }
    }
}
