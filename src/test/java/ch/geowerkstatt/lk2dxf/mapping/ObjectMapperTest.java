package ch.geowerkstatt.lk2dxf.mapping;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.StdListener;
import ch.geowerkstatt.lk2dxf.EhiLogAdapter;
import ch.geowerkstatt.lk2dxf.IomObjectHelper;
import ch.geowerkstatt.lk2dxf.MappedObject;
import ch.geowerkstatt.lk2dxf.XtfStreamReader;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ObjectMapperTest {
    private static final String TEST_FILE = "src/test/data/MapperTest/MapWithText.xtf";

    @BeforeAll
    public static void setup() {
        // Configure logging
        Configurator.setRootLevel(Level.TRACE);
        EhiLogger.getInstance().addListener(new EhiLogAdapter());
        EhiLogger.getInstance().removeListener(StdListener.getInstance());
    }

    @Test
    public void mapObject() throws Exception {
        try (XtfStreamReader xtfReader = new XtfStreamReader(new File(TEST_FILE))) {
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

    @Test
    public void mappingContainsNonexistentPath() {
        var layerMappings = createTextLayerMapping(Map.of("NonExistent_LOGREMAT", List.of("NonExistent_RENAPHIP")));
        var exception = assertThrows(IllegalArgumentException.class, () -> new ObjectMapper(layerMappings));
        assertEquals("Attribute or role not found: NonExistent_LOGREMAT", exception.getMessage());
    }

    @Test
    public void mappingContainsNonExistentValue() {
        var layerMappings = createTextLayerMapping(Map.of("Plantyp", List.of("NonExistent_RENAPHIP")));
        var exception = assertThrows(IllegalArgumentException.class, () -> new ObjectMapper(layerMappings));
        assertEquals("Enumeration value not found: NonExistent_RENAPHIP", exception.getMessage());
    }

    @Test
    public void mappingContainsTextAttributeFilter() {
        var layerMappings = createTextLayerMapping(Map.of("Bemerkung", List.of("SULTIOND")));
        var exception = assertThrows(IllegalArgumentException.class, () -> new ObjectMapper(layerMappings));
        assertEquals("Only enumeration types supported: Bemerkung", exception.getMessage());
    }

    @Test
    public void pathEndsAtReference() {
        var layerMappings = createTextLayerMapping(Map.of("LKObjektRef", List.of("Abwasser")));
        var exception = assertThrows(IllegalArgumentException.class, () -> new ObjectMapper(layerMappings));
        assertEquals("Expected the path to continue but it ended at: [LKObjektRef]", exception.getMessage());
    }

    @Test
    public void pathEndsAtStruct() {
        var layerMappings = createTextLayerMapping(Map.of("LKObjektRef->Metaattribute", List.of("Abwasser")));
        var exception = assertThrows(IllegalArgumentException.class, () -> new ObjectMapper(layerMappings));
        assertEquals("Expected the path to continue but it ended at: [Metaattribute]", exception.getMessage());
    }

    @Test
    public void pathContinuesAfterAttribute() {
        var layerMappings = createTextLayerMapping(Map.of("LKObjektRef->Objektart->UnexpectedAttribute", List.of("Abwasser")));
        var exception = assertThrows(IllegalArgumentException.class, () -> new ObjectMapper(layerMappings));
        assertEquals("Expected the path to end at an attribute, but the path goes on: [Objektart, UnexpectedAttribute]", exception.getMessage());
    }

    @Test
    public void mappingContainsEnumFilter() throws Exception {
        var layerMappings = createTextLayerMapping(Map.of("Plantyp", List.of("Leitungskataster")));
        var objectMapper = new ObjectMapper(layerMappings);

        var objA = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "obj_A",
                o -> o.addattrvalue("Plantyp", "Leitungskataster"));
        var objB = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "obj_B",
                o -> o.addattrvalue("Plantyp", "Werkplan"));

        String[] actual = getMappedLayers(objectMapper, objA, objB);
        assertArrayEquals(new String[] {"Test", "CatchAllText"}, actual);
    }

    @Test
    public void mappingContainsEnumFilterWithMultipleOptions() throws Exception {
        var layerMappings = createTextLayerMapping(Map.of("Plantyp", List.of("Werkplan", "Leitungskataster")));
        var objectMapper = new ObjectMapper(layerMappings);

        var objA = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "obj_A",
                o -> o.addattrvalue("Plantyp", "Leitungskataster"));
        var objB = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "obj_B",
                o -> o.addattrvalue("Plantyp", "Werkplan"));

        String[] actual = getMappedLayers(objectMapper, objA, objB);
        assertArrayEquals(new String[] {"Test", "Test"}, actual);
    }

    @Test
    public void mapTranslatedObject() throws Exception {
        var layerMappings = createTextLayerMapping(Map.of("Plantyp", List.of("Werkplan", "Uebersichtsplan.UeP10")));
        var objectMapper = new ObjectMapper(layerMappings);

        var objA = IomObjectHelper.createIomObject("SIA405_LKMap_2015_f_LV95.SIA405_LKMap_f.LKOBJET_Texte", "obj_A",
                o -> o.addattrvalue("TYPE_DE_PLAN", "plan_de_reseau"));
        var objB = IomObjectHelper.createIomObject("SIA405_LKMap_2015_f_LV95.SIA405_LKMap_f.LKOBJET_Texte", "obj_B",
                o -> o.addattrvalue("TYPE_DE_PLAN", "plan_d_ensemble.pe10"));

        String[] actual = getMappedLayers(objectMapper, objA, objB);
        assertArrayEquals(new String[] {"Test", "Test"}, actual);
    }

    @Test
    public void mapSubEnum() throws Exception {
        var layerMappings = createTextLayerMapping(Map.of("Plantyp", List.of("Uebersichtsplan")));
        var objectMapper = new ObjectMapper(layerMappings);

        var objA = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "obj_A",
                o -> o.addattrvalue("Plantyp", "Uebersichtsplan.UeP5"));
        var objB = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "obj_B",
                o -> o.addattrvalue("Plantyp", "Werkplan"));

        String[] actual = getMappedLayers(objectMapper, objA, objB);
        assertArrayEquals(new String[] {"Test", "CatchAllText"}, actual);
    }

    @Test
    public void mapObjectWithForwardRef() throws Exception {
        var layerMappings = createTextLayerMapping(Map.of("LKObjektRef->Objektart", List.of("Elektrizitaet")));
        var objectMapper = new ObjectMapper(layerMappings);

        var textObj = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "textObj",
                o -> o.addattrobj("LKObjektRef", IomObjectHelper.createIomObject("REF", null,
                        r -> r.setobjectrefoid("punktObj"))));

        var pointObj = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKPunkt", "punktObj",
                o -> o.addattrvalue("Objektart", "Elektrizitaet"));

        String[] actual = getMappedLayers(objectMapper, textObj, pointObj);
        assertArrayEquals(new String[] {"Test"}, actual);
    }

    @Test
    public void mapObjectWithBackwardRef() throws Exception {
        var layerMappings = createTextLayerMapping(Map.of("LKObjektRef->Objektart", List.of("Elektrizitaet")));
        var objectMapper = new ObjectMapper(layerMappings);

        var textObj = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "textObj",
                o -> o.addattrobj("LKObjektRef", IomObjectHelper.createIomObject("REF", null,
                        r -> r.setobjectrefoid("punktObj"))));

        var pointObj = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKPunkt", "punktObj",
                o -> o.addattrvalue("Objektart", "Elektrizitaet"));

        String[] actual = getMappedLayers(objectMapper, pointObj, textObj);
        assertArrayEquals(new String[] {"Test"}, actual);
    }

    @Test
    public void mapObjectWithMissingRef() throws Exception {
        var layerMappings = createTextLayerMapping(Map.of("LKObjektRef->Objektart", List.of("Elektrizitaet")));
        var objectMapper = new ObjectMapper(layerMappings);

        var textObj = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "textObj",
                o -> o.addattrobj("LKObjektRef", IomObjectHelper.createIomObject("REF", null,
                        r -> r.setobjectrefoid("punktObj"))));

        var exception = assertThrows(IllegalStateException.class, () -> objectMapper.mapObjects(Stream.of(textObj)).toList());
        assertEquals("Unresolved reference in object with id \"textObj\".", exception.getMessage());
    }

    @Test
    public void mapObjectWithEmptyRef() throws Exception {
        var layerMappings = createTextLayerMapping(Map.of("LKObjektRef->Objektart", List.of("Elektrizitaet")));
        var objectMapper = new ObjectMapper(layerMappings);

        var textObj = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "textObj");

        String[] actual = getMappedLayers(objectMapper, textObj);
        assertArrayEquals(new String[] {"CatchAllText"}, actual);
    }

    @Test
    public void resolvePathWithStruct() throws Exception {
        var layerMappings = List.of(
                new LayerMapping("Test", List.of("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text"), LayerMapping.OutputType.TEXT,
                        "TextPos", 1, "TextOri", "TextVAli", "TextHAli", "LKObjektRef->Metaattribute->Datenlieferant",
                        "TestSymbol", "", 0.25, 1.25, "arial", Map.of("LKObjektRef->Objektart", List.of("Elektrizitaet"))));
        var objectMapper = new ObjectMapper(layerMappings);

        var textObj = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text", "textObj",
                o -> o.addattrobj("LKObjektRef", IomObjectHelper.createIomObject("REF", null,
                        r -> r.setobjectrefoid("punktObj"))));
        var pointObj = IomObjectHelper.createIomObject("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKPunkt", "punktObj",
                o -> o.addattrvalue("Objektart", "Elektrizitaet"),
                o -> o.addattrobj("Metaattribute", IomObjectHelper.createIomObject("Metaattribute", null,
                        m -> m.addattrvalue("Datenlieferant", "MINATERI"))));

        List<MappedObject> output = objectMapper.mapObjects(Stream.of(textObj, pointObj)).toList();
        assertEquals(1, output.size());
        assertEquals("MINATERI", output.getFirst().text());
    }

    private List<LayerMapping> createTextLayerMapping(Map<String, List<String>> mapping) {
        return List.of(
                createTextLayerMapping("Test", mapping),
                createTextLayerMapping("CatchAllText", Map.of())
        );
    }

    private LayerMapping createTextLayerMapping(String layerName, Map<String, List<String>> mapping) {
        return new LayerMapping(layerName,
                List.of("SIA405_LKMap_2015_LV95.SIA405_LKMap.LKObjekt_Text",
                        "SIA405_LKMap_2015_f_LV95.SIA405_LKMap_f.LKOBJET_Texte"),
                LayerMapping.OutputType.TEXT,
                "TextPos",
                1,
                "TextOri",
                "TextVAli",
                "TextHAli",
                "Textinhalt",
                "TestSymbol",
                "",
                0.25,
                1.25,
                "arial",
                mapping);
    }

    private String[] getMappedLayers(ObjectMapper objectMapper, IomObject... objects) {
        return objectMapper.mapObjects(Stream.of(objects))
                .map(o -> o.layerMapping().layer())
                .toArray(String[]::new);
    }
}
