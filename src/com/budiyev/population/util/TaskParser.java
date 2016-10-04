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

import com.budiyev.population.model.State;
import com.budiyev.population.model.Task;
import com.budiyev.population.model.Transition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import javafx.collections.FXCollections;

public final class TaskParser {
    private static final String FORMAT_NAME = "PopulationModelingTask";
    private static final int FORMAT_VERSION = 1;
    private static final String KEY_STATES_OPEN = "States";
    private static final String KEY_STATES_CLOSE = "//States";
    private static final String KEY_TRANSITIONS_OPEN = "Transitions";
    private static final String KEY_TRANSITIONS_CLOSE = "//Transitions";
    private static final char SEPARATOR = ',';

    private TaskParser() {
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void encode(File file, Task task) {
        StringTable table = new StringTable();
        table.add(new StringRow(FORMAT_NAME, FORMAT_VERSION));
        table.add(new StringRow(Task.Keys.START_POINT, task.getStartPoint()));
        table.add(new StringRow(Task.Keys.STEPS_COUNT, task.getStepsCount()));
        table.add(new StringRow(Task.Keys.PARALLEL, task.isParallel()));
        table.add(new StringRow(Task.Keys.HIGHER_ACCURACY, task.isHigherAccuracy()));
        table.add(new StringRow(Task.Keys.ALLOW_NEGATIVE, task.isAllowNegative()));
        table.add(new StringRow(Task.Keys.COLUMN_SEPARATOR, task.getColumnSeparator()));
        table.add(new StringRow(Task.Keys.DECIMAL_SEPARATOR, task.getDecimalSeparator()));
        table.add(new StringRow(Task.Keys.LINE_SEPARATOR, task.getLineSeparator()));
        table.add(new StringRow(Task.Keys.ENCODING, task.getEncoding()));
        table.add(new StringRow(KEY_STATES_OPEN));
        for (State state : task.getStates()) {
            table.add(new StringRow(state.getId(), state.getName(), state.getCount(),
                    state.getDescription()));
        }
        table.add(new StringRow(KEY_STATES_CLOSE));
        table.add(new StringRow(KEY_TRANSITIONS_OPEN));
        for (Transition transition : task.getTransitions()) {
            table.add(new StringRow(transition.getSourceState(), transition.getSourceCoefficient(),
                    transition.getSourceDelay(), transition.getOperandState(),
                    transition.getOperandCoefficient(), transition.getOperandDelay(),
                    transition.getResultState(), transition.getResultCoefficient(),
                    transition.getProbability(), transition.getType(), transition.getMode(),
                    transition.getDescription()));
        }
        table.add(new StringRow(KEY_TRANSITIONS_CLOSE));
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            CsvParser.encode(table, new FileOutputStream(file), SEPARATOR, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Task parse(File file) {
        StringTable table = null;
        try {
            table = CsvParser.parse(new FileInputStream(file), SEPARATOR, "UTF-8");
        } catch (FileNotFoundException ignored) {
        }
        if (table == null) {
            return null;
        }
        Task task = new Task();
        task.setName(file.getAbsolutePath());
        task.setStates(FXCollections.observableArrayList());
        task.setTransitions(FXCollections.observableArrayList());
        boolean readingStates = false;
        boolean readingTransitions = false;
        for (StringRow row : table) {
            if (Objects.equals(row.cell(0), KEY_STATES_OPEN)) {
                readingStates = true;
                continue;
            } else if (Objects.equals(row.cell(0), KEY_STATES_CLOSE)) {
                readingStates = false;
                continue;
            } else if (Objects.equals(row.cell(0), KEY_TRANSITIONS_OPEN)) {
                readingTransitions = true;
                continue;
            } else if (Objects.equals(row.cell(0), KEY_TRANSITIONS_CLOSE)) {
                readingTransitions = false;
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.START_POINT)) {
                task.setStartPoint(Integer.valueOf(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.STEPS_COUNT)) {
                task.setStepsCount(Integer.valueOf(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.PARALLEL)) {
                task.setParallel(Boolean.valueOf(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.HIGHER_ACCURACY)) {
                task.setHigherAccuracy(Boolean.valueOf(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.ALLOW_NEGATIVE)) {
                task.setAllowNegative(Boolean.valueOf(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.COLUMN_SEPARATOR)) {
                task.setColumnSeparator(row.cell(1).charAt(0));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.DECIMAL_SEPARATOR)) {
                task.setDecimalSeparator(row.cell(1).charAt(0));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.LINE_SEPARATOR)) {
                task.setLineSeparator(row.cell(1));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.ENCODING)) {
                task.setEncoding(row.cell(1));
                continue;
            }
            if (readingStates) {
                task.getStates().add(new State(Integer.valueOf(row.cell(0)), row.cell(1),
                        Double.valueOf(row.cell(2)), row.cell(3)));
            }
            if (readingTransitions) {
                task.getTransitions().add(new Transition(Integer.valueOf(row.cell(0)),
                        Double.valueOf(row.cell(1)), Integer.valueOf(row.cell(2)),
                        Integer.valueOf(row.cell(3)), Double.valueOf(row.cell(4)),
                        Integer.valueOf(row.cell(5)), Integer.valueOf(row.cell(6)),
                        Double.valueOf(row.cell(7)), Double.valueOf(row.cell(8)),
                        Integer.valueOf(row.cell(9)), Integer.valueOf(row.cell(10)), row.cell(11)));
            }
        }
        return task;
    }

}
