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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Результат вычислений для вывода в табличном виде
 */
public class TableResult {
    private final ArrayList<DoubleProperty> mValues;
    private final IntegerProperty mNumber;

    public TableResult(double[] states, int number) {
        mNumber = new SimpleIntegerProperty(number);
        mValues = new ArrayList<>(states.length);
        for (double state : states) {
            mValues.add(new SimpleDoubleProperty(state));
        }
    }

    public IntegerProperty numberProperty() {
        return mNumber;
    }

    public int getNumber() {
        return mNumber.get();
    }

    public DoubleProperty valueDoubleProperty(int position) {
        return mValues.get(position);
    }

    public double getValue(int position) {
        return mValues.get(position).get();
    }

    public int valueCount() {
        return mValues.size();
    }
}
