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

import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;

public class ChartSeries {
    private static final String[] STROKE_COLORS =
            {"#f3622d", "#fba71b", "#57b757", "#41a9c9", "#4258c9", "#9a42c8", "#c84164", "#888888",
                    "#ffff00", "#ff00ff", "#00ffff", "#ff0000", "#00ff00", "#0000ff", "#000000"};
    private static final String[] STROKE_THICKNESSES = {"1.25", "2", "3", "4"};
    private static final String[] DASH_ARRAYS = {null, "4 8", "8 8", "16 8"};
    private final XYChart.Series<Number, Number> mData;
    private final int mStartPoint;
    private final IntegerProperty mColor;
    private final IntegerProperty mDash;
    private final IntegerProperty mThickness;
    private final BooleanProperty mVisibility;
    private Path mLinePath;
    private Label mLegendLabel;

    public ChartSeries(XYChart.Series<Number, Number> data, int startPoint, int color, int dash,
            int thickness, boolean visible) {
        mData = data;
        mStartPoint = startPoint;
        mVisibility = new SimpleBooleanProperty(visible);
        mColor = new SimpleIntegerProperty(color);
        mDash = new SimpleIntegerProperty(dash);
        mThickness = new SimpleIntegerProperty(thickness);
        mData.nameProperty().addListener((observable, oldValue, newValue) -> {
            Label legendLabel = mLegendLabel;
            if (legendLabel != null) {
                legendLabel.setText(newValue);
            }
        });
    }

    private String buildStyle() {
        int color = getColor();
        int thickness = getThickness();
        int dash = getDash();
        StringBuilder builder = new StringBuilder();
        builder.append("-fx-stroke: ").append(STROKE_COLORS[color]).append("; -fx-stroke-width: ")
                .append(STROKE_THICKNESSES[thickness]);
        String dashArray = DASH_ARRAYS[dash];
        if (dashArray != null) {
            builder.append("; ").append("-fx-stroke-dash-array: ").append(dashArray);
        }
        builder.append(";");
        return builder.toString();
    }

    public void refreshStyle() {
        Path linePath = mLinePath;
        Label legendLabel = mLegendLabel;
        if (linePath == null && legendLabel == null) {
            return;
        }
        String style = buildStyle();
        if (linePath != null) {
            linePath.setStyle(style);
        }
        if (legendLabel != null) {
            Line line = new Line(0, 0, 36, 0);
            line.setStyle(style);
            legendLabel.setGraphic(line);
        }
    }

    public XYChart.Series<Number, Number> getData() {
        return mData;
    }

    public String getName() {
        return mData.getName();
    }

    public StringProperty nameProperty() {
        return mData.nameProperty();
    }

    public void setName(String name) {
        mData.setName(name);
    }

    public int getStartPoint() {
        return mStartPoint;
    }

    public boolean getVisibility() {
        return mVisibility.get();
    }

    public BooleanProperty visibilityProperty() {
        return mVisibility;
    }

    public void setVisibility(boolean visibility) {
        mVisibility.set(visibility);
    }

    public int getColor() {
        return mColor.get();
    }

    public IntegerProperty colorProperty() {
        return mColor;
    }

    public void setColor(int color) {
        mColor.set(color);
    }

    public int getDash() {
        return mDash.get();
    }

    public IntegerProperty dashProperty() {
        return mDash;
    }

    public void setDash(int dash) {
        mDash.set(dash);
    }

    public int getThickness() {
        return mThickness.get();
    }

    public IntegerProperty thicknessProperty() {
        return mThickness;
    }

    public void setThickness(int thickness) {
        mThickness.set(thickness);
    }

    public Path getLinePath() {
        return mLinePath;
    }

    public void setLinePath(Path linePath) {
        mLinePath = linePath;
    }

    public Label getLegendLabel() {
        return mLegendLabel;
    }

    public void setLegendLabel(Label legendLabel) {
        mLegendLabel = legendLabel;
    }

