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
import com.budiyev.population.model.Result;
import com.budiyev.population.model.TableResult;
import com.budiyev.population.model.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;

public final class Utils {
    public static final String DECIMAL_FORMAT_COMMON =
            buildDecimalFormat(Calculator.HIGHER_ACCURACY_SCALE);

    public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
            (thread, throwable) -> {
                if (Launcher.isConsoleMode()) {
                    System.out.println("Error");
                    System.out.println(buildErrorText(throwable, Integer.MAX_VALUE));
                } else {
                    String errorText = buildErrorText(throwable, 10);
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, errorText, ButtonType.CLOSE);
                        alert.initStyle(StageStyle.UTILITY);
                        alert.setTitle("Error");
                        alert.setHeaderText("An unexpected error occurred");
                        alert.showAndWait();
                    });
                }
            };

    public static final ThreadFactory THREAD_FACTORY = runnable -> {
        Thread thread = new Thread(runnable, "Population background thread");
        thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
        if (!thread.isDaemon()) {
            thread.setDaemon(true);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    };

    private static ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool(THREAD_FACTORY);

    private Utils() {
    }

    public static String buildErrorText(Throwable throwable, int maxStackTraceSize) {
        for (; ; ) {
            Throwable cause = throwable.getCause();
            if (cause == null) {
                break;
            }
            throwable = cause;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(throwable.getClass().getSimpleName()).append(": ")
                .append(throwable.getLocalizedMessage()).append(System.lineSeparator())
                .append("Stack trace:").append(System.lineSeparator());
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        for (int i = 0; i < stackTrace.length && i < maxStackTraceSize; i++) {
            stringBuilder.append(stackTrace[i]);
            if (i == maxStackTraceSize - 1 && stackTrace.length > maxStackTraceSize) {
                stringBuilder.append("...");
            } else {
                stringBuilder.append(System.lineSeparator());
            }
        }
        return stringBuilder.toString();
    }

    public static Future<?> runAsync(Runnable runnable) {
        return ASYNC_EXECUTOR.submit(runnable);
    }

    public static <T> Future<T> callAsync(Callable<T> callable) {
        return ASYNC_EXECUTOR.submit(callable);
    }

    public static String createRepeatingString(char character, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, character);
        return new String(chars);
    }

    public static String buildDecimalFormat(int scale) {
        if (scale <= 0) {
            return "#";
        } else {
            return "#." + createRepeatingString('#', scale);
        }
    }

    public static boolean isNullOrEmpty(CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }

    public static <T> void refreshList(ObservableList<T> observableList) {
        ArrayList<T> temp = new ArrayList<>(observableList.size());
        temp.addAll(observableList);
        observableList.clear();
        observableList.addAll(temp);
        temp.clear();
    }

    public static String nullOrString(String value) {
        if (Objects.equals("null", value)) {
            return "";
        } else {
            return value;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void exportResults(ArrayList<Result> results, File file, char columnSeparator,
            char decimalSeparator, String lineSeparator, String encoding,
            ResourceBundle resources) throws IOException {
        if (results == null || file == null ||
            lineSeparator == null || encoding == null || resources == null) {
            return;
        }
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (Result result : results) {
            int startPoint = result.getStartPoint();
            int size = result.getTableData().size() + startPoint;
            if (end <= size) {
                end = size;
            }
            if (startPoint < start) {
                start = startPoint;
            }
        }
        String quote = "\"";
        String doubleQuote = "\"\"";
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        DecimalFormat formatter = new DecimalFormat(Utils.DECIMAL_FORMAT_COMMON);
        formatter.getDecimalFormatSymbols().setDecimalSeparator(decimalSeparator);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), encoding))) {
            writer.append(quote).append(resources.getString("step")).append(quote)
                    .append(columnSeparator);
            for (int i = 0; i < results.size(); i++) {
                ArrayList<String> headers = results.get(i).getDataNames();
                for (int j = 0; j < headers.size(); j++) {
                    writer.append(quote).append(headers.get(j).replace(quote, doubleQuote))
                            .append(quote);
                    if (i != results.size() - 1 || j != headers.size() - 1) {
                        writer.append(columnSeparator);
                    }
                }
            }
            writer.append(lineSeparator);
            for (int i = start; i < end; i++) {
                boolean empty = true;
                StringBuilder rowBuilder = new StringBuilder();
                TableResult firstExistent = null;
                for (int j = 0; j < results.size(); j++) {
                    Result result = results.get(j);
                    ArrayList<TableResult> data = result.getTableData();
                    int localIndex = i - result.getStartPoint();
                    if (localIndex >= 0 && localIndex < data.size()) {
                        TableResult localResult = data.get(localIndex);
                        for (int k = 0; k < localResult.valueCount(); k++) {
                            rowBuilder.append(quote)
                                    .append(formatter.format(localResult.getValue(k)))
                                    .append(quote);
                            if (j != results.size() - 1 || k != localResult.valueCount() - 1) {
                                rowBuilder.append(columnSeparator);
                            }
                        }
                        if (firstExistent == null) {
                            firstExistent = localResult;
                        }
                        empty = false;
                    } else {
                        int size = result.getDataNames().size();
                        for (int k = 0; k < size; k++) {
                            rowBuilder.append(quote).append("---").append(quote);
                            if (j != results.size() - 1 || k != size - 1) {
                                rowBuilder.append(columnSeparator);
                            }
                        }
                    }
                }
                if (!empty) {
                    writer.append(quote).append(String.valueOf(firstExistent.getNumber()))
                            .append(quote).append(columnSeparator).append(rowBuilder.toString())
                            .append(lineSeparator);
                }
            }
        }
    }

    public static void exportResults(File file, Result result, char columnSeparator,
            char decimalSeparator, String lineSeparator, String encoding,
            ResourceBundle resources) throws IOException {
        ArrayList<Result> resultList = new ArrayList<>(1);
        resultList.add(result);
        exportResults(resultList, file, columnSeparator, decimalSeparator, lineSeparator, encoding,
                resources);
    }

    public static void setDefaultTaskSettings(HashMap<String, String> taskSettings) {
        taskSettings.put(Task.Keys.STEPS_COUNT, String.valueOf(0));
        taskSettings.put(Task.Keys.HIGHER_ACCURACY, String.valueOf(false));
        taskSettings.put(Task.Keys.ALLOW_NEGATIVE, String.valueOf(false));
        taskSettings.put(Task.Keys.COLUMN_SEPARATOR, String.valueOf(','));
        taskSettings.put(Task.Keys.DECIMAL_SEPARATOR,
                String.valueOf(DecimalFormatSymbols.getInstance().getDecimalSeparator()));
        taskSettings.put(Task.Keys.LINE_SEPARATOR, System.lineSeparator());
        taskSettings.put(Task.Keys.ENCODING, Charset.defaultCharset().name());
    }
}
