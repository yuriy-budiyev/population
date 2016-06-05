/**
 * Population
 * Copyright (C) 2016  Yuriy Budiyev [yuriy.budiyev@yandex.ru]
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

import com.budiyev.population.model.Calculator;

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

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;

public final class Utils {
    public static final String DECIMAL_FORMAT_COMMON =
            "#." + createRepeatingString('#', Calculator.SCALE);
    public static final String DECIMAL_FORMAT_RESULTS_TABLE = "#.#####";

    public static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
            (thread, throwable) -> {
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
                        .append(System.lineSeparator()).append("Stack trace:")
                        .append(System.lineSeparator());
                StackTraceElement[] stackTrace = throwable.getStackTrace();
                for (int i = 0; i < stackTrace.length && i < 10; i++) {
                    stringBuilder.append(stackTrace[i]);
                    if (i == 9) {
                        stringBuilder.append("...");
                    } else {
                        stringBuilder.append(System.lineSeparator());
                    }
                }
                String contentText = stringBuilder.toString();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, contentText, ButtonType.CLOSE);
                    alert.initStyle(StageStyle.UTILITY);
                    alert.setTitle("Error");
                    alert.setHeaderText("An unexpected error occurred");
                    alert.showAndWait();
                });
            };

    private Utils() {
    }

    public static String createRepeatingString(char character, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, character);
        return new String(chars);
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
    public static void exportResults(ArrayList<Calculator.Results> results, File file,
            char columnSeparator, char decimalSeparator, String lineSeparator, String encoding,
            ResourceBundle resources) throws IOException {
        if (results == null || file == null ||
            lineSeparator == null || encoding == null || resources == null) {
            return;
        }
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (Calculator.Results result : results) {
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
                Calculator.Result firstExistent = null;
                for (int j = 0; j < results.size(); j++) {
                    Calculator.Results result = results.get(j);
                    ArrayList<Calculator.Result> data = result.getTableData();
                    int localIndex = i - result.getStartPoint();
                    if (localIndex >= 0 && localIndex < data.size()) {
                        Calculator.Result localResult = data.get(localIndex);
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

    public static void exportResults(File file, Calculator.Results results, char columnSeparator,
            char decimalSeparator, String lineSeparator, String encoding,
            ResourceBundle resources) throws IOException {
        ArrayList<Calculator.Results> resultsList = new ArrayList<>(1);
        resultsList.add(results);
        exportResults(resultsList, file, columnSeparator, decimalSeparator, lineSeparator, encoding,
                resources);
    }

    public static void setDefaultTaskSettings(HashMap<String, String> taskSettings) {
        taskSettings.put(TaskParser.Settings.STEPS_COUNT, String.valueOf(0));
        taskSettings.put(TaskParser.Settings.HIGHER_ACCURACY, String.valueOf(false));
        taskSettings.put(TaskParser.Settings.ALLOW_NEGATIVE, String.valueOf(false));
        taskSettings.put(TaskParser.Settings.COLUMN_SEPARATOR, String.valueOf(','));
        taskSettings.put(TaskParser.Settings.DECIMAL_SEPARATOR,
                String.valueOf(DecimalFormatSymbols.getInstance().getDecimalSeparator()));
        taskSettings.put(TaskParser.Settings.LINE_SEPARATOR, System.lineSeparator());
        taskSettings.put(TaskParser.Settings.ENCODING, Charset.defaultCharset().name());
    }
}
