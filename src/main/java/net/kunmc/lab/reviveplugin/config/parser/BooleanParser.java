package net.kunmc.lab.reviveplugin.config.parser;

import java.util.function.Function;

public class BooleanParser implements Function<String, Object> {
    @Override
    public Object apply(String str) {
        if (str.equals("true")) {
            return true;
        } else if (str.equals("false")) {
            return false;
        } else {
            return null;
        }
    }
}
