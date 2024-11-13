package ch.geowerkstatt.lk2dxf;

import ch.geowerkstatt.lk2dxf.mapping.LayerMapping;
import ch.interlis.iom.IomObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Stream;

public final class DxfWriter implements AutoCloseable {
    private final DecimalFormat decimalFormat;
    private final Writer dxfWriter;
    private int handle = 1;

    public DxfWriter(String filePath) throws IOException {
        this(filePath, 3, Collections.emptyList());
    }

    public DxfWriter(String filePath, int doublePrecision, Collection<LayerMapping> layerMappings) throws IOException {
        if (doublePrecision < 0) {
            throw new IllegalArgumentException("doublePrecision must be positive or zero.");
        }
        if (layerMappings == null) {
            throw new IllegalArgumentException("layerMappings must not be null.");
        }

        dxfWriter = new FileWriter(filePath, StandardCharsets.UTF_8);
        decimalFormat = new DecimalFormat("0." + "#".repeat(doublePrecision), new DecimalFormatSymbols(Locale.ROOT));

        prepareDxfForWritingEntities(layerMappings);
    }

    private void prepareDxfForWritingEntities(Collection<LayerMapping> layerMappings) throws IOException {
        var layers = new TreeMap<String, ContentWriter>();
        layers.put("0", () -> writeLayer("0", "Continuous", 0));
        for (var mapping : layerMappings) {
            layers.putIfAbsent(mapping.layer(), () -> writeLayer(mapping.layer(), mapping.linetype(), mapping.color()));
        }

        List<String> symbols = layerMappings.stream().map(LayerMapping::symbol).distinct().filter(s -> !s.isEmpty()).toList();
        var blockSymbolRecords = Stream.concat(Stream.of("*Model_Space", "*Paper_Space"), symbols.stream())
                .map(s -> (ContentWriter) (() -> writeBlockRecord(s))).toArray(ContentWriter[]::new);
        var blockSymbols = Stream.concat(Stream.of(
                        () -> writeBlock("*Model_Space"),
                        () -> writeBlock("*Paper_Space")
                ), symbols.stream()
                        .map(s -> (ContentWriter) (() -> writeBlock(s, () -> writeCircle("0", 0, 0, 0.5)))))
                .toArray(ContentWriter[]::new);

        writeHeader();
        writeSection("CLASSES");

        writeSection("TABLES",
                () -> writeTable("VPORT",
                        this::writeDefaultViewport),
                () -> writeTable("LTYPE",
                        () -> writeLineType("ByLayer"),
                        () -> writeLineType("ByBlock"),
                        () -> writeLineType("Continuous"),
                        () -> writeLineType("Dashed", 0.5, -0.25),
                        () -> writeLineType("DashDotDot", 0.5, -0.25, 0.0, -0.25, 0.0, -0.25)),
                () -> writeTable("LAYER", layers.values().toArray(ContentWriter[]::new)),
                () -> writeTable("STYLE",
                        () -> writeStyle("cadastra", "cadastra_regular.ttf")),
                () -> writeTable("VIEW"),
                () -> writeTable("UCS"),
                () -> writeTable("APPID",
                        this::writeDefaultAppid),
                this::writeMinimalDimstyleTable,
                () -> writeTable("BLOCK_RECORD", blockSymbolRecords));

        writeSection("BLOCKS", blockSymbols);

        writeElement(0, "SECTION");
        writeElement(2, "ENTITIES");
    }

    private void finishDxfAfterWritingEntities() throws IOException {
        writeElement(0, "ENDSEC");
        writeSection("OBJECTS", this::writeMinimalDictionary);
        writeElement(0, "EOF");
    }

    public void writeLwPolyline(String layerName, IomObject polyline) throws IOException {
        var segments = polyline.getattrobj("sequence", 0);
        var segmentCount = segments.getattrvaluecount("segment");

        writeElement(0, "LWPOLYLINE");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
        writeElement(100, "AcDbPolyline");

        var segment = segments.getattrobj("segment", 0);
        var lastSegment = segments.getattrobj("segment", segmentCount - 1);
        var isClosed = segment.getattrvalue("C1").equals(lastSegment.getattrvalue("C1")) && segment.getattrvalue("C2").equals(lastSegment.getattrvalue("C2"));
        writeElement(90, segmentCount - (isClosed ? 1 : 0));
        writeElement(70, isClosed ? 1 : 0);

        writePolylinePoints(polyline, isClosed);
    }

