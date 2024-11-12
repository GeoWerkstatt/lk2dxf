package ch.geowerkstatt.lk2dxf;

import ch.interlis.iom.IomObject;
import com.jsevy.jdxf.DXFDocument;

import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

public class DxfWriter implements AutoCloseable {

    private final DecimalFormat decimalFormat = new DecimalFormat("0.###", new DecimalFormatSymbols(Locale.ROOT));
    private final Writer dxfWriter;
    private int handle = 1;

    public DxfWriter(String filePath) throws IOException {
        dxfWriter = new FileWriter(filePath, StandardCharsets.UTF_8);
    }

    DXFDocument dxfDocument = new DXFDocument();
    Graphics2D dxfGraphics = dxfDocument.getGraphics();

    private static final Path2D.Double marker = new Path2D.Double();
    static {
        marker.moveTo(0.2, -0.2);
        marker.lineTo(-1, 1);
        marker.lineTo(2, 0);
        marker.lineTo(-1, -1);
        marker.lineTo(0.2, 0.2);
    }

    private Path2D.Double createPath(IomObject polyline) {
        if (!polyline.getobjecttag().equals("POLYLINE")) throw new IllegalArgumentException("Expected POLYLINE object, got: " + polyline.getobjecttag());

        var path = new Path2D.Double();
        var segments = polyline.getattrobj("sequence", 0);
        var startPoint = segments.getattrobj("segment", 0);
        path.moveTo(Double.parseDouble(startPoint.getattrvalue("C1")), Double.parseDouble(startPoint.getattrvalue("C2")));
        for (int i = 1; i < segments.getattrvaluecount("segment"); i++) {
            var segment = segments.getattrobj("segment", i);
            switch (segment.getobjecttag()) {
                case "COORD" -> path.lineTo(Double.parseDouble(segment.getattrvalue("C1")), Double.parseDouble(segment.getattrvalue("C2")));
                default -> throw new IllegalArgumentException("Unsupported Line-Type: " + segment.getobjecttag());
            }
        }

        return path;
    }

    public void writePolyline2d(String layerName, IomObject polyline) throws IOException {
        var segments = polyline.getattrobj("sequence", 0);
        var segmentCount = segments.getattrvaluecount("segment");

        writeElement(0, "POLYLINE");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
        writeElement(370, -1); // Lineweight by layer
        writeElement(100, "AcDb2dPolyline");
        writeElement(66, 1); // entities follow
        writeElement(10, 0);
        writeElement(20, 0);
        writeElement(30, 0);
        writeElement(70, 0); // closed polyline

        var segment = segments.getattrobj("segment", 0);
        var prevX = Double.parseDouble(segment.getattrvalue("C1"));
        var prevY = Double.parseDouble(segment.getattrvalue("C2"));
        for (int i = 1; i < segmentCount; i++) {
            segment = segments.getattrobj("segment", i);
            var x = Double.parseDouble(segment.getattrvalue("C1"));
            var y = Double.parseDouble(segment.getattrvalue("C2"));

            writeElement(0, "VERTEX");
            writeElement(5, getNextHandle());
            writeElement(100, "AcDbEntity");
            writeElement(8, layerName);
            writeElement(370, -1); // Lineweight by layer
            writeElement(100, "AcDbVertex");
            writeElement(100, "AcDb2dVertex");
            writeElement(10, prevX);
            writeElement(20, prevY);

            if (segment.getobjecttag().equals("ARC")) {
                var ax = Double.parseDouble(segment.getattrvalue("A1"));
                var ay = Double.parseDouble(segment.getattrvalue("A2"));

                var bulge = Math.tan((Math.PI + Math.atan2(y - ay, x - ax) - Math.atan2(prevY - ay, prevX - ax)) / 2.0);
                if (Double.isFinite(bulge)) {
                    writeElement(42, bulge);
                }
            }

            prevX = x;
            prevY = y;
        }

        writeElement(0, "VERTEX");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
        writeElement(370, -1); // Lineweight by layer
        writeElement(100, "AcDbVertex");
        writeElement(100, "AcDb2dVertex");
        writeElement(10, prevX);
        writeElement(20, prevY);

        writeElement(0, "SEQEND");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
    }

