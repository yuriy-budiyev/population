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

import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class State {
    public static final int EXTERNAL = -1;
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();
    private final int mId;
    private final StringProperty mName;
    private final DoubleProperty mCount;
    private final StringProperty mDescription;

    public State() {
        ID_COUNTER.compareAndSet(Integer.MAX_VALUE, 0);
        mId = ID_COUNTER.incrementAndGet();
        mName = new SimpleStringProperty("");
        mCount = new SimpleDoubleProperty();
        mDescription = new SimpleStringProperty("");
    }

    public State(int id, String name, double count, String description) {
        ID_COUNTER.accumulateAndGet(id, (left, right) -> left > right ? left : right);
        ID_COUNTER.compareAndSet(Integer.MAX_VALUE, 0);
        mId = id;
        mName = new SimpleStringProperty(name);
        mCount = new SimpleDoubleProperty(count);
        mDescription = new SimpleStringProperty(description);
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName.get();
    }

    public StringProperty nameProperty() {
        return mName;
    }

    public void setName(String name) {
        mName.set(name);
    }

    public double getCount() {
        return mCount.get();
    }

    public DoubleProperty countProperty() {
        return mCount;
    }

    public void setCount(double count) {
        this.mCount.set(count);
    }

    public String getDescription() {
        return mDescription.get();
    }

    public StringProperty descriptionProperty() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription.set(description);
    }
}
