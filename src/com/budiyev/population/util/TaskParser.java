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

import com.budiyev.population.model.State;
import com.budiyev.population.model.Transition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

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
    public static void encode(File file, Collection<State> states,
            Collection<Transition> transitions, HashMap<String, String> settings) {
        CsvParser.Table table = new CsvParser.Table();
        table.add(new CsvParser.Row(FORMAT_NAME, FORMAT_VERSION));
        settings.forEach((key, value) -> table.add(new CsvParser.Row(key, value)));
        table.add(new CsvParser.Row(KEY_STATES_OPEN));
        for (State state : states) {
            table.add(new CsvParser.Row(state.getId(), state.getName(), state.getCount(),
                    state.getDescription()));
        }
        table.add(new CsvParser.Row(KEY_STATES_CLOSE));
        table.add(new CsvParser.Row(KEY_TRANSITIONS_OPEN));
        for (Transition transition : transitions) {
            table.add(new CsvParser.Row(transition.getSourceState(),
                    transition.getSourceCoefficient(), transition.getSourceDelay(),
                    transition.getOperandState(), transition.getOperandCoefficient(),
                    transition.getOperandDelay(), transition.getResultState(),
                    transition.getResultCoefficient(), transition.getProbability(),
                    transition.getType(), transition.getMode(), transition.getDescription()));
        }
        table.add(new CsvParser.Row(KEY_TRANSITIONS_CLOSE));
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            CsvParser.encode(table, new FileOutputStream(file), SEPARATOR, "UTF-8");
        } catch (IOException ignored) {
        }
    }

    public static void parse(File file, Collection<State> states,
            Collection<Transition> transitions, HashMap<String, String> settings) {
        CsvParser.Table table = null;
        try {
            table = CsvParser.parse(new FileInputStream(file), SEPARATOR, "UTF-8");
        } catch (FileNotFoundException ignored) {
        }
        if (table == null) {
            return;
        }
        boolean readingStates = false;
        boolean readingTransitions = false;
        for (CsvParser.Row row : table) {
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
            } else if (Objects.equals(row.cell(0), Settings.START_POINT)) {
                settings.put(Settings.START_POINT, row.cell(1));
                continue;
            } else if (Objects.equals(row.cell(0), Settings.STEPS_COUNT)) {
                settings.put(Settings.STEPS_COUNT, row.cell(1));
                continue;
            } else if (Objects.equals(row.cell(0), Settings.HIGHER_ACCURACY)) {
                settings.put(Settings.HIGHER_ACCURACY, row.cell(1));
                continue;
            } else if (Objects.equals(row.cell(0), Settings.ALLOW_NEGATIVE)) {
                settings.put(Settings.ALLOW_NEGATIVE, row.cell(1));
                continue;
            } else if (Objects.equals(row.cell(0), Settings.COLUMN_SEPARATOR)) {
                settings.put(Settings.COLUMN_SEPARATOR, row.cell(1));
                continue;
            } else if (Objects.equals(row.cell(0), Settings.DECIMAL_SEPARATOR)) {
                settings.put(Settings.DECIMAL_SEPARATOR, row.cell(1));
                continue;
            } else if (Objects.equals(row.cell(0), Settings.LINE_SEPARATOR)) {
                settings.put(Settings.LINE_SEPARATOR, row.cell(1));
                continue;
            } else if (Objects.equals(row.cell(0), Settings.ENCODING)) {
                settings.put(Settings.ENCODING, row.cell(1));
                continue;
            }
            if (readingStates) {
                states.add(new State(Integer.valueOf(row.cell(0)), row.cell(1),
                        Double.valueOf(row.cell(2)), row.cell(3)));
            }
            if (readingTransitions) {
                transitions.add(new Transition(Integer.valueOf(row.cell(0)),
                        Double.valueOf(row.cell(1)), Integer.valueOf(row.cell(2)),
                        Integer.valueOf(row.cell(3)), Double.valueOf(row.cell(4)),
                        Integer.valueOf(row.cell(5)), Integer.valueOf(row.cell(6)),
                        Double.valueOf(row.cell(7)), Double.valueOf(row.cell(8)),
                        Integer.valueOf(row.cell(9)), Integer.valueOf(row.cell(10)), row.cell(11)));
            }
        }
    }

    public static final class Settings {
        public static final String START_POINT = "StartPoint";
        public static final String STEPS_COUNT = "StepsCount";
        public static final String HIGHER_ACCURACY = "HigherAccuracy";
        public static final String ALLOW_NEGATIVE = "AllowNegative";
        public static final String COLUMN_SEPARATOR = "ColumnSeparator";
        public static final String DECIMAL_SEPARATOR = "DecimalSeparator";
        public static final String LINE_SEPARATOR = "LineSeparator";
        public static final String ENCODING = "Encoding";

        private Settings() {
        }
    }
}