    public static class Color {
        public static final int RED_ORANGE = 0;
        public static final int LIGHT_ORANGE = 1;
        public static final int SOFT_GREEN = 2;
        public static final int LIGHT_BLUE = 3;
        public static final int VIOLET_BLUE = 4;
        public static final int DARK_PURPLE = 5;
        public static final int DARK_PINK = 6;
        public static final int GRAY = 7;
        public static final int YELLOW = 8;
        public static final int MAGENTA = 9;
        public static final int CYAN = 10;
        public static final int RED = 11;
        public static final int GREEN = 12;
        public static final int BLUE = 13;
        public static final int BLACK = 14;
        public static final Number[] ARRAY =
                {BLACK, GRAY, RED, GREEN, BLUE, YELLOW, MAGENTA, CYAN, RED_ORANGE, LIGHT_ORANGE,
                        SOFT_GREEN, LIGHT_BLUE, VIOLET_BLUE, DARK_PURPLE, DARK_PINK};
        public static final ObservableList<Number> LIST =
                FXCollections.observableList(Arrays.asList(ARRAY));

        private Color() {
        }

        public static String getName(int color, ResourceBundle resources) {
            switch (color) {
                case RED_ORANGE: {
                    return resources.getString("color_red_orange");
                }
                case LIGHT_ORANGE: {
                    return resources.getString("color_light_orange");
                }
                case SOFT_GREEN: {
                    return resources.getString("color_soft_green");
                }
                case LIGHT_BLUE: {
                    return resources.getString("color_light_blue");
                }
                case VIOLET_BLUE: {
                    return resources.getString("color_violet_blue");
                }
                case DARK_PURPLE: {
                    return resources.getString("color_dark_purple");
                }
                case DARK_PINK: {
                    return resources.getString("color_dark_pink");
                }
                case YELLOW: {
                    return resources.getString("color_yellow");
                }
                case MAGENTA: {
                    return resources.getString("color_magenta");
                }
                case CYAN: {
                    return resources.getString("color_cyan");
                }
                case RED: {
                    return resources.getString("color_red");
                }
                case GREEN: {
                    return resources.getString("color_green");
                }
                case BLUE: {
                    return resources.getString("color_blue");
                }
                case GRAY: {
                    return resources.getString("color_gray");
                }
                case BLACK: {
                    return resources.getString("color_black");
                }
                default: {
                    return resources.getString("unnamed");
                }
            }
        }
    }

    public static class Dash {
        public static final int NONE = 0;
        public static final int SMALL = 1;
        public static final int MEDIUM = 2;
        public static final int LARGE = 3;
        public static final Number[] ARRAY = {NONE, SMALL, MEDIUM, LARGE};
        public static final ObservableList<Number> LIST =
                FXCollections.observableList(Arrays.asList(ARRAY));

        private Dash() {
        }

        public static String getName(int dash, ResourceBundle resources) {
            switch (dash) {
                case NONE: {
                    return resources.getString("dash_none");
                }
                case SMALL: {
                    return resources.getString("dash_small");
                }
                case MEDIUM: {
                    return resources.getString("dash_medium");
                }
                case LARGE: {
                    return resources.getString("dash_large");
                }
                default: {
                    return resources.getString("unnamed");
                }
            }
        }
    }

    public static class Thickness {
        public static final int THIN = 0;
        public static final int MEDIUM = 1;
        public static final int THICK = 2;
        public static final int VERY_THICK = 3;
        public static final Number[] ARRAY = {THIN, MEDIUM, THICK, VERY_THICK};
        public static final ObservableList<Number> LIST =
                FXCollections.observableList(Arrays.asList(ARRAY));

        private Thickness() {
        }

        public static String getName(int thickness, ResourceBundle resources) {
            switch (thickness) {
                case THIN: {
                    return resources.getString("thickness_thin");
                }
                case MEDIUM: {
                    return resources.getString("thickness_medium");
                }
                case THICK: {
                    return resources.getString("thickness_thick");
                }
                case VERY_THICK: {
                    return resources.getString("thickness_very_thick");
                }
                default: {
                    return resources.getString("unnamed");
                }
            }
        }
    }
}
