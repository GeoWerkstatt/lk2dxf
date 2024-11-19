package ch.geowerkstatt.lk2dxf.mapping;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.*;
import java.util.regex.Pattern;

public final class ObjectClassConverter extends StdConverter<String, Collection<String>> {
    private static final Pattern VALUE_SEPARATOR = Pattern.compile("\\|");

    @Override
    public Collection<String> convert(String s) {
        return new HashSet<>(Arrays.asList(VALUE_SEPARATOR.split(s)));
    }
}
