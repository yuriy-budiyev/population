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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

public class Calculator {
    private static final double NORMAL_ACCURACY_PROGRESS_THRESHOLD = 0.005;
    private static final double HIGHER_ACCURACY_PROGRESS_THRESHOLD = 0.00005;
    /**
     * Количество десяничных знаков после разделителя в вещественных числах
     * в режиме повышенной точности
     */
    public static final int SCALE = Double.MAX_EXPONENT + 1;
    private final int mStepsCount;
    private final int mStartPoint;
    private final int mStatesCount;
    private final double[][] mStates;
    private final boolean mAllowNegative;
    private final int[] mStateIds;
    private final String[] mStateNames;
    private final TransitionValues[] mTransitions;
    private final ExecutorService mExecutor;
    private final ResultCallback mResultCallback;
    private final ProgressCallback mProgressCallback;
    private final boolean mHigherAccuracy;
    private final boolean mParallel;
    private final boolean mPrepareResultsTableData;
    private final boolean mPrepareResultsChartData;
    private double mProgress;

    private Calculator(List<State> initialStates, List<Transition> transitions, int startPoint,
            int stepsCount, boolean allowNegative, boolean higherAccuracy, boolean parallel,
            boolean prepareResultsTableData, boolean prepareResultsChartData,
            ResultCallback resultCallback, ProgressCallback progressCallback) {
        mStepsCount = stepsCount;
        mStartPoint = startPoint;
        mStatesCount = initialStates.size();
        mAllowNegative = allowNegative;
        mHigherAccuracy = higherAccuracy;
        mParallel = parallel;
        mPrepareResultsTableData = prepareResultsTableData;
        mPrepareResultsChartData = prepareResultsChartData;
        mResultCallback = resultCallback;
        mProgressCallback = progressCallback;
        mStates = new double[mStepsCount][mStatesCount];
        mStateIds = new int[mStatesCount];
        mStateNames = new String[mStatesCount];
        for (int i = 0; i < mStatesCount; i++) {
            State state = initialStates.get(i);
            mStates[0][i] = state.getCount();
            mStateIds[i] = state.getId();
            mStateNames[i] = state.getName();
        }
        int transitionsCount = transitions.size();
        mTransitions = new TransitionValues[transitionsCount];
        for (int i = 0; i < transitionsCount; i++) {
            mTransitions[i] = new TransitionValues(transitions.get(i));
        }
        if (mParallel) {
            mExecutor = Executors
                    .newFixedThreadPool(Runtime.getRuntime().availableProcessors(), runnable -> {
                        Thread thread = new Thread(runnable, "Population transition thread");
                        thread.setUncaughtExceptionHandler(Utils.UNCAUGHT_EXCEPTION_HANDLER);
                        thread.setDaemon(true);
                        return thread;
                    });
        } else {
            mExecutor = null;
        }
    }

    private double getState(int step, int state) {
        synchronized (mStates) {
            return mStates[step][state];
        }
    }

    private void setState(int step, int state, double value) {
        synchronized (mStates) {
            mStates[step][state] = value;
        }
    }

    private void checkStateNegativeness(int step, int state) {
        if (!mAllowNegative) {
            synchronized (mStates) {
                if (mStates[step][state] < 0) {
                    mStates[step][state] = 0;
                }
            }
        }
    }

    private void incrementState(int step, int state, double value) {
        synchronized (mStates) {
            mStates[step][state] += value;
        }
    }

    private void incrementState(int step, int state, BigDecimal value) {
        synchronized (mStates) {
            mStates[step][state] = doubleValue(decimalValue(mStates[step][state]).add(value));
        }
    }

    private void decrementState(int step, int state, double value) {
        incrementState(step, state, -value);
    }

    private void decrementState(int step, int state, BigDecimal value) {
        synchronized (mStates) {
            mStates[step][state] = doubleValue(decimalValue(mStates[step][state]).subtract(value));
        }
    }

    /**
     * Поиск позиции состояния
     *
     * @param id идентификатор состояния
     * @return позиция состояния
     */
    private int findState(int id) {
        if (id == State.EXTERNAL) {
            return State.EXTERNAL;
        }
        for (int i = 0; i < mStateIds.length; i++) {
            if (mStateIds[i] == id) {
                return i;
            }
        }
        return -1;
    }

    private void callbackResults(Results results) {
        ResultCallback resultCallback = mResultCallback;
        if (resultCallback != null) {
            resultCallback.onResult(results);
        }
    }

