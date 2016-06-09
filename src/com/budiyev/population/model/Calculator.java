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
    public static final int HIGHER_ACCURACY_SCALE = 384;
    private final int mStepsCount; // Количество шагов
    private final int mStartPoint; // Начало отсчёта
    private final int mStatesCount; // Количество состояний
    private final double[][] mStates; // Состояния
    private final boolean mAllowNegative; // Разрешить отрицательные значения
    private final int[] mStateIds; // Идентификаторы состояний
    private final String[] mStateNames; // Имена состояний
    private final TransitionValues[] mTransitions; // Переходы
    private final ExecutorService mExecutor; // Исполнитель (для параллельного режима)
    private final ResultCallback mResultCallback; // Обратный вызов результата
    private final ProgressCallback mProgressCallback; // Обратный вызов прогресса вычислений
    private final boolean mHigherAccuracy; // Повышенная точность
    private final boolean mParallel; // Параллельно
    private final boolean mPrepareResultsTableData; // Подготовить результат в табличном виде
    private final boolean mPrepareResultsChartData; // Подготовить результат в графическом виде
    private double mProgress; // Прогресс вычислений

    /**
     * Вычислитель
     *
     * @param initialStates           начальные состояния
     * @param transitions             переходы
     * @param startPoint              начало отсчёта
     * @param stepsCount              количество шагов
     * @param allowNegative           разрешить отрицательные значения
     * @param higherAccuracy          повышенная точность
     * @param parallel                паралельно
     * @param prepareResultsTableData подготовить результат в табличном виде
     * @param prepareResultsChartData подготовить результат в графическом виде
     * @param resultCallback          обратный вызов результата
     * @param progressCallback        обратный вызов прогресса вычислений
     */
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
            mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                    Utils.THREAD_FACTORY);
        } else {
            mExecutor = null;
        }
    }

    /**
     * Значение состояния в шаге
     *
     * @param step  номер шага
     * @param state идентификатор состояния
     * @return значение состояния
     */
    private double getState(int step, int state) {
        synchronized (mStates) {
            return mStates[step][state];
        }
    }

    /**
     * Установка значения заданного состояния для заднного шага
     *
     * @param step  шаг
     * @param state идентификатор состояния
     * @param value значение
     */
    private void setState(int step, int state, double value) {
        synchronized (mStates) {
            mStates[step][state] = value;
        }
    }

    /**
     * Проверка состояния на отрицательность и корректировка значения, если это необходимо
     *
     * @param step  номер шага
     * @param state идентификатор состояния
     */
    private void checkStateNegativeness(int step, int state) {
        if (!mAllowNegative) {
            synchronized (mStates) {
                if (mStates[step][state] < 0) {
                    mStates[step][state] = 0;
                }
            }
        }
    }

    /**
     * Увеличение значения заданного состояния на заданном шаге на заданное значение
     *
     * @param step  номер шага
     * @param state идентификатор состояния
     * @param value значение
     */
    private void incrementState(int step, int state, double value) {
        synchronized (mStates) {
            mStates[step][state] += value;
        }
    }

    /**
     * Увеличение значения заданного состояния на заданном шаге на заданное значение
     *
     * @param step  номер шага
     * @param state идентификатор состояния
     * @param value значение
     */
    private void incrementState(int step, int state, BigDecimal value) {
        synchronized (mStates) {
            mStates[step][state] = doubleValue(decimalValue(mStates[step][state]).add(value));
        }
    }

    /**
     * Уменьшение значения заданного состояния на заданном шаге на заданное значение
     *
     * @param step  номер шага
     * @param state идентификатор состояния
     * @param value значение
     */
    private void decrementState(int step, int state, double value) {
        incrementState(step, state, -value);
    }

    /**
     * Уменьшение значения заданного состояния на заданном шаге на заданное значение
     *
     * @param step  номер шага
     * @param state идентификатор состояния
     * @param value значение
     */
    private void decrementState(int step, int state, BigDecimal value) {
        synchronized (mStates) {
            mStates[step][state] = doubleValue(decimalValue(mStates[step][state]).subtract(value));
        }
    }

    /**
     * Поиск позиции состояния по идентификатору
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

    /**
     * Выполнение обратного вызова результата, если он задан
     *
     * @param results результаты
     */
    private void callbackResults(Results results) {
        ResultCallback resultCallback = mResultCallback;
        if (resultCallback != null) {
            resultCallback.onResult(results);
        }
    }

    /**
     * Выполнение обратного вызова прогресса вычислений, если это необходимо и он задан
     *
     * @param step      номер шага
     * @param threshold минимальное изменение прогресса, на которое стоит реагировать
     */
    private void callbackProgress(int step, double threshold) {
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
     * Вычисление с обычной точностью
     */
    private Results calculateNormalAccuracy() {
        callbackProgress(0, NORMAL_ACCURACY_PROGRESS_THRESHOLD);
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
                    transitionNormalAccuracy(step, totalCount, transition);
                }
            }
            callbackProgress(step, NORMAL_ACCURACY_PROGRESS_THRESHOLD);
        }
        return new Results(mStartPoint, mStates, mStateNames, mPrepareResultsTableData,
                mPrepareResultsChartData);
    }

    /**
     * Вычисление с повышенной точностью
     */
    private Results calculateHigherAccuracy() {
        callbackProgress(0, HIGHER_ACCURACY_PROGRESS_THRESHOLD);
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
                    transitionHigherAccuracy(step, totalCount, transition);
                }
            }
            callbackProgress(step, HIGHER_ACCURACY_PROGRESS_THRESHOLD);
        }
        return new Results(mStartPoint, mStates, mStateNames, mPrepareResultsTableData,
                mPrepareResultsChartData);
    }

    /**
     * Вычисление перехода с обычной точностью
     *
     * @param step       номер шага
     * @param totalCount общее количество автоматов на прошлом шаге
     * @param transition переход
     */
    private void transitionNormalAccuracy(int step, double totalCount,
            TransitionValues transition) {
        int sourceState = findState(transition.sourceState);
        int operandState = findState(transition.operandState);
        int resultState = findState(transition.resultState);
        boolean sourceExternal = isStateExternal(sourceState);
        boolean operandExternal = isStateExternal(operandState);
        boolean resultExternal = isStateExternal(resultState);
        if (sourceExternal && operandExternal) {
            return;
        }
        int sourceIndex = delay(step - 1, transition.sourceDelay);
        int operandIndex = delay(step - 1, transition.operandDelay);
        double value = 0;
        if (transition.type == TransitionType.LINEAR) {
            if (sourceExternal) {
                double operandDensity = applyCoefficientLinear(getState(operandIndex, operandState),
                        transition.operandCoefficient);
                value = operandDensity * transition.probability;
                if (transition.mode == TransitionMode.RESIDUAL) {
                    value = operandDensity - value * transition.operandCoefficient;
                }
            } else if (operandExternal) {
                value = applyCoefficientLinear(getState(sourceIndex, sourceState),
                        transition.sourceCoefficient) * transition.probability;
            } else if (sourceState == operandState) {
                double density = applyCoefficientLinear(getState(sourceIndex, sourceState),
                        transition.sourceCoefficient + transition.operandCoefficient - 1);
                value = applyTransitionCommon(density, density, transition);
            } else {
                double sourceDensity = applyCoefficientLinear(getState(sourceIndex, sourceState),
                        transition.sourceCoefficient);
                double operandDensity = applyCoefficientLinear(getState(operandIndex, operandState),
                        transition.operandCoefficient);
                value = applyTransitionCommon(Math.min(sourceDensity, operandDensity),
                        operandDensity, transition);
            }
        } else if (transition.type == TransitionType.SOLUTE) {
            if (totalCount > 0) {
                if (sourceExternal) {
                    double operandDensity =
                            applyCoefficientPower(getState(operandIndex, operandState),
                                    transition.operandCoefficient);
                    value = operandDensity;
                    if (transition.operandCoefficient > 1) {
                        value /= Math.pow(totalCount, transition.operandCoefficient - 1);
                    }
                    value = applyTransitionCommon(value, operandDensity, transition);
                } else if (operandExternal) {
                    value = applyCoefficientPower(getState(sourceIndex, sourceState),
                            transition.sourceCoefficient);
                    if (transition.sourceCoefficient > 1) {
                        value /= Math.pow(totalCount, transition.sourceCoefficient - 1);
                    }
                    value *= transition.probability;
                } else if (sourceState == operandState) {
                    double density = applyCoefficientPower(getState(sourceIndex, sourceState),
                            transition.sourceCoefficient + transition.operandCoefficient);
                    value = density / Math.pow(totalCount,
                            transition.sourceCoefficient + transition.operandCoefficient - 1);
                    value = applyTransitionCommon(value, density, transition);
                } else {
                    double sourceDensity = applyCoefficientPower(getState(sourceIndex, sourceState),
                            transition.sourceCoefficient);
                    double operandDensity =
                            applyCoefficientPower(getState(operandIndex, operandState),
                                    transition.operandCoefficient);
                    value = sourceDensity * operandDensity / Math.pow(totalCount,
                            transition.sourceCoefficient + transition.operandCoefficient - 1);
                    value = applyTransitionCommon(value, operandDensity, transition);
                }
            }
        } else if (transition.type == TransitionType.BLEND) {
            if (sourceExternal) {
                double operandCount = getState(operandIndex, operandState);
                if (operandCount > 0) {
                    double operandDensity =
                            applyCoefficientPower(operandCount, transition.operandCoefficient);
                    value = operandDensity;
                    if (transition.operandCoefficient > 1) {
                        value /= Math.pow(operandCount, transition.operandCoefficient - 1);
                    }
                    value = applyTransitionCommon(value, operandDensity, transition);
                }
            } else if (operandExternal) {
                double sourceCount = getState(sourceIndex, sourceState);
                if (sourceCount > 0) {
                    value = applyCoefficientPower(sourceCount, transition.sourceCoefficient);
                    if (transition.sourceCoefficient > 1) {
                        value /= Math.pow(sourceCount, transition.sourceCoefficient - 1);
                    }
                    value *= transition.probability;
                }
            } else if (sourceState == operandState) {
                double count = getState(sourceIndex, sourceState);
                if (count > 0) {
                    double density = applyCoefficientPower(count,
                            transition.sourceCoefficient + transition.operandCoefficient);
                    value = density / Math.pow(count,
                            transition.sourceCoefficient + transition.operandCoefficient - 1);
                    value = applyTransitionCommon(value, density, transition);
                }
            } else {
                double sourceCount = getState(sourceIndex, sourceState);
                double operandCount = getState(operandIndex, operandState);
                double sum = sourceCount + operandCount;
                if (sum > 0) {
                    double sourceDensity =
                            applyCoefficientPower(sourceCount, transition.sourceCoefficient);
                    double operandDensity =
                            applyCoefficientPower(operandCount, transition.operandCoefficient);
                    value = sourceDensity * operandDensity / Math.pow(sum,
                            transition.sourceCoefficient + transition.operandCoefficient - 1);
                    value = applyTransitionCommon(value, operandDensity, transition);
                }
            }
        }
        if (!sourceExternal && transition.mode == TransitionMode.REMOVING) {
            decrementState(step, sourceState, value * transition.sourceCoefficient);
            checkStateNegativeness(step, sourceState);
        }
        if (!operandExternal) {
            if (transition.mode == TransitionMode.INHIBITOR ||
                transition.mode == TransitionMode.RESIDUAL) {
                decrementState(step, operandState, value);
            } else if (transition.mode != TransitionMode.RETAINING) {
                decrementState(step, operandState, value * transition.operandCoefficient);
            }
            checkStateNegativeness(step, operandState);
        }
        if (!resultExternal) {
            incrementState(step, resultState, value * transition.resultCoefficient);
            checkStateNegativeness(step, resultState);
        }
    }

    /**
     * Вычисление перехода с повышенной точностью
     *
     * @param step       номер шага
     * @param totalCount общее количество автоматов на прошлом шаге
     * @param transition переход
     */
    private void transitionHigherAccuracy(int step, BigDecimal totalCount,
            TransitionValues transition) {
        int sourceState = findState(transition.sourceState);
        int operandState = findState(transition.operandState);
        int resultState = findState(transition.resultState);
        boolean sourceExternal = isStateExternal(sourceState);
        boolean operandExternal = isStateExternal(operandState);
        boolean resultExternal = isStateExternal(resultState);
        if (sourceExternal && operandExternal) {
            return;
        }
        int sourceIndex = delay(step - 1, transition.sourceDelay);
        int operandIndex = delay(step - 1, transition.operandDelay);
        BigDecimal value = BigDecimal.ZERO;
        if (transition.type == TransitionType.LINEAR) {
            if (sourceExternal) {
                BigDecimal operandDensity =
                        applyCoefficientLinear(decimalValue(getState(operandIndex, operandState)),
                                transition.operandCoefficient);
                value = multiply(operandDensity, decimalValue(transition.probability));
                if (transition.mode == TransitionMode.RESIDUAL) {
                    value = operandDensity
                            .subtract(multiply(value, decimalValue(transition.operandCoefficient)));
                }
            } else if (operandExternal) {
                value = multiply(
                        applyCoefficientLinear(decimalValue(getState(sourceIndex, sourceState)),
                                transition.sourceCoefficient),
                        decimalValue(transition.probability));
            } else if (sourceState == operandState) {
                BigDecimal density =
                        applyCoefficientLinear(decimalValue(getState(sourceIndex, sourceState)),
                                transition.sourceCoefficient + transition.operandCoefficient - 1);
                value = applyTransitionCommon(density, density, transition);
            } else {
                BigDecimal sourceDensity =
                        applyCoefficientLinear(decimalValue(getState(sourceIndex, sourceState)),
                                transition.sourceCoefficient);
                BigDecimal operandDensity =
                        applyCoefficientLinear(decimalValue(getState(operandIndex, operandState)),
                                transition.operandCoefficient);
                value = applyTransitionCommon(sourceDensity.min(operandDensity), operandDensity,
                        transition);
            }
        } else if (transition.type == TransitionType.SOLUTE) {
            if (totalCount.compareTo(BigDecimal.ZERO) > 0) {
                if (sourceExternal) {
                    BigDecimal operandDensity = applyCoefficientPower(
                            decimalValue(getState(operandIndex, operandState)),
                            transition.operandCoefficient);
                    value = operandDensity;
                    if (transition.operandCoefficient > 1) {
                        value = divide(value, power(totalCount, transition.operandCoefficient - 1));
                    }
                    value = applyTransitionCommon(value, operandDensity, transition);
                } else if (operandExternal) {
                    value = applyCoefficientPower(decimalValue(getState(sourceIndex, sourceState)),
                            transition.sourceCoefficient);
                    if (transition.sourceCoefficient > 1) {
                        value = divide(value, power(totalCount, transition.sourceCoefficient - 1));
                    }
                    value = multiply(value, decimalValue(transition.probability));
                } else if (sourceState == operandState) {
                    BigDecimal density =
                            applyCoefficientPower(decimalValue(getState(sourceIndex, sourceState)),
                                    transition.sourceCoefficient + transition.operandCoefficient);
                    value = divide(density, power(totalCount,
                            transition.sourceCoefficient + transition.operandCoefficient - 1));
                    value = applyTransitionCommon(value, density, transition);
                } else {
                    BigDecimal sourceDensity =
                            applyCoefficientPower(decimalValue(getState(sourceIndex, sourceState)),
                                    transition.sourceCoefficient);
                    BigDecimal operandDensity = applyCoefficientPower(
                            decimalValue(getState(operandIndex, operandState)),
                            transition.operandCoefficient);
                    value = divide(multiply(sourceDensity, operandDensity), power(totalCount,
                            transition.sourceCoefficient + transition.operandCoefficient - 1));
                    value = applyTransitionCommon(value, operandDensity, transition);
                }
            }
        } else if (transition.type == TransitionType.BLEND) {
            if (sourceExternal) {
                BigDecimal operandCount = decimalValue(getState(operandIndex, operandState));
                if (operandCount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal operandDensity =
                            applyCoefficientPower(operandCount, transition.operandCoefficient);
                    value = operandDensity;
                    if (transition.operandCoefficient > 1) {
                        value = divide(value,
                                power(operandCount, transition.operandCoefficient - 1));
                    }
                    value = applyTransitionCommon(value, operandDensity, transition);
                }
            } else if (operandExternal) {
                BigDecimal sourceCount = decimalValue(getState(sourceIndex, sourceState));
                if (sourceCount.compareTo(BigDecimal.ZERO) > 0) {
                    value = applyCoefficientPower(sourceCount, transition.sourceCoefficient);
                    if (transition.sourceCoefficient > 1) {
                        value = divide(value, power(sourceCount, transition.sourceCoefficient - 1));
                    }
                    value = multiply(value, decimalValue(transition.probability));
                }
            } else if (sourceState == operandState) {
                BigDecimal count = decimalValue(getState(sourceIndex, sourceState));
                if (count.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal density = applyCoefficientPower(count,
                            transition.sourceCoefficient + transition.operandCoefficient);
                    value = divide(density, power(count,
                            transition.sourceCoefficient + transition.operandCoefficient - 1));
                    value = applyTransitionCommon(value, density, transition);
                }
            } else {
                BigDecimal sourceCount = decimalValue(getState(sourceIndex, sourceState));
                BigDecimal operandCount = decimalValue(getState(operandIndex, operandState));
                BigDecimal sum = sourceCount.add(operandCount);
                if (sum.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal sourceDensity =
                            applyCoefficientPower(sourceCount, transition.sourceCoefficient);
                    BigDecimal operandDensity =
                            applyCoefficientPower(operandCount, transition.operandCoefficient);
                    value = divide(multiply(sourceDensity, operandDensity), power(sum,
                            transition.sourceCoefficient + transition.operandCoefficient - 1));
                    value = applyTransitionCommon(value, operandDensity, transition);
                }
            }
        }
        if (!sourceExternal && transition.mode == TransitionMode.REMOVING) {
            decrementState(step, sourceState,
                    multiply(value, decimalValue(transition.sourceCoefficient)));
            checkStateNegativeness(step, sourceState);
        }
        if (!operandExternal) {
            if (transition.mode == TransitionMode.INHIBITOR ||
                transition.mode == TransitionMode.RESIDUAL) {
                decrementState(step, operandState, value);
            } else if (transition.mode != TransitionMode.RETAINING) {
                decrementState(step, operandState,
                        multiply(value, decimalValue(transition.operandCoefficient)));
            }
            checkStateNegativeness(step, operandState);
        }
        if (!resultExternal) {
            incrementState(step, resultState,
                    multiply(value, decimalValue(transition.resultCoefficient)));
            checkStateNegativeness(step, resultState);
        }
    }

    /**
     * Выполнение расчётов синхронно
     *
     * @return результаты вычислений
     */
    public Results calculateSync() {
        Results results;
        if (mHigherAccuracy) {
            results = calculateHigherAccuracy();
        } else {
            results = calculateNormalAccuracy();
        }
        callbackResults(results);
        return results;
    }

    /**
     * Выполнение расчётов асинхронно
     */
    public void calculateAsync() {
        Utils.runAsync(() -> {
            Results results;
            if (mHigherAccuracy) {
                results = calculateHigherAccuracy();
            } else {
                results = calculateNormalAccuracy();
            }
            callbackResults(results);
        });
    }

    /**
     * Применение задержки
     *
     * @param step  номер шага
     * @param delay задержка
     * @return номер шага с задержкой
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
     * Определение, является состояние внешним или нет
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

    /**
     * Деление с точностью по-умолчанию для режима повышенной точности
     *
     * @param u делимое
     * @param v делитель
     * @return частное
     */
    private static BigDecimal divide(BigDecimal u, BigDecimal v) {
        return divide(u, v, HIGHER_ACCURACY_SCALE);
    }

    /**
     * Умножение с точностью по-умолчанию для режима повышенной точности
     *
     * @param u множитель
     * @param v множитель
     * @return произведение
     */
    private static BigDecimal multiply(BigDecimal u, BigDecimal v) {
        return multiply(u, v, HIGHER_ACCURACY_SCALE);
    }

    /**
     * Возведение в степень с точностью по-умолчанию для режима повышенной точности
     *
     * @param u        основание
     * @param exponent показатель
     * @return результат
     */
    private static BigDecimal power(BigDecimal u, double exponent) {
        return power(u, exponent, HIGHER_ACCURACY_SCALE);
    }

    private static BigDecimal exponent0(BigDecimal u, int scale) {
        BigDecimal a = BigDecimal.ONE;
        BigDecimal b = u;
        BigDecimal c = u.add(BigDecimal.ONE);
        BigDecimal d;
        for (int i = 2; ; i++) {
            b = multiply(b, u, scale);
            a = a.multiply(BigDecimal.valueOf(i));
            BigDecimal e = divide(b, a, scale);
            d = c;
            c = c.add(e);
            if (c.compareTo(d) == 0) {
                break;
            }
        }
        return c;
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
        return probabilisticFactorialBig(u, HIGHER_ACCURACY_SCALE);
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
     * Вычисление (синхронно)
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
     * Вычисление (асинхронно)
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

    /**
     * Действие, представляющее собой вычисление перехода с обычной точностью
     */
    private class TransitionActionNormalAccuracy implements Runnable {
        private final int mStep;
        private final double mTotalCount;
        private final TransitionValues mTransition;

        /**
         * @param step       номер шага
         * @param totalCount общее количество автоматов на прошлом шаге
         * @param transition переход
         */
        private TransitionActionNormalAccuracy(int step, double totalCount,
                TransitionValues transition) {
            mStep = step;
            mTotalCount = totalCount;
            mTransition = transition;
        }

        @Override
        public void run() {
            transitionNormalAccuracy(mStep, mTotalCount, mTransition);
        }
    }

    /**
     * Действие, представляющее собой вычисление перехода с повышенной точностью
     */
    private class TransitionActionHigherAccuracy implements Runnable {
        private final int mStep;
        private final BigDecimal mTotalCount;
        private final TransitionValues mTransition;

        /**
         * @param step       номер шага
         * @param totalCount общее количество автоматов на прошлом шаге
         * @param transition переход
         */
        private TransitionActionHigherAccuracy(int step, BigDecimal totalCount,
                TransitionValues transition) {
            mStep = step;
            mTotalCount = totalCount;
            mTransition = transition;
        }

        @Override
        public void run() {
            transitionHigherAccuracy(mStep, mTotalCount, mTransition);
        }
    }

    /**
     * Характеристики перехода
     */
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

        /**
         * @param transition переход
         */
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

    /**
     * Результаты вычислений
     */
    public static class Results {
        private final int mStartPoint;
        private final ArrayList<String> mDataNames;
        private final ArrayList<Result> mTableData;
        private final ArrayList<XYChart.Series<Number, Number>> mChartData;

        /**
         * @param startPoint              начало отсчёта
         * @param states                  состояния
         * @param stateNames              имена состояний
         * @param prepareResultsTableData подготовить результат в табличном виде
         * @param prepareResultsChartData подготовить результат в графическом виде
         */
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

        /**
         * @return начало отсчёта
         */
        public int getStartPoint() {
            return mStartPoint;
        }

        /**
         * @return имена состояний
         */
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