    public void writeLwPolyline(String layerName, boolean isClosed, double[] ... points) throws IOException {
        writeElement(0, "LWPOLYLINE");
        writeElement(5, getNextHandle());
        writeElement(8, layerName);
        writeElement(100, "AcDbEntity");
        writeElement(100, "AcDbPolyline");
        writeElement(90, points.length);
        writeElement(70, isClosed ? 1 : 0); // open polyline

        for (var point : points) {
            writeElement(10, point[0]);
            writeElement(20, point[1]);
            if (point.length > 2) {
                writeElement(42, point[2]);
            }
        }
    }

    public void writeLwPolyline(String layerName, IomObject polyline) throws IOException {
        var segments = polyline.getattrobj("sequence", 0);
        var segmentCount = segments.getattrvaluecount("segment");

        writeElement(0, "LWPOLYLINE");
        writeElement(5, getNextHandle());
        writeElement(8, layerName);
        writeElement(100, "AcDbEntity");
        writeElement(100, "AcDbPolyline");
        writeElement(90, segmentCount);
        writeElement(70, 0); // open polyline

        var segment = segments.getattrobj("segment", 0);
        var prevX = Double.parseDouble(segment.getattrvalue("C1"));
        var prevY = Double.parseDouble(segment.getattrvalue("C2"));
        for (int i = 1; i < segmentCount; i++) {
            segment = segments.getattrobj("segment", i);
            var x = Double.parseDouble(segment.getattrvalue("C1"));
            var y = Double.parseDouble(segment.getattrvalue("C2"));

            writeElement(10, prevX);
            writeElement(20, prevY);

            if (segment.getobjecttag().equals("ARC")) {
                var ax = Double.parseDouble(segment.getattrvalue("A1"));
                var ay = Double.parseDouble(segment.getattrvalue("A2"));

                var bulge = Math.tan((Math.PI + Math.atan2(y - ay, x - ax) - Math.atan2(prevY - ay, prevX - ax)) / 2.0);
                if (Double.isFinite(bulge)) {
                    writeElement(42, bulge);
                }
            }

            prevX = x;
            prevY = y;
        }

        writeElement(10, prevX);
        writeElement(20, prevY);
    }

    public void writeSurface(IomObject multiSurface) {
        if (!multiSurface.getobjecttag().equals("MULTISURFACE")) throw new IllegalArgumentException("Expected POLYLINE object, got: " + multiSurface.getobjecttag());

        for (int i = 0; i < multiSurface.getattrvaluecount("surface"); i++) {
            var surface = multiSurface.getattrobj("surface", i);

            for (int j = 0; j < surface.getattrvaluecount("boundary"); j++) {
                if (j > 0) throw new IllegalArgumentException("Holes in Surface not supported");
                var boundary = surface.getattrobj("boundary", j);

                var path = createPath(boundary.getattrobj("polyline", 0));
                dxfGraphics.fill(path);
            }

        }
    }

    public void writeCircle(String layerName, double centerX, double centerY, double radius) throws IOException {
        writeElement(0, "CIRCLE");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
        writeElement(100, "AcDbCircle");
        writeElement(10, centerX);
        writeElement(20, centerY);
        writeElement(40, radius);
    }

    public void writeElement(String type, String definition) throws IOException {
        writeElement(0, type);
        writeElement(5, getNextHandle());
        dxfWriter.write(definition);
    }

    @Deprecated
    public void writeHatch(String layerName) throws IOException {
        writeElement(0, "HATCH");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
        writeElement(370, -1); // Lineweight by layer
        writeElement(100, "AcDbHatch");
        writeElement(10, 0);
        writeElement(20, 0);
        writeElement(30, 0);
        writeElement(210, 0);
        writeElement(220, 0);
        writeElement(230, 1);
        writeElement(2, "SOLID");
        writeElement(70, 1); // solid fill
        writeElement(71, 0); // not associative

        writeElement(91, 1); // boundary count

        writeElement(92, 1); // external boundary
        writeElement(72, 3); // elliptic arc
        writeElement(73, 1); // is counterclockwise flag
        writeElement(10, 0); // center x
        writeElement(20, 0); // center y
        writeElement(11, 0.333); // major axis x
        writeElement(21, 0.333); // major axis y
        writeElement(40, 1.0); // Length of minor axis (percentage of major axis length)
        writeElement(50, 315.0); // start angle
        writeElement(51, 675.0); // end angle
        writeElement(97, 0);

        writeElement(75, 0); // hatch "odd parity"
        writeElement(76, 1); // hatch pattern predefined
        writeElement(98, 0); // number of seed points
    }

