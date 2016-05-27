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
package com.budiyev.population;

import com.budiyev.population.controller.AbstractAboutController;
import com.budiyev.population.controller.AbstractController;
import com.budiyev.population.controller.AbstractExportController;
import com.budiyev.population.model.Calculator;
import com.budiyev.population.model.State;
import com.budiyev.population.model.Transition;
import com.budiyev.population.util.CsvParser;
import com.budiyev.population.util.TaskParser;
import com.budiyev.population.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class PopulationApplication extends Application {
    private static final int PRIMARY_STAGE_MIN_WIDTH = 750;
    private static final int PRIMARY_STAGE_MIN_HEIGHT = 450;
    private final HashMap<String, String> mSettings = new HashMap<>();
    private Stage mPrimaryStage;
    private ResourceBundle mResources;

    private void loadSettings() {
        File settingsFile = new File(System.getProperty("user.home"), Settings.FILE);
        if (!settingsFile.exists()) {
            return;
        }
        CsvParser.Table settingsTable;
        try {
            settingsTable = CsvParser.parse(new FileInputStream(settingsFile), ',', "UTF-8");
        } catch (FileNotFoundException e) {
            return;
        }
        try {
            for (CsvParser.Row row : settingsTable) {
                if (Objects.equals(row.cell(0), Settings.WORK_DIRECTORY)) {
                    mSettings.put(Settings.WORK_DIRECTORY, Utils.nullOrString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_X)) {
                    mSettings.put(Settings.PRIMARY_STAGE_X, Utils.nullOrString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_Y)) {
                    mSettings.put(Settings.PRIMARY_STAGE_Y, Utils.nullOrString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_WIDTH)) {
                    mSettings.put(Settings.PRIMARY_STAGE_WIDTH, Utils.nullOrString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_HEIGHT)) {
                    mSettings.put(Settings.PRIMARY_STAGE_HEIGHT, Utils.nullOrString(row.cell(1)));
                } else if (Objects.equals(row.cell(0), Settings.PRIMARY_STAGE_MAXIMIZED)) {
                    mSettings
                            .put(Settings.PRIMARY_STAGE_MAXIMIZED, Utils.nullOrString(row.cell(1)));
                }
            }
            mSettings.putIfAbsent(Settings.WORK_DIRECTORY, System.getProperty("user.home"));
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
        CsvParser.Table settingsTable = new CsvParser.Table();
        settingsTable.add(new CsvParser.Row(Settings.WORK_DIRECTORY,
                mSettings.get(Settings.WORK_DIRECTORY)));
        settingsTable.add(new CsvParser.Row(Settings.PRIMARY_STAGE_X, mPrimaryStage.getX()));
        settingsTable.add(new CsvParser.Row(Settings.PRIMARY_STAGE_Y, mPrimaryStage.getY()));
        settingsTable
                .add(new CsvParser.Row(Settings.PRIMARY_STAGE_WIDTH, mPrimaryStage.getWidth()));
        settingsTable
                .add(new CsvParser.Row(Settings.PRIMARY_STAGE_HEIGHT, mPrimaryStage.getHeight()));
        settingsTable.add(new CsvParser.Row(Settings.PRIMARY_STAGE_MAXIMIZED,
                mPrimaryStage.isMaximized()));
        try {
            settingsFile.createNewFile();
            CsvParser.encode(settingsTable, new FileOutputStream(settingsFile), ',', "UTF-8");
        } catch (IOException ignored) {
        }
    }

    private void initializeResources() {
        mResources = ResourceBundle
                .getBundle("com.budiyev.population.resource.strings", Locale.getDefault());
    }

    private void showPrimaryStage(Stage primaryStage) {
        mPrimaryStage = primaryStage;
        mPrimaryStage.heightProperty().addListener((observable, oldValue, newValue) -> {
            long rounded = Math.round(newValue.doubleValue());
            if (rounded % 2 != 0) {
                rounded++;
                mPrimaryStage.setHeight(rounded);
            }
        }); // We need this to avoid shitty JavaFX's drawing blurring in chart legend (WTF).
        mPrimaryStage.setTitle(mResources.getString("application_name"));
        mPrimaryStage.setMinWidth(PRIMARY_STAGE_MIN_WIDTH);
        mPrimaryStage.setMinHeight(PRIMARY_STAGE_MIN_HEIGHT);
        mPrimaryStage.getIcons()
                .add(new Image(getClass().getResourceAsStream("resource/icon.png")));
        FXMLLoader sceneLoader =
                new FXMLLoader(getClass().getResource("view/PrimaryView.fxml"), mResources);
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
            double value = Double.valueOf(setting);
            if (value > 0) {
                mPrimaryStage.setX(value);
            }
        }
        setting = mSettings.get(Settings.PRIMARY_STAGE_Y);
        if (setting != null) {
            double value = Double.valueOf(setting);
            if (value > 0) {
                mPrimaryStage.setY(value);
            }
        }
        setting = mSettings.get(Settings.PRIMARY_STAGE_WIDTH);
        if (setting != null) {
            double value = Double.valueOf(setting);
            if (value > PRIMARY_STAGE_MIN_WIDTH) {
                mPrimaryStage.setWidth(value);
            }
        }
        setting = mSettings.get(Settings.PRIMARY_STAGE_HEIGHT);
        if (setting != null) {
            double value = Double.valueOf(setting);
            if (value > PRIMARY_STAGE_MIN_HEIGHT) {
                mPrimaryStage.setHeight(value);
            }
        }
        setting = mSettings.get(Settings.PRIMARY_STAGE_MAXIMIZED);
        if (setting != null) {
            mPrimaryStage.setMaximized(Boolean.valueOf(setting));
        }
        try {
            Scene mainScene = new Scene(sceneLoader.load(), 1, 1);
            mainScene.getStylesheets().add("com/budiyev/population/resource/style.css");
            mPrimaryStage.setScene(mainScene);
            mPrimaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Thread.currentThread().setUncaughtExceptionHandler(Utils.UNCAUGHT_EXCEPTION_HANDLER);
        loadSettings();
        initializeResources();
        showPrimaryStage(primaryStage);
    }

    @Override
    public void stop() throws Exception {
        saveSettings();
    }

    public void showExportDialog(ArrayList<Calculator.Results> results,
            HashMap<String, String> taskSettings) {
        Stage exportStage = new Stage(StageStyle.UTILITY);
        exportStage.initModality(Modality.APPLICATION_MODAL);
        exportStage.initOwner(mPrimaryStage.getOwner());
        exportStage.setResizable(false);
        exportStage.setTitle(mResources.getString("export"));
        FXMLLoader sceneLoader = new FXMLLoader(getClass().getResource("view/ExportView.fxml"));
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
            exportStage.setScene(new Scene(sceneLoader.load(), -1, -1));
            exportStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showAboutDialog() {
        Stage aboutStage = new Stage(StageStyle.UTILITY);
        aboutStage.initModality(Modality.APPLICATION_MODAL);
        aboutStage.initOwner(mPrimaryStage.getOwner());
        aboutStage.setResizable(false);
        aboutStage.setTitle(mResources.getString("about"));
        FXMLLoader sceneLoader = new FXMLLoader(getClass().getResource("view/AboutView.fxml"));
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
            aboutStage.setScene(new Scene(sceneLoader.load(), -1, -1));
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

    private static void calculate(File inputFile, File outputFile, ResourceBundle resources) throws
            Exception {
        ArrayList<State> initialStates = new ArrayList<>();
        ArrayList<Transition> transitions = new ArrayList<>();
        HashMap<String, String> settings = new HashMap<>();
        TaskParser.parse(inputFile, initialStates, transitions, settings);
        int startPoint = Integer.valueOf(settings.get(TaskParser.Settings.START_POINT));
        int stepsCount = Integer.valueOf(settings.get(TaskParser.Settings.STEPS_COUNT));
        boolean higherAccuracy = Boolean.valueOf(settings.get(TaskParser.Settings.HIGHER_ACCURACY));
        boolean allowNegative = Boolean.valueOf(settings.get(TaskParser.Settings.ALLOW_NEGATIVE));
        char columnSeparator = settings.get(TaskParser.Settings.COLUMN_SEPARATOR).charAt(0);
        char decimalSeparator = settings.get(TaskParser.Settings.DECIMAL_SEPARATOR).charAt(0);
        String lineSeparator = settings.get(TaskParser.Settings.LINE_SEPARATOR);
        String encoding = settings.get(TaskParser.Settings.ENCODING);
        Calculator.Results results = Calculator
                .calculate(initialStates, transitions, stepsCount, startPoint, higherAccuracy,
                        allowNegative);
        Utils.exportResults(outputFile, results, columnSeparator, decimalSeparator, lineSeparator,
                encoding, resources);
    }

    public static void main(String[] args) {
        if (args.length == 2 || args.length == 3) {
            try {
                File inputFile = new File(args[0]);
                File outputFile = new File(args[1]);
                boolean needExit = false;
                if (!inputFile.exists()) {
                    System.out.println("Error: " + inputFile + " doesn't exist.");
                    needExit = true;
                }
                if (!outputFile.exists()) {
                    System.out.println("Error: " + outputFile + " doesn't exist.");
                    needExit = true;
                }
                if (needExit) {
                    System.exit(1);
                }
                Locale locale;
                if (args.length == 3) {
                    locale = Locale.forLanguageTag(args[2].toLowerCase());
                } else {
                    locale = Locale.getDefault();
                }
                calculate(inputFile, outputFile, ResourceBundle
                        .getBundle("com.budiyev.population.resource.strings", locale));
            } catch (Exception e) {
                System.out.println(
                        "Error: " + e.getClass().getSimpleName() + " - " + e.getLocalizedMessage());
                System.exit(1);
            }
            System.exit(0);
        } else {
            launch(PopulationApplication.class, args);
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

        private Settings() {
        }
    }
}
