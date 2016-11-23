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
package com.budiyev.population.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

/**
 * Результаты вычислений
 */
public class Result {
    private final int mStartPoint;
    private final ArrayList<String> mDataNames;
    private final ArrayList<TableResult> mTableData;
    private final ArrayList<XYChart.Series<Number, Number>> mChartData;

    /**
     * @param startPoint              начало отсчёта
     * @param states                  состояния
     * @param initialStates           начальные состояния
     * @param prepareResultsTableData подготовить результат в табличном виде
     * @param prepareResultsChartData подготовить результат в графическом виде
     */
    public Result(int startPoint, double[][] states, List<State> initialStates,
            boolean prepareResultsTableData, boolean prepareResultsChartData) {
        mStartPoint = startPoint;
        mDataNames = new ArrayList<>(initialStates.size());
        mDataNames.addAll(initialStates.stream().map(State::getName).collect(Collectors.toList()));
        if (prepareResultsTableData) {
            mTableData = new ArrayList<>(states.length);
            for (int i = 0; i < states.length; i++) {
                mTableData.add(new TableResult(states[i], i + mStartPoint));
            }
        } else {
            mTableData = null;
        }
        if (prepareResultsChartData) {
            mChartData = new ArrayList<>(mDataNames.size());
            for (int i = 0; i < mDataNames.size(); i++) {
                ObservableList<XYChart.Data<Number, Number>> data =
                        FXCollections.observableList(new ArrayList<>(states.length));
                for (int j = 0; j < states.length; j++) {
                    data.add(new XYChart.Data<>(j + mStartPoint, states[j][i]));
                }
                XYChart.Series<Number, Number> series =
                        new XYChart.Series<>(mDataNames.get(i), data);
                mChartData.add(series);
            }
        } else {
            mChartData = null;
        }
    }

    /**
     * @return начало отсчёта
     */
    public int getStartPoint() {
        return mStartPoint;
    }

    /**
     * @return имена состояний
     */
    public ArrayList<String> getDataNames() {
        return mDataNames;
    }

    public ArrayList<TableResult> getTableData() {
        return mTableData;
    }

    public ArrayList<XYChart.Series<Number, Number>> getChartData() {
        return mChartData;
    }
}
