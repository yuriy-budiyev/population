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
package com.budiyev.population.model;

import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public final class TransitionType {
    public static final int LINEAR = 0;
    public static final int SOLUTE = 1;
    public static final int BLEND = 2;
    public static final ObservableList<Number> TYPES =
            FXCollections.observableArrayList(new Number[]{LINEAR, SOLUTE, BLEND});

    private TransitionType() {
    }

    public static String getName(int type, ResourceBundle resources) {
        switch (type) {
            case LINEAR: {
                return resources.getString("type_linear");
            }
            case SOLUTE: {
                return resources.getString("type_solute");
            }
            case BLEND: {
                return resources.getString("type_blend");
            }
            default: {
                return resources.getString("unnamed");
            }
        }
    }
}
