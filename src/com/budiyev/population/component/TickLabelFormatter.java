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
package com.budiyev.population.component;

import java.text.DecimalFormat;
import java.text.Format;

import javafx.util.StringConverter;

public class TickLabelFormatter extends StringConverter<Number> {
    private static final int THRESHOLD = 10000;
    private final Format mNormalFormat = new DecimalFormat("#.#");
    private final Format mExponentialFormat = new DecimalFormat("#.##E0");

    @Override
    public String toString(Number number) {
        if (number == null) {
            return "";
        }
        double value = number.doubleValue();
        if (Math.abs(value) < THRESHOLD) {
            return mNormalFormat.format(value);
        } else {
            return mExponentialFormat.format(value);
        }
    }

    @Override
    public Number fromString(String string) {
        return null;
    }
}