    public void writeHatch(String layerName, IomObject multiSurface) throws IOException {
        writeElement(0, "HATCH");
        writeElement(5, getNextHandle());
        writeElement(8, layerName);
        writeElement(100, "AcDbEntity");
        writeElement(100, "AcDbHatch");
        writeElement(10, 0.0);
        writeElement(20, 0.0);
        writeElement(30, 0.0);
        writeElement(210, 0.0);
        writeElement(220, 0.0);
        writeElement(230, 1.0);
        writeElement(2, "SOLID");
        writeElement(70, 1); // solid fill
        writeElement(71, 0); // not associative

        int boundaryCount = 0;
        for (int i = 0; i < multiSurface.getattrvaluecount("surface"); i++) {
            var surface = multiSurface.getattrobj("surface", i);
            boundaryCount += surface.getattrvaluecount("boundary");
        }

        writeElement(91, boundaryCount);

        for (int i = 0; i < multiSurface.getattrvaluecount("surface"); i++) {
            var surface = multiSurface.getattrobj("surface", i);

            for (int j = 0; j < surface.getattrvaluecount("boundary"); j++) {
                var boundary = surface.getattrobj("boundary", j);
                var polyline = boundary.getattrobj("polyline", 0);
                var segments = polyline.getattrobj("sequence", 0);
                var segmentCount = segments.getattrvaluecount("segment");

                writeElement(92, (j == 0 ? 1 /*external*/ : 16 /*outermost*/) + 2 /*polygon*/);

                writeElement(72, 1); // has bulge
                writeElement(73, 1); // is closed
                writeElement(93, segmentCount);

                var segment = segments.getattrobj("segment", 0);
                var prevX = Double.parseDouble(segment.getattrvalue("C1"));
                var prevY = Double.parseDouble(segment.getattrvalue("C2"));
                for (int k = 1; k < segmentCount; k++) {
                    segment = segments.getattrobj("segment", k);
                    var x = Double.parseDouble(segment.getattrvalue("C1"));
                    var y = Double.parseDouble(segment.getattrvalue("C2"));

                    writeElement(10, prevX);
                    writeElement(20, prevY);

                    if (segment.getobjecttag().equals("ARC")) {
                        var ax = Double.parseDouble(segment.getattrvalue("A1"));
                        var ay = Double.parseDouble(segment.getattrvalue("A2"));

                        var bulge = Math.tan((Math.PI + Math.atan2(y - ay, x - ax) - Math.atan2(prevY - ay, prevX - ax)) / 2.0);
                        if (Double.isFinite(bulge)) {
                            writeElement(42, bulge);
                        }
                    }
                    else {
                        writeElement(42, 0.0);
                    }

                    prevX = x;
                    prevY = y;
                }

                writeElement(10, prevX);
                writeElement(20, prevY);
                writeElement(42, 0.0);

                writeElement(97, 0); // no source boundaries
            }
        }

        writeElement(75, 0); // hatch "odd parity"
        writeElement(76, 1); // hatch pattern predefined
        writeElement(98, 0); // no seed points
    }

    public void writePolyline(IomObject polyline) {
        dxfGraphics.draw(createPath(polyline));
    }

    public void writeBlockInsert(String layerName, String blockName, double rotation, IomObject point) throws IOException {
        writeElement(0, "INSERT");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
        writeElement(100, "AcDbBlockReference");
        writeElement(2, blockName);
        writeElement(10, Double.parseDouble(point.getattrvalue("C1")));
        writeElement(20, Double.parseDouble(point.getattrvalue("C2")));
        writeElement(50, rotation);
    }

