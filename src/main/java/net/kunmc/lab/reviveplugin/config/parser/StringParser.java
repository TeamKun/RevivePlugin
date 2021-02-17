package net.kunmc.lab.reviveplugin.config.parser;

import java.util.function.Function;

public class StringParser implements Function<String, Object> {
    public StringParser() {
    }

    @Override
    public Object apply(String str) {
        return str;
    }
}
