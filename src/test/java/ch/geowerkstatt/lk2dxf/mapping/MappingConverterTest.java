package ch.geowerkstatt.lk2dxf.mapping;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class MappingConverterTest {
    @Test
    public void convertSingleValue() {
        var converter = new MappingConverter();
        var result = converter.convert("key=value");
        assertEquals(Map.of("key", List.of("value")), result);
    }

    @Test
    public void convertMultipleMappings() {
        var converter = new MappingConverter();
        var result = converter.convert("key=value key2=abc");

        var expected = Map.of(
                "key", List.of("value"),
                "key2", List.of("abc"));
        assertEquals(expected, result);
    }

    @Test
    public void convertMultipleValues() {
        var converter = new MappingConverter();
        var result = converter.convert("key=value1|value2 key2=abc");

        var expected = Map.of(
                "key", List.of("value1", "value2"),
                "key2", List.of("abc"));
        assertEquals(expected, result);
    }
}
