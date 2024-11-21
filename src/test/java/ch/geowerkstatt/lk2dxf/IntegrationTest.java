package ch.geowerkstatt.lk2dxf;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.StdListener;
import ch.geowerkstatt.lk2dxf.mapping.ObjectMapper;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.xtf.XtfWriter;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.EndBasketEvent;
import ch.interlis.iox_j.EndTransferEvent;
import ch.interlis.iox_j.ObjectEvent;
import ch.interlis.iox_j.StartBasketEvent;
import ch.interlis.iox_j.StartTransferEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IntegrationTest {
    private static final String TEST_OUT_DIR = "src/test/data/Results/IntegrationTest/";

    @BeforeAll
    static void initAll() {
        new File(TEST_OUT_DIR).mkdirs();

        // Configure logging
        Configurator.setRootLevel(Level.TRACE);
        EhiLogger.getInstance().addListener(new EhiLogAdapter());
        EhiLogger.getInstance().removeListener(StdListener.getInstance());
    }

    @Test
    public void multipleFilesWithPerimeter() throws IOException, URISyntaxException, Ili2cException, IoxException {
        // Clear output file
        var output = TEST_OUT_DIR + "multipleFilesWithPerimeter" + ".dxf";
        var outputFile = new File(output);
        if (outputFile.exists()) {
            assertTrue(outputFile.delete(), "Failed to delete existing output file");
        }
        assertFalse(outputFile.exists());

        // Generate input files
        var files = new ArrayList<File>();
        var objectCount = 2_000;
        for (int i = 0; i < 3; i++) {
            var file = new File(TEST_OUT_DIR + "multipleFilesWithPerimeter" + "_" + i + ".xtf");
            files.add(file);
            writeTestXTF(file, i * objectCount, objectCount);
        }

        // Run the main method
        var perimeter = "Polygon ((2740857.55514180986210704 1256024.12385561061091721, 2607598.44303888687863946 1264279.11310092429630458, 2508533.20150150312110782 1106392.40145171224139631, 2578116.33859133720397949 1101537.89655045163817704, 2625680.80043338378891349 1178584.46284004743210971, 2706265.21925668558105826 1173867.3261284395121038, 2740857.55514180986210704 1101930.99127641879022121, 2795890.81677723582834005 1100751.7070985168684274, 2740857.55514180986210704 1256024.12385561061091721))";
        Main.main(Stream.concat(files.stream().map(File::getAbsolutePath), Stream.of(output, "--perimeter", perimeter)).toArray(String[]::new));

        // Check output file
        assertTrue(outputFile.exists());
        assertTrue(outputFile.isFile());
        assertTrue(outputFile.length() < 1_150_000, "Output file is too large <" + outputFile.length() + "> did the perimeter filter work?");
        assertTrue(outputFile.length() > 10_000, "The output file is too small <" + outputFile.length() + "> it contains not much more than the default dxf content!");
    }

    private void writeTestXTF(File file, int seed, int objectCount) throws IOException, URISyntaxException, Ili2cException, IoxException {
        var objectMapper = new ObjectMapper();
        var rand = new Random(seed);

        XtfWriter writer = new XtfWriter(file, objectMapper.getTransferDescription());
        try {
            writer.write(new StartTransferEvent());
            writer.write(new StartBasketEvent("SIA405_LKMap_2015_LV95.SIA405_LKMap", "BASKET1"));
            for (int i = seed; i < objectCount + seed; i += 4) {
                var x = rand.nextDouble() * 300_000 + 2510_000;
                var y = rand.nextDouble() * 200_000 + 1085_000;
                var size = rand.nextDouble() * 7000 + 3000;
                writer.write(new ObjectEvent(createTextObject(i, x, y, "obj_" + (i + 1))));
                writer.write(new ObjectEvent(createLineObject(i + 1, x, y, size)));
                writer.write(new ObjectEvent(createFlaecheObject(i + 2, x, y)));
                writer.write(new ObjectEvent(createPointObject(i + 3, x, y)));
            }
            writer.write(new EndBasketEvent());
            writer.write(new EndTransferEvent());
        } finally {
            writer.close();
        }
    }

    private final String[] precisionValues = new String[]{"genau", "unbekannt", "ungenau"};
    private final String[] lineObjectTypeValues = new String[]{"Abwasser.Fernwirkkabel", "Abwasser.Haltung_Kanal", "Abwasser.Schutzrohr", "Elektrizitaet.AnkerStrebe", "Elektrizitaet.Trasse.oberirdisch", "Elektrizitaet.Trasse.unterirdisch", "Fernwaerme.Fernwirkkabel", "Fernwaerme.Trasse", "Gas.Fernwirkkabel", "Gas.Leitung", "Gas.Schutzrohr", "Kommunikation.Trasse.oberirdisch", "Kommunikation.Trasse.unterirdisch", "Wasser.Fernwirkkabel", "Wasser.Leitung", "Wasser.Schutzrohr", "weitereMedien.Fernwirkkabel", "weitereMedien.Leitung", "weitereMedien.Schutzrohr"};
    private final String[] objectTypeValues = new String[]{"Abwasser", "Elektrizitaet", "Fernwaerme", "Gas", "Kommunikation", "Wasser", "weitereMedien"};
    private final String[] objectStatusValues = new String[]{"ausser_Betrieb", "in_Betrieb", "tot", "unbekannt", "weitere"};

    private IomObject createLineObject(int oid, double x, double y, double size) {
        var rings = 4;
        var angleCount = 7;
        var segments = new ArrayList<IomObject>();
        for (double r = size; r > 0.1; r -= size / rings) {
            for (int i = 0; i < angleCount; i++) {
                var angle = Math.TAU / angleCount * i;
                var segment = IomObjectHelper.createCoord(Double.toString(x + r * Math.cos(angle)), Double.toString(y + r * Math.sin(angle)));
                segments.add(segment);
            }
        }

        var polyline = IomObjectHelper.createPolyline(segments.toArray(new IomObject[0]));
        var rand = new Random(oid);
        return IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKLinie", "obj_" + oid,
                o -> o.addattrobj("Linie", polyline),
                o -> o.addattrvalue("Objektart", lineObjectTypeValues[rand.nextInt(lineObjectTypeValues.length)]),
                o -> o.addattrvalue("Lagebestimmung", precisionValues[rand.nextInt(precisionValues.length)]),
                o -> o.addattrvalue("Status", objectStatusValues[rand.nextInt(objectStatusValues.length)]),
                o -> o.addattrvalue("Eigentuemer", "Keine_Angabe"));
    }

    private IomObject createFlaecheObject(int oid, double x, double y) {
        var rand = new Random(oid);
        return IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKFlaeche", "obj_" + oid,
                o -> o.addattrobj("Flaeche", IomObjectHelper.createRectangleGeometry(Double.toString(x), Double.toString(y), Double.toString(x - 1.5), Double.toString(y + 10))),
                o -> o.addattrvalue("Objektart", objectTypeValues[rand.nextInt(objectTypeValues.length)]),
                o -> o.addattrvalue("Lagebestimmung", precisionValues[rand.nextInt(precisionValues.length)]),
                o -> o.addattrvalue("Status", objectStatusValues[rand.nextInt(objectStatusValues.length)]),
                o -> o.addattrvalue("Eigentuemer", "Keine_Angabe"));
    }

    private IomObject createPointObject(int oid, double x, double y) {
        var rand = new Random(oid);
        return IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKPunkt", "obj_" + oid,
                o -> o.addattrobj("SymbolPos", IomObjectHelper.createCoord(Double.toString(x), Double.toString(y))),
                o -> o.addattrvalue("Objektart", objectTypeValues[rand.nextInt(objectTypeValues.length)]),
                o -> o.addattrvalue("Lagebestimmung", precisionValues[rand.nextInt(precisionValues.length)]),
                o -> o.addattrvalue("Status", objectStatusValues[rand.nextInt(objectStatusValues.length)]),
                o -> o.addattrvalue("Eigentuemer", "Keine_Angabe"));
    }

    private IomObject createTextObject(int oid, double x, double y, String objectRef) {
        return IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "obj_" + oid,
                o -> o.addattrobj("TextPos", IomObjectHelper.createCoord(Double.toString(x), Double.toString(y))),
                o -> o.addattrvalue("TextOri", "90"),
                o -> o.addattrvalue("TextHAli", "Left"),
                o -> o.addattrvalue("TextVAli", "Base"),
                o -> o.addattrvalue("Textinhalt", "obj_" + oid),
                o -> o.addattrobj("LKObjektRef", IomObjectHelper.createIomObject("REF", null,
                        r -> r.setobjectrefoid(objectRef))));
    }
}
