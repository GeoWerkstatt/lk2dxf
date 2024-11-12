package ch.geowerkstatt.lk2dxf.mapping;

import ch.geowerkstatt.lk2dxf.LKMapXtfReader;
import ch.interlis.iom.IomObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ObjectMapperTest {
    private static final String TEST_FILE = "src/test/data/MapperTest/MapWithText.xtf";

    @Test
    public void mapObject() throws Exception {
        try (LKMapXtfReader xtfReader = new LKMapXtfReader(new File(TEST_FILE))) {
            ObjectMapper mapper = new ObjectMapper();

            List<IomObject> objects = xtfReader.readObjects().toList();
            assertEquals(4, objects.size());

            String[] layerMappings = mapper.mapObjects(objects.stream())
                    .map(o -> o.layerMapping().layer())
                    .toArray(String[]::new);
            String[] expected = new String[] {"WAS-FLAECHE", "WAS-PUNKT", "FER-PUNKT", "FER-TEXT"};
            assertArrayEquals(expected, layerMappings);
        }
    }
}