    public void writePoint(IomObject point, double orientation /*, String symbolName */) {
        if (!point.getobjecttag().equals("COORD")) throw new IllegalArgumentException("Expected COORD object, got: " + point.getobjecttag());

        dxfDocument.insertShapesAsBlocks(true);
        var currentTransform = dxfGraphics.getTransform();
        dxfGraphics.translate(Double.parseDouble(point.getattrvalue("C1")), Double.parseDouble(point.getattrvalue("C2")));
        dxfGraphics.rotate(Math.toRadians(orientation));
        dxfGraphics.draw(marker);
        dxfGraphics.setTransform(currentTransform);
        dxfDocument.insertShapesAsBlocks(false);
    }

    public void writeText(String text, IomObject position, String hAlignment, String vAlignment, double orientation) {
        if (!position.getobjecttag().equals("COORD")) throw new IllegalArgumentException("Expected COORD object, got: " + position.getobjecttag());

        var currentTransform = dxfGraphics.getTransform();
        dxfGraphics.translate(Double.parseDouble(position.getattrvalue("C1")), Double.parseDouble(position.getattrvalue("C2")));
        dxfGraphics.rotate(Math.toRadians(orientation) - (Math.PI / 2));
        dxfGraphics.drawString(text, 0, 0);
        dxfGraphics.setTransform(currentTransform);
    }

    public void writeText(String layerName, String textStyle, String text, String hAlignment, String vAlignment, double orientation, IomObject position) throws IOException {
        var hAlignmentValue = switch (hAlignment) {
            case "Right" -> 2;
            case "Center" -> 1;
            default -> 0; // Left
        };
        var vAlignmentValue = switch (vAlignment) {
            case "Top", "Cap" -> 3;
            case "Half" -> 2;
            case "Bottom" -> 1;
            default -> 0; // Base
        };
        var isDefaultAlignment = hAlignmentValue == 0 && vAlignmentValue == 0;

        // Convert transfer orientation to DXF orientation
        orientation = (-orientation + 90 + 360) % 360;

        writeElement(0, "TEXT");
        writeElement(5, getNextHandle());
        writeElement(8, layerName);
        writeElement(100, "AcDbEntity");
        writeElement(100, "AcDbText");
        writeElement(7, textStyle);
        writeElement(10, isDefaultAlignment ? Double.parseDouble(position.getattrvalue("C1")) : 0.0);
        writeElement(20, isDefaultAlignment ? Double.parseDouble(position.getattrvalue("C2")) : 0.0);
        writeElement(40, 1.25); // text height
        writeElement(1, text);
        if (hAlignmentValue != 0) {
            writeElement(72, hAlignmentValue);
        }
        if (!isDefaultAlignment) {
            writeElement(11, Double.parseDouble(position.getattrvalue("C1")));
            writeElement(21, Double.parseDouble(position.getattrvalue("C2")));
        }
        if (orientation != 0) {
            writeElement(50, orientation);
        }
        writeElement(100, "AcDbText");
        if (vAlignmentValue != 0) {
            writeElement(73, vAlignmentValue);
        }
    }

    public void writeToFile(String filePath) throws IOException {
        String dxfText = dxfDocument.toDXFString();
        try (var fileWriter = new FileWriter(filePath)) {
            fileWriter.write(dxfText);
            fileWriter.flush();
        }
    }

    public void writeHeader() throws IOException {
        writeSection("HEADER",
                () -> writeElement(9, "$ACADVER"),
                () -> writeElement(1, "AC1021"),
                () -> writeElement(9, "$HANDSEED"),
                () -> writeElement(5, Integer.toHexString(1_000_000_000)),
                () -> writeElement(9, "$INSUNITS"),
                () -> writeElement(70, 6 /* Meters */));
    }

    public void writeDefaultAppid() throws IOException {
        writeElement(0, "APPID");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbRegAppTableRecord");
        writeElement(2, "ACAD");
        writeElement(70, 0);
    }

    public void writeMinimalDimstyleTable() throws IOException {
        writeElement(0, "TABLE");
        writeElement(2, "DIMSTYLE");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTable");
        writeElement(100, "AcDbDimStyleTable");
        writeElement(70, 0);
        writeElement(71, 1);
        writeElement(0, "ENDTAB");
    }