    private void updateProgress(int step, double threshold) {
        if (mProgressCallback != null) {
            final double progress;
            boolean needUpdate;
            if (step == 0 || mStepsCount == 0) {
                progress = 0;
                needUpdate = true;
            } else if (step == mStepsCount - 1 || mStepsCount == 1) {
                progress = 1;
                needUpdate = true;
            } else {
                progress = (double) step / (double) (mStepsCount - 1);
                needUpdate = progress - mProgress > threshold;
            }
            if (needUpdate) {
                mProgress = progress;
                mProgressCallback.onProgressUpdate(progress);
            }
        }
    }

    /**
     * Вычисления с обычной точностью
     */
    private Results calculateInternalNormalAccuracy() {
        updateProgress(0, NORMAL_ACCURACY_PROGRESS_THRESHOLD);
        for (int step = 1; step < mStepsCount; step++) {
            double totalCount = 0;
            for (int state = 0; state < mStatesCount; state++) {
                double count = getState(step - 1, state);
                setState(step, state, count);
                totalCount += count;
            }
            if (mParallel) {
                Future<?>[] futures = new Future<?>[mTransitions.length];
                for (int i = 0; i < mTransitions.length; i++) {
                    futures[i] = mExecutor
                            .submit(new TransitionActionNormalAccuracy(step, totalCount,
                                    mTransitions[i]));
                }
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException ignored) {
                    }
                }
            } else {
                for (TransitionValues transition : mTransitions) {
                    new TransitionActionNormalAccuracy(step, totalCount, transition).run();
                }
            }
            updateProgress(step, NORMAL_ACCURACY_PROGRESS_THRESHOLD);
        }
        return new Results(mStartPoint, mStates, mStateNames, mPrepareResultsTableData,
                mPrepareResultsChartData);
    }

    /**
     * Вычисления с повышенной точностью
     */
    private Results calculateInternalHigherAccuracy() {
        updateProgress(0, HIGHER_ACCURACY_PROGRESS_THRESHOLD);
        for (int step = 1; step < mStepsCount; step++) {
            BigDecimal totalCount = decimalValue(0);
            for (int state = 0; state < mStatesCount; state++) {
                double count = getState(step - 1, state);
                setState(step, state, count);
                totalCount = totalCount.add(decimalValue(count));
            }
            if (mParallel) {
                Future<?>[] futures = new Future<?>[mTransitions.length];
                for (int i = 0; i < mTransitions.length; i++) {
                    futures[i] = mExecutor
                            .submit(new TransitionActionHigherAccuracy(step, totalCount,
                                    mTransitions[i]));
                }
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException ignored) {
                    }
                }
            } else {
                for (TransitionValues transition : mTransitions) {
                    new TransitionActionHigherAccuracy(step, totalCount, transition).run();
                }
            }

            updateProgress(step, HIGHER_ACCURACY_PROGRESS_THRESHOLD);
        }
        return new Results(mStartPoint, mStates, mStateNames, mPrepareResultsTableData,
                mPrepareResultsChartData);
    }

    public Results calculateSync() {
        Results results;
        if (mHigherAccuracy) {
            results = calculateInternalHigherAccuracy();
        } else {
            results = calculateInternalNormalAccuracy();
        }
        callbackResults(results);
        return results;
    }

    public void calculateAsync() {
        Thread thread = new Thread(() -> {
            Results results;
            if (mHigherAccuracy) {
                results = calculateInternalHigherAccuracy();
            } else {
                results = calculateInternalNormalAccuracy();
            }
            callbackResults(results);
        }, "Population calculation thread");
        thread.setUncaughtExceptionHandler(Utils.UNCAUGHT_EXCEPTION_HANDLER);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Применение задержки
     *
     * @param step  шаг
     * @param delay задержка
     * @return шаг с задержкой
     */
    private static int delay(int step, int delay) {
        if (step > delay) {
            return step - delay;
        } else {
            return 0;
        }
    }

    /**
     * Применение степенного коэффициента
     */
    private static double applyCoefficientPower(double u, double coefficient) {
        if (coefficient <= 1) {
            return u;
        }
        return Math.pow(u, coefficient) / probabilisticFactorial(coefficient);
    }

    /**
     * Применение линейного коэффициента
     */
    private static double applyCoefficientLinear(double u, double coefficient) {
        if (coefficient <= 1) {
            return u;
        }
        return u / coefficient;
    }

    /**
     * Внешнее соснояние или нет
     */
    private static boolean isStateExternal(int stateId) {
        return stateId == State.EXTERNAL;
    }

    /**
     * Применение степенного коэффициента
     */
    private static BigDecimal applyCoefficientPower(BigDecimal u, double coefficient) {
        if (coefficient <= 1) {
            return u;
        }
        return divide(power(u, coefficient), probabilisticFactorialBig(coefficient));
    }

    /**
     * Применение линейного коэффициента
     */
    private static BigDecimal applyCoefficientLinear(BigDecimal u, double coefficient) {
        if (coefficient <= 1) {
            return u;
        }
        return divide(u, decimalValue(coefficient));
    }

    /**
     * Применение основных операций перехода
     */
    private static BigDecimal applyTransitionCommon(BigDecimal u, BigDecimal operandDensity,
            TransitionValues transition) {
        if (transition.mode == TransitionMode.INHIBITOR) {
            u = operandDensity.subtract(multiply(u, decimalValue(transition.operandCoefficient)));
        }
        u = multiply(u, decimalValue(transition.probability));
        if (transition.mode == TransitionMode.RESIDUAL) {
            u = operandDensity.subtract(multiply(u, decimalValue(transition.operandCoefficient)));
        }
        return u;
    }

    /**
     * Применение основных операций перехода
     */
    private static double applyTransitionCommon(double u, double operandDensity,
            TransitionValues transition) {
        if (transition.mode == TransitionMode.INHIBITOR) {
            u = operandDensity - u * transition.operandCoefficient;
        }
        u *= transition.probability;
        if (transition.mode == TransitionMode.RESIDUAL) {
            u = operandDensity - u * transition.operandCoefficient;
        }
        return u;
    }

    private static BigDecimal divide(BigDecimal u, BigDecimal v) {
        return divide(u, v, SCALE);
    }

    private static BigDecimal multiply(BigDecimal u, BigDecimal v) {
        return multiply(u, v, SCALE);
    }

    private static BigDecimal power(BigDecimal u, double exponent) {
        return power(u, exponent, SCALE);
    }

    private static BigDecimal exponent0(BigDecimal u, int scale) {
        BigDecimal a = BigDecimal.ONE;
        BigDecimal b = u;
        BigDecimal c;
        BigDecimal d = u.add(BigDecimal.ONE);
        for (int i = 2; ; i++) {
            b = multiply(b, u, scale);
            a = a.multiply(BigDecimal.valueOf(i));
            BigDecimal e = divide(b, a, scale);
            c = d;
            d = d.add(e);
            if (d.compareTo(c) == 0) {
                break;
            }
        }
        return d;
    }

    private static BigDecimal naturalLogarithm0(BigDecimal u, int scale) {
        int s = scale + 1;
        BigDecimal a = u;
        BigDecimal b;
        BigDecimal c = decimalValue(5).movePointLeft(s);
        for (; ; ) {
            BigDecimal d = exponent(u, s);
            b = d.subtract(a).divide(d, s, RoundingMode.DOWN);
            u = u.subtract(b);
            if (b.compareTo(c) <= 0) {
                break;
            }
        }
        return u.setScale(scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Преобразование int в BigDecimal
     *
     * @param u исходное значение типа int
     * @return результат типа BigDecimal
     */
    public static BigDecimal decimalValue(int u) {
        return new BigDecimal(u);
    }

    /**
     * Преобразование long в BigDecimal
     *
     * @param u исходное значение типа long
     * @return результат типа BigDecimal
     */
    public static BigDecimal decimalValue(long u) {
        return new BigDecimal(u);
    }

    /**
     * Преобразование double в BigDecimal
     *
     * @param u исходное значение типа double
     * @return результат типа BigDecimal
     */
    public static BigDecimal decimalValue(double u) {
        return new BigDecimal(u);
    }

    /**
     * Преобразование BigDecimal в double
     *
     * @param u исходное значение типа BigDecimal
     * @return результат
     */
    public static double doubleValue(BigDecimal u) {
        return u.doubleValue();
    }

    /**
     * Вероятностный факториал.
     * Нахождение факториала вещественного числа как математическое ожидание
     * от факториалов двух соседних целых.
     *
     * @param u исходное значение
     * @return результат
     */
    public static double probabilisticFactorial(double u) {
        double result = 1;
        double r = u % 1;
        if (r > 0) {
            double v = Math.floor(u);
            for (double i = 2; i <= v; i++) {
                result *= i;
            }
            result = result * (1 - r) + result * (v + 1) * r;
        } else {
            for (double i = 2; i <= u; i++) {
                result *= i;
            }
        }
        return result;
    }

    /**
     * Вероятностный факториал.
     * Нахождение факториала вещественного числа как математическое ожидание
     * от факториалов двух соседних целых.
     *
     * @param u     исходное значение
     * @param scale количество знаков в дробной части результата
     * @return результат
     */
    public static BigDecimal probabilisticFactorialBig(double u, int scale) {
        BigDecimal result = BigDecimal.ONE;
        double r = u % 1;
        if (r > 0) {
            double v = Math.floor(u);
            for (double i = 2; i <= v; i++) {
                result = result.multiply(decimalValue(i));
            }
            result = result.multiply(BigDecimal.ONE.subtract(decimalValue(r)))
                    .add(result.multiply(decimalValue(v).add(BigDecimal.ONE))
                            .multiply(decimalValue(r)));
        } else {
            for (double i = 2; i <= u; i++) {
                result = result.multiply(decimalValue(i));
            }
        }
        return result.setScale(scale, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal probabilisticFactorialBig(double u) {
        return probabilisticFactorialBig(u, SCALE);
    }

    /**
     * Деление
     *
     * @param u     делимое
     * @param v     делитель
     * @param scale количество знаков в дробной части результата
     * @return частное
     */
    public static BigDecimal divide(BigDecimal u, BigDecimal v, int scale) {
        return u.divide(v, scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Умножение
     *
     * @param u     множитель
     * @param v     множитель
     * @param scale количество знаков в дробной части результата
     * @return произведение
     */
    public static BigDecimal multiply(BigDecimal u, BigDecimal v, int scale) {
        return u.multiply(v).setScale(scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Возведение в целочисленную степень
     *
     * @param u        основание
     * @param exponent показатель
     * @param scale    количество знаков в дробной части результата
     * @return результат
     */
    public static BigDecimal power(BigDecimal u, long exponent, int scale) {
        if (exponent < 0) {
            return BigDecimal.ONE.divide(power(u, -exponent, scale), scale, RoundingMode.HALF_EVEN);
        }
        BigDecimal p = BigDecimal.ONE;
        for (; exponent > 0; exponent >>= 1) {
            if ((exponent & 1) == 1) {
                p = p.multiply(u).setScale(scale, RoundingMode.HALF_EVEN);
            }
            u = u.multiply(u).setScale(scale, RoundingMode.HALF_EVEN);
        }
        return p;
    }

    /**
     * Возведение в вещественную степень
     *
     * @param u        основание
     * @param exponent показатель
     * @param scale    количество знаков в дробной части результата
     * @return результат
     */
    public static BigDecimal power(BigDecimal u, double exponent, int scale) {
        if (exponent % 1 == 0 && exponent <= Long.MAX_VALUE) {
            return power(u, (long) exponent, scale);
        }
        return exponent(decimalValue(exponent).multiply(naturalLogarithm(u, scale)), scale);
    }

    /**
     * Нахождение целочисленного корня
     *
     * @param u     исходное значение
     * @param index степень корня
     * @param scale количество знаков в дробной части результата
     * @return результат
     */
    public static BigDecimal root(BigDecimal u, long index, int scale) {
        int s = scale + 1;
        BigDecimal a = u;
        BigDecimal b = decimalValue(index);
        BigDecimal c = decimalValue(index - 1);
        BigDecimal d = decimalValue(5).movePointLeft(s);
        BigDecimal e;
        u = divide(u, b, scale);
        for (; ; ) {
            BigDecimal f = power(u, index - 1, s);
            BigDecimal g = multiply(u, f, s);
            BigDecimal h = a.add(c.multiply(g)).setScale(s, RoundingMode.HALF_EVEN);
            BigDecimal l = multiply(b, f, s);
            e = u;
            u = h.divide(l, s, RoundingMode.DOWN);
            if (u.subtract(e).abs().compareTo(d) <= 0) {
                break;
            }
        }
        return u;
    }

    /**
     * Возведение числа Эйлера в указанную степень
     *
     * @param u     исходное значение
     * @param scale количество знаков в дробной части результата
     * @return результат
     */
    public static BigDecimal exponent(BigDecimal u, int scale) {
        if (u.signum() == 0) {
            return BigDecimal.ONE;
        } else if (u.signum() == -1) {
            return divide(BigDecimal.ONE, exponent(u.negate(), scale));
        }
        BigDecimal a = u.setScale(0, RoundingMode.DOWN);
        if (a.signum() == 0) {
            return exponent0(u, scale);
        }
        BigDecimal b = u.subtract(a);
        BigDecimal c = BigDecimal.ONE.add(divide(b, a, scale));
        BigDecimal d = exponent0(c, scale);
        BigDecimal e = decimalValue(Long.MAX_VALUE);
        BigDecimal f = BigDecimal.ONE;
        for (; a.compareTo(e) >= 0; ) {
            f = multiply(f, power(d, Long.MAX_VALUE, scale), scale);
            a = a.subtract(e);
        }
        return multiply(f, power(d, a.longValue(), scale), scale);
    }

    /**
     * Нахождение натурального логарифма от указанного значения
     *
     * @param u     исходное значение
     * @param scale количество знаков в дробной части результата
     * @return результат
     */
    public static BigDecimal naturalLogarithm(BigDecimal u, int scale) {
        int a = u.toString().length() - u.scale() - 1;
        if (a < 3) {
            return naturalLogarithm0(u, scale);
        } else {
            BigDecimal b = root(u, a, scale);
            BigDecimal c = naturalLogarithm0(b, scale);
            return multiply(decimalValue(a), c, scale);
        }
    }

    /**
     * Интерполяция позиций в указанных границах
     *
     * @param start      начало
     * @param end        конец
     * @param resultSize резмер результата
     * @return результат
     */
    public static int[] interpolateIndexes(int start, int end, int resultSize) {
        int[] array = new int[resultSize];
        for (int i = 0; i < resultSize; ++i) {
            array[i] = (int) Math.round(interpolate(start, end, i / (double) resultSize));
        }
        return array;
    }

    /**
     * Линейная интерполяция
     */
    public static double interpolate(double u, double v, double f) {
        return u * (1D - f) + v * f;
    }

    /**
     * Вычислить (синхронно)
     *
     * @param initialStates           начальные состояния
     * @param transitions             переходы
     * @param startPoint              начало отсчёта
     * @param stepsCount              количество шагов
     * @param higherAccuracy          повышенная точность
     * @param allowNegative           разрешить отрицательные значания
     * @param parallel                вычислять переходы параллельно
     * @param prepareResultsTableData подготовить результат для вывода в табличном виде
     * @param prepareResultsChartData подготовить результат для вывода в графическом виде
     * @return результаты вычислений
     */
    public static Results calculateSync(List<State> initialStates, List<Transition> transitions,
            int startPoint, int stepsCount, boolean higherAccuracy, boolean allowNegative,
            boolean parallel, boolean prepareResultsTableData, boolean prepareResultsChartData) {
        return new Calculator(initialStates, transitions, startPoint, stepsCount, allowNegative,
                higherAccuracy, parallel, prepareResultsTableData, prepareResultsChartData, null,
                null).calculateSync();
    }

    /**
     * Вычислить (асинхронно)
     *
     * @param initialStates           начальные состояния
     * @param transitions             переходы
     * @param startPoint              начало отсчёта
     * @param stepsCount              количество шагов
     * @param higherAccuracy          повышенная точность
     * @param allowNegative           разрешить отрицательные значания
     * @param parallel                вычислять переходы параллельно
     * @param prepareResultsTableData подготовить результат для вывода в табличном виде
     * @param prepareResultsChartData подготовить результат для вывода в графическом виде
     * @param resultCallback          обратный вызов результата
     * @param progressCallback        обратный вызов прогресса вычислений
     */
    public static void calculateAsync(List<State> initialStates, List<Transition> transitions,
            int startPoint, int stepsCount, boolean higherAccuracy, boolean allowNegative,
            boolean parallel, boolean prepareResultsTableData, boolean prepareResultsChartData,
            ResultCallback resultCallback, ProgressCallback progressCallback) {
        new Calculator(initialStates, transitions, startPoint, stepsCount, allowNegative,
                higherAccuracy, parallel, prepareResultsTableData, prepareResultsChartData,
                resultCallback, progressCallback).calculateAsync();
    }

    private class TransitionActionNormalAccuracy implements Runnable {
        private final int mStep;
        private final double mTotalCount;
        private final TransitionValues mTransition;

        private TransitionActionNormalAccuracy(int step, double totalCount,
                TransitionValues transition) {
            mStep = step;
            mTotalCount = totalCount;
            mTransition = transition;
        }

        @Override
        public void run() {
            int sourceState = findState(mTransition.sourceState);
            int operandState = findState(mTransition.operandState);
            int resultState = findState(mTransition.resultState);
            boolean sourceExternal = isStateExternal(sourceState);
            boolean operandExternal = isStateExternal(operandState);
            boolean resultExternal = isStateExternal(resultState);
            if (sourceExternal && operandExternal) {
                return;
            }
            int sourceIndex = delay(mStep - 1, mTransition.sourceDelay);
            int operandIndex = delay(mStep - 1, mTransition.operandDelay);
            double value = 0;
            if (mTransition.type == TransitionType.LINEAR) {
                if (sourceExternal) {
                    double operandDensity =
                            applyCoefficientLinear(getState(operandIndex, operandState),
                                    mTransition.operandCoefficient);
                    value = operandDensity * mTransition.probability;
                    if (mTransition.mode == TransitionMode.RESIDUAL) {
                        value = operandDensity - value * mTransition.operandCoefficient;
                    }
                } else if (operandExternal) {
                    value = applyCoefficientLinear(getState(sourceIndex, sourceState),
                            mTransition.sourceCoefficient) * mTransition.probability;
                } else if (sourceState == operandState) {
                    double density = applyCoefficientLinear(getState(sourceIndex, sourceState),
                            mTransition.sourceCoefficient + mTransition.operandCoefficient - 1);
                    value = applyTransitionCommon(density, density, mTransition);
                } else {
                    double sourceDensity =
                            applyCoefficientLinear(getState(sourceIndex, sourceState),
                                    mTransition.sourceCoefficient);
                    double operandDensity =
                            applyCoefficientLinear(getState(operandIndex, operandState),
                                    mTransition.operandCoefficient);
                    value = applyTransitionCommon(Math.min(sourceDensity, operandDensity),
                            operandDensity, mTransition);
                }
            } else if (mTransition.type == TransitionType.SOLUTE) {
                if (mTotalCount > 0) {
                    if (sourceExternal) {
                        double operandDensity =
                                applyCoefficientPower(getState(operandIndex, operandState),
                                        mTransition.operandCoefficient);
                        value = operandDensity;
                        if (mTransition.operandCoefficient > 1) {
                            value /= Math.pow(mTotalCount, mTransition.operandCoefficient - 1);
                        }
                        value = applyTransitionCommon(value, operandDensity, mTransition);
                    } else if (operandExternal) {
                        value = applyCoefficientPower(getState(sourceIndex, sourceState),
                                mTransition.sourceCoefficient);
                        if (mTransition.sourceCoefficient > 1) {
                            value /= Math.pow(mTotalCount, mTransition.sourceCoefficient - 1);
                        }
                        value *= mTransition.probability;
                    } else if (sourceState == operandState) {
                        double density = applyCoefficientPower(getState(sourceIndex, sourceState),
                                mTransition.sourceCoefficient + mTransition.operandCoefficient);
                        value = density / Math.pow(mTotalCount,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient - 1);
                        value = applyTransitionCommon(value, density, mTransition);
                    } else {
                        double sourceDensity =
                                applyCoefficientPower(getState(sourceIndex, sourceState),
                                        mTransition.sourceCoefficient);
                        double operandDensity =
                                applyCoefficientPower(getState(operandIndex, operandState),
                                        mTransition.operandCoefficient);
                        value = sourceDensity * operandDensity / Math.pow(mTotalCount,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient - 1);
                        value = applyTransitionCommon(value, operandDensity, mTransition);
                    }
                }
            } else if (mTransition.type == TransitionType.BLEND) {
                if (sourceExternal) {
                    double operandCount = getState(operandIndex, operandState);
                    if (operandCount > 0) {
                        double operandDensity =
                                applyCoefficientPower(operandCount, mTransition.operandCoefficient);
                        value = operandDensity;
                        if (mTransition.operandCoefficient > 1) {
                            value /= Math.pow(operandCount, mTransition.operandCoefficient - 1);
                        }
                        value = applyTransitionCommon(value, operandDensity, mTransition);
                    }
                } else if (operandExternal) {
                    double sourceCount = getState(sourceIndex, sourceState);
                    if (sourceCount > 0) {
                        value = applyCoefficientPower(sourceCount, mTransition.sourceCoefficient);
                        if (mTransition.sourceCoefficient > 1) {
                            value /= Math.pow(sourceCount, mTransition.sourceCoefficient - 1);
                        }
                        value *= mTransition.probability;
                    }
                } else if (sourceState == operandState) {
                    double count = getState(sourceIndex, sourceState);
                    if (count > 0) {
                        double density = applyCoefficientPower(count,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient);
                        value = density / Math.pow(count,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient - 1);
                        value = applyTransitionCommon(value, density, mTransition);
                    }
                } else {
                    double sourceCount = getState(sourceIndex, sourceState);
                    double operandCount = getState(operandIndex, operandState);
                    double sum = sourceCount + operandCount;
                    if (sum > 0) {
                        double sourceDensity =
                                applyCoefficientPower(sourceCount, mTransition.sourceCoefficient);
                        double operandDensity =
                                applyCoefficientPower(operandCount, mTransition.operandCoefficient);
                        value = sourceDensity * operandDensity / Math.pow(sum,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient - 1);
                        value = applyTransitionCommon(value, operandDensity, mTransition);
                    }
                }
            }
            if (!sourceExternal && mTransition.mode == TransitionMode.REMOVING) {
                decrementState(mStep, sourceState, value * mTransition.sourceCoefficient);
                checkStateNegativeness(mStep, sourceState);
            }
            if (!operandExternal) {
                if (mTransition.mode == TransitionMode.INHIBITOR ||
                    mTransition.mode == TransitionMode.RESIDUAL) {
                    decrementState(mStep, operandState, value);
                } else if (mTransition.mode != TransitionMode.RETAINING) {
                    decrementState(mStep, operandState, value * mTransition.operandCoefficient);
                }
                checkStateNegativeness(mStep, operandState);
            }
            if (!resultExternal) {
                incrementState(mStep, resultState, value * mTransition.resultCoefficient);
                checkStateNegativeness(mStep, resultState);
            }
        }
    }

    private class TransitionActionHigherAccuracy implements Runnable {
        private final int mStep;
        private final BigDecimal mTotalCount;
        private final TransitionValues mTransition;

        private TransitionActionHigherAccuracy(int step, BigDecimal totalCount,
                TransitionValues transition) {
            mStep = step;
            mTotalCount = totalCount;
            mTransition = transition;
        }

        @Override
        public void run() {
            int sourceState = findState(mTransition.sourceState);
            int operandState = findState(mTransition.operandState);
            int resultState = findState(mTransition.resultState);
            boolean sourceExternal = isStateExternal(sourceState);
            boolean operandExternal = isStateExternal(operandState);
            boolean resultExternal = isStateExternal(resultState);
            if (sourceExternal && operandExternal) {
                return;
            }
            int sourceIndex = delay(mStep - 1, mTransition.sourceDelay);
            int operandIndex = delay(mStep - 1, mTransition.operandDelay);
            BigDecimal value = BigDecimal.ZERO;
            if (mTransition.type == TransitionType.LINEAR) {
                if (sourceExternal) {
                    BigDecimal operandDensity = applyCoefficientLinear(
                            decimalValue(getState(operandIndex, operandState)),
                            mTransition.operandCoefficient);
                    value = multiply(operandDensity, decimalValue(mTransition.probability));
                    if (mTransition.mode == TransitionMode.RESIDUAL) {
                        value = operandDensity.subtract(
                                multiply(value, decimalValue(mTransition.operandCoefficient)));
                    }
                } else if (operandExternal) {
                    value = multiply(
                            applyCoefficientLinear(decimalValue(getState(sourceIndex, sourceState)),
                                    mTransition.sourceCoefficient),
                            decimalValue(mTransition.probability));
                } else if (sourceState == operandState) {
                    BigDecimal density =
                            applyCoefficientLinear(decimalValue(getState(sourceIndex, sourceState)),
                                    mTransition.sourceCoefficient + mTransition.operandCoefficient -
                                    1);
                    value = applyTransitionCommon(density, density, mTransition);
                } else {
                    BigDecimal sourceDensity =
                            applyCoefficientLinear(decimalValue(getState(sourceIndex, sourceState)),
                                    mTransition.sourceCoefficient);
                    BigDecimal operandDensity = applyCoefficientLinear(
                            decimalValue(getState(operandIndex, operandState)),
                            mTransition.operandCoefficient);
                    value = applyTransitionCommon(sourceDensity.min(operandDensity), operandDensity,
                            mTransition);
                }
            } else if (mTransition.type == TransitionType.SOLUTE) {
                if (mTotalCount.compareTo(BigDecimal.ZERO) > 0) {
                    if (sourceExternal) {
                        BigDecimal operandDensity = applyCoefficientPower(
                                decimalValue(getState(operandIndex, operandState)),
                                mTransition.operandCoefficient);
                        value = operandDensity;
                        if (mTransition.operandCoefficient > 1) {
                            value = divide(value,
                                    power(mTotalCount, mTransition.operandCoefficient - 1));
                        }
                        value = applyTransitionCommon(value, operandDensity, mTransition);
                    } else if (operandExternal) {
                        value = applyCoefficientPower(
                                decimalValue(getState(sourceIndex, sourceState)),
                                mTransition.sourceCoefficient);
                        if (mTransition.sourceCoefficient > 1) {
                            value = divide(value,
                                    power(mTotalCount, mTransition.sourceCoefficient - 1));
                        }
                        value = multiply(value, decimalValue(mTransition.probability));
                    } else if (sourceState == operandState) {
                        BigDecimal density = applyCoefficientPower(
                                decimalValue(getState(sourceIndex, sourceState)),
                                mTransition.sourceCoefficient + mTransition.operandCoefficient);
                        value = divide(density, power(mTotalCount,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient -
                                1));
                        value = applyTransitionCommon(value, density, mTransition);
                    } else {
                        BigDecimal sourceDensity = applyCoefficientPower(
                                decimalValue(getState(sourceIndex, sourceState)),
                                mTransition.sourceCoefficient);
                        BigDecimal operandDensity = applyCoefficientPower(
                                decimalValue(getState(operandIndex, operandState)),
                                mTransition.operandCoefficient);
                        value = divide(multiply(sourceDensity, operandDensity), power(mTotalCount,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient -
                                1));
                        value = applyTransitionCommon(value, operandDensity, mTransition);
                    }
                }
            } else if (mTransition.type == TransitionType.BLEND) {
                if (sourceExternal) {
                    BigDecimal operandCount = decimalValue(getState(operandIndex, operandState));
                    if (operandCount.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal operandDensity =
                                applyCoefficientPower(operandCount, mTransition.operandCoefficient);
                        value = operandDensity;
                        if (mTransition.operandCoefficient > 1) {
                            value = divide(value,
                                    power(operandCount, mTransition.operandCoefficient - 1));
                        }
                        value = applyTransitionCommon(value, operandDensity, mTransition);
                    }
                } else if (operandExternal) {
                    BigDecimal sourceCount = decimalValue(getState(sourceIndex, sourceState));
                    if (sourceCount.compareTo(BigDecimal.ZERO) > 0) {
                        value = applyCoefficientPower(sourceCount, mTransition.sourceCoefficient);
                        if (mTransition.sourceCoefficient > 1) {
                            value = divide(value,
                                    power(sourceCount, mTransition.sourceCoefficient - 1));
                        }
                        value = multiply(value, decimalValue(mTransition.probability));
                    }
                } else if (sourceState == operandState) {
                    BigDecimal count = decimalValue(getState(sourceIndex, sourceState));
                    if (count.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal density = applyCoefficientPower(count,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient);
                        value = divide(density, power(count,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient -
                                1));
                        value = applyTransitionCommon(value, density, mTransition);
                    }
                } else {
                    BigDecimal sourceCount = decimalValue(getState(sourceIndex, sourceState));
                    BigDecimal operandCount = decimalValue(getState(operandIndex, operandState));
                    BigDecimal sum = sourceCount.add(operandCount);
                    if (sum.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal sourceDensity =
                                applyCoefficientPower(sourceCount, mTransition.sourceCoefficient);
                        BigDecimal operandDensity =
                                applyCoefficientPower(operandCount, mTransition.operandCoefficient);
                        value = divide(multiply(sourceDensity, operandDensity), power(sum,
                                mTransition.sourceCoefficient + mTransition.operandCoefficient -
                                1));
                        value = applyTransitionCommon(value, operandDensity, mTransition);
                    }
                }
            }
            if (!sourceExternal && mTransition.mode == TransitionMode.REMOVING) {
                decrementState(mStep, sourceState,
                        multiply(value, decimalValue(mTransition.sourceCoefficient)));
                checkStateNegativeness(mStep, sourceState);
            }
            if (!operandExternal) {
                if (mTransition.mode == TransitionMode.INHIBITOR ||
                    mTransition.mode == TransitionMode.RESIDUAL) {
                    decrementState(mStep, operandState, value);
                } else if (mTransition.mode != TransitionMode.RETAINING) {
                    decrementState(mStep, operandState,
                            multiply(value, decimalValue(mTransition.operandCoefficient)));
                }
                checkStateNegativeness(mStep, operandState);
            }
            if (!resultExternal) {
                incrementState(mStep, resultState,
                        multiply(value, decimalValue(mTransition.resultCoefficient)));
                checkStateNegativeness(mStep, resultState);
            }
        }
    }

    public static class TransitionValues {
        public final int sourceState; // Состояние - источник
        public final double sourceCoefficient; // Коэффициент источника
        public final int sourceDelay; // Задержка источника
        public final int operandState; // Состояние - операнд
        public final double operandCoefficient; // Коэффициент операнда
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
        private final ArrayList<XYChart.Series<Number, Number>> mChartData;

        private Results(int startPoint, double[][] states, String[] stateNames,
                boolean prepareResultsTableData, boolean prepareResultsChartData) {
            mStartPoint = startPoint;
            mDataNames = new ArrayList<>(stateNames.length);
            Collections.addAll(mDataNames, stateNames);
            if (prepareResultsTableData) {
                mTableData = new ArrayList<>(states.length);
                for (int i = 0; i < states.length; i++) {
                    mTableData.add(new Result(states[i], i + mStartPoint));
                }
            } else {
                mTableData = null;
            }
            if (prepareResultsChartData) {
                mChartData = new ArrayList<>(stateNames.length);
                for (int i = 0; i < stateNames.length; i++) {
                    ObservableList<XYChart.Data<Number, Number>> data =
                            FXCollections.observableList(new ArrayList<>(states.length));
                    for (int j = 0; j < states.length; j++) {
                        data.add(new XYChart.Data<>(j + mStartPoint, states[j][i]));
                    }
                    XYChart.Series<Number, Number> series =
                            new XYChart.Series<>(stateNames[i], data);
                    mChartData.add(series);
                }
            } else {
                mChartData = null;
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

        public ArrayList<XYChart.Series<Number, Number>> getChartData() {
            return mChartData;
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
        /**
         * Вызывается при завершении вычислений
         *
         * @param results результаты
         */
        void onResult(Results results);
    }

    public interface ProgressCallback {
        /**
         * Вызывается при обновлении прогресса вычислений
         *
         * @param progress прогресс (0 - 1)
         */
        void onProgressUpdate(double progress);
    }
}
