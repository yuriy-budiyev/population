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
package com.budiyev.population.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.budiyev.population.model.State;
import com.budiyev.population.model.Task;
import com.budiyev.population.model.Transition;
import com.budiyev.population.model.TransitionMode;
import com.budiyev.population.model.TransitionType;

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
            table.add(new StringRow(state.getId(), state.getName(), state.getCount(), state.getDescription()));
        }
        table.add(new StringRow(KEY_STATES_CLOSE));
        table.add(new StringRow(KEY_TRANSITIONS_OPEN));
        for (Transition transition : task.getTransitions()) {
            table.add(new StringRow(transition.getSourceState(), transition.getSourceCoefficient(),
                    transition.getSourceDelay(), transition.getOperandState(), transition.getOperandCoefficient(),
                    transition.getOperandDelay(), transition.getResultState(), transition.getResultCoefficient(),
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
        task.setStates(new ArrayList<>());
        task.setTransitions(new ArrayList<>());
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
                task.setStartPoint(Integer.parseInt(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.STEPS_COUNT)) {
                task.setStepsCount(Integer.parseInt(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.PARALLEL)) {
                task.setParallel(Boolean.parseBoolean(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.HIGHER_ACCURACY)) {
                task.setHigherAccuracy(Boolean.parseBoolean(row.cell(1)));
                continue;
            } else if (Objects.equals(row.cell(0), Task.Keys.ALLOW_NEGATIVE)) {
                task.setAllowNegative(Boolean.parseBoolean(row.cell(1)));
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
                task.getStates()
                        .add(new State(Integer.parseInt(row.cell(0)), row.cell(1), Double.parseDouble(row.cell(2)),
                                row.cell(3)));
            }
            if (readingTransitions) {
                task.getTransitions().add(new Transition(Integer.parseInt(row.cell(0)), Double.parseDouble(row.cell(1)),
                        Integer.parseInt(row.cell(2)), Integer.parseInt(row.cell(3)), Double.parseDouble(row.cell(4)),
                        Integer.parseInt(row.cell(5)), Integer.parseInt(row.cell(6)), Double.parseDouble(row.cell(7)),
                        Double.parseDouble(row.cell(8)), Integer.parseInt(row.cell(9)), Integer.parseInt(row.cell(10)),
                        row.cell(11)));
            }
        }
        return task;
    }

    public static Task parseLegacy(File file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "windows-1251"))) {
            Task task = new Task();
            task.setName(file.getAbsolutePath());
            int statesCount = Integer.parseInt(reader.readLine());
            List<State> states = new ArrayList<>(statesCount);
            task.setStates(states);
            for (int i = 0; i < statesCount; i++) {
                State state = new State();
                state.setName(reader.readLine());
                state.setCount(Double.parseDouble(reader.readLine().replace(',', '.')));
                states.add(state);
            }
            int transitionsCount = Integer.parseInt(reader.readLine()) - 1;
            List<Transition> transitions = new ArrayList<>(transitionsCount);
            task.setTransitions(transitions);
            for (int i = 0; i < transitionsCount; i++) {
                Transition transition = new Transition();
                transition.setSourceState(findState(reader.readLine(), states));
                transition.setOperandState(findState(reader.readLine(), states));
                transition.setResultState(findState(reader.readLine(), states));
                double intensity = Double.parseDouble(reader.readLine().replace(',', '.'));
                if (intensity > 1) {
                    transition.setProbability(1);
                    transition.setResultCoefficient(intensity);
                } else {
                    transition.setProbability(intensity);
                }
                String typeAndModeString = reader.readLine();
                if (typeAndModeString.trim().length() == 0) {
                    transition.setType(TransitionType.LINEAR);
                    transition.setMode(TransitionMode.SIMPLE);
                } else {
                    int typeAndMode = Integer.parseInt(typeAndModeString);
                    transition.setType(getTransitionType(typeAndMode));
                    transition.setMode(getTransitionMode(typeAndMode));
                }
                transition.setSourceDelay(getDelay(reader.readLine()));
                transition.setOperandDelay(getDelay(reader.readLine()));
                transitions.add(transition);
            }
            return task;
        } catch (IOException e) {
            return null;
        }
    }

    private static int findState(String name, List<State> states) {
        if ("*".equals(name)) {
            return State.EXTERNAL;
        }
        for (State state : states) {
            if (Objects.equals(name, state.getName())) {
                return state.getId();
            }
        }
        return State.UNDEFINED;
    }

    private static int getTransitionMode(int typeAndMode) {
        switch (typeAndMode / 3) {
            case 1: {
                return TransitionMode.RETAINING;
            }
            case 2: {
                return TransitionMode.REMOVING;
            }
            case 3: {
                return TransitionMode.RESIDUAL;
            }
            case 4: {
                return TransitionMode.INHIBITOR;
            }
            default: {
                return TransitionMode.SIMPLE;
            }
        }
    }

    private static int getTransitionType(int typeAndMode) {
        switch (typeAndMode) {
            case 1:
            case 4:
            case 7:
            case 10:
            case 13: {
                return TransitionType.SOLUTE;
            }
            case 2:
            case 5:
            case 8:
            case 11:
            case 14: {
                return TransitionType.BLEND;
            }
            default: {
                return TransitionType.LINEAR;
            }
        }
    }

    private static int getDelay(String string) {
        if (string.trim().length() != 0) {
            return Integer.parseInt(string);
        } else {
            return 0;
        }
    }
}
