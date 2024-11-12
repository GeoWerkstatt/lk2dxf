package ch.geowerkstatt.lk2dxf.mapping;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MappingConverter extends StdConverter<String, Map<String, List<String>>> {
    /**
     * Pattern to match key-value pairs using the format {@code key=value} separated by whitespace.
     */
    private static final Pattern MAPPING_PATTERN = Pattern.compile("(?<=^|\\s)(.*?)=(.*?)(?=$|\\s)");
    private static final Pattern VALUE_SEPARATOR = Pattern.compile("\\|");

    @Override
    public Map<String, List<String>> convert(String s) {
        Map<String, List<String>> mappings = new HashMap<>();
        Matcher matcher = MAPPING_PATTERN.matcher(s);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            mappings.put(key, List.of(VALUE_SEPARATOR.split(value)));
        }
        return mappings;
    }
}
