package ch.geowerkstatt.lk2dxf;

import ch.geowerkstatt.lk2dxf.mapping.LayerMapping;
import ch.interlis.iom.IomObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DxfWriterTest {
    private static final String TEST_OUT_DIR = "src/test/data/Results/DxfWriter/";

    private StringWriter stringWriter;
    private Writer testOutputWriter;

    @BeforeAll
    static void initAll() {
        new File(TEST_OUT_DIR).mkdirs();
    }

    @BeforeEach
    void init(TestInfo testInfo) throws IOException {
        stringWriter = new StringWriter();
        testOutputWriter = new MultiWriter(stringWriter, new FileWriter(TEST_OUT_DIR + testInfo.getDisplayName().replaceAll("\\W+", "") + ".dxf"));
    }

    @Test
    public void writeEmptyDxf() throws Exception {
        var dxfWriter = new DxfWriter(testOutputWriter);
        dxfWriter.close();

        assertEquals("0\nSECTION\n2\nHEADER\n9\n$ACADVER\n1\nAC1021\n9\n$HANDSEED\n5\n3b9aca00\n9\n$INSUNITS\n70\n6\n0\nENDSEC\n0\nSECTION\n2\nCLASSES\n0\nENDSEC\n0\nSECTION\n2\nTABLES\n0\nTABLE\n2\nVPORT\n5\n1\n100\nAcDbSymbolTable\n70\n1\n0\nVPORT\n5\n2\n100\nAcDbSymbolTableRecord\n100\nAcDbViewportTableRecord\n2\n*ACTIVE\n70\n0\n0\nENDTAB\n0\nTABLE\n2\nLTYPE\n5\n3\n100\nAcDbSymbolTable\n70\n5\n0\nLTYPE\n5\n4\n100\nAcDbSymbolTableRecord\n100\nAcDbLinetypeTableRecord\n2\nByLayer\n70\n0\n72\n65\n73\n0\n40\n0\n0\nLTYPE\n5\n5\n100\nAcDbSymbolTableRecord\n100\nAcDbLinetypeTableRecord\n2\nByBlock\n70\n0\n72\n65\n73\n0\n40\n0\n0\nLTYPE\n5\n6\n100\nAcDbSymbolTableRecord\n100\nAcDbLinetypeTableRecord\n2\nContinuous\n70\n0\n72\n65\n73\n0\n40\n0\n0\nLTYPE\n5\n7\n100\nAcDbSymbolTableRecord\n100\nAcDbLinetypeTableRecord\n2\nDashed\n70\n0\n72\n65\n73\n2\n40\n0.75\n49\n0.5\n74\n0\n49\n-0.25\n74\n0\n0\nLTYPE\n5\n8\n100\nAcDbSymbolTableRecord\n100\nAcDbLinetypeTableRecord\n2\nDashDotDot\n70\n0\n72\n65\n73\n6\n40\n1.25\n49\n0.5\n74\n0\n49\n-0.25\n74\n0\n49\n0\n74\n0\n49\n-0.25\n74\n0\n49\n0\n74\n0\n49\n-0.25\n74\n0\n0\nENDTAB\n0\nTABLE\n2\nLAYER\n5\n9\n100\nAcDbSymbolTable\n70\n1\n0\nLAYER\n5\nA\n100\nAcDbSymbolTableRecord\n100\nAcDbLayerTableRecord\n2\n0\n6\nContinuous\n370\n25\n62\n0\n70\n0\n390\n0\n0\nENDTAB\n0\nTABLE\n2\nSTYLE\n5\nB\n100\nAcDbSymbolTable\n70\n0\n0\nENDTAB\n0\nTABLE\n2\nVIEW\n5\nC\n100\nAcDbSymbolTable\n70\n0\n0\nENDTAB\n0\nTABLE\n2\nUCS\n5\nD\n100\nAcDbSymbolTable\n70\n0\n0\nENDTAB\n0\nTABLE\n2\nAPPID\n5\nE\n100\nAcDbSymbolTable\n70\n1\n0\nAPPID\n5\nF\n100\nAcDbSymbolTableRecord\n100\nAcDbRegAppTableRecord\n2\nACAD\n70\n0\n0\nENDTAB\n0\nTABLE\n2\nDIMSTYLE\n5\n10\n100\nAcDbSymbolTable\n100\nAcDbDimStyleTable\n70\n0\n71\n1\n0\nENDTAB\n0\nTABLE\n2\nBLOCK_RECORD\n5\n11\n100\nAcDbSymbolTable\n70\n2\n0\nBLOCK_RECORD\n5\n12\n100\nAcDbSymbolTableRecord\n100\nAcDbBlockTableRecord\n2\n*Model_Space\n70\n0\n280\n1\n281\n0\n0\nBLOCK_RECORD\n5\n13\n100\nAcDbSymbolTableRecord\n100\nAcDbBlockTableRecord\n2\n*Paper_Space\n70\n0\n280\n1\n281\n0\n0\nENDTAB\n0\nENDSEC\n0\nSECTION\n2\nBLOCKS\n0\nBLOCK\n5\n14\n8\n0\n100\nAcDbEntity\n100\nAcDbBlockBegin\n2\n*Model_Space\n70\n0\n10\n0\n20\n0\n30\n0\n0\nENDBLK\n5\n15\n8\n0\n100\nAcDbEntity\n100\nAcDbBlockEnd\n0\nBLOCK\n5\n16\n8\n0\n100\nAcDbEntity\n100\nAcDbBlockBegin\n2\n*Paper_Space\n70\n0\n10\n0\n20\n0\n30\n0\n0\nENDBLK\n5\n17\n8\n0\n100\nAcDbEntity\n100\nAcDbBlockEnd\n0\nENDSEC\n0\nSECTION\n2\nENTITIES\n0\nENDSEC\n0\nSECTION\n2\nOBJECTS\n0\nDICTIONARY\n5\n18\n330\n0\n100\nAcDbDictionary\n281\n1\n3\nACAD_GROUP\n350\n19\n0\nDICTIONARY\n5\n19\n330\n18\n100\nAcDbDictionary\n281\n1\n0\nENDSEC\n0\nEOF\n", stringWriter.toString());
    }

    @Test
    public void writeCircle() throws Exception {
        try (var dxfWriter = new DxfWriter(testOutputWriter)) {
            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeCircle("LAYER_NAME", 1, 2, 3);
            assertEquals("0\nCIRCLE\n5\n18\n100\nAcDbEntity\n8\nLAYER_NAME\n100\nAcDbCircle\n10\n1\n20\n2\n40\n3\n", stringWriter.toString());
        }
    }

    @Test
    public void writeOpenPolyline() throws Exception {
        var polyline = IomObjectHelper.createPolyline(
                IomObjectHelper.createCoord("1", "2"),
                IomObjectHelper.createCoord("3", "4"),
                IomObjectHelper.createCoord("5", "4.5"));

        try (var dxfWriter = new DxfWriter(testOutputWriter)) {
            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeLwPolyline("LAYER_NAME", polyline);
            assertEquals("0\nLWPOLYLINE\n5\n18\n100\nAcDbEntity\n8\nLAYER_NAME\n100\nAcDbPolyline\n90\n3\n70\n0\n10\n1\n20\n2\n42\n0\n10\n3\n20\n4\n42\n0\n10\n5\n20\n4.5\n42\n0\n", stringWriter.toString());
        }
    }

    @Test
    public void writeClosedPolyline() throws Exception {
        var polyline = IomObjectHelper.createPolyline(
                IomObjectHelper.createCoord("10", "50"),
                IomObjectHelper.createCoord("20", "70"),
                IomObjectHelper.createCoord("20", "50"),
                IomObjectHelper.createCoord("10", "50"));

        try (var dxfWriter = new DxfWriter(testOutputWriter)) {
            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeLwPolyline("LAYER_NAME", polyline);
            assertEquals("0\nLWPOLYLINE\n5\n18\n100\nAcDbEntity\n8\nLAYER_NAME\n100\nAcDbPolyline\n90\n3\n70\n1\n10\n10\n20\n50\n42\n0\n10\n20\n20\n70\n42\n0\n10\n20\n20\n50\n42\n0\n", stringWriter.toString());
        }
    }

    @Test
    public void writePolylineWithArcs() throws Exception {
        var polylineCcwArcs = IomObjectHelper.createPolyline(createArcTestSegments(10, 12, 5));
        var polylineCwArcs = IomObjectHelper.createPolyline(createArcTestSegments(9, 6, 5));

        try (var dxfWriter = new DxfWriter(testOutputWriter)) {
            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeLwPolyline("CCW-Arcs", polylineCcwArcs);
            assertEquals("0\nLWPOLYLINE\n5\n18\n100\nAcDbEntity\n8\nCCW-Arcs\n100\nAcDbPolyline\n90\n6\n70\n0\n10\n10\n20\n0\n42\n0.665\n10\n3.09\n20\n9.511\n42\n0.665\n10\n-8.09\n20\n5.878\n42\n0.665\n10\n-8.09\n20\n-5.878\n42\n0.665\n10\n3.09\n20\n-9.511\n42\n0.665\n10\n10\n20\n-0\n42\n0\n", stringWriter.toString());

            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeLwPolyline("CW-Arcs", polylineCwArcs);
            assertEquals("0\nLWPOLYLINE\n5\n19\n100\nAcDbEntity\n8\nCW-Arcs\n100\nAcDbPolyline\n90\n6\n70\n0\n10\n9\n20\n0\n42\n-0.242\n10\n2.781\n20\n8.56\n42\n-0.242\n10\n-7.281\n20\n5.29\n42\n-0.242\n10\n-7.281\n20\n-5.29\n42\n-0.242\n10\n2.781\n20\n-8.56\n42\n-0.242\n10\n9\n20\n-0\n42\n0\n", stringWriter.toString());
        }
    }

    @Test
    public void writeHatch() throws Exception {
        var surface = IomObjectHelper.createRectangleGeometry("10", "20", "50", "70");

        try (var dxfWriter = new DxfWriter(testOutputWriter)) {
            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeHatch("LAYER_NAME", surface);
            assertEquals("0\nHATCH\n5\n18\n100\nAcDbEntity\n8\nLAYER_NAME\n100\nAcDbHatch\n10\n0\n20\n0\n30\n0\n210\n0\n220\n0\n230\n1\n2\nSOLID\n70\n1\n71\n0\n91\n1\n92\n3\n72\n1\n73\n1\n93\n4\n10\n10\n20\n20\n42\n0\n10\n10\n20\n70\n42\n0\n10\n50\n20\n70\n42\n0\n10\n50\n20\n20\n42\n0\n97\n0\n75\n0\n76\n1\n98\n0\n", stringWriter.toString());
        }
    }

    @Test
    public void writeHatchWithHoles() throws Exception {
        var surface = IomObjectHelper.createPolygonFromBoundaries(
                IomObjectHelper.createRectangleBoundary("10", "50", "20", "70"),
                IomObjectHelper.createRectangleBoundary("11", "51", "19", "59.5"),
                IomObjectHelper.createRectangleBoundary("11", "60.5", "19", "69"));

        try (var dxfWriter = new DxfWriter(testOutputWriter)) {
            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeHatch("LAYER_NAME", surface);
            assertEquals("0\nHATCH\n5\n18\n100\nAcDbEntity\n8\nLAYER_NAME\n100\nAcDbHatch\n10\n0\n20\n0\n30\n0\n210\n0\n220\n0\n230\n1\n2\nSOLID\n70\n1\n71\n0\n91\n3\n92\n3\n72\n1\n73\n1\n93\n4\n10\n10\n20\n50\n42\n0\n10\n10\n20\n70\n42\n0\n10\n20\n20\n70\n42\n0\n10\n20\n20\n50\n42\n0\n97\n0\n92\n18\n72\n1\n73\n1\n93\n4\n10\n11\n20\n51\n42\n0\n10\n11\n20\n59.5\n42\n0\n10\n19\n20\n59.5\n42\n0\n10\n19\n20\n51\n42\n0\n97\n0\n92\n18\n72\n1\n73\n1\n93\n4\n10\n11\n20\n60.5\n42\n0\n10\n11\n20\n69\n42\n0\n10\n19\n20\n69\n42\n0\n10\n19\n20\n60.5\n42\n0\n97\n0\n75\n0\n76\n1\n98\n0\n", stringWriter.toString());
        }
    }

    @Test
    public void writeHatchWithArcs() throws Exception {
        var surface = IomObjectHelper.createPolygonFromBoundaries(
                IomObjectHelper.createBoundary(createArcTestSegments(20, 25, 6)));

        try (var dxfWriter = new DxfWriter(testOutputWriter)) {
            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeHatch("LAYER_NAME", surface);
            assertEquals("0\nHATCH\n5\n18\n100\nAcDbEntity\n8\nLAYER_NAME\n100\nAcDbHatch\n10\n0\n20\n0\n30\n0\n210\n0\n220\n0\n230\n1\n2\nSOLID\n70\n1\n71\n0\n91\n1\n92\n3\n72\n1\n73\n1\n93\n6\n10\n20\n20\n0\n42\n0.768\n10\n10\n20\n17.321\n42\n0.768\n10\n-10\n20\n17.321\n42\n0.768\n10\n-20\n20\n0\n42\n0.768\n10\n-10\n20\n-17.321\n42\n0.768\n10\n10\n20\n-17.321\n42\n0.768\n97\n0\n75\n0\n76\n1\n98\n0\n", stringWriter.toString());
        }
    }

    @Test
    public void writeBlockInsert() throws Exception {
        var point = IomObjectHelper.createCoord("10", "20");

        try (var dxfWriter = new DxfWriter(testOutputWriter, 3, createTestLayerMappings(), null)) {
            stringWriter.getBuffer().setLength(0);
            dxfWriter.writeBlockInsert("Test", "TestSymbol", 30, point);
            assertEquals("0\nINSERT\n5\n1E\n100\nAcDbEntity\n8\nTest\n100\nAcDbBlockReference\n2\nTestSymbol\n10\n10\n20\n20\n50\n60\n", stringWriter.toString());
        }
    }

    @Test
    public void writeTextAlignment() throws Exception {
        try (var dxfWriter = new DxfWriter(testOutputWriter, 3, createTestLayerMappings(), null)) {
            stringWriter.getBuffer().setLength(0);

            int y = 0;
            for (var vAlign : List.of("Top", "Cap", "Half", "Base", "Bottom")) {
                for (var hAlign : List.of("Left", "Center", "Right")) {
                    dxfWriter.writeText("Test", "arial", vAlign + "-" + hAlign, hAlign, vAlign, 90, IomObjectHelper.createCoord("0", Integer.toString(y)));
                    dxfWriter.writeCircle("Anchor", 0, y, 0.5);
                    y += 4;
                }
            }

            assertEquals("0\nTEXT\n5\n1E\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nTop-Left\n11\n0\n21\n0\n100\nAcDbText\n73\n3\n0\nCIRCLE\n5\n1F\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n0\n40\n0.5\n0\nTEXT\n5\n20\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nTop-Center\n72\n1\n11\n0\n21\n4\n100\nAcDbText\n73\n3\n0\nCIRCLE\n5\n21\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n4\n40\n0.5\n0\nTEXT\n5\n22\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nTop-Right\n72\n2\n11\n0\n21\n8\n100\nAcDbText\n73\n3\n0\nCIRCLE\n5\n23\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n8\n40\n0.5\n0\nTEXT\n5\n24\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nCap-Left\n11\n0\n21\n12\n100\nAcDbText\n73\n3\n0\nCIRCLE\n5\n25\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n12\n40\n0.5\n0\nTEXT\n5\n26\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nCap-Center\n72\n1\n11\n0\n21\n16\n100\nAcDbText\n73\n3\n0\nCIRCLE\n5\n27\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n16\n40\n0.5\n0\nTEXT\n5\n28\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nCap-Right\n72\n2\n11\n0\n21\n20\n100\nAcDbText\n73\n3\n0\nCIRCLE\n5\n29\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n20\n40\n0.5\n0\nTEXT\n5\n2A\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nHalf-Left\n11\n0\n21\n24\n100\nAcDbText\n73\n2\n0\nCIRCLE\n5\n2B\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n24\n40\n0.5\n0\nTEXT\n5\n2C\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nHalf-Center\n72\n1\n11\n0\n21\n28\n100\nAcDbText\n73\n2\n0\nCIRCLE\n5\n2D\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n28\n40\n0.5\n0\nTEXT\n5\n2E\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nHalf-Right\n72\n2\n11\n0\n21\n32\n100\nAcDbText\n73\n2\n0\nCIRCLE\n5\n2F\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n32\n40\n0.5\n0\nTEXT\n5\n30\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n36\n40\n1.25\n1\nBase-Left\n100\nAcDbText\n0\nCIRCLE\n5\n31\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n36\n40\n0.5\n0\nTEXT\n5\n32\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nBase-Center\n72\n1\n11\n0\n21\n40\n100\nAcDbText\n0\nCIRCLE\n5\n33\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n40\n40\n0.5\n0\nTEXT\n5\n34\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nBase-Right\n72\n2\n11\n0\n21\n44\n100\nAcDbText\n0\nCIRCLE\n5\n35\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n44\n40\n0.5\n0\nTEXT\n5\n36\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nBottom-Left\n11\n0\n21\n48\n100\nAcDbText\n73\n1\n0\nCIRCLE\n5\n37\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n48\n40\n0.5\n0\nTEXT\n5\n38\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nBottom-Center\n72\n1\n11\n0\n21\n52\n100\nAcDbText\n73\n1\n0\nCIRCLE\n5\n39\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n52\n40\n0.5\n0\nTEXT\n5\n3A\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nBottom-Right\n72\n2\n11\n0\n21\n56\n100\nAcDbText\n73\n1\n0\nCIRCLE\n5\n3B\n100\nAcDbEntity\n8\nAnchor\n100\nAcDbCircle\n10\n0\n20\n56\n40\n0.5\n", stringWriter.toString());
        }
    }

    @Test
    public void writeTextOrientation() throws Exception {
        try (var dxfWriter = new DxfWriter(testOutputWriter, 3, createTestLayerMappings(), null)) {
            stringWriter.getBuffer().setLength(0);

            for (int orientation = 0; orientation < 360; orientation += 45) {
                dxfWriter.writeText("Test", "arial", "Orientation: " + orientation + "°", "Left", "Base", orientation, IomObjectHelper.createCoord("0", "0"));
            }
            assertEquals("0\nTEXT\n5\n1E\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nOrientation: 0°\n50\n90\n100\nAcDbText\n0\nTEXT\n5\n1F\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nOrientation: 45°\n50\n45\n100\nAcDbText\n0\nTEXT\n5\n20\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nOrientation: 90°\n100\nAcDbText\n0\nTEXT\n5\n21\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nOrientation: 135°\n50\n315\n100\nAcDbText\n0\nTEXT\n5\n22\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nOrientation: 180°\n50\n270\n100\nAcDbText\n0\nTEXT\n5\n23\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nOrientation: 225°\n50\n225\n100\nAcDbText\n0\nTEXT\n5\n24\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nOrientation: 270°\n50\n180\n100\nAcDbText\n0\nTEXT\n5\n25\n100\nAcDbEntity\n8\nTest\n100\nAcDbText\n7\narial\n10\n0\n20\n0\n40\n1.25\n1\nOrientation: 315°\n50\n135\n100\nAcDbText\n", stringWriter.toString());
        }
    }

    private IomObject[] createArcTestSegments(double pointRadius, double midPointRadius, int segmentCount) {
        var segments = new IomObject[segmentCount + 1];
        segments[0] = IomObjectHelper.createCoord(Double.toString(pointRadius), "0");
        for (int i = 0; i < segmentCount; i++) {
            var pointAngle = Math.TAU * ((i + 1.0) / segmentCount);
            var midAngle = Math.TAU * ((i + 0.5) / segmentCount);
            segments[i + 1] = IomObjectHelper.createArc(
                    Double.toString(midPointRadius * Math.cos(midAngle)),
                    Double.toString(midPointRadius * Math.sin(midAngle)),
                    Double.toString(pointRadius * Math.cos(pointAngle)),
                    Double.toString(pointRadius * Math.sin(pointAngle)));
        }

        return segments;
    }

    private Collection<LayerMapping> createTestLayerMappings() {
        return List.of(new LayerMapping("Test", "", "", "", 1, "", "", "", "", "TestSymbol", "", 0.25, 1.25, "arial", null));
    }
}
