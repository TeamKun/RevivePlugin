package net.kunmc.lab.reviveplugin.config.parser;

import java.util.function.Function;

public class IntParser implements Function<String, Object> {
    private final int min;
    private final int max;

    public IntParser(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Object apply(String str) {
        try {
            int value = Integer.parseInt(str);
            if (min <= value && value <= max) {
                return value;
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
