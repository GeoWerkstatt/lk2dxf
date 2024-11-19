package ch.geowerkstatt.lk2dxf.mapping;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Read and interpret the layer mapping configuration.
 */
public final class MappingReader {
    private static final String MAPPING_RESOURCE = "/mappings.csv";
    private static final CsvMapper MAPPER = CsvMapper
            .builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .build();

    private MappingReader() { }

    /**
     * Reads the XTF to DXF layer mappings from the CSV resource file.
     *
     * @return the list of mappings
     * @throws IOException if an I/O error occurs
     */
    public static List<LayerMapping> readMappings() throws IOException {
        CsvSchema headerSchema = CsvSchema.emptySchema().withHeader();
        ObjectReader reader = MAPPER
                .readerFor(LayerMapping.class)
                .with(headerSchema)
                .with(CsvParser.Feature.TRIM_SPACES);

        try (
                InputStream csvStream = openMappingResource();
                MappingIterator<LayerMapping> it = reader.readValues(csvStream)
        ) {
            return it.readAll();
        }
    }

    private static InputStream openMappingResource() {
        return MappingReader.class.getResourceAsStream(MAPPING_RESOURCE);
    }
}
