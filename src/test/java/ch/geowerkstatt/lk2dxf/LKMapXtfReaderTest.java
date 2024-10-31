package ch.geowerkstatt.lk2dxf;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LKMapXtfReaderTest {
    private static final String TEST_DIR = "src/test/data/LKMapXtfReaderTest/";

    @Test
    public void readValidXtf() throws Exception {
        try (LKMapXtfReader reader = new LKMapXtfReader(new File(TEST_DIR + "Valid.xtf"))) {
            List<String> objectIds = new ArrayList<>();

            reader.readObjects(iomObject -> objectIds.add(iomObject.getobjectoid()));

            assertArrayEquals(new String[] {"obj1"}, objectIds.toArray());
        }
    }

    @Test
    public void readXtfForWrongModel() throws Exception {
        try (LKMapXtfReader reader = new LKMapXtfReader(new File(TEST_DIR + "WrongModel.xtf"))) {
            assertThrows(IllegalStateException.class, () -> reader.readObjects(iomObject -> { }));
        }
    }
}
