package ch.geowerkstatt.lk2dxf;

import ch.interlis.iom.IomObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class XtfStreamReaderTest {
    private static final String TEST_DIR = "src/test/data/LKMapXtfReaderTest/";

    @Test
    public void readValidXtf() throws Exception {
        try (XtfStreamReader reader = new XtfStreamReader(new File(TEST_DIR + "Valid.xtf"))) {
            String[] objectIds = reader
                    .readObjects()
                    .map(IomObject::getobjectoid)
                    .toArray(String[]::new);

            assertArrayEquals(new String[] {"obj1"}, objectIds);
        }
    }

    @Test
    public void readValidXtfMultipleBaskets() throws Exception {
        try (XtfStreamReader reader = new XtfStreamReader(new File(TEST_DIR + "ValidMultipleBaskets.xtf"))) {
            String[] objectIds = reader
                    .readObjects()
                    .map(IomObject::getobjectoid)
                    .toArray(String[]::new);

            assertArrayEquals(new String[] {"basket1object001", "basket1object002", "basket2object001"}, objectIds);
        }
    }

    @Test
    public void readXtfForWrongModel() throws Exception {
        try (XtfStreamReader reader = new XtfStreamReader(new File(TEST_DIR + "WrongModel.xtf"))) {
            Stream<IomObject> objectStream = reader.readObjects();
            assertEquals(1, objectStream.count(), "Reading unknown data should add the objects to the stream anyway");
        }
    }

    @Test
    public void multipleReadsNotAllowed() throws Exception {
        try (XtfStreamReader reader = new XtfStreamReader(new File(TEST_DIR + "Valid.xtf"))) {
            reader.readObjects();
            assertThrows(IllegalStateException.class, reader::readObjects, "Multiple calls to readObjects should throw an exception");
        }
    }
}
