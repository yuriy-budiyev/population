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
package com.budiyev.population.controller.base;

import com.budiyev.population.PopulationApplication;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.stage.Stage;

public abstract class AbstractController implements Initializable {
    private PopulationApplication mApplication;
    private URL mLocation;
    private ResourceBundle mResources;
    private Stage mStage;

    @Override
    public final void initialize(URL location, ResourceBundle resources) {
        mLocation = location;
        mResources = resources;
        initialize();
    }

    public PopulationApplication getApplication() {
        return mApplication;
    }

    public final void setApplication(PopulationApplication application) {
        mApplication = application;
    }

    public URL getLocation() {
        return mLocation;
    }

    public ResourceBundle getResources() {
        return mResources;
    }

    public String getString(String key) {
        return mResources.getString(key);
    }

    public Stage getStage() {
        return mStage;
    }

    public final void setStage(Stage stage) {
        mStage = stage;
    }

    public abstract void initialize();
}
