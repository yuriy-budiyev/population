package com.budiyev.population;

import com.budiyev.population.util.Console;

public final class Launcher {
    public static final String VERSION = "3.0.0";
    private static volatile boolean sConsoleMode;

    public static void main(String[] args) {
        if (args.length > 1) {
            sConsoleMode = true;
            Console.launch(args);
        } else {
            sConsoleMode = false;
            PopulationApplication.launch(PopulationApplication.class, args);
        }
    }

    public static boolean isConsoleMode() {
        return sConsoleMode;
    }
}
