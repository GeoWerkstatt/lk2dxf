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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        for (int i = 0; i < 3; i++) {
            var file = new File(TEST_OUT_DIR + "multipleFilesWithPerimeter" + "_" + i + ".xtf");
            files.add(file);
            writeTestXTF(file, i, 1000);
        }

        // Run the main method
        var perimeter = "Polygon ((740857.55514181009493768 256024.12385561052360572, 607598.44303888664580882 264279.11310092435451224, 526620.92948961758520454 99572.42292061494663358, 578116.33859133732039481 101537.89655045152176172, 625680.80043338367249817 178584.46284004737390205, 706265.21925668546464294 173867.32612843945389614, 740857.55514181009493768 101930.99127641884842888, 795890.81677723571192473 100751.7070985168684274, 740857.55514181009493768 256024.12385561052360572))";
        Main.main(Stream.concat(files.stream().map(File::getAbsolutePath), Stream.of(output, "--perimeter", perimeter)).toArray(String[]::new));

        // Check output file
        assertTrue(outputFile.exists());
        assertTrue(outputFile.isFile());
        assertEquals(816018, outputFile.length());
    }

    private void writeTestXTF(File file, int seed, int objectCount) throws IOException, URISyntaxException, Ili2cException, IoxException {
        var objectMapper = new ObjectMapper();
        var rand = new Random(seed);

        XtfWriter writer = new XtfWriter(file, objectMapper.getTransferDescription());
        try {
            writer.write(new StartTransferEvent());
            writer.write(new StartBasketEvent("SIA405_LKMap_2015_LV95.SIA405_LKMap", "BASKET1"));
            for (int i = 0; i < objectCount; i += 2) {
                var x = rand.nextDouble() * 300_000 + 510_000;
                var y = rand.nextDouble() * 200_000 + 85_000;
                var size = rand.nextDouble() * 5000 + 5000;
                writer.write(new ObjectEvent(createTextObject(i, x, y, "obj_" + (i + 1))));
                writer.write(new ObjectEvent(createLineObject(i + 1, x, y, size)));
            }
            writer.write(new EndBasketEvent());
            writer.write(new EndTransferEvent());
        } finally {
            writer.close();
        }
    }

    private final String[] precisionValues = new String[]{"genau", "unbekannt", "ungenau"};
    private final String[] lineObjectTypeValues = new String[]{"Abwasser.Fernwirkkabel", "Abwasser.Haltung_Kanal", "Abwasser.Schutzrohr", "Elektrizitaet.AnkerStrebe", "Elektrizitaet.Trasse.oberirdisch", "Elektrizitaet.Trasse.unterirdisch", "Fernwaerme.Fernwirkkabel", "Fernwaerme.Trasse", "Gas.Fernwirkkabel", "Gas.Leitung", "Gas.Schutzrohr", "Kommunikation.Trasse.oberirdisch", "Kommunikation.Trasse.unterirdisch", "Wasser.Fernwirkkabel", "Wasser.Leitung", "Wasser.Schutzrohr", "weitereMedien.Fernwirkkabel", "weitereMedien.Leitung", "weitereMedien.Schutzrohr"};
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
        return IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKLinie", "obj_" + oid,
                o -> o.addattrobj("Linie", polyline),
                o -> o.addattrvalue("Objektart", lineObjectTypeValues[oid % lineObjectTypeValues.length]),
                o -> o.addattrvalue("Lagebestimmung", precisionValues[oid % precisionValues.length]),
                o -> o.addattrvalue("Status", objectStatusValues[oid % objectStatusValues.length]),
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
