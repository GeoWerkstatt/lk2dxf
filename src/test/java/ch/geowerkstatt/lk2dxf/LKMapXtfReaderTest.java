package ch.geowerkstatt.lk2dxf;

import ch.geowerkstatt.lk2dxf.mapping.ObjectMapper;
import ch.interlis.iom.IomObject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LKMapXtfReaderTest {
    private static final String TEST_DIR = "src/test/data/LKMapXtfReaderTest/";

    @Test
    public void readValidXtf() throws Exception {
        try (LKMapXtfReader reader = new LKMapXtfReader(new File(TEST_DIR + "Valid.xtf"))) {
            String[] objectIds = reader
                    .readObjects()
                    .map(IomObject::getobjectoid)
                    .toArray(String[]::new);

            assertArrayEquals(new String[] {"obj1"}, objectIds);
        }
    }

    @Test
    public void readValidXtfMultipleBaskets() throws Exception {
        try (LKMapXtfReader reader = new LKMapXtfReader(new File(TEST_DIR + "ValidMultipleBaskets.xtf"))) {
            String[] objectIds = reader
                    .readObjects()
                    .map(IomObject::getobjectoid)
                    .toArray(String[]::new);

            assertArrayEquals(new String[] {"basket1object001", "basket1object002", "basket2object001"}, objectIds);
        }
    }

    @Test
    public void readXtfForWrongModel() throws Exception {
        try (LKMapXtfReader reader = new LKMapXtfReader(new File(TEST_DIR + "WrongModel.xtf"))) {
            Stream<IomObject> objectStream = reader.readObjects();
            assertThrows(IllegalStateException.class, objectStream::toList, "Streaming invalid data should throw an exception");
        }
    }

    @Test
    public void multipleReadsNotAllowed() throws Exception {
        try (LKMapXtfReader reader = new LKMapXtfReader(new File(TEST_DIR + "Valid.xtf"))) {
            reader.readObjects();
            assertThrows(IllegalStateException.class, reader::readObjects, "Multiple calls to readObjects should throw an exception");
        }
    }

    @Test
    public void readXtfForKulm() throws Exception {
        final var dxfWriter = new DxfWriter(TEST_DIR + "partial_bad.dxf", 3, ObjectMapper.getLayerMappings());
        try(dxfWriter) {
            var polyline = IomObjectHelper.createPolyline(
                    IomObjectHelper.createCoord("0", "0"),
                    IomObjectHelper.createCoord("0", "200"),
                    IomObjectHelper.createArc("120", "120", "200", "0"),
                    IomObjectHelper.createCoord("100", "0"));

            var surface = IomObjectHelper.createPolygonFromBoundaries(
                    IomObjectHelper.createBoundary(
                            IomObjectHelper.createCoord("10", "10"),
                            IomObjectHelper.createCoord("10", "190"),
                            IomObjectHelper.createArc("70", "70", "190", "10"),
                            IomObjectHelper.createCoord("10", "10")),
                    //IomObjectHelper.createRectangleBoundary("0", "0", "60", "60"),
                    IomObjectHelper.createRectangleBoundary("20", "20", "60", "60"));

            var point = IomObjectHelper.createCoord("120", "120");

            dxfWriter.writeLwPolyline("ELE-LINIE-UNGENAU", polyline);
            dxfWriter.writeBlockInsert("ELE-PUNKT", "BEW15", 0, point);
        }
    }
}
