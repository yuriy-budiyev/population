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
package com.budiyev.population;

public final class Launcher {
    public static final String VERSION = "3.0.2";
    private static volatile boolean sConsoleMode;

    public static void main(String[] args) {
        if (args.length > 0) {
            sConsoleMode = true;
            Console.main(args);
        } else {
            sConsoleMode = false;
            PopulationApplication.main(args);
        }
    }

    public static boolean isConsoleMode() {
        return sConsoleMode;
    }
}
