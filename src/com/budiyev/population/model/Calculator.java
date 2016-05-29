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
package com.budiyev.population.model;

import com.budiyev.population.util.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

public class Calculator {
    private Calculator() {
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(Double.MAX_EXPONENT, BigDecimal.ROUND_HALF_EVEN);
    }

    private static BigDecimal decimalValue(double value) {
        return scale(new BigDecimal(value));
    }

    private static BigDecimal decimalValue(int value) {
        return scale(new BigDecimal(value));
    }

    private static double doubleValue(BigDecimal value) {
        return Double.valueOf(value.toString());
    }

    private static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return a.divide(b, Double.MAX_EXPONENT, BigDecimal.ROUND_HALF_EVEN);
    }

    private static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return scale(a.multiply(b));
    }

    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b);
    }

    private static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b);
    }

    private static BigDecimal power(BigDecimal value, int power) {
        if (power <= 1) {
            return value;
        }
        BigDecimal result = value;
        for (int i = 2; i <= power; i++) {
            result = multiply(result, value);
        }
        return result;
    }

    private static int compare(BigDecimal a, BigDecimal b) {
        return a.compareTo(b);
    }

    private static BigDecimal minimum(BigDecimal a, BigDecimal b) {
        return a.min(b);
    }

    private static double factorial(int value) {
        double result = 1;
        for (int i = 2; i <= value; i++) {
            result *= i;
        }
        return result;
    }

    private static BigDecimal factorialDecimal(int value) {
        BigDecimal result = decimalValue(1);
        for (int i = 2; i <= value; i++) {
            result = multiply(result, decimalValue(i));
        }
        return result;
    }

    private static int findState(int id, int[] stateIds) {
        if (id == State.EXTERNAL) {
            return State.EXTERNAL;
        }
        for (int i = 0; i < stateIds.length; i++) {
            if (stateIds[i] == id) {
                return i;
            }
        }
        return -1;
    }

    private static int delay(int step, int delay) {
        if (step > delay) {
            return step - delay;
        } else {
            return 0;
        }
    }

    private static void updateProgress(double[] oldProgress, int step, int stepsCount,
            double threshold, ProgressCallback progressCallback) {
        if (progressCallback != null) {
            final double progress;
            boolean needUpdate;
            if (step == 0 || stepsCount == 0) {
                progress = 0;
                needUpdate = true;
            } else if (step == stepsCount - 1 || stepsCount == 1) {
                progress = 1;
                needUpdate = true;
            } else {
                progress = (double) step / (double) (stepsCount - 1);
                needUpdate = progress - oldProgress[0] > threshold;
            }
            if (needUpdate) {
                oldProgress[0] = progress;
                progressCallback.onProgressUpdate(progress);
            }
        }
    }

    public static int[] interpolateIndexes(int start, int end, int resultSize) {
        int[] array = new int[resultSize];
        for (int i = 0; i < resultSize; ++i) {
            array[i] = (int) Math.round(interpolate(start, end, i / (double) resultSize));
        }
        return array;
    }

    public static double interpolate(double a, double b, double f) {
        return a * (1D - f) + b * f;
    }

    private static double applyCoefficientPower(double value, int coefficient) {
        if (coefficient <= 1) {
            return value;
        }
        return Math.pow(value, coefficient) / factorial(coefficient);
    }

    private static double applyCoefficientLinear(double value, int coefficient) {
        if (coefficient <= 1) {
            return value;
        }
        return value / coefficient;
    }

    private static boolean isStateExternal(int stateId) {
        return stateId == State.EXTERNAL;
    }

    private static BigDecimal applyCoefficientPower(BigDecimal value, int coefficient) {
        if (coefficient <= 1) {
            return value;
        }
        return divide(power(value, coefficient), factorialDecimal(coefficient));
    }

    private static BigDecimal applyCoefficientLinear(BigDecimal value, int coefficient) {
        if (coefficient <= 1) {
            return value;
        }
        return divide(value, decimalValue(coefficient));
    }

    private static BigDecimal applyTransitionCommon(BigDecimal value, BigDecimal operandDensity,
            TransitionValues transition) {
        if (transition.mode == TransitionMode.INHIBITOR) {
            value = subtract(operandDensity, value);
        }
        value = multiply(value, decimalValue(transition.probability));
        if (transition.mode == TransitionMode.RESIDUAL) {
            value = subtract(operandDensity, value);
        }
        return value;
    }

    private static double applyTransitionCommon(double value, double operandDensity,
            TransitionValues transition) {
        if (transition.mode == TransitionMode.INHIBITOR) {
            value = operandDensity - value;
        }
        value *= transition.probability;
        if (transition.mode == TransitionMode.RESIDUAL) {
            value = operandDensity - value;
        }
        return value;
    }

    private static void calculateInternal(List<State> initialStates, List<Transition> transitions,
            int stepsCount, int startPoint, boolean allowNegative, ResultCallback resultCallback,
            ProgressCallback progressCallback) {
        int statesCount = initialStates.size();
        double[][] states = new double[stepsCount][statesCount];
        int[] stateIds = new int[statesCount];
        String[] stateNames = new String[statesCount];
        for (int i = 0; i < statesCount; i++) {
            State state = initialStates.get(i);
            states[0][i] = state.getCount();
            stateIds[i] = state.getId();
            stateNames[i] = state.getName();
        }
        int transitionsCount = transitions.size();
        TransitionValues[] transitionsInternal = new TransitionValues[transitionsCount];
        for (int i = 0; i < transitionsCount; i++) {
            transitionsInternal[i] = new TransitionValues(transitions.get(i));
        }
        double[] progress = new double[1];
        updateProgress(progress, 0, stepsCount, 0.001, progressCallback);
        for (int step = 1; step < stepsCount; step++) {
            double totalCount = 0;
            for (int state = 0; state < statesCount; state++) {
                double count = states[step - 1][state];
                states[step][state] = count;
                totalCount += count;
            }
            for (TransitionValues transition : transitionsInternal) {
                int sourceState = findState(transition.sourceState, stateIds);
                int operandState = findState(transition.operandState, stateIds);
                int resultState = findState(transition.resultState, stateIds);
                boolean sourceExternal = isStateExternal(sourceState);
                boolean operandExternal = isStateExternal(operandState);
                boolean resultExternal = isStateExternal(resultState);
                if (sourceExternal && operandExternal) {
                    continue;
                }
                int sourceIndex = delay(step - 1, transition.sourceDelay);
                int operandIndex = delay(step - 1, transition.operandDelay);
                double value = 0;
                if (transition.type == TransitionType.LINEAR) {
                    if (sourceExternal) {
                        double operandDensity =
                                applyCoefficientLinear(states[operandIndex][operandState],
                                        transition.operandCoefficient);
                        value = operandDensity * transition.probability;
                        if (transition.mode == TransitionMode.RESIDUAL) {
                            value = operandDensity - value;
                        }
                    } else if (operandExternal) {
                        value = applyCoefficientLinear(states[sourceIndex][sourceState],
                                transition.sourceCoefficient) * transition.probability;
                    } else {
                        double sourceDensity =
                                applyCoefficientLinear(states[sourceIndex][sourceState],
                                        transition.sourceCoefficient);
                        double operandDensity =
                                applyCoefficientLinear(states[operandIndex][operandState],
                                        transition.operandCoefficient);
                        value = applyTransitionCommon(Math.min(sourceDensity, operandDensity),
                                operandDensity, transition);
                    }
                } else if (transition.type == TransitionType.SOLUTE) {
                    if (totalCount > 0) {
                        if (sourceExternal) {
                            double operandDensity =
                                    applyCoefficientPower(states[operandIndex][operandState],
                                            transition.operandCoefficient);
                            value = operandDensity;
                            if (transition.operandCoefficient > 1) {
                                value /= Math.pow(totalCount, transition.operandCoefficient - 1);
                            }
                            value = applyTransitionCommon(value, operandDensity, transition);
                        } else if (operandExternal) {
                            value = applyCoefficientPower(states[sourceIndex][sourceState],
                                    transition.sourceCoefficient);
                            if (transition.sourceCoefficient > 1) {
                                value /= Math.pow(totalCount, transition.sourceCoefficient - 1);
                            }
                            value *= transition.probability;
                        } else {
                            double sourceDensity =
                                    applyCoefficientPower(states[sourceIndex][sourceState],
                                            transition.sourceCoefficient);
                            double operandDensity =
                                    applyCoefficientPower(states[operandIndex][operandState],
                                            transition.operandCoefficient);
                            value = sourceDensity * operandDensity / Math.pow(totalCount,
                                    transition.sourceCoefficient + transition.operandCoefficient -
                                    1);
                            if (sourceState == operandState) {
                                value /= 2;
                            }
                            value = applyTransitionCommon(value, operandDensity, transition);
                        }
                    }
                } else if (transition.type == TransitionType.BLEND) {
                    if (sourceExternal) {
                        double operandCount = states[operandIndex][operandState];
                        if (operandCount > 0) {
                            double operandDensity = applyCoefficientPower(operandCount,
                                    transition.operandCoefficient);
                            value = operandDensity;
                            if (transition.operandCoefficient > 1) {
                                value /= Math.pow(operandCount, transition.operandCoefficient - 1);
                            }
                            value = applyTransitionCommon(value, operandDensity, transition);
                        }
                    } else if (operandExternal) {
                        double sourceCount = states[sourceIndex][sourceState];
                        if (sourceCount > 0) {
                            value = applyCoefficientPower(sourceCount,
                                    transition.sourceCoefficient);
                            if (transition.sourceCoefficient > 1) {
                                value /= Math.pow(sourceCount, transition.sourceCoefficient - 1);
                            }
                            value *= transition.probability;
                        }
                    } else {
                        double sourceCount = states[sourceIndex][sourceState];
                        double operandCount = states[operandIndex][operandState];
                        double sum = sourceCount + operandCount;
                        if (sum > 0) {
                            double sourceDensity = applyCoefficientPower(sourceCount,
                                    transition.sourceCoefficient);
                            double operandDensity = applyCoefficientPower(operandCount,
                                    transition.operandCoefficient);
                            value = sourceDensity * operandDensity / Math.pow(sum,
                                    transition.sourceCoefficient + transition.operandCoefficient -
                                    1);
                            value = applyTransitionCommon(value, operandDensity, transition);
                        }
                    }
                }
                if (transition.mode == TransitionMode.REMOVING) {
                    if (sourceState != operandState && !sourceExternal) {
                        states[step][sourceState] -= value;
                        if (!allowNegative && states[step][sourceState] < 0) {
                            states[step][sourceState] = 0;
                        }
                    }
                }
                if (!resultExternal) {
                    states[step][resultState] += value * transition.resultCoefficient;
                    if (!allowNegative && states[step][resultState] < 0) {
                        states[step][resultState] = 0;
                    }
                }
                if (transition.mode != TransitionMode.RETAINING && !operandExternal) {
                    states[step][operandState] -= value;
                    if (!allowNegative && states[step][operandState] < 0) {
                        states[step][operandState] = 0;
                    }
                }
            }
            updateProgress(progress, step, stepsCount, 0.001, progressCallback);
        }
        if (resultCallback != null) {
            resultCallback.onResult(new Results(startPoint, states, stateNames));
        }
    }

    private static void calculateInternalHigherAccuracy(List<State> initialStates,
            List<Transition> transitions, int stepsCount, int startPoint, boolean allowNegative,
            ResultCallback resultCallback, ProgressCallback progressCallback) {
        int statesCount = initialStates.size();
        double[][] states = new double[stepsCount][statesCount];
        int[] stateIds = new int[statesCount];
        String[] stateNames = new String[statesCount];
        for (int i = 0; i < statesCount; i++) {
            State state = initialStates.get(i);
            states[0][i] = state.getCount();
            stateIds[i] = state.getId();
            stateNames[i] = state.getName();
        }
        int transitionsCount = transitions.size();
        TransitionValues[] transitionsInternal = new TransitionValues[transitionsCount];
        for (int i = 0; i < transitionsCount; i++) {
            transitionsInternal[i] = new TransitionValues(transitions.get(i));
        }
        double[] progress = new double[1];
        updateProgress(progress, 0, stepsCount, 0.0001, progressCallback);
        for (int step = 1; step < stepsCount; step++) {
            BigDecimal totalCount = decimalValue(0);
            for (int state = 0; state < statesCount; state++) {
                double count = states[step - 1][state];
                states[step][state] = count;
                totalCount = add(totalCount, decimalValue(count));
            }
            for (TransitionValues transition : transitionsInternal) {
                int sourceState = findState(transition.sourceState, stateIds);
                int operandState = findState(transition.operandState, stateIds);
                int resultState = findState(transition.resultState, stateIds);
                boolean sourceExternal = isStateExternal(sourceState);
                boolean operandExternal = isStateExternal(operandState);
                boolean resultExternal = isStateExternal(resultState);
                if (sourceExternal && operandExternal) {
                    continue;
                }
                int sourceIndex = delay(step - 1, transition.sourceDelay);
                int operandIndex = delay(step - 1, transition.operandDelay);
                BigDecimal value = decimalValue(0);
                if (transition.type == TransitionType.LINEAR) {
                    if (sourceExternal) {
                        BigDecimal operandDensity = applyCoefficientLinear(
                                decimalValue(states[operandIndex][operandState]),
                                transition.operandCoefficient);
                        value = multiply(operandDensity, decimalValue(transition.probability));
                        if (transition.mode == TransitionMode.RESIDUAL) {
                            value = subtract(operandDensity, value);
                        }
                    } else if (operandExternal) {
                        value = multiply(applyCoefficientLinear(
                                decimalValue(states[sourceIndex][sourceState]),
                                transition.sourceCoefficient),
                                decimalValue(transition.probability));
                    } else {
                        BigDecimal sourceDensity = applyCoefficientLinear(
                                decimalValue(states[sourceIndex][sourceState]),
                                transition.sourceCoefficient);
                        BigDecimal operandDensity = applyCoefficientLinear(
                                decimalValue(states[operandIndex][operandState]),
                                transition.operandCoefficient);
                        value = applyTransitionCommon(minimum(sourceDensity, operandDensity),
                                operandDensity, transition);
                    }
                } else if (transition.type == TransitionType.SOLUTE) {
                    if (compare(totalCount, decimalValue(0)) > 0) {
                        if (sourceExternal) {
                            BigDecimal operandDensity = applyCoefficientPower(
                                    decimalValue(states[operandIndex][operandState]),
                                    transition.operandCoefficient);
                            value = operandDensity;
                            if (transition.operandCoefficient > 1) {
                                value = divide(value,
                                        power(totalCount, transition.operandCoefficient - 1));
                            }
                            value = applyTransitionCommon(value, operandDensity, transition);
                        } else if (operandExternal) {
                            value = applyCoefficientPower(
                                    decimalValue(states[sourceIndex][sourceState]),
                                    transition.sourceCoefficient);
                            if (transition.sourceCoefficient > 1) {
                                value = divide(value,
                                        power(totalCount, transition.sourceCoefficient - 1));
                            }
                            value = multiply(value, decimalValue(transition.probability));
                        } else {
                            BigDecimal sourceDensity = applyCoefficientPower(
                                    decimalValue(states[sourceIndex][sourceState]),
                                    transition.sourceCoefficient);
                            BigDecimal operandDensity = applyCoefficientPower(
                                    decimalValue(states[operandIndex][operandState]),
                                    transition.operandCoefficient);
                            value = divide(multiply(sourceDensity, operandDensity),
                                    power(totalCount, transition.sourceCoefficient +
                                                      transition.operandCoefficient - 1));
                            if (sourceState == operandState) {
                                value = divide(value, decimalValue(2));
                            }
                            value = applyTransitionCommon(value, operandDensity, transition);
                        }
                    }
                } else if (transition.type == TransitionType.BLEND) {
                    if (sourceExternal) {
                        BigDecimal operandCount = decimalValue(states[operandIndex][operandState]);
                        if (compare(operandCount, decimalValue(0)) > 0) {
                            BigDecimal operandDensity = applyCoefficientPower(operandCount,
                                    transition.operandCoefficient);
                            value = operandDensity;
                            if (transition.operandCoefficient > 1) {
                                value = divide(value,
                                        power(operandCount, transition.operandCoefficient - 1));
                            }
                            value = applyTransitionCommon(value, operandDensity, transition);
                        }
                    } else if (operandExternal) {
                        BigDecimal sourceCount = decimalValue(states[sourceIndex][sourceState]);
                        if (compare(sourceCount, decimalValue(0)) > 0) {
                            value = applyCoefficientPower(sourceCount,
                                    transition.sourceCoefficient);
                            if (transition.sourceCoefficient > 1) {
                                value = divide(value,
                                        power(sourceCount, transition.sourceCoefficient - 1));
                            }
                            value = multiply(value, decimalValue(transition.probability));
                        }
                    } else {
                        BigDecimal sourceCount = decimalValue(states[sourceIndex][sourceState]);
                        BigDecimal operandCount = decimalValue(states[operandIndex][operandState]);
                        BigDecimal sum = add(sourceCount, operandCount);
                        if (compare(sum, decimalValue(0)) > 0) {
                            BigDecimal sourceDensity = applyCoefficientPower(sourceCount,
                                    transition.sourceCoefficient);
                            BigDecimal operandDensity = applyCoefficientPower(operandCount,
                                    transition.operandCoefficient);
                            value = divide(multiply(sourceDensity, operandDensity), power(sum,
                                    transition.sourceCoefficient + transition.operandCoefficient -
                                    1));
                            value = applyTransitionCommon(value, operandDensity, transition);
                        }
                    }
                }
                if (transition.mode == TransitionMode.REMOVING) {
                    if (sourceState != operandState && !sourceExternal) {
                        states[step][sourceState] = doubleValue(
                                subtract(decimalValue(states[step][sourceState]), value));
                        if (!allowNegative && states[step][sourceState] < 0) {
                            states[step][sourceState] = 0;
                        }
                    }
                }
                if (!resultExternal) {
                    states[step][resultState] = doubleValue(
                            add(decimalValue(states[step][resultState]),
                                    multiply(value, decimalValue(transition.resultCoefficient))));
                    if (!allowNegative && states[step][resultState] < 0) {
                        states[step][resultState] = 0;
                    }
                }
                if (transition.mode != TransitionMode.RETAINING && !operandExternal) {
                    states[step][operandState] =
                            doubleValue(subtract(decimalValue(states[step][operandState]), value));
                    if (!allowNegative && states[step][operandState] < 0) {
                        states[step][operandState] = 0;
                    }
                }
            }
            updateProgress(progress, step, stepsCount, 0.0001, progressCallback);
        }
        if (resultCallback != null) {
            resultCallback.onResult(new Results(startPoint, states, stateNames));
        }
    }

    public static void calculate(List<State> initialStates, List<Transition> transitions,
            int stepsCount, int startPoint, boolean higherAccuracy, boolean allowNegative,
            ResultCallback resultCallback, ProgressCallback progressCallback) {
        Thread thread = new Thread(
                new CalculateAction(initialStates, transitions, stepsCount, startPoint,
                        higherAccuracy, allowNegative, resultCallback, progressCallback),
                "Population Application Calculation Thread");
        thread.setUncaughtExceptionHandler(Utils.UNCAUGHT_EXCEPTION_HANDLER);
        thread.setDaemon(true);
        thread.start();
    }

    public static Results calculate(List<State> initialStates, List<Transition> transitions,
            int stepsCount, int startPoint, boolean higherAccuracy, boolean allowNegative) {
        Results[] resultsHolder = new Results[1];
        new CalculateAction(initialStates, transitions, stepsCount, startPoint, higherAccuracy,
                allowNegative, results -> resultsHolder[0] = results, null).run();
        return resultsHolder[0];
    }

    private static class CalculateAction implements Runnable {
        private final List<State> mInitialStates;
        private final List<Transition> mTransitions;
        private final int mStepsCount;
        private final int mStartPoint;
        private final boolean mHigherAccuracy;
        private final boolean mAllowNegative;
        private final ResultCallback mResultCallback;
        private final ProgressCallback mProgressCallback;

        private CalculateAction(List<State> initialStates, List<Transition> transitions,
                int stepsCount, int startPoint, boolean higherAccuracy, boolean allowNegative,
                ResultCallback resultCallback, ProgressCallback progressCallback) {
            mInitialStates = initialStates;
            mTransitions = transitions;
            mStepsCount = stepsCount;
            mStartPoint = startPoint;
            mHigherAccuracy = higherAccuracy;
            mAllowNegative = allowNegative;
            mResultCallback = resultCallback;
            mProgressCallback = progressCallback;
        }

        @Override
        public void run() {
            if (mHigherAccuracy) {
                calculateInternalHigherAccuracy(mInitialStates, mTransitions, mStepsCount,
                        mStartPoint, mAllowNegative, mResultCallback, mProgressCallback);
            } else {
                calculateInternal(mInitialStates, mTransitions, mStepsCount, mStartPoint,
                        mAllowNegative, mResultCallback, mProgressCallback);
            }
        }
    }

    public static class TransitionValues {
        public final int sourceState; // Состояние - источник
        public final int sourceCoefficient; // Коэффициент источника
        public final int sourceDelay; // Задержка источника
        public final int operandState; // Состояние - операнд
        public final int operandCoefficient; // Коэффициент операнда
        public final int operandDelay; // Задержка операнда
        public final int resultState; // Состояние - результат
        public final double resultCoefficient; // Коэффициент результата
        public final double probability; // Вероятность перехода
        public final int type; // Тип перехода
        public final int mode; // Вид перехода

        private TransitionValues(Transition transition) {
            sourceCoefficient = transition.getSourceCoefficient();
            sourceState = transition.getSourceState();
            sourceDelay = transition.getSourceDelay();
            operandCoefficient = transition.getOperandCoefficient();
            operandState = transition.getOperandState();
            operandDelay = transition.getOperandDelay();
            resultCoefficient = transition.getResultCoefficient();
            resultState = transition.getResultState();
            probability = transition.getProbability();
            mode = transition.getMode();
            type = transition.getType();
        }
    }

    public static class Results {
        private final int mStartPoint;
        private final ArrayList<String> mDataNames;
        private final ArrayList<Result> mTableData;
        private ArrayList<XYChart.Series<Number, Number>> mChartData;

        private Results(int startPoint, double[][] states, String[] stateNames) {
            mStartPoint = startPoint;
            mDataNames = new ArrayList<>(stateNames.length);
            Collections.addAll(mDataNames, stateNames);
            mTableData = new ArrayList<>(states.length);
            for (int i = 0; i < states.length; i++) {
                mTableData.add(new Result(states[i], i + mStartPoint));
            }
            mChartData = new ArrayList<>(stateNames.length);
            for (int i = 0; i < stateNames.length; i++) {
                ObservableList<XYChart.Data<Number, Number>> data =
                        FXCollections.observableList(new ArrayList<>(states.length));
                for (int j = 0; j < states.length; j++) {
                    data.add(new XYChart.Data<>(j + mStartPoint, states[j][i]));
                }
                XYChart.Series<Number, Number> series = new XYChart.Series<>(stateNames[i], data);
                mChartData.add(series);
            }
        }

        public int getStartPoint() {
            return mStartPoint;
        }

        public ArrayList<String> getDataNames() {
            return mDataNames;
        }

        public ArrayList<Result> getTableData() {
            return mTableData;
        }

        public ArrayList<XYChart.Series<Number, Number>> getChartData(boolean clearReference) {
            ArrayList<XYChart.Series<Number, Number>> chartData = mChartData;
            if (clearReference) {
                mChartData = null;
            }
            return chartData;
        }
    }

    public static class Result {
        private final ArrayList<DoubleProperty> mValues;
        private final IntegerProperty mNumber;

        private Result(double[] states, int number) {
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

    public interface ResultCallback {
        void onResult(Results results);
    }

    public interface ProgressCallback {
        void onProgressUpdate(double progress);
    }
}