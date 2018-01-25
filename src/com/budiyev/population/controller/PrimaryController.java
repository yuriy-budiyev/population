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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.budiyev.population.PopulationApplication;
import com.budiyev.population.component.Calculator;
import com.budiyev.population.component.ChartSeries;
import com.budiyev.population.component.TickLabelFormatter;
import com.budiyev.population.controller.base.AbstractController;
import com.budiyev.population.model.Result;
import com.budiyev.population.model.State;
import com.budiyev.population.model.TableResult;
import com.budiyev.population.model.Task;
import com.budiyev.population.model.Transition;
import com.budiyev.population.model.TransitionMode;
import com.budiyev.population.model.TransitionType;
import com.budiyev.population.util.TaskParser;
import com.budiyev.population.util.Utils;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;

public class PrimaryController extends AbstractController {
    private final ObservableList<State> mStates = FXCollections.observableArrayList();
    private final ObservableList<Number> mStatesIdList = FXCollections.observableArrayList();
    private final ObservableList<Transition> mTransitions = FXCollections.observableArrayList();
    private final ObservableList<ChartSeries> mResultsChartData = FXCollections.observableArrayList();
    private final HashMap<Number, State> mStatesIdMap = new HashMap<>();
    private final HashMap<String, String> mTaskSettings = new HashMap<>();
    private final ArrayList<Result> mResultsTableData = new ArrayList<>();
    private final int[] mCurrentChartBounds = {0, 100};
    private volatile boolean mZoomingChart;
    private volatile boolean mCalculating;
    private int mResultsTablePrecision = 3;
    private File mTaskFile;
    public MenuItem mClearMenuItem;
    public MenuItem mOpenMenuItem;
    public MenuItem mImportMenuItem;
    public MenuItem mSaveMenuItem;
    public MenuItem mSaveAsMenuItem;
    public MenuItem mQuitMenuItem;
    public MenuItem mLangRussianMenuItem;
    public MenuItem mLangEnglishMenuItem;
    public MenuItem mAboutMenuItem;
    public TableView<State> mStatesTable;
    public TableColumn<State, String> mStatesTableNameColumn;
    public TableColumn<State, Number> mStatesTableCountColumn;
    public TableColumn<State, String> mStatesTableDescriptionColumn;
    public TableView<Transition> mTransitionsTable;
    public TableColumn<Transition, Number> mTransitionsTableSourceStateColumn;
    public TableColumn<Transition, Number> mTransitionsTableSourceCoefficientColumn;
    public TableColumn<Transition, Number> mTransitionsTableSourceDelayColumn;
    public TableColumn<Transition, Number> mTransitionsTableOperandStateColumn;
    public TableColumn<Transition, Number> mTransitionsTableOperandCoefficientColumn;
    public TableColumn<Transition, Number> mTransitionsTableOperandDelayColumn;
    public TableColumn<Transition, Number> mTransitionsTableResultStateColumn;
    public TableColumn<Transition, Number> mTransitionsTableResultCoefficientColumn;
    public TableColumn<Transition, Number> mTransitionsTableProbabilityColumn;
    public TableColumn<Transition, Number> mTransitionsTableTypeColumn;
    public TableColumn<Transition, Number> mTransitionsTableModeColumn;
    public TableColumn<Transition, String> mTransitionsTableDescriptionColumn;
    public TableView<ChartSeries> mResultsChartSettingsTable;
    public TableColumn<ChartSeries, Boolean> mChartSettingsTableVisibilityColumn;
    public TableColumn<ChartSeries, String> mChartSettingsTableNameColumn;
    public TableColumn<ChartSeries, Number> mChartSettingsTableColorColumn;
    public TableColumn<ChartSeries, Number> mChartSettingsTableDashColumn;
    public TableColumn<ChartSeries, Number> mChartSettingsTableThicknessColumn;
    public Label mStartPointLabel;
    public Label mStepsCountLabel;
    public Label mResultsTablePrecisionLabel;
    public TextField mStepsCountField;
    public TextField mStartPointField;
    public TextField mResultsTablePrecisionField;
    public ProgressBar mCalculationProgressBar;
    public TableView<ArrayList<TableResult>> mResultsTable;
    public LineChart<Number, Number> mResultsChart;
    public AnchorPane mResultsChartContainer;
    public Button mAddStateButton;
    public Button mRemoveStateButton;
    public Button mMoveStateUpButton;
    public Button mMoveStateDownButton;
    public Button mAddTransitionButton;
    public Button mRemoveTransitionButton;
    public Button mMoveTransitionUpButton;
    public Button mMoveTransitionDownButton;
    public Button mCalculateButton;
    public Button mClearResultsChartButton;
    public Button mClearResultsTableButton;
    public Button mExportResultsButton;
    public Button mApplyResultsTablePrecisionButton;
    public CheckBox mParallel;
    public CheckBox mHigherAccuracy;
    public CheckBox mAllowNegativeNumbers;
    public CheckBox mResultsOnChart;
    public CheckBox mResultsInTable;

