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

import com.budiyev.population.controller.base.AbstractAboutController;
import com.budiyev.population.controller.base.AbstractController;
import com.budiyev.population.controller.base.AbstractExportController;
import com.budiyev.population.model.Result;
import com.budiyev.population.util.CsvParser;
import com.budiyev.population.util.PopulationThreadFactory;
import com.budiyev.population.util.StringRow;
import com.budiyev.population.util.StringTable;
import com.budiyev.population.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class PopulationApplication extends Application {
    private static final double PRIMARY_STAGE_MIN_WIDTH = 800;
    private static final double PRIMARY_STAGE_MIN_HEIGHT = 480;
    private static final double WINDOW_OFFSET = 40;
    private final Map<String, String> mSettings = new HashMap<>();
    private Stage mPrimaryStage;
    private ResourceBundle mResources;

    private final Thread.UncaughtExceptionHandler mUncaughtExceptionHandler =
            (thread, throwable) -> {
                throwable.printStackTrace();
                ResourceBundle resources = getResources();
                showAlert(resources.getString("alert_error"),
                        resources.getString("alert_unexpected_error"),
                        Utils.buildErrorText(throwable, 10, resources.getString("stack_trace")),
                        Alert.AlertType.ERROR);
            };

    private final ThreadFactory mThreadFactory =
            new PopulationThreadFactory(mUncaughtExceptionHandler);

    private void loadSettings() {
        File settingsFile = new File(System.getProperty("user.home"), Settings.FILE);
        if (!settingsFile.exists()) {
            return;
        }
        StringTable settingsTable;
        try {
            settingsTable = CsvParser.parse(new FileInputStream(settingsFile), ',', "UTF-8");
        } catch (FileNotFoundException e) {
            return;
        }
        if (settingsTable == null) {
            return;
        }
        try {
            for (StringRow row : settingsTable) {
                if (Objects.equals(row.cell(0), Settings.WORK_DIRECTORY)) {
                    mSettings.put(Settings.WORK_DIRECTORY, emptyIfNullString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_X)) {
                    mSettings.put(Settings.PRIMARY_STAGE_X, emptyIfNullString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_Y)) {
                    mSettings.put(Settings.PRIMARY_STAGE_Y, emptyIfNullString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_WIDTH)) {
                    mSettings.put(Settings.PRIMARY_STAGE_WIDTH, emptyIfNullString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_HEIGHT)) {
                    mSettings.put(Settings.PRIMARY_STAGE_HEIGHT, emptyIfNullString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_MAXIMIZED)) {
                    mSettings.put(Settings.PRIMARY_STAGE_MAXIMIZED, emptyIfNullString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.LOCALE)) {
                    mSettings.put(Settings.LOCALE, emptyIfNullString(row.cell(1)));
                }
            }
        } catch (Exception e) {
            mSettings.clear();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveSettings() {
        File settingsFile = new File(System.getProperty("user.home"), Settings.FILE);
        if (settingsFile.exists()) {
            settingsFile.delete();
        }
        StringTable settingsTable = new StringTable();
        settingsTable.add(Settings.LOCALE, mSettings.get(Settings.LOCALE));
        settingsTable.add(Settings.WORK_DIRECTORY, mSettings.get(Settings.WORK_DIRECTORY));
        settingsTable.add(Settings.PRIMARY_STAGE_X, mPrimaryStage.getX());
        settingsTable.add(Settings.PRIMARY_STAGE_Y, mPrimaryStage.getY());
        settingsTable.add(Settings.PRIMARY_STAGE_WIDTH, mPrimaryStage.getWidth());
        settingsTable.add(Settings.PRIMARY_STAGE_HEIGHT, mPrimaryStage.getHeight());
        settingsTable.add(Settings.PRIMARY_STAGE_MAXIMIZED, mPrimaryStage.isMaximized());
        try {
            settingsFile.createNewFile();
            CsvParser.encode(settingsTable, new FileOutputStream(settingsFile), ',', "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeSettings() {
        mSettings.putIfAbsent(Settings.WORK_DIRECTORY, System.getProperty("user.home"));
        mSettings.putIfAbsent(Settings.LOCALE, Locale.getDefault().getLanguage());
    }

    private void initializeResources() {
        mResources = ResourceBundle.getBundle("com.budiyev.population.resource.strings",
                Locale.forLanguageTag(mSettings.get(Settings.LOCALE)));
    }

    private void showPrimaryStage(Stage primaryStage) {
        mPrimaryStage = primaryStage;
        mPrimaryStage.setTitle(mResources.getString("application_name"));
        mPrimaryStage.setMinWidth(PRIMARY_STAGE_MIN_WIDTH);
        mPrimaryStage.setMinHeight(PRIMARY_STAGE_MIN_HEIGHT);
        mPrimaryStage.getIcons()
                .add(new Image(getClass().getResourceAsStream("resource/icon.png")));
        FXMLLoader sceneLoader =
                new FXMLLoader(getClass().getResource("resource/view/PrimaryView.fxml"),
                        mResources);
        sceneLoader.setControllerFactory(controllerClass -> {
            try {
                Object controller = controllerClass.newInstance();
                if (controller instanceof AbstractController) {
                    AbstractController abstractController = (AbstractController) controller;
                    abstractController.setApplication(PopulationApplication.this);
                    abstractController.setStage(mPrimaryStage);
                }
                return controller;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        String setting = mSettings.get(Settings.PRIMARY_STAGE_X);
        if (setting != null) {
            double value = Double.parseDouble(setting);
            if (value > 0) {
                mPrimaryStage.setX(value);
            }
        }
        setting = mSettings.get(Settings.PRIMARY_STAGE_Y);
        if (setting != null) {
            double value = Double.parseDouble(setting);
            if (value > 0) {
                mPrimaryStage.setY(value);
            }
        }
        setting = mSettings.get(Settings.PRIMARY_STAGE_WIDTH);
        if (setting != null) {
            double value = Double.parseDouble(setting);
            if (value > PRIMARY_STAGE_MIN_WIDTH) {
                mPrimaryStage.setWidth(value);
            }
        }
        setting = mSettings.get(Settings.PRIMARY_STAGE_HEIGHT);
        if (setting != null) {
            double value = Double.parseDouble(setting);
            if (value > PRIMARY_STAGE_MIN_HEIGHT) {
                mPrimaryStage.setHeight(value);
            }
        }
        setting = mSettings.get(Settings.PRIMARY_STAGE_MAXIMIZED);
        if (setting != null) {
            mPrimaryStage.setMaximized(Boolean.parseBoolean(setting));
        }
        try {
            Scene primaryScene = new Scene(sceneLoader.load(), 1, 1);
            primaryScene.getStylesheets().add("com/budiyev/population/resource/style/primary.css");
            mPrimaryStage.setScene(primaryScene);
            mPrimaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.currentThread().setUncaughtExceptionHandler(mUncaughtExceptionHandler);
        loadSettings();
        initializeSettings();
        initializeResources();
        showPrimaryStage(primaryStage);
    }

    @Override
    public void stop() throws Exception {
        saveSettings();
    }

    public void showExportDialog(ArrayList<Result> results, HashMap<String, String> taskSettings) {
        Stage exportStage = new Stage(StageStyle.UTILITY);
        exportStage.initModality(Modality.APPLICATION_MODAL);
        Stage primaryStage = mPrimaryStage;
        exportStage.initOwner(primaryStage.getOwner());
        exportStage.setResizable(false);
        exportStage.setTitle(mResources.getString("export"));
        FXMLLoader sceneLoader =
                new FXMLLoader(getClass().getResource("resource/view/ExportView.fxml"));
        sceneLoader.setResources(mResources);
        sceneLoader.setControllerFactory(controllerClass -> {
            try {
                Object controller = controllerClass.newInstance();
                if (controller instanceof AbstractExportController) {
                    AbstractExportController exportController =
                            (AbstractExportController) controller;
                    exportController.setApplication(PopulationApplication.this);
                    exportController.setStage(exportStage);
                    exportController.setResults(results);
                    exportController.setTaskSettings(taskSettings);
                }
                return controller;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            Scene exportScene = new Scene(sceneLoader.load(), -1, -1);
            exportScene.getStylesheets().add("com/budiyev/population/resource/style/export.css");
            exportStage.setScene(exportScene);
            exportStage.setX(primaryStage.getX() + WINDOW_OFFSET);
            exportStage.setY(primaryStage.getY() + WINDOW_OFFSET);
            exportStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showAboutDialog() {
        Stage aboutStage = new Stage(StageStyle.UTILITY);
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        Stage primaryStage = mPrimaryStage;
        aboutStage.initOwner(primaryStage.getOwner());
        aboutStage.setResizable(false);
        aboutStage.setTitle(mResources.getString("about"));
        FXMLLoader sceneLoader =
                new FXMLLoader(getClass().getResource("resource/view/AboutView.fxml"));
        sceneLoader.setResources(mResources);
        sceneLoader.setControllerFactory(controllerClass -> {
            try {
                Object controller = controllerClass.newInstance();
                if (controller instanceof AbstractAboutController) {
                    AbstractAboutController aboutController = (AbstractAboutController) controller;
                    aboutController.setApplication(PopulationApplication.this);
                    aboutController.setStage(aboutStage);
                    aboutController.setImage(
                            new Image(getClass().getResourceAsStream("resource/icon.png")));
                }
                return controller;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            Scene aboutScene = new Scene(sceneLoader.load(), -1, -1);
            aboutScene.getStylesheets().add("com/budiyev/population/resource/style/about.css");
            aboutStage.setScene(aboutScene);
            aboutStage.setX(primaryStage.getX() + WINDOW_OFFSET);
            aboutStage.setY(primaryStage.getY() + WINDOW_OFFSET);
            aboutStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getWorkDirectory() {
        return mSettings.get(Settings.WORK_DIRECTORY);
    }

    public void setWorkDirectory(String workDirectory) {
        mSettings.put(Settings.WORK_DIRECTORY, workDirectory);
    }

    public Stage getPrimaryStage() {
        return mPrimaryStage;
    }

    public ResourceBundle getResources() {
        return mResources;
    }

    public ThreadFactory getThreadFactory() {
        return mThreadFactory;
    }

    public void showAlert(String title, String header, String content, Alert.AlertType type) {
        Stage primaryStage = mPrimaryStage;
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.getDialogPane().getStylesheets()
                    .add("com/budiyev/population/resource/style/alert.css");
            alert.setX(primaryStage.getX() + WINDOW_OFFSET);
            alert.setY(primaryStage.getY() + WINDOW_OFFSET);
            alert.initStyle(StageStyle.UTILITY);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.getButtonTypes().clear();
            alert.getButtonTypes().add(new ButtonType(getResources().getString("alert_close"),
                    ButtonBar.ButtonData.OK_DONE));
            alert.showAndWait();
        });
    }

    public boolean selectLanguage(String langTag) {
        return !Objects.equals(mSettings.put(Settings.LOCALE, langTag), langTag);
    }

    public static void main(String[] args) {
        launch(PopulationApplication.class, args);
    }

    private static String emptyIfNullString(String value) {
        if ("null".equalsIgnoreCase(value)) {
            return "";
        } else {
            return value;
        }
    }

    private static final class Settings {
        public static final String FILE = ".population";
        public static final String WORK_DIRECTORY = "WorkDirectory";
        public static final String PRIMARY_STAGE_X = "PrimaryStageX";
        public static final String PRIMARY_STAGE_Y = "PrimaryStageY";
        public static final String PRIMARY_STAGE_WIDTH = "PrimaryStageWidth";
        public static final String PRIMARY_STAGE_HEIGHT = "PrimaryStageHeight";
        public static final String PRIMARY_STAGE_MAXIMIZED = "PrimaryStageMaximized";
        public static final String LOCALE = "Locale";

        private Settings() {
        }
    }
}
