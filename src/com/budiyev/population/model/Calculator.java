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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

public class Calculator {
    /**
     * Количество десяничных знаков после разделителя в вещественных числах
     * в режиме повышенной точности
     */
    public static final int SCALE = 384;

    private Calculator() {
    }

    /**
     * Преобразование int в BigDecimal
     *
     * @param u исходное значение типа int
     * @return результат типа BigDecimal
     */
    private static BigDecimal decimalValue(int u) {
        return new BigDecimal(u);
    }

    /**
     * Преобразование long в BigDecimal
     *
     * @param u исходное значение типа long
     * @return результат типа BigDecimal
     */
    private static BigDecimal decimalValue(long u) {
        return new BigDecimal(u);
    }

    /**
     * Преобразование double в BigDecimal
     *
     * @param u исходное значение типа double
     * @return результат типа BigDecimal
     */
    private static BigDecimal decimalValue(double u) {
        return new BigDecimal(u);
    }

    /**
     * Преобразование BigDecimal в double
     *
     * @param u исходное значение типа BigDecimal
     * @return результат
     */
    private static double doubleValue(BigDecimal u) {
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
    private static double probabilisticFactorial(double u) {
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
    private static BigDecimal probabilisticFactorialBig(double u, int scale) {
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
    private static BigDecimal divide(BigDecimal u, BigDecimal v, int scale) {
        return u.divide(v, scale, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal divide(BigDecimal u, BigDecimal v) {
        return divide(u, v, SCALE);
    }

    /**
     * Умножение
     *
     * @param u     множитель
     * @param v     множитель
     * @param scale количество знаков в дробной части результата
     * @return произведение
     */
    private static BigDecimal multiply(BigDecimal u, BigDecimal v, int scale) {
        return u.multiply(v).setScale(scale, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal multiply(BigDecimal u, BigDecimal v) {
        return multiply(u, v, SCALE);
    }

    /**
     * Возведение в целочисленную степень
     *
     * @param u        основание
     * @param exponent показатель
     * @param scale    количество знаков в дробной части результата
     * @return результат
     */
    private static BigDecimal power(BigDecimal u, long exponent, int scale) {
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
    private static BigDecimal power(BigDecimal u, double exponent, int scale) {
        if (exponent % 1 == 0 && exponent <= Long.MAX_VALUE) {
            return power(u, (long) exponent, scale);
        }
        return exponent(decimalValue(exponent).multiply(naturalLogarithm(u, scale)), scale);
    }

    private static BigDecimal power(BigDecimal u, double exponent) {
        return power(u, exponent, SCALE);
    }

    /**
     * Нахождение целочисленного корня
     *
     * @param u     исходное значение
     * @param index степень корня
     * @param scale количество знаков в дробной части результата
     * @return результат
     */
    private static BigDecimal root(BigDecimal u, long index, int scale) {
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
    private static BigDecimal exponent(BigDecimal u, int scale) {
        if (u.signum() == 0) {
            return BigDecimal.ONE;
        } else if (u.signum() == -1) {
            return divide(BigDecimal.ONE, exponent(u.negate(), scale));
        }
        BigDecimal a = u.setScale(0, RoundingMode.DOWN);
        if (a.signum() == 0) {
            return exponent2(u, scale);
        }
        BigDecimal b = u.subtract(a);
        BigDecimal c = BigDecimal.ONE.add(divide(b, a, scale));
        BigDecimal d = exponent2(c, scale);
        BigDecimal e = decimalValue(Long.MAX_VALUE);
        BigDecimal f = BigDecimal.ONE;
        for (; a.compareTo(e) >= 0; ) {
            f = multiply(f, power(d, Long.MAX_VALUE, scale), scale);
            a = a.subtract(e);
        }
        return multiply(f, power(d, a.longValue(), scale), scale);
    }

    private static BigDecimal exponent2(BigDecimal u, int scale) {
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

    /**
     * Нахождение натурального логарифма от указанного значения
     *
     * @param u     исходное значение
     * @param scale количество знаков в дробной части результата
     * @return результат
     */
    private static BigDecimal naturalLogarithm(BigDecimal u, int scale) {
        int a = u.toString().length() - u.scale() - 1;
        if (a < 3) {
            return naturalLogarithm2(u, scale);
        } else {
            BigDecimal b = root(u, a, scale);
            BigDecimal c = naturalLogarithm2(b, scale);
            return multiply(decimalValue(a), c, scale);
        }
    }

    private static BigDecimal naturalLogarithm2(BigDecimal u, int scale) {
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
     * Поиск позиции состояния
     *
     * @param id       идентификатор состояния
     * @param stateIds список идентификаторов
     * @return позиция состояния
     */
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
            u = operandDensity.subtract(u);
        }
        u = multiply(u, decimalValue(transition.probability));
        if (transition.mode == TransitionMode.RESIDUAL) {
            u = operandDensity.subtract(u);
        }
        return u;
    }

    /**
     * Применение основных операций перехода
     */
    private static double applyTransitionCommon(double u, double operandDensity,
            TransitionValues transition) {
        if (transition.mode == TransitionMode.INHIBITOR) {
            u = operandDensity - u;
        }
        u *= transition.probability;
        if (transition.mode == TransitionMode.RESIDUAL) {
            u = operandDensity - u;
        }
        return u;
    }

    /**
     * Вычисления с обычной точностью
     */
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
                    } else if (sourceState == operandState) {
                        double density = applyCoefficientLinear(states[sourceIndex][sourceState],
                                transition.sourceCoefficient + transition.operandCoefficient - 1);
                        value = applyTransitionCommon(density, density, transition);
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
                        } else if (sourceState == operandState) {
                            double density = applyCoefficientPower(states[sourceIndex][sourceState],
                                    transition.sourceCoefficient + transition.operandCoefficient);
                            value = density / Math.pow(totalCount,
                                    transition.sourceCoefficient + transition.operandCoefficient -
                                    1);
                            value = applyTransitionCommon(value, density, transition);
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
                    } else if (sourceState == operandState) {
                        double count = states[sourceIndex][sourceState];
                        if (count > 0) {
                            double density = applyCoefficientPower(count,
                                    transition.sourceCoefficient + transition.operandCoefficient);
                            value = density / Math.pow(count,
                                    transition.sourceCoefficient + transition.operandCoefficient -
                                    1);
                            value = applyTransitionCommon(value, density, transition);
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

    /**
     * Вычисления с повышенной точностью
     */
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
                totalCount = totalCount.add(decimalValue(count));
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
                            value = operandDensity.subtract(value);
                        }
                    } else if (operandExternal) {
                        value = multiply(applyCoefficientLinear(
                                decimalValue(states[sourceIndex][sourceState]),
                                transition.sourceCoefficient),
                                decimalValue(transition.probability));
                    } else if (sourceState == operandState) {
                        BigDecimal density = applyCoefficientLinear(
                                decimalValue(states[sourceIndex][sourceState]),
                                transition.sourceCoefficient + transition.operandCoefficient - 1);
                        value = applyTransitionCommon(density, density, transition);
                    } else {
                        BigDecimal sourceDensity = applyCoefficientLinear(
                                decimalValue(states[sourceIndex][sourceState]),
                                transition.sourceCoefficient);
                        BigDecimal operandDensity = applyCoefficientLinear(
                                decimalValue(states[operandIndex][operandState]),
                                transition.operandCoefficient);
                        value = applyTransitionCommon(sourceDensity.min(operandDensity),
                                operandDensity, transition);
                    }
                } else if (transition.type == TransitionType.SOLUTE) {
                    if (totalCount.compareTo(BigDecimal.ZERO) > 0) {
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
                        } else if (sourceState == operandState) {
                            BigDecimal density = applyCoefficientPower(
                                    decimalValue(states[sourceIndex][sourceState]),
                                    transition.sourceCoefficient);
                            value = divide(density, power(totalCount,
                                    transition.sourceCoefficient + transition.operandCoefficient -
                                    1));
                            value = applyTransitionCommon(value, density, transition);
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
                            value = applyTransitionCommon(value, operandDensity, transition);
                        }
                    }
                } else if (transition.type == TransitionType.BLEND) {
                    if (sourceExternal) {
                        BigDecimal operandCount = decimalValue(states[operandIndex][operandState]);
                        if (operandCount.compareTo(BigDecimal.ZERO) > 0) {
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
                        if (sourceCount.compareTo(BigDecimal.ZERO) > 0) {
                            value = applyCoefficientPower(sourceCount,
                                    transition.sourceCoefficient);
                            if (transition.sourceCoefficient > 1) {
                                value = divide(value,
                                        power(sourceCount, transition.sourceCoefficient - 1));
                            }
                            value = multiply(value, decimalValue(transition.probability));
                        }
                    } else if (sourceState == operandState) {
                        BigDecimal count = decimalValue(states[sourceIndex][sourceState]);
                        if (count.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal density = applyCoefficientPower(count,
                                    transition.sourceCoefficient + transition.operandCoefficient);
                            value = divide(density, power(count,
                                    transition.sourceCoefficient + transition.operandCoefficient -
                                    1));
                            value = applyTransitionCommon(value, density, transition);
                        }
                    } else {
                        BigDecimal sourceCount = decimalValue(states[sourceIndex][sourceState]);
                        BigDecimal operandCount = decimalValue(states[operandIndex][operandState]);
                        BigDecimal sum = sourceCount.add(operandCount);
                        if (sum.compareTo(BigDecimal.ZERO) > 0) {
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
                                decimalValue(states[step][sourceState]).subtract(value));
                        if (!allowNegative && states[step][sourceState] < 0) {
                            states[step][sourceState] = 0;
                        }
                    }
                }
                if (!resultExternal) {
                    states[step][resultState] = doubleValue(decimalValue(states[step][resultState])
                            .add(multiply(value, decimalValue(transition.resultCoefficient))));
                    if (!allowNegative && states[step][resultState] < 0) {
                        states[step][resultState] = 0;
                    }
                }
                if (transition.mode != TransitionMode.RETAINING && !operandExternal) {
                    states[step][operandState] =
                            doubleValue(decimalValue(states[step][operandState]).subtract(value));
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

    /**
     * Вычислить (асинхронно)
     *
     * @param initialStates    начальные состояния
     * @param transitions      переходы
     * @param stepsCount       количество шагов
     * @param startPoint       начало отсчёта
     * @param higherAccuracy   повышенная точность
     * @param allowNegative    разрешить отрицательные значания
     * @param resultCallback   обратный вызов результата
     * @param progressCallback обратный вызов прогресса вычислений
     */
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

    /**
     * Вычислить
     *
     * @param initialStates  начальные состояния
     * @param transitions    переходы
     * @param stepsCount     количество шагов
     * @param startPoint     начало отсчёта
     * @param higherAccuracy повышенная точность
     * @param allowNegative  разрешить отрицательные значания
     * @return результат
     */
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

        public ArrayList<XYChart.Series<Number, Number>> getChartData() {
            return mChartData;
        }

        public void clearChartData() {
            mChartData = null;
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