    private void initializeStatesTable() {
        mStatesIdList.add(State.EXTERNAL);
        mStatesTableNameColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<String>() {
            @Override
            public String toString(String object) {
                return Utils.isNullOrEmpty(object) ? getString("unnamed") : object;
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        }));
        mStatesTableNameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
        mStatesTableNameColumn.setOnEditCommit(event -> {
            String value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setName(value);
                Utils.refreshList(mStatesIdList);
                Utils.refreshList(mTransitions);
            }
        });
        mStatesTableCountColumn.setCellFactory(doubleCell(x -> x >= 0, 0, Utils.DECIMAL_FORMAT_COMMON));
        mStatesTableCountColumn.setCellValueFactory(param -> param.getValue().countProperty());
        mStatesTableCountColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setCount(value.doubleValue());
            }
        });
        mStatesTableDescriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        mStatesTableDescriptionColumn.setCellValueFactory(param -> param.getValue().descriptionProperty());
        mStatesTableDescriptionColumn.setOnEditCommit(event -> {
            String value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setDescription(value);
            }
        });
        mStatesTable.setItems(mStates);
        mStates.addListener((ListChangeListener<State>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (int i = c.getFrom(); i < c.getTo(); i++) {
                        State state = c.getList().get(i);
                        int id = state.getId();
                        mStatesIdMap.put(id, state);
                        mStatesIdList.add(i, id);
                    }
                } else if (c.wasRemoved()) {
                    int position = c.getTo();
                    for (int i = position + c.getRemovedSize() - 1; i >= position; i--) {
                        mStatesIdMap.remove(mStatesIdList.remove(i));
                    }
                }
            }
        });
        mStatesTable.setPlaceholder(new Rectangle());
    }

    private void initializeTransitionsTable() {
        mTransitionsTableSourceStateColumn.setCellFactory(stateCell());
        mTransitionsTableSourceStateColumn.setCellValueFactory(param -> param.getValue().sourceStateProperty());
        mTransitionsTableSourceStateColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setSourceState(value.intValue());
            }
        });
        mTransitionsTableSourceCoefficientColumn.setCellFactory(doubleCell(x -> x > 0, 1, Utils.DECIMAL_FORMAT_COMMON));
        mTransitionsTableSourceCoefficientColumn
                .setCellValueFactory(param -> param.getValue().sourceCoefficientProperty());
        mTransitionsTableSourceCoefficientColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow())
                        .setSourceCoefficient(value.doubleValue());
            }
        });
        mTransitionsTableSourceDelayColumn.setCellFactory(integerCell(x -> x >= 0, 0));
        mTransitionsTableSourceDelayColumn.setCellValueFactory(param -> param.getValue().sourceDelayProperty());
        mTransitionsTableSourceDelayColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setSourceDelay(value.intValue());
            }
        });
        mTransitionsTableOperandStateColumn.setCellFactory(stateCell());
        mTransitionsTableOperandStateColumn.setCellValueFactory(param -> param.getValue().operandStateProperty());
        mTransitionsTableOperandStateColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow())
                        .setOperandState(value.intValue());
            }
        });
        mTransitionsTableOperandCoefficientColumn
                .setCellFactory(doubleCell(x -> x > 0, 1, Utils.DECIMAL_FORMAT_COMMON));
        mTransitionsTableOperandCoefficientColumn
                .setCellValueFactory(param -> param.getValue().operandCoefficientProperty());
        mTransitionsTableOperandCoefficientColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow())
                        .setOperandCoefficient(value.doubleValue());
            }
        });
        mTransitionsTableOperandDelayColumn.setCellFactory(integerCell(x -> x >= 0, 0));
        mTransitionsTableOperandDelayColumn.setCellValueFactory(param -> param.getValue().operandDelayProperty());
        mTransitionsTableOperandDelayColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow())
                        .setOperandDelay(value.intValue());
            }
        });
        mTransitionsTableResultStateColumn.setCellFactory(stateCell());
        mTransitionsTableResultStateColumn.setCellValueFactory(param -> param.getValue().resultStateProperty());
        mTransitionsTableResultStateColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setResultState(value.intValue());
            }
        });
        mTransitionsTableResultCoefficientColumn.setCellFactory(doubleCell(x -> x > 0, 1, Utils.DECIMAL_FORMAT_COMMON));
        mTransitionsTableResultCoefficientColumn
                .setCellValueFactory(param -> param.getValue().resultCoefficientProperty());
        mTransitionsTableResultCoefficientColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow())
                        .setResultCoefficient(value.doubleValue());
            }
        });
        mTransitionsTableProbabilityColumn.setCellFactory(doubleCell(x -> x >= 0, 0, Utils.DECIMAL_FORMAT_COMMON));
        mTransitionsTableProbabilityColumn.setCellValueFactory(param -> param.getValue().probabilityProperty());
        mTransitionsTableProbabilityColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow())
                        .setProbability(value.doubleValue());
            }
        });
        mTransitionsTableTypeColumn.setCellFactory(typeCell());
        mTransitionsTableTypeColumn.setCellValueFactory(param -> param.getValue().typeProperty());
        mTransitionsTableTypeColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setType(value.intValue());
            }
        });
        mTransitionsTableModeColumn.setCellFactory(modeCell());
        mTransitionsTableModeColumn.setCellValueFactory(param -> param.getValue().modeProperty());
        mTransitionsTableModeColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setMode(value.intValue());
            }
        });
        mTransitionsTableDescriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        mTransitionsTableDescriptionColumn.setCellValueFactory(param -> param.getValue().descriptionProperty());
        mTransitionsTableDescriptionColumn.setOnEditCommit(event -> {
            String value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setDescription(value);
            }
        });
        mTransitionsTable.setPlaceholder(new Rectangle());
        mTransitionsTable.setItems(mTransitions);
    }

    private void initializeChartSettingsTable() {
        mChartSettingsTableVisibilityColumn.setCellFactory(CheckBoxTableCell.forTableColumn(null, null));
        mChartSettingsTableVisibilityColumn.setCellValueFactory(param -> param.getValue().visibilityProperty());
        mChartSettingsTableNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        mChartSettingsTableNameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
        mChartSettingsTableNameColumn.setOnEditCommit(event -> {
            String value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setName(value);
            }
        });
        mChartSettingsTableColorColumn.setCellFactory(ChoiceBoxTableCell.forTableColumn(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return ChartSeries.Color.getName(object.intValue(), getResources());
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        }, ChartSeries.Color.LIST));
        mChartSettingsTableColorColumn.setCellValueFactory(param -> param.getValue().colorProperty());
        mChartSettingsTableColorColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setColor(value.intValue());
                refreshResultsChart();
            }
        });
        mChartSettingsTableDashColumn.setCellFactory(ChoiceBoxTableCell.forTableColumn(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return ChartSeries.Dash.getName(object.intValue(), getResources());
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        }, ChartSeries.Dash.LIST));
        mChartSettingsTableDashColumn.setCellValueFactory(param -> param.getValue().dashProperty());
        mChartSettingsTableDashColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setDash(value.intValue());
                refreshResultsChart();
            }
        });
        mChartSettingsTableThicknessColumn
                .setCellFactory(ChoiceBoxTableCell.forTableColumn(new StringConverter<Number>() {
                    @Override
                    public String toString(Number object) {
                        return ChartSeries.Thickness.getName(object.intValue(), getResources());
                    }

                    @Override
                    public Number fromString(String string) {
                        return null;
                    }
                }, ChartSeries.Thickness.LIST));
        mChartSettingsTableThicknessColumn.setCellValueFactory(param -> param.getValue().thicknessProperty());
        mChartSettingsTableThicknessColumn.setOnEditCommit(event -> {
            Number value = event.getNewValue();
            if (value != null) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setThickness(value.intValue());
                refreshResultsChart();
            }
        });
        mResultsChartSettingsTable.setPlaceholder(new Rectangle());
        mResultsChartSettingsTable.setItems(mResultsChartData);
    }

    private void initializeResultsChart() {
        Rectangle zoomRect = new Rectangle();
        zoomRect.setManaged(false);
        zoomRect.setFill(Color.LIGHTBLUE.deriveColor(0, 1, 1, 0.5));
        mResultsChartContainer.getChildren().add(zoomRect);
        mResultsChart.setCreateSymbols(false);
        mResultsChart.setAnimated(false);
        final ObjectProperty<Point2D> mouseAnchor = new SimpleObjectProperty<>();
        mResultsChart.setOnMousePressed(event -> {
            mouseAnchor.set(new Point2D(event.getX(), event.getY()));
            zoomRect.setWidth(0);
            zoomRect.setHeight(0);
        });
        mResultsChart.setOnMouseDragged(event -> {
            double x = event.getX();
            double y = event.getY();
            zoomRect.setX(Math.min(x, mouseAnchor.get().getX()));
            zoomRect.setY(Math.min(y, mouseAnchor.get().getY()));
            zoomRect.setWidth(Math.abs(x - mouseAnchor.get().getX()));
            zoomRect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
        });
        ScheduledExecutorService executor =
                Executors.newSingleThreadScheduledExecutor(getApplication().getThreadFactory());
        int[] zoomedBounds = new int[2];
        ScheduledFuture<?>[] refreshChart = new ScheduledFuture<?>[1];
        getStage().widthProperty().addListener((observable, oldValue, newValue) -> {
            if (refreshChart[0] != null) {
                refreshChart[0].cancel(false);
            }
            refreshChart[0] = executor.schedule(() -> {
                if (mResultsChartData.size() == 0) {
                    return;
                }
                if (mZoomingChart) {
                    setResultsChartBounds(zoomedBounds[0], zoomedBounds[1]);
                } else {
                    resetResultsChartBounds();
                }
                Platform.runLater(PrimaryController.this::refreshResultsChart);
            }, 500, TimeUnit.MILLISECONDS);
        });
        NumberAxis xAxis = (NumberAxis) mResultsChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) mResultsChart.getYAxis();
        TickLabelFormatter tickLabelFormatter = new TickLabelFormatter();
        xAxis.setTickLabelFormatter(tickLabelFormatter);
        yAxis.setTickLabelFormatter(tickLabelFormatter);
        xAxis.setAutoRanging(false);
        xAxis.setUpperBound(100);
        xAxis.setTickUnit(10);
        mResultsChart.setOnMouseReleased(event -> {
            double zoomRectWidth = zoomRect.getWidth();
            double zoomRectHeight = zoomRect.getHeight();
            if (zoomRectHeight < 5 || zoomRectWidth < 5 || mResultsChartData.size() == 0) {
                zoomRect.setWidth(0);
                zoomRect.setHeight(0);
                resetResultsChartScale();
                return;
            }
            Point2D chartInScene = mResultsChart.localToScene(0, 0);
            Point2D xAxisInScene = xAxis.localToScene(0, 0);
            Point2D yAxisInScene = yAxis.localToScene(0, 0);
            double xScale = xAxis.getScale();
            double yScale = yAxis.getScale();
            double zoomAreaWidth = applyScale(xScale, zoomRectWidth);
            double zoomAreaHeight = applyScale(yScale, zoomRectHeight);
            if (zoomAreaWidth < 0.5 || zoomAreaHeight < 0.5) {
                zoomRect.setWidth(0);
                zoomRect.setHeight(0);
                return;
            }
            double zoomAreaX = applyScale(xScale, zoomRect.getX() - yAxisInScene.getX() - yAxis.getWidth());
            double zoomAreaY =
                    applyScale(yScale, xAxisInScene.getY() - zoomRect.getY() - zoomRectHeight - chartInScene.getY());
            double xLowerBound = xAxis.getLowerBound();
            double yLowerBound = yAxis.getLowerBound();
            yAxis.setAutoRanging(false);
            double xNLowerBound = Math.floor(xLowerBound + zoomAreaX);
            double xNUpperBound = Math.ceil(xNLowerBound + zoomAreaWidth);
            double yNLowerBound = Math.floor(yLowerBound + zoomAreaY);
            double yNUpperBound = Math.ceil(yNLowerBound + zoomAreaHeight);
            xNLowerBound = correctLowerBound(xNLowerBound, zoomAreaWidth);
            xNUpperBound = correctUpperBound(xNUpperBound, zoomAreaWidth);
            yNLowerBound = correctLowerBound(yNLowerBound, zoomAreaHeight);
            yNUpperBound = correctUpperBound(yNUpperBound, zoomAreaHeight);
            zoomedBounds[0] = (int) xNLowerBound - 1;
            zoomedBounds[1] = (int) xNUpperBound + 1;
            setResultsChartBounds(zoomedBounds[0], zoomedBounds[1]);
            refreshResultsChart();
            xAxis.setLowerBound(xNLowerBound);
            xAxis.setUpperBound(xNUpperBound);
            yAxis.setLowerBound(yNLowerBound);
            yAxis.setUpperBound(yNUpperBound);
            updateAxisTickUnit(xAxis);
            updateAxisTickUnit(yAxis);
            zoomRect.setWidth(0);
            zoomRect.setHeight(0);
            mZoomingChart = true;
        });
    }

    private void resetResultsChartScale() {
        resetResultsChartBounds();
        refreshResultsChart();
        mResultsChart.getYAxis().setAutoRanging(true);
        mZoomingChart = false;
    }

    private double applyScale(double scale, double value) {
        return value / Math.abs(scale);
    }

    private int getResultsChartWidth() {
        return (int) (Math.ceil(mResultsChart.getWidth()));
    }

    private double correctLowerBound(double lower, double size) {
        if (size > 100) {
            return lower - (lower % 10);
        } else if (size > 50) {
            return lower - (lower % 5);
        } else if (size > 10) {
            return lower - (lower % 2);
        } else {
            return lower;
        }
    }

    private double correctUpperBound(double upper, double size) {
        if (size > 100) {
            return upper + 10 - (upper % 10);
        } else if (size > 50) {
            return upper + 5 - (upper % 5);
        } else if (size > 10) {
            return upper + 2 - (upper % 2);
        } else {
            return upper;
        }
    }

    private Callback<TableColumn<Transition, Number>, TableCell<Transition, Number>> stateCell() {
        return ChoiceBoxTableCell.forTableColumn(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object == null) {
                    return getString("unselected");
                }
                if (object.intValue() == State.EXTERNAL) {
                    return getString("state_external");
                }
                State state = mStatesIdMap.get(object);
                if (state == null) {
                    return getString("unselected");
                }
                String name = state.getName();
                if (Utils.isNullOrEmpty(name)) {
                    return getString("unnamed");
                }
                return name;
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        }, mStatesIdList);
    }

    private Callback<TableColumn<Transition, Number>, TableCell<Transition, Number>> typeCell() {
        return ChoiceBoxTableCell.forTableColumn(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return TransitionType.getName((int) object, getResources());
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        }, TransitionType.TYPES);
    }

    private Callback<TableColumn<Transition, Number>, TableCell<Transition, Number>> modeCell() {
        return ChoiceBoxTableCell.forTableColumn(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return TransitionMode.getName((int) object, getResources());
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        }, TransitionMode.MODES);
    }

    private <T> Callback<TableColumn<T, Number>, TableCell<T, Number>> integerCell(Predicate<Integer> validator,
            int defaultValue) {
        return TextFieldTableCell.forTableColumn(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object == null) {
                    return null;
                }
                return object.toString();
            }

            @Override
            public Number fromString(String string) {
                try {
                    int value = Integer.parseInt(string);
                    if (validator.test(value)) {
                        return value;
                    } else {
                        return defaultValue;
                    }
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        });
    }

    private <T> Callback<TableColumn<T, Number>, TableCell<T, Number>> doubleCell(Predicate<Double> validator,
            double defaultValue, String format) {
        DecimalFormat formatter = new DecimalFormat(format);
        return TextFieldTableCell.forTableColumn(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object == null) {
                    return null;
                }
                return formatter.format(object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                try {
                    double value = NumberFormat.getInstance().parse(string).doubleValue();
                    if (validator.test(value)) {
                        return value;
                    } else {
                        return defaultValue;
                    }
                } catch (NumberFormatException | ParseException | NullPointerException e) {
                    return defaultValue;
                }
            }
        });
    }

    private FileChooser getTaskFileChooser(String title) {
        FileChooser fileChooser = getFileChooser(title);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(getString("task"), "*.pmt"));
        return fileChooser;
    }

    private FileChooser getImportTaskFileChooser() {
        FileChooser fileChooser = getFileChooser(getString("import_task"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(getString("text_document"), "*.txt"));
        return fileChooser;
    }

    private FileChooser getFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        String workDirectory = getApplication().getWorkDirectory();
        if (workDirectory != null) {
            File file = new File(workDirectory);
            if (file.exists()) {
                fileChooser.setInitialDirectory(file);
            }
        }
        return fileChooser;
    }

    private void setTitle(File file) {
        String applicationName = getString("application_name");
        getStage().setTitle(file == null ? applicationName : file.getName() + " - " + applicationName);
    }

    private void setTitle() {
        setTitle(null);
    }

    private HashMap<String, String> buildSettings() {
        int startPoint = -1;
        try {
            startPoint = Integer.parseInt(mStartPointField.getText());
        } catch (NumberFormatException ignored) {
        }
        int stepsCount = -1;
        try {
            stepsCount = Integer.parseInt(mStepsCountField.getText());
        } catch (NumberFormatException ignored) {
        }
        mTaskSettings.put(Task.Keys.START_POINT, String.valueOf(startPoint));
        mTaskSettings.put(Task.Keys.STEPS_COUNT, String.valueOf(stepsCount));
        mTaskSettings.put(Task.Keys.PARALLEL, String.valueOf(mParallel.isSelected()));
        mTaskSettings.put(Task.Keys.HIGHER_ACCURACY, String.valueOf(mHigherAccuracy.isSelected()));
        mTaskSettings.put(Task.Keys.ALLOW_NEGATIVE, String.valueOf(mAllowNegativeNumbers.isSelected()));
        return mTaskSettings;
    }

    private void readSettings(HashMap<String, String> settings) {
        settings.forEach(mTaskSettings::put);
        if (settings.containsKey(Task.Keys.START_POINT)) {
            mStartPointField.setText(settings.get(Task.Keys.START_POINT));
        }
        if (settings.containsKey(Task.Keys.STEPS_COUNT)) {
            mStepsCountField.setText(settings.get(Task.Keys.STEPS_COUNT));
        }
        if (settings.containsKey(Task.Keys.PARALLEL)) {
            mParallel.setSelected(Boolean.parseBoolean(settings.get(Task.Keys.PARALLEL)));
        }
        if (settings.containsKey(Task.Keys.HIGHER_ACCURACY)) {
            mHigherAccuracy.setSelected(Boolean.parseBoolean(settings.get(Task.Keys.HIGHER_ACCURACY)));
        }
        if (settings.containsKey(Task.Keys.ALLOW_NEGATIVE)) {
            mAllowNegativeNumbers.setSelected(Boolean.parseBoolean(settings.get(Task.Keys.ALLOW_NEGATIVE)));
        }
    }

    private ObservableList<XYChart.Series<Number, Number>> buildChart(int start, int end, int width) {
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        int minStart = Integer.MAX_VALUE;
        int maxStart = Integer.MIN_VALUE;
        for (ChartSeries chartSeries : mResultsChartData) {
            if (!chartSeries.getVisibility()) {
                continue;
            }
            int startPoint = chartSeries.getStartPoint();
            if (startPoint < minStart) {
                minStart = startPoint;
            }
            if (startPoint > maxStart) {
                maxStart = startPoint;
            }
        }
        if (start < minStart) {
            start = minStart;
        }
        int maxSize = 0;
        for (ChartSeries chartSeries : mResultsChartData) {
            if (!chartSeries.getVisibility()) {
                continue;
            }
            XYChart.Series<Number, Number> series = chartSeries.getData();
            int size = series.getData().size() + chartSeries.getStartPoint();
            if (size > maxSize) {
                maxSize = size;
            }
        }
        if (end > maxSize) {
            end = maxSize;
        }
        setResultsChartBounds(start, end);
        int dataWidth;
        if (start < 0 && end >= 0) {
            dataWidth = -1 * start + end;
        } else {
            dataWidth = Math.abs(end - start);
        }
        float fraction = dataWidth / (float) width;
        if (fraction <= 1) {
            ArrayList<XYChart.Series<Number, Number>> chart = new ArrayList<>(mResultsChartData.size());
            for (ChartSeries chartSeries : mResultsChartData) {
                if (!chartSeries.getVisibility()) {
                    continue;
                }
                XYChart.Series<Number, Number> originalSeries = chartSeries.getData();
                ObservableList<XYChart.Data<Number, Number>> originalData = originalSeries.getData();
                ArrayList<XYChart.Data<Number, Number>> data = new ArrayList<>();
                int localStart = chartSeries.getStartPoint();
                for (int j = start; j < end; j++) {
                    int localIndex = j - localStart;
                    if (localIndex >= 0 && localIndex < originalData.size()) {
                        data.add(originalData.get(localIndex));
                    }
                }
                chart.add(new XYChart.Series<>(originalSeries.getName(), FXCollections.observableList(data)));
            }
            return FXCollections.observableList(chart);
        } else {
            ArrayList<XYChart.Series<Number, Number>> chart = new ArrayList<>(mResultsChartData.size());
            int[] indexes = Calculator.interpolateIndexes(start, end, width);
            for (ChartSeries chartSeries : mResultsChartData) {
                if (!chartSeries.getVisibility()) {
                    continue;
                }
                XYChart.Series<Number, Number> originalSeries = chartSeries.getData();
                ObservableList<XYChart.Data<Number, Number>> originalData = originalSeries.getData();
                ArrayList<XYChart.Data<Number, Number>> data = new ArrayList<>(indexes.length);
                int localStart = chartSeries.getStartPoint();
                for (int index : indexes) {
                    int localIndex = index - localStart;
                    if (localIndex >= 0 && localIndex < originalData.size()) {
                        data.add(originalData.get(localIndex));
                    }
                }
                chart.add(new XYChart.Series<>(originalSeries.getName(), FXCollections.observableList(data)));
            }
            return FXCollections.observableList(chart);
        }
    }

    private void refreshResultsChartStyle() {
        mResultsChart.applyCss();
        Set<Node> legendSet = mResultsChart.lookupAll("Label.chart-legend-item");
        Iterator<Node> iterator = legendSet.iterator();
        for (ChartSeries chartSeries : mResultsChartData) {
            chartSeries.setLinePath(null);
            chartSeries.setLegendLabel(null);
        }
        List<ChartSeries> visible =
                mResultsChartData.stream().filter(ChartSeries::getVisibility).collect(Collectors.toList());
        for (int i = 0; i < visible.size(); i++) {
            ChartSeries chartSeries = visible.get(i);
            if (iterator.hasNext()) {
                chartSeries.setLegendLabel((Label) iterator.next());
            }
            Set<Node> seriesSet = mResultsChart.lookupAll(".series" + i);
            for (Node node : seriesSet) {
                if (node instanceof Path) {
                    chartSeries.setLinePath((Path) node);
                    break;
                }
            }
            chartSeries.refreshStyle();
        }
    }

    private ObservableList<XYChart.Series<Number, Number>> buildChart(int width) {
        return buildChart(mCurrentChartBounds[0], mCurrentChartBounds[1], width);
    }

    private void setResultsChartBounds(int start, int end) {
        mCurrentChartBounds[0] = start;
        mCurrentChartBounds[1] = end;
    }

    private void resetResultsChartBounds() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (ChartSeries chartSeries : mResultsChartData) {
            ObservableList<XYChart.Data<Number, Number>> data = chartSeries.getData().getData();
            int size = data.size();
            if (size == 0) {
                continue;
            }
            int startPoint = chartSeries.getStartPoint();
            int endPoint = data.get(size - 1).getXValue().intValue();
            if (startPoint < min) {
                min = startPoint;
            }
            if (endPoint > max) {
                max = endPoint;
            }
        }
        if (min == Integer.MAX_VALUE) {
            min = 0;
        }
        if (max == Integer.MIN_VALUE) {
            max = 100;
        }
        setResultsChartBounds(min, max);
        NumberAxis xAxis = (NumberAxis) mResultsChart.getXAxis();
        xAxis.setLowerBound(min);
        xAxis.setUpperBound(max);
        updateAxisTickUnit(xAxis);
    }

    private void updateAxisTickUnit(NumberAxis axis) {
        double size = Math.abs(axis.getUpperBound() - axis.getLowerBound());
        double tick = Math.ceil(size / 10);
        if (tick > 100) {
            tick = tick - (tick % 100);
        } else if (tick > 10) {
            tick = tick - (tick % 10);
        } else if (tick > 5) {
            tick = tick - (tick % 5);
        } else if (tick > 2) {
            tick = tick - (tick % 2);
        }
        axis.setTickUnit(tick);
    }

    private void refreshResultsChart() {
        mResultsChart.getData().clear();
        if (mResultsChartData.size() > 0) {
            mResultsChart.setData(buildChart(getResultsChartWidth()));
            refreshResultsChartStyle();
        }
    }

    private int getResultsTableSelectedStep() {
        ArrayList<TableResult> selectedItem = mResultsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return Integer.MIN_VALUE;
        }
        for (TableResult result : selectedItem) {
            if (result != null) {
                return result.getNumber();
            }
        }
        return Integer.MIN_VALUE;
    }

    private void selectResultsTableRowByStep(int step) {
        ObservableList<ArrayList<TableResult>> items = mResultsTable.getItems();
        for (int i = 0; i < items.size(); i++) {
            boolean found = false;
            ArrayList<TableResult> resultStates = items.get(i);
            for (TableResult result : resultStates) {
                if (result != null && result.getNumber() == step) {
                    mResultsTable.getSelectionModel().select(i);
                    mResultsTable.scrollTo(i);
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
    }

    private void refreshResultsTable() {
        int selectedStep = getResultsTableSelectedStep();
        mResultsTable.getItems().clear();
        mResultsTable.getColumns().clear();
        TableColumn<ArrayList<TableResult>, Number> numberColumn = new TableColumn<>();
        numberColumn.setText(getString("step"));
        numberColumn.setCellFactory(integerCell(x -> true, 0));
        numberColumn.setMinWidth(30);
        numberColumn.setSortable(false);
        numberColumn.setEditable(false);
        numberColumn.setCellValueFactory(param -> {
            ArrayList<TableResult> resultStates = param.getValue();
            for (TableResult result : resultStates) {
                if (result != null) {
                    return result.numberProperty();
                }
            }
            return null;
        });
        mResultsTable.getColumns().add(numberColumn);
        String decimalFormat = Utils.buildDecimalFormat(mResultsTablePrecision);
        for (int i = 0; i < mResultsTableData.size(); i++) {
            ArrayList<String> headers = mResultsTableData.get(i).getDataNames();
            for (int j = 0; j < headers.size(); j++) {
                TableColumn<ArrayList<TableResult>, Number> valueColumn = new TableColumn<>();
                valueColumn.setText(headers.get(j));
                valueColumn.setCellFactory(doubleCell(x -> true, 0, decimalFormat));
                valueColumn.setMinWidth(50);
                final int resultIndex = i;
                final int stateIndex = j;
                valueColumn.setCellValueFactory(param -> {
                    TableResult result = param.getValue().get(resultIndex);
                    if (result == null) {
                        return null;
                    }
                    return result.valueDoubleProperty(stateIndex);
                });
                valueColumn.setSortable(false);
                valueColumn.setEditable(false);
                mResultsTable.getColumns().add(valueColumn);
            }
        }
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        int columnCount = 0;
        for (Result result : mResultsTableData) {
            columnCount += result.getDataNames().size();
            int startPoint = result.getStartPoint();
            int size = result.getTableData().size() + startPoint;
            if (end <= size) {
                end = size;
            }
            if (startPoint < start) {
                start = startPoint;
            }
        }
        int dataSize;
        if (start < 0 && end >= 0) {
            dataSize = -1 * start + end;
        } else {
            dataSize = Math.abs(end - start);
        }
        end = start + dataSize;
        ArrayList<ArrayList<TableResult>> table = new ArrayList<>();
        for (int i = start; i < end; i++) {
            boolean empty = true;
            ArrayList<TableResult> row = new ArrayList<>(columnCount);
            for (Result result : mResultsTableData) {
                ArrayList<TableResult> data = result.getTableData();
                int localIndex = i - result.getStartPoint();
                if (localIndex >= 0 && localIndex < data.size()) {
                    row.add(data.get(localIndex));
                    empty = false;
                } else {
                    row.add(null);
                }
            }
            if (!empty) {
                table.add(row);
            }
        }
        mResultsTable.setItems(FXCollections.observableList(table));
        selectResultsTableRowByStep(selectedStep);
    }

    private void publishResult(Result result) {
        if (mResultsOnChart.isSelected()) {
            int colorsCount = ChartSeries.Color.ARRAY.length - 8;
            int dashesCount = ChartSeries.Dash.ARRAY.length;
            int thicknessesCount = ChartSeries.Thickness.ARRAY.length;
            ArrayList<XYChart.Series<Number, Number>> chart = result.getChartData();
            for (XYChart.Series<Number, Number> series : chart) {
                int size = mResultsChartData.size();
                int color = size % colorsCount;
                int dash = (size / colorsCount) % dashesCount;
                int thickness = (size / (colorsCount * dashesCount)) % thicknessesCount;
                ChartSeries chartSeries = new ChartSeries(series, result.getStartPoint(), color, dash, thickness, true);
                chartSeries.visibilityProperty().addListener((observable, oldValue, newValue) -> refreshResultsChart());
                mResultsChartData.add(chartSeries);
            }
            refreshResultsChart();
            resetResultsChartScale();
        }
        if (mResultsInTable.isSelected()) {
            mResultsTableData.add(result);
            refreshResultsTable();
        }
    }

    private void setControlsDisable(boolean value) {
        mStatesTable.setDisable(value);
        mTransitionsTable.setDisable(value);
        mAddStateButton.setDisable(value);
        mRemoveStateButton.setDisable(value);
        mMoveStateUpButton.setDisable(value);
        mMoveStateDownButton.setDisable(value);
        mAddTransitionButton.setDisable(value);
        mRemoveTransitionButton.setDisable(value);
        mMoveTransitionUpButton.setDisable(value);
        mMoveTransitionDownButton.setDisable(value);
        mStartPointLabel.setDisable(value);
        mStartPointField.setDisable(value);
        mStepsCountLabel.setDisable(value);
        mStepsCountField.setDisable(value);
        mCalculateButton.setDisable(value);
        mClearResultsChartButton.setDisable(value);
        mClearResultsTableButton.setDisable(value);
        mExportResultsButton.setDisable(value);
        mParallel.setDisable(value);
        mHigherAccuracy.setDisable(value);
        mAllowNegativeNumbers.setDisable(value);
        mResultsOnChart.setDisable(value);
        mResultsInTable.setDisable(value);
        mResultsChart.setDisable(value);
        mResultsChartSettingsTable.setDisable(value);
        mResultsTable.setDisable(value);
        mResultsTablePrecisionLabel.setDisable(value);
        mResultsTablePrecisionField.setDisable(value);
        mApplyResultsTablePrecisionButton.setDisable(value);
    }

    @Override
    public void initialize() {
        Utils.setDefaultTaskSettings(mTaskSettings);
        mResultsOnChart.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                mResultsInTable.setSelected(true);
            }
        });
        mResultsInTable.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                mResultsOnChart.setSelected(true);
            }
        });
        initializeStatesTable();
        initializeTransitionsTable();
        initializeChartSettingsTable();
        initializeResultsChart();
        showCurrentPrecision();
        mCalculationProgressBar.setVisible(false);
        mResultsTable.setPlaceholder(new Rectangle());
    }

    public void calculate() {
        if (mStates.size() == 0) {
            getApplication()
                    .showAlert(getString("alert_error"), null, getString("states_missing"), Alert.AlertType.WARNING);
            return;
        }
        if (mTransitions.size() == 0) {
            getApplication().showAlert(getString("alert_error"), null, getString("transitions_missing"),
                    Alert.AlertType.WARNING);
            return;
        }
        for (Transition transition : mTransitions) {
            if (transition.getSourceState() == State.UNDEFINED || transition.getOperandState() == State.UNDEFINED ||
                    transition.getResultState() == State.UNDEFINED) {
                getApplication().showAlert(getString("alert_error"), null, getString("transitions_incorrect"),
                        Alert.AlertType.WARNING);
                return;
            }
        }
        int startPoint;
        try {
            startPoint = Integer.parseInt(mStartPointField.getText());
        } catch (NumberFormatException e) {
            getApplication().showAlert(getString("alert_error"), null, getString("start_point_invalid"),
                    Alert.AlertType.WARNING);
            return;
        }
        int stepsCount;
        try {
            stepsCount = Integer.parseInt(mStepsCountField.getText());
            if (stepsCount < 1 || stepsCount == Integer.MAX_VALUE) {
                getApplication().showAlert(getString("alert_error"), null, getString("steps_count_invalid"),
                        Alert.AlertType.WARNING);
                return;
            }
        } catch (NumberFormatException e) {
            getApplication().showAlert(getString("alert_error"), null, getString("steps_count_invalid"),
                    Alert.AlertType.WARNING);
            return;
        }
        mCalculating = true;
        stepsCount++;
        setControlsDisable(true);
        mCalculationProgressBar.setProgress(0);
        mCalculationProgressBar.setVisible(true);
        Task task = new Task();
        task.setStates(mStates);
        task.setTransitions(mTransitions);
        task.setStartPoint(startPoint);
        task.setStepsCount(stepsCount);
        task.setHigherAccuracy(mHigherAccuracy.isSelected());
        task.setAllowNegative(mAllowNegativeNumbers.isSelected());
        task.setParallel(mParallel.isSelected());
        Calculator.calculateAsync(task, mResultsInTable.isSelected(), mResultsOnChart.isSelected(),
                result -> Platform.runLater(() -> {
                    publishResult(result);
                    mCalculationProgressBar.setVisible(false);
                    setControlsDisable(false);
                    mCalculating = false;
                }), progress -> Platform.runLater(() -> mCalculationProgressBar.setProgress(progress)),
                getApplication().getThreadFactory());
    }

    public void addState() {
        mStates.add(new State());
    }

    public void removeState() {
        int row = mStatesTable.getFocusModel().getFocusedCell().getRow();
        if (row > -1) {
            mStates.remove(row);
        }
    }

    public void moveStateUp() {
        int row = mStatesTable.getSelectionModel().getSelectedIndex();
        if (row > 0) {
            int topIndex = row - 1;
            State top = mStates.get(topIndex);
            State bottom = mStates.get(row);
            mStates.remove(row);
            mStates.remove(topIndex);
            mStates.add(topIndex, bottom);
            mStates.add(row, top);
            mStatesTable.getSelectionModel().select(topIndex);
        }
    }

    public void moveStateDown() {
        int row = mStatesTable.getSelectionModel().getSelectedIndex();
        if (row > -1 && row < mStates.size() - 1) {
            int bottomIndex = row + 1;
            State top = mStates.get(row);
            State bottom = mStates.get(bottomIndex);
            mStates.remove(bottomIndex);
            mStates.remove(row);
            mStates.add(row, bottom);
            mStates.add(bottomIndex, top);
            mStatesTable.getSelectionModel().select(bottomIndex);
        }
    }

    public void addTransition() {
        mTransitions.add(new Transition());
    }

    public void removeTransition() {
        int row = mTransitionsTable.getFocusModel().getFocusedCell().getRow();
        if (row > -1) {
            mTransitions.remove(row);
        }
    }

    public void moveTransitionUp() {
        int row = mTransitionsTable.getSelectionModel().getSelectedIndex();
        if (row > 0) {
            int topIndex = row - 1;
            Transition top = mTransitions.get(topIndex);
            Transition bottom = mTransitions.get(row);
            mTransitions.remove(row);
            mTransitions.remove(topIndex);
            mTransitions.add(topIndex, bottom);
            mTransitions.add(row, top);
            mTransitionsTable.getSelectionModel().select(topIndex);
        }
    }

    public void moveTransitionDown() {
        int row = mTransitionsTable.getSelectionModel().getSelectedIndex();
        if (row > -1 && row < mTransitions.size() - 1) {
            int bottomIndex = row + 1;
            Transition top = mTransitions.get(row);
            Transition bottom = mTransitions.get(bottomIndex);
            mTransitions.remove(bottomIndex);
            mTransitions.remove(row);
            mTransitions.add(row, bottom);
            mTransitions.add(bottomIndex, top);
            mTransitionsTable.getSelectionModel().select(bottomIndex);
        }
    }

    public void openTask() {
        if (mCalculating) {
            return;
        }
        File file = getTaskFileChooser(getString("open_task")).showOpenDialog(mStatesTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        Task task = TaskParser.parse(file);
        if (task == null) {
            return;
        }
        mStates.clear();
        mTransitions.clear();
        mStates.addAll(task.getStates());
        mTransitions.addAll(task.getTransitions());
        HashMap<String, String> settings = new HashMap<>();
        Task.readSettings(task, settings);
        readSettings(settings);
        mTaskFile = file;
        getApplication().setWorkDirectory(file.getParent());
        setTitle(file);
    }

    public void importTask() {
        if (mCalculating) {
            return;
        }
        File file = getImportTaskFileChooser().showOpenDialog(mStatesTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        Task task = TaskParser.parseLegacy(file);
        if (task == null) {
            return;
        }
        mTaskFile = null;
        buildSettings();
        mStates.clear();
        mTransitions.clear();
        mStates.addAll(task.getStates());
        mTransitions.addAll(task.getTransitions());
        getApplication().setWorkDirectory(file.getParent());
        setTitle(file);
    }

    public void clearTask() {
        if (mCalculating) {
            return;
        }
        mStates.clear();
        mTransitions.clear();
        mStartPointField.setText("0");
        mStepsCountField.setText("0");
        mHigherAccuracy.setSelected(false);
        mAllowNegativeNumbers.setSelected(false);
        mTaskFile = null;
        mTaskSettings.clear();
        Utils.setDefaultTaskSettings(mTaskSettings);
        setTitle();
    }

    public void clearResultsChart() {
        mResultsChartData.clear();
        mResultsChart.getData().clear();
        mResultsChart.getYAxis().setAutoRanging(true);
        resetResultsChartBounds();
    }

    public void clearResultsTable() {
        mResultsTableData.clear();
        mResultsTable.getItems().clear();
        mResultsTable.getColumns().clear();
    }

    public void saveTaskAs() {
        File file = getTaskFileChooser(getString("save_task")).showSaveDialog(mStatesTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        String path = file.getPath();
        int dotIndex = path.indexOf('.');
        if (dotIndex < 0) {
            path += ".pmt";
            file = new File(path);
        }
        Task task = new Task();
        task.setStates(mStates);
        task.setTransitions(mTransitions);
        Task.writeSettings(task, buildSettings());
        TaskParser.encode(file, task);
        mTaskFile = file;
        getApplication().setWorkDirectory(file.getParent());
        setTitle(file);
    }

    public void saveTask() {
        if (mTaskFile == null) {
            saveTaskAs();
        } else {
            Task task = new Task();
            task.setStates(mStates);
            task.setTransitions(mTransitions);
            Task.writeSettings(task, buildSettings());
            TaskParser.encode(mTaskFile, task);
        }
    }

    public void exportResults() {
        getApplication().showExportDialog(mResultsTableData, mTaskSettings);
    }

    public void applyResultsTablePrecision() {
        int precision;
        try {
            precision = Integer.parseInt(mResultsTablePrecisionField.getText());
        } catch (NumberFormatException e) {
            showCurrentPrecision();
            return;
        }
        if (precision == mResultsTablePrecision) {
            return;
        }
        if (precision < 0) {
            mResultsTablePrecision = 0;
            showCurrentPrecision();
        } else if (precision > Utils.MAX_PRECISION) {
            mResultsTablePrecision = Utils.MAX_PRECISION;
            showCurrentPrecision();
        } else {
            mResultsTablePrecision = precision;
        }
        refreshResultsTable();
    }

    private void showCurrentPrecision() {
        mResultsTablePrecisionField.setText(String.valueOf(mResultsTablePrecision));
    }

    public void about() {
        getApplication().showAboutDialog();
    }

    public void quit() {
        Platform.exit();
    }

    public void selectLangRussian() {
        selectLanguage("ru");
    }

    public void selectLangEnglish() {
        selectLanguage("en");
    }

    private void selectLanguage(String langTag) {
        PopulationApplication application = getApplication();
        if (application.selectLanguage(langTag)) {
            ResourceBundle resources = getResources();
            application.showAlert(resources.getString("lang"), null, resources.getString("lang_change"),
                    Alert.AlertType.INFORMATION);
        }
    }
}
