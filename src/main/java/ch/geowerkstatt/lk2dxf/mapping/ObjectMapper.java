package ch.geowerkstatt.lk2dxf.mapping;

import ch.geowerkstatt.lk2dxf.MappedObject;
import ch.interlis.iom.IomObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ObjectMapper {
    private static final String OBJECT_TYPE_ATTRIBUTE = "Objektart";
    private static final String OBJECT_REF_ATTRIBUTE = "LKObjektRef";
    private static final List<LayerMapping> LAYER_MAPPINGS;

    private final Map<String, String> objectTypeCache = new HashMap<>();
    private final List<IomObject> objectsWithRef = new ArrayList<>();

    static {
        try {
            LAYER_MAPPINGS = MappingReader.readMappings();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read layer mappings.", e);
        }
    }

    /**
     * Maps the given {@link IomObject} stream to a stream containing their layer information.
     * @param iomObjects The {@link IomObject} stream to map.
     * @return A stream of mapped objects.
     */
    public Stream<MappedObject> mapObjects(Stream<IomObject> iomObjects) {
        // Combine streams using flatMap instead of concat to process objectsWithRef
        // after all objects have been processed by the first stream.
        var combinedStream = Stream.<Supplier<Stream<Optional<MappedObject>>>>of(
                () -> iomObjects.map(this::mapObject),
                () -> objectsWithRef.stream().map(this::getLayerMapping)
        ).flatMap(Supplier::get);

        return combinedStream
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<MappedObject> mapObject(IomObject iomObject) {
        String objectType = iomObject.getattrvalue(OBJECT_TYPE_ATTRIBUTE);
        if (objectType == null) {
            // Process objects with reference (LKObjekt_Text) after all other objects have been read.
            objectsWithRef.add(iomObject);
            return Optional.empty();
        }

        objectTypeCache.put(iomObject.getobjectoid(), objectType);
        return getLayerMapping(iomObject);
    }

    private Optional<MappedObject> getLayerMapping(IomObject iomObject) {
        Optional<LayerMapping> layerMapping = LAYER_MAPPINGS.stream()
                .filter(mapping -> matchesMapping(mapping, iomObject))
                .findFirst();
        if (layerMapping.isEmpty()) {
            System.err.println("No mapping found for object with id \"" + iomObject.getobjectoid() + "\".");
        }
        return layerMapping.map(mapping -> MappedObject.create(iomObject, mapping));
    }

    private boolean matchesMapping(LayerMapping layerMapping, IomObject iomObject) {
        if (!iomObject.getobjecttag().endsWith("." + layerMapping.objectClass())) {
            return false;
        }

        return layerMapping.mapping()
                .entrySet()
                .stream()
                .allMatch(e -> {
                    String attrName = e.getKey();
                    String attrValue = getAttributeValue(iomObject, attrName);
                    List<String> values = e.getValue();
                    return values.contains(attrValue) || matchesEnumSubValue(values, attrValue);
                });
    }

    private String getAttributeValue(IomObject iomObject, String attributeName) {
        String attrValue = iomObject.getattrvalue(attributeName);
        if (attrValue == null && attributeName.equals(OBJECT_TYPE_ATTRIBUTE)) {
            String lkObjektId = iomObject.getattrobj(OBJECT_REF_ATTRIBUTE, 0).getobjectrefoid();
            attrValue = objectTypeCache.get(lkObjektId);
        }
        return attrValue;
    }

    private boolean matchesEnumSubValue(List<String> allowedValues, String attrValue) {
        return allowedValues.stream().anyMatch(value -> attrValue.startsWith(value + "."));
    }
}