    public void writeHatch(String layerName, IomObject multiSurface) throws IOException {
        writeElement(0, "HATCH");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
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
                writeElement(93, segmentCount - 1);

                writePolylinePoints(polyline, true);
                writeElement(97, 0); // no source boundaries
            }
        }

        writeElement(75, 0); // hatch "odd parity"
        writeElement(76, 1); // hatch pattern predefined
        writeElement(98, 0); // no seed points
    }

    private void writePolylinePoints(IomObject polyline, boolean isClosed) throws IOException {
        var segments = polyline.getattrobj("sequence", 0);

        var segment = segments.getattrobj("segment", 0);
        var prevX = Double.parseDouble(segment.getattrvalue("C1"));
        var prevY = Double.parseDouble(segment.getattrvalue("C2"));

        for (int i = 1; i < segments.getattrvaluecount("segment"); i++) {
            segment = segments.getattrobj("segment", i);
            var x = Double.parseDouble(segment.getattrvalue("C1"));
            var y = Double.parseDouble(segment.getattrvalue("C2"));

            writeElement(10, prevX);
            writeElement(20, prevY);

            if (segment.getobjecttag().equals("ARC")) {
                var ax = Double.parseDouble(segment.getattrvalue("A1"));
                var ay = Double.parseDouble(segment.getattrvalue("A2"));

                var bulge = Math.tan((Math.PI + Math.atan2(y - ay, x - ax) - Math.atan2(prevY - ay, prevX - ax)) / 2.0);
                writeElement(42, Double.isFinite(bulge) ? bulge : 0.0);
            } else {
                writeElement(42, 0.0);
            }

            prevX = x;
            prevY = y;
        }

        if (!isClosed) {
            writeElement(10, prevX);
            writeElement(20, prevY);
            writeElement(42, 0.0);
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
        writeElement(100, "AcDbEntity");
        writeElement(8, layerName);
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

    private void writeHeader() throws IOException {
        writeSection("HEADER",
                () -> writeElement(9, "$ACADVER"),
                () -> writeElement(1, "AC1021"),
                () -> writeElement(9, "$HANDSEED"),
                () -> writeElement(5, Integer.toHexString(1_000_000_000)),
                () -> writeElement(9, "$INSUNITS"),
                () -> writeElement(70, 6 /* Meters */));
    }

    private void writeDefaultAppid() throws IOException {
        writeElement(0, "APPID");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbRegAppTableRecord");
        writeElement(2, "ACAD");
        writeElement(70, 0);
    }

    private void writeMinimalDimstyleTable() throws IOException {
        writeElement(0, "TABLE");
        writeElement(2, "DIMSTYLE");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTable");
        writeElement(100, "AcDbDimStyleTable");
        writeElement(70, 0);
        writeElement(71, 1);
        writeElement(0, "ENDTAB");
    }

    private void writeSection(String name, ContentWriter... writeContent) throws IOException {
        writeElement(0, "SECTION");
        writeElement(2, name);
        for (var content : writeContent) {
            content.write();
        }
        writeElement(0, "ENDSEC");
    }

    private void writeTable(String name, ContentWriter... writeContent) throws IOException {
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

    private void writeBlockRecord(String name) throws IOException {
        writeElement(0, "BLOCK_RECORD");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbBlockTableRecord");
        writeElement(2, name);
        writeElement(70, 0);
        writeElement(280, 1); // is explodable
        writeElement(281, 0); // is scalable
    }

    private void writeBlock(String name, ContentWriter... writeContent) throws IOException {
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

    private void writeDefaultViewport() throws IOException {
        writeElement(0, "VPORT");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbViewportTableRecord");
        writeElement(2, "*ACTIVE");
        writeElement(70, 0);
    }

    private void writeStyle(String name, String font) throws IOException {
        writeElement(0, "STYLE");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbTextStyleTableRecord");
        writeElement(2, name);
        writeElement(3, font);
        writeElement(70, 0);
    }

    private void writeLayer(String name, String lineTypeName, int color) throws IOException {
        writeElement(0, "LAYER");
        writeElement(5, getNextHandle());
        writeElement(100, "AcDbSymbolTableRecord");
        writeElement(100, "AcDbLayerTableRecord");
        writeElement(2, name);
        writeElement(6, lineTypeName.isEmpty() ? "Continuous" : lineTypeName);
        writeElement(370, 25); // lineweight
        writeElement(62, color);
        writeElement(70, 0);
        writeElement(390, 0);
    }

    private void writeLineType(String name, double... pattern) throws IOException {
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
        finishDxfAfterWritingEntities();
        dxfWriter.close();
    }

    private interface ContentWriter {
        void write() throws IOException;
    }
}
