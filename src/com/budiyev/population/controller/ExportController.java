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
package com.budiyev.population.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Objects;

import com.budiyev.population.controller.base.AbstractExportController;
import com.budiyev.population.model.Result;
import com.budiyev.population.model.Task;
import com.budiyev.population.util.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import javafx.util.StringConverter;

public class ExportController extends AbstractExportController {
    private ObservableList<Pair<Character, String>> mColumnSeparators = FXCollections.observableArrayList();
    private ObservableList<Pair<Character, String>> mDecimalSeparators = FXCollections.observableArrayList();
    private ObservableList<Pair<String, String>> mLineSeparators = FXCollections.observableArrayList();
    private ObservableList<String> mEncodings = FXCollections.observableArrayList();
    public ChoiceBox<Pair<Character, String>> mColumnSeparatorChoiceBox;
    public ChoiceBox<Pair<Character, String>> mDecimalSeparatorChoiceBox;
    public ChoiceBox<Pair<String, String>> mLineSeparatorChoiceBox;
    public ChoiceBox<String> mEncodingChoiceBox;
    public Button mSaveButton;

    private FileChooser getResultsFileChooser() {
        FileChooser fileChooser = new FileChooser();
        String workDirectory = getApplication().getWorkDirectory();
        if (workDirectory != null) {
            File file = new File(workDirectory);
            if (file.exists()) {
                fileChooser.setInitialDirectory(file);
            }
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(getString("csv_format"), "*.csv"));
        fileChooser.setTitle(getString("save_results"));
        return fileChooser;
    }

    private void readSelectionFromSettings() {
        char separator = getTaskSettings().get(Task.Keys.COLUMN_SEPARATOR).charAt(0);
        int selected = 0;
        for (int i = 0; i < mColumnSeparators.size(); i++) {
            if (Objects.equals(mColumnSeparators.get(i).getKey(), separator)) {
                selected = i;
                break;
            }
        }
        mColumnSeparatorChoiceBox.getSelectionModel().select(selected);
        separator = getTaskSettings().get(Task.Keys.DECIMAL_SEPARATOR).charAt(0);
        for (int i = 0; i < mDecimalSeparators.size(); i++) {
            if (Objects.equals(mDecimalSeparators.get(i).getKey(), separator)) {
                selected = i;
                break;
            }
        }
        mDecimalSeparatorChoiceBox.getSelectionModel().select(selected);
        String lineSeparator = getTaskSettings().get(Task.Keys.LINE_SEPARATOR);
        selected = 1;
        for (int i = 0; i < mLineSeparators.size(); i++) {
            if (Objects.equals(mLineSeparators.get(i).getKey(), lineSeparator)) {
                selected = i;
                break;
            }
        }
        mLineSeparatorChoiceBox.getSelectionModel().select(selected);
        selected = 0;
        String encoding = getTaskSettings().get(Task.Keys.ENCODING);
        for (int i = 0; i < mEncodings.size(); i++) {
            if (Objects.equals(mEncodings.get(i), encoding)) {
                selected = i;
                break;
            }
        }
        mEncodingChoiceBox.getSelectionModel().select(selected);
    }

    @Override
    public void initialize() {
        mColumnSeparators.add(new Pair<>(',', getString("export_comma")));
        mColumnSeparators.add(new Pair<>(';', getString("export_semicolon")));
        mColumnSeparators.add(new Pair<>('\t', getString("export_tabulation")));
        mDecimalSeparators.add(new Pair<>('.', getString("export_dot")));
        mDecimalSeparators.add(new Pair<>(',', getString("export_comma")));
        mLineSeparators.add(new Pair<>("\r", getString("export_carriage_return")));
        mLineSeparators.add(new Pair<>("\n", getString("export_line_feed")));
        mLineSeparators.add(new Pair<>("\r\n", getString("export_line_crlf")));
        Charset.availableCharsets().forEach((string, charset) -> {
            if (!string.startsWith("x-") && !string.startsWith("X-")) {
                mEncodings.add(string);
            }
        });
        StringConverter<Pair<Character, String>> converter = new StringConverter<Pair<Character, String>>() {
            @Override
            public String toString(Pair<Character, String> object) {
                return object.getValue();
            }

            @Override
            public Pair<Character, String> fromString(String string) {
                return null;
            }
        };
        mColumnSeparatorChoiceBox.setConverter(converter);
        mDecimalSeparatorChoiceBox.setConverter(converter);
        mLineSeparatorChoiceBox.setConverter(new StringConverter<Pair<String, String>>() {
            @Override
            public String toString(Pair<String, String> object) {
                return object.getValue();
            }

            @Override
            public Pair<String, String> fromString(String string) {
                return null;
            }
        });
        mColumnSeparatorChoiceBox.setItems(mColumnSeparators);
        mDecimalSeparatorChoiceBox.setItems(mDecimalSeparators);
        mLineSeparatorChoiceBox.setItems(mLineSeparators);
        mEncodingChoiceBox.setItems(mEncodings);
        readSelectionFromSettings();
        mColumnSeparatorChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> getTaskSettings()
                .put(Task.Keys.COLUMN_SEPARATOR, String.valueOf(newValue.getKey())));
        mDecimalSeparatorChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> getTaskSettings()
                .put(Task.Keys.DECIMAL_SEPARATOR, String.valueOf(newValue.getKey())));
        mLineSeparatorChoiceBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> getTaskSettings().put(Task.Keys.LINE_SEPARATOR, newValue.getKey()));
        mEncodingChoiceBox.valueProperty()
                .addListener((observable, oldValue, newValue) -> getTaskSettings().put(Task.Keys.ENCODING, newValue));
        ArrayList<Result> results = getResults();
        mSaveButton.setDisable(results == null || results.size() == 0);
    }

    public void restoreDefaults() {
        Utils.setDefaultTaskSettings(getTaskSettings());
        readSelectionFromSettings();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void save() {
        try {
            File file = getResultsFileChooser().showSaveDialog(getStage().getOwner());
            if (file == null) {
                return;
            }
            String path = file.getPath();
            int dotIndex = path.indexOf('.');
            if (dotIndex < 0) {
                path += ".csv";
                file = new File(path);
            }
            Utils.exportResults(getResults(), file,
                    mColumnSeparatorChoiceBox.getSelectionModel().getSelectedItem().getKey(),
                    mDecimalSeparatorChoiceBox.getSelectionModel().getSelectedItem().getKey(),
                    mLineSeparatorChoiceBox.getSelectionModel().getSelectedItem().getKey(),
                    mEncodingChoiceBox.getSelectionModel().getSelectedItem(), getResources());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        close();
    }

    public void close() {
        getStage().close();
    }
}