    public void writeSection(String name, ContentWriter ... writeContent) throws IOException {
        writeElement(0, "SECTION");
        writeElement(2, name);
        for (var content : writeContent) {
            content.write();
        }
        writeElement(0, "ENDSEC");
    }

    public void writeTable(String name, ContentWriter ... writeContent) throws IOException {
        writeElement(0, "TABLE");
        writeElement(2, name);
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTable");
        writeElement(70, writeContent.length);
        for (var content : writeContent) {
            content.write();
        }
        writeElement(0, "ENDTAB");
    }

    public void writeBlockRecord(String name) throws IOException {
        writeElement(0, "BLOCK_RECORD");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbBlockTableRecord");
        writeElement(2, name);
        writeElement(70, 0);
        writeElement(280, 1); // is explodable
        writeElement(281, 0); // is scalable
    }

    public void writeBlock(String name, ContentWriter ... writeContent) throws IOException {
        writeElement(0, "BLOCK");
        writeElement(5, getNextHandle());
        writeElement(8, "0"); // layer
        writeElement(100, "AcDbEntity");
        writeElement(100, "AcDbBlockBegin");
        writeElement(2, name);
        writeElement(70, 0);
        writeElement(10, 0.0);
        writeElement(20, 0.0);
        writeElement(30, 0.0);
        for (var content : writeContent) {
            content.write();
        }
        writeElement(0, "ENDBLK");
        writeElement(5, getNextHandle());
        writeElement(8, "0"); // layer
        writeElement(100, "AcDbEntity");
        writeElement(100, "AcDbBlockEnd");
    }

    public void writeMinimalDictionary() throws IOException {
        var rootHandle = getNextHandle();
        var entryHandle = getNextHandle();

        writeElement(0, "DICTIONARY");
        writeElement(5, rootHandle);
        writeElement(330, "0");
        writeElement(100, "AcDbDictionary");
        writeElement(281, 1);
        writeElement(3, "ACAD_GROUP");
        writeElement(350, entryHandle);

        writeElement(0, "DICTIONARY");
        writeElement(5, entryHandle);
        writeElement(330, rootHandle);
        writeElement(100, "AcDbDictionary");
        writeElement(281, 1);
    }

    public void writeDefaultViewport() throws IOException {
        writeElement(0, "VPORT");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbViewportTableRecord");
        writeElement(2, "*ACTIVE");
        writeElement(70, 0);
    }

    public void writeStyle(String name, String font) throws IOException {
        writeElement(0, "STYLE");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbTextStyleTableRecord");
        writeElement(2, name);
        writeElement(3, font);
        writeElement(70, 0);
    }

    public void writeLayer(String name, String lineTypeName, int color) throws IOException {
        writeElement(0, "LAYER");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbLayerTableRecord");
        writeElement(2, name);
        writeElement(6, lineTypeName);
        writeElement(370, 25); // lineweight
        writeElement(62, color);
        writeElement(70, 0);
        writeElement(390, 0);
    }

    public void writeLineType(String name, double ... pattern) throws IOException {
        writeElement(0, "LTYPE");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbLinetypeTableRecord");
        writeElement(2, name);
        writeElement(70, 0);
        writeElement(72, 65);
        writeElement(73, pattern.length);
        writeElement(40, Arrays.stream(pattern).map(Math::abs).sum());
        for (double value : pattern) {
            writeElement(49, value);
            writeElement(74, 0); // Complex linetype element type
        }
    }

    private void writeElement(int code, double value) throws IOException {
        writeElement(code, decimalFormat.format(value));
    }

    private void writeElement(int code, int value) throws IOException {
        writeElement(code, String.valueOf(value));
    }

    private void writeElement(int code, String value) throws IOException {
        dxfWriter.write(String.valueOf(code));
        dxfWriter.write("\n");
        dxfWriter.write(value);
        dxfWriter.write("\n");
    }

    private String getNextHandle() {
        return Integer.toHexString(handle++).toUpperCase(Locale.ROOT);
    }

    @Override
    public void close() throws Exception {
        writeElement(0, "EOF");
        dxfWriter.close();
    }

    public interface ContentWriter {
        void write() throws IOException;
    }
}
