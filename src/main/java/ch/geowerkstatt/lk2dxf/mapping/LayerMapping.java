package ch.geowerkstatt.lk2dxf.mapping;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record LayerMapping(
        String layer,
        @JsonDeserialize(converter = ObjectClassConverter.class)
        Collection<String> objectClass,
        OutputType output,
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

    public enum OutputType {
        /**
         * The object has a surface and is exported as a filled polygon.
         */
        SURFACE,

        /**
         * The object has a line geometry.
         */
        LINE,

        /**
         * The object represents text with font, alignment and orientation.
         */
        TEXT,

        /**
         * The object has a point geometry that gets marked with a symbol.
         */
        POINT,
    }
}
