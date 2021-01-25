package net.kunmc.lab.reviveplugin.config.parser;

import java.util.function.Function;

public class DoubleParser implements Function<String, Object> {
    private final double min;
    private final double max;

    public DoubleParser(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Object apply(String str) {
        try {
            double value = Double.parseDouble(str);
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
