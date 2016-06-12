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
package com.budiyev.population.controller;

import com.budiyev.population.Launcher;
import com.budiyev.population.controller.base.AbstractAboutController;

import java.util.Locale;

import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class AboutController extends AbstractAboutController {
    public ImageView mApplicationIcon;
    public Label mApplicationDetails;

    @Override
    public void initialize() {
        mApplicationIcon.setImage(getImage());
        mApplicationDetails.setText(
                String.format(Locale.getDefault(), getString("about_details"), Launcher.VERSION));
    }

    public void close() {
        getStage().close();
    }
}
