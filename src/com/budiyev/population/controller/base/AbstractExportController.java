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
package com.budiyev.population.controller.base;

import java.util.ArrayList;
import java.util.HashMap;

import com.budiyev.population.model.Result;

public abstract class AbstractExportController extends AbstractController {
    private ArrayList<Result> mResults;
    private HashMap<String, String> mTaskSettings;

    public ArrayList<Result> getResults() {
        return mResults;
    }

    public final void setResults(ArrayList<Result> results) {
        mResults = results;
    }

    public HashMap<String, String> getTaskSettings() {
        return mTaskSettings;
    }

    public final void setTaskSettings(HashMap<String, String> taskSettings) {
        mTaskSettings = taskSettings;
    }
}
