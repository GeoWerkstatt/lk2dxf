package ch.geowerkstatt.lk2dxf;

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
        final var dxfWriter = new DxfWriter(TEST_DIR + "partial.dxf");
        try(dxfWriter) {
            final var count = new AtomicInteger(0);
            try (LKMapXtfReader reader = new LKMapXtfReader(new File(TEST_DIR + "Test.xtf" /*"ckw-kulm_ele.xtf"*/))) {

                reader.readObjects().forEach(iomObject -> {
                    switch (iomObject.getobjecttag()) {
                        case "SIA405_LKMap_2015_LV95.SIA405_LKMap.LKFlaeche" -> {
                            iomObject.getattrobj("Flaeche", 0);
                        }
                        case "SIA405_LKMap_2015_LV95.SIA405_LKMap.LKLinie" -> {
                            iomObject.getattrobj("Linie", 0);
                        }
                        case "SIA405_LKMap_2015_LV95.SIA405_LKMap.LKPunkt" -> {
                            iomObject.getattrobj("SymbolPos", 0);
                            Double.parseDouble(iomObject.getattrvalue("SymbolOri"));
                        }
                        case "SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text" -> {
                            //System.out.println(iomObject.getattrvalue("Plantyp"));
                            iomObject.getattrvalue("Textinhalt");
                            iomObject.getattrobj("TextPos", 0);
                            iomObject.getattrvalue("TextHAli");
                            iomObject.getattrvalue("TextVAli");
                            Double.parseDouble(iomObject.getattrvalue("TextOri"));
                        }
                        default -> {
                            System.out.println("Unsupported object: " + iomObject.getobjecttag());
                        }
                    }

                    count.incrementAndGet();
                });
            }

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

            dxfWriter.writeHeader();
            dxfWriter.writeSection("CLASSES");

            dxfWriter.writeSection("TABLES",
                    () -> dxfWriter.writeTable("VPORT",
                            dxfWriter::writeDefaultViewport),
                    () -> dxfWriter.writeTable("LTYPE",
                            () -> dxfWriter.writeLineType("ByLayer"),
                            () -> dxfWriter.writeLineType("ByBlock"),
                            () -> dxfWriter.writeLineType("Continuous"),
                            () -> dxfWriter.writeLineType("Dashed", 0.5, -0.25)),
                    () -> dxfWriter.writeTable("LAYER",
                            () -> dxfWriter.writeLayer("0", "Continuous", 0),
                            () -> dxfWriter.writeLayer("Surface", "Continuous", 3),
                            () -> dxfWriter.writeLayer("Polyline", "Dashed", 1),
                            () -> dxfWriter.writeLayer("Point", "Continuous", 40)),
                    () -> dxfWriter.writeTable("STYLE",
                            () -> dxfWriter.writeStyle("cadastra", "cadastra_regular.ttf")),
                    () -> dxfWriter.writeTable("VIEW"),
                    () -> dxfWriter.writeTable("UCS"),
                    () -> dxfWriter.writeTable("APPID",
                            dxfWriter::writeDefaultAppid),
                    dxfWriter::writeMinimalDimstyleTable,
                    () -> dxfWriter.writeTable("BLOCK_RECORD",
                            () -> dxfWriter.writeBlockRecord("*Model_Space"),
                            () -> dxfWriter.writeBlockRecord("*Paper_Space"),
                            () -> dxfWriter.writeBlockRecord("BAW15")));

            dxfWriter.writeSection("BLOCKS",
                    () -> dxfWriter.writeBlock("*Model_Space"),
                    () -> dxfWriter.writeBlock("*Paper_Space"),
                    () -> dxfWriter.writeBlock("BAW15",
                            () -> dxfWriter.writeCircle("0", 0, 0, 0.5)));

            dxfWriter.writeSection("ENTITIES",
                    () -> dxfWriter.writeHatch("Surface", surface),
                    () -> dxfWriter.writeLwPolyline("Polyline", polyline),
                    () -> dxfWriter.writeCircle("Point", 100, 100, 100),
                    () -> dxfWriter.writeText("Point", "cadastra", "Mid-Point", "Right", "Top", 90, point),
                    () -> dxfWriter.writeBlockInsert("Point", "BAW15", -90, point));

            dxfWriter.writeSection("OBJECTS",
                    dxfWriter::writeMinimalDictionary);
        }
    }
}
