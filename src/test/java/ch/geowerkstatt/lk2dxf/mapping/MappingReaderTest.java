package ch.geowerkstatt.lk2dxf.mapping;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MappingReaderTest {
    @Test
    public void readMappings() throws IOException {
        List<LayerMapping> mappings = MappingReader.readMappings();

        assertTrue(mappings.stream().noneMatch(m -> m.layer().isBlank()), "Layer is required");
        assertTrue(mappings.stream().noneMatch(m -> m.objectClass().isEmpty()), "ObjectType is required");
        assertTrue(mappings.stream().noneMatch(m -> m.output().toString().isBlank()), "GeometryType is required");
        assertTrue(mappings.stream().noneMatch(m -> m.geometry().isBlank()), "Geometry is required");
        assertTrue(mappings.stream().noneMatch(m -> m.mapping().isEmpty()), "Mapping is required");
        assertEquals("STILLGELEGTE-LEITUNGEN", mappings.getFirst().layer());
    }
}
