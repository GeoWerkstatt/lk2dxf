package ch.geowerkstatt.lk2dxf.mapping;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;

public record LayerMapping(
        String layer,
        String objectType,
        String geometry,
        int color,
        String orientation,
        String vAlign,
        String hAlign,
        String text,
        String symbol,
        String linetype,
        double lineweight,
        double textsize,
        String font,
        @JsonDeserialize(converter = MappingConverter.class)
        Map<String, List<String>> mapping) {
}
