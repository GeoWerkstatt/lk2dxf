package ch.geowerkstatt.lk2dxf.mapping;

import ch.geowerkstatt.lk2dxf.MappedObject;
import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.metamodel.AbstractClassDef;
import ch.interlis.ili2c.metamodel.AbstractLeafElement;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.CompositionType;
import ch.interlis.ili2c.metamodel.EnumerationType;
import ch.interlis.ili2c.metamodel.ExtendableContainer;
import ch.interlis.ili2c.metamodel.RoleDef;
import ch.interlis.ili2c.metamodel.Table;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import ch.interlis.iox_j.validator.Value;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ObjectMapper {
    private static final String MODELS_RESOURCE = "/models";
    private static final List<LayerMapping> LAYER_MAPPINGS;
    private static final TransferDescription TRANSFER_DESCRIPTION;

    private static final Map<AbstractClassDef<?>, Set<PathElement>> CACHE_REQUIREMENTS = new HashMap<AbstractClassDef<?>, Set<PathElement>>();
    private static final List<Mapper> FILTERS;

    static {
        try {
            LAYER_MAPPINGS = MappingReader.readMappings();
            TRANSFER_DESCRIPTION = getTransferDescription();
            FILTERS = analyzeLayerMappings();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read layer mappings.", e);
        }
    }

    private final Map<String, IomObject> objectCache = new HashMap<>();
    private final Set<IomObject> objectsWithUnresolvedRef = new HashSet<>();

    private static List<Mapper> analyzeLayerMappings() {
        var mappers = new ArrayList<Mapper>();
        for (LayerMapping layerMapping : ObjectMapper.LAYER_MAPPINGS) {
            for (String objectClass : layerMapping.objectClass()) {
                var element = TRANSFER_DESCRIPTION.getElement(objectClass);
                if (element == null) {
                    throw new IllegalArgumentException("No element found for object with id \"" + objectClass + "\".");
                }
                if (!(element instanceof Table classDef)) {
                    throw new IllegalArgumentException("Element is not an AbstractClassDef for object with id \"" + objectClass + "\".");
                }

                var filter = new ArrayList<Filter>();
                filter.add(new TagFilter(classDef.getScopedName()));

                for (var baseAttributeName : layerMapping.mapping().keySet()) {
                    var values = layerMapping.mapping().get(baseAttributeName);
                    var pathElements = getTranslatedPath(classDef, Arrays.asList(baseAttributeName.split("->")));
                    analyzeCacheRequirements(pathElements, CACHE_REQUIREMENTS);
                    var type = ((AttributeDef) pathElements.getLast().element).getDomainResolvingAliases();
                    if (!(type instanceof EnumerationType enumerationType)) {
                        throw new IllegalArgumentException("Only enumeratino types supported" + baseAttributeName);
                    }

                    var attrFilter = new PathMatcher(pathElements, values.stream().map(v -> getTranslatedEnumValue(enumerationType, v)).toList());
                    filter.add(attrFilter);
                }

                // translate attributes
                var mapper = switch (layerMapping.output()) {
                    case SURFACE, LINE -> new Mapper(filter,
                            layerMapping,
                            getTranslatedPath(classDef, layerMapping.geometry()),
                            null,
                            null,
                            null,
                            null);
                    case TEXT -> new Mapper(filter,
                            layerMapping,
                            getTranslatedPath(classDef, layerMapping.geometry()),
                            getTranslatedPath(classDef, layerMapping.orientation()),
                            getTranslatedPath(classDef, layerMapping.vAlign()),
                            getTranslatedPath(classDef, layerMapping.hAlign()),
                            getTranslatedPath(classDef, layerMapping.text()));
                    case POINT -> new Mapper(filter,
                            layerMapping,
                            getTranslatedPath(classDef, layerMapping.geometry()),
                            getTranslatedPath(classDef, layerMapping.orientation()),
                            null,
                            null,
                            null);
                };
                mappers.add(mapper);
            }
        }
        return mappers;
    }

    /**
     * Get the layer mappings as an immutable list.
     */
    public static List<LayerMapping> getLayerMappings() {
        return Collections.unmodifiableList(LAYER_MAPPINGS);
    }

    private static boolean matchesEnumSubValue(List<String> allowedValues, String attrValue) {
        return allowedValues.stream().anyMatch(value -> attrValue.startsWith(value + "."));
    }

    /**
     * Resolve the value of the given path in the given {@link IomObject}.
     *
     * @param iomObject   The {@link IomObject} to resolve the path in.
     * @param path        The path to resolve.
     * @param objectCache The cache of objects to resolve references.
     * @return The resolved value. If the path could not be resolved, {@link Value#createUndefined()} is returned. If the
     * path fails to resolve because a reference was not found in the cache, {@link Value#createSkipEvaluation()} is returned.
     */
    private static Value resolve(IomObject iomObject, List<PathElement> path, Map<String, IomObject> objectCache) {
        if (path == null) {
            return Value.createUndefined();
        }

        var current = iomObject;
        for (var element : path) {
            if (current == null) {
                break;
            }
            switch (element.resolution) {
                case ATTRIBUTE -> {
                    var value = current.getattrvalue(element.name());
                    if (value != null) {
                        return new Value(null, value);
                    } else {
                        var complexValue = current.getattrobj(element.name(), 0);
                        if (complexValue != null) {
                            return new Value(List.of(complexValue));
                        } else {
                            return Value.createUndefined();
                        }
                    }
                }
                case REFERENCE -> {
                    var refOid = Optional.ofNullable(current.getattrobj(element.name(), 0)).map(IomObject::getobjectrefoid);
                    if (refOid.isEmpty()) {
                        current = null;
                    } else {
                        current = objectCache.get(refOid.get());
                        if (current == null) {
                            return Value.createSkipEvaluation();
                        }
                    }
                }
                case STRUCTURE -> {
                    current = current.getattrobj(element.name(), 0);
                }
                default -> throw new AssertionError("Unexpected value: " + element.resolution);
            }
        }

        if (current != null) {
            return new Value(List.of(current));
        } else {
            return Value.createUndefined();
        }
    }

    /**
     * Analyze a path for references that need a referenced object in the cache. Update the {@link #CACHE_REQUIREMENTS} accordingly.
     */
    private static void analyzeCacheRequirements(List<PathElement> path, Map<AbstractClassDef<?>, Set<PathElement>> cacheRequirements) {
        for (int i = 0; i < path.size(); i++) {
            var pathElement = path.get(i);
            if (pathElement.resolution == PathElement.Resolution.REFERENCE) {
                var it = ((RoleDef) pathElement.element).iteratorDestination();
                while (it.hasNext()) {
                    for (var extension : it.next().getExtensions()) {
                        cacheRequirements.computeIfAbsent((AbstractClassDef<?>) extension, k -> new HashSet<>()).add(path.get(i + 1));
                    }
                }
            }
        }
    }

    /**
     * @see #getTranslatedPath(AbstractClassDef, List)
     */
    private static List<PathElement> getTranslatedPath(AbstractClassDef<?> viewable, String basePathElements) {
        return getTranslatedPath(viewable, Arrays.asList(basePathElements.split("->")));
    }

    /**
     * Get the translated path elements for the given attribute path.
     *
     * @param viewable         The viewable where the path starts
     * @param basePathElements The path elements to translate
     * @return The {@link PathElement}s in the language of the {@code TRANSLATION OF} model.
     */
    private static List<PathElement> getTranslatedPath(AbstractClassDef<?> viewable, final List<String> basePathElements) {
        var element = getTranslatedAttributeOrRole(viewable, basePathElements.getFirst());
        return switch (element) {
            case AttributeDef attributeDef -> {
                Type type = attributeDef.getDomainResolvingAll();
                if (type instanceof CompositionType compositionType) {
                    if (basePathElements.size() == 1) {
                        throw new IllegalArgumentException("Expected the path to continue but it ended at: " + basePathElements);
                    }
                    var pathElements = getTranslatedPath(compositionType.getComponentType(), basePathElements.subList(1, basePathElements.size()));
                    pathElements.addFirst(new PathElement(attributeDef.getName(), PathElement.Resolution.STRUCTURE, attributeDef));
                    yield pathElements;
                } else {
                    if (basePathElements.size() > 1) {
                        throw new IllegalArgumentException("Expected the path to end at an attribute, but the path goes on: " + basePathElements);
                    }
                    var pathElements = new ArrayList<PathElement>();
                    pathElements.add(new PathElement(attributeDef.getName(), PathElement.Resolution.ATTRIBUTE, attributeDef));
                    yield pathElements;
                }
            }
            case RoleDef roleDef -> {
                if (basePathElements.size() == 1) {
                    throw new IllegalArgumentException("Expected the path to continue but it ended at: " + basePathElements);
                }
                var pathElements = getTranslatedPath(roleDef.iteratorDestination().next(), basePathElements.subList(1, basePathElements.size()));
                pathElements.addFirst(new PathElement(roleDef.getName(), PathElement.Resolution.REFERENCE, roleDef));
                yield pathElements;
            }
            default -> throw new IllegalStateException("Unexpected value: " + element);
        };
    }

    /**
     * Checks if an attribute or role exists and translates it to the name in the {@code TRANSLATION OF} model.
     */
    private static AbstractLeafElement getTranslatedAttributeOrRole(Viewable<?> viewable, String attributeBaseName) {
        for (ExtendableContainer<?> extension : viewable.getExtensions()) {
            for (var it = ((Viewable<?>) extension).getAttributesAndRoles2(); it.hasNext();) {
                var result = switch (it.next().obj) {
                    case AttributeDef attributeDef ->
                            (attributeDef.getTranslationOfOrSame().getName().equals(attributeBaseName) ? attributeDef : null);
                    case RoleDef roleDef ->
                            (roleDef.getTranslationOfOrSame().getName().equals(attributeBaseName) ? roleDef : null);
                    default -> throw new IllegalStateException("Unexpected value: " + it.next().obj);
                };

                if (result != null) {
                    return result;
                }
            }
        }

        throw new IllegalArgumentException("Attribute or role not found: " + attributeBaseName);
    }

    /**
     * Checks if an enumeration value exists and translates it to the name in the {@code TRANSLATION OF} model.
     */
    private static String getTranslatedEnumValue(EnumerationType type, String enumerationBaseName) {
        var baseElements = enumerationBaseName.split("\\.");
        var translatedElements = new ArrayList<String>();
        var enumeration = type.getConsolidatedEnumeration();
        for (var baseElement : baseElements) {
            for (var it = enumeration.getElements(); it.hasNext();) {
                var element = it.next();
                if (element.getTranslationOfOrSame().getName().equals(baseElement)) {
                    translatedElements.add(element.getName());
                    enumeration = element.getSubEnumeration();
                    break;
                }
            }
        }

        if (translatedElements.size() != baseElements.length) {
            throw new IllegalArgumentException("Enumeration value not found: " + enumerationBaseName);
        }

        return String.join(".", translatedElements);
    }

    /**
     * Get the {@link TransferDescription} with all models used in the layerMappings.
     */
    private static TransferDescription getTransferDescription() throws Ili2cException, IOException, URISyntaxException {
        var requiredModels = ObjectMapper.LAYER_MAPPINGS.stream()
                .map(LayerMapping::objectClass)
                .flatMap(Collection::stream)
                .map(c -> c.substring(0, c.indexOf('.')))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));

        // prepare path to models from resources
        var resourceUri = ObjectMapper.class.getResource(MODELS_RESOURCE).toURI();
        var tempDir = Files.createTempDirectory("lk2dxf_");
        var iliModelsPath = tempDir.toString().replace("\\", "/");
        if (resourceUri.getScheme().equals("jar")) {
            try (var fs = FileSystems.newFileSystem(resourceUri, Collections.emptyMap());
                 var sourceFiles = Files.walk(fs.getPath(MODELS_RESOURCE)).filter(Files::isRegularFile)) {
                for (var source : sourceFiles.toArray(Path[]::new)) {
                    Files.copy(source, Paths.get(tempDir.toString(), source.getFileName().toString()));
                }
            }
        } else {
            try (var sourceFiles = Files.walk(Paths.get(resourceUri))) {
                for (var source : sourceFiles.toArray(Path[]::new)) {
                    Files.copy(source, Paths.get(tempDir.toString(), source.getFileName().toString()));
                }
            }
        }

        try {
            System.out.println("iliModelsPath: " + iliModelsPath);
            var modelManager = new IliManager();
            modelManager.setRepositories(new String[]{iliModelsPath});
            var ili2cConfig = modelManager.getConfig(requiredModels, 0.0);
            return Ili2c.runCompiler(ili2cConfig);
        } finally {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    /**
     * Maps the given {@link IomObject} stream to a stream containing their layer information.
     *
     * @param iomObjects The {@link IomObject} stream to map.
     * @return A stream of mapped objects.
     */
    public Stream<MappedObject> mapObjects(Stream<IomObject> iomObjects) {
        // Combine streams using flatMap instead of concat to process objectsWithRef
        // after all objects have been processed by the first stream.
        var combinedStream = Stream.<Supplier<Stream<Optional<MappedObject>>>>of(
                () -> iomObjects.map(b -> mapObject(b, true)),
                () -> objectsWithUnresolvedRef.stream().map(b -> mapObject(b, false))
        ).flatMap(Supplier::get);

        return combinedStream
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<MappedObject> mapObject(IomObject iomObject, boolean unresolvedReferencesAllowed) {
        var element = TRANSFER_DESCRIPTION.getElement(iomObject.getobjecttag());
        if (element == null) {
            System.out.println("No element found for object with id \"" + iomObject.getobjectoid() + "\".");
            return Optional.empty();
        }
        if (!(element instanceof AbstractClassDef<?> classDef)) {
            System.out.println("Element is not an AbstractClassDef for object with id \"" + iomObject.getobjectoid() + "\".");
            return Optional.empty();
        }

        // cache part of the object if necessary
        var pathElements = CACHE_REQUIREMENTS.get(classDef);
        if (pathElements != null) {
            IomObject cacheObject = new Iom_jObject(iomObject.getobjecttag(), iomObject.getobjectoid());
            for (var pathElement : pathElements) {
                var value = resolve(iomObject, List.of(pathElement), objectCache);
                if (value.skipEvaluation() || value.isUndefined()) {
                    continue;
                }
                if (value.getValue() != null) {
                    cacheObject.setattrvalue(pathElement.name, value.getValue());
                } else if (value.getComplexObjects() != null) {
                    cacheObject.addattrobj(pathElement.name, value.getComplexObjects().iterator().next());
                }
            }
            objectCache.put(iomObject.getobjectoid(), cacheObject);
        }

        return FILTERS.stream()
                .filter(filter -> filter.filter.stream().allMatch((f) ->
                        switch (f.matches(iomObject, objectCache)) {
                            case UNRESOLVED_REF -> {
                                if (unresolvedReferencesAllowed) {
                                    objectsWithUnresolvedRef.add(iomObject);
                                    yield false;
                                } else {
                                    throw new IllegalStateException("Unresolved reference in object with id \"" + iomObject.getobjectoid() + "\".");
                                }
                            }
                            case MATCH -> true;
                            case NO_MATCH -> false;
                        }))
                .findFirst()
                .map(filter -> new MappedObject(
                        iomObject.getobjectoid(),
                        Optional.ofNullable(resolve(iomObject, filter.geometry(), objectCache).getComplexObjects()).map(Collection::iterator).map(Iterator::next).orElse(null),
                        Optional.ofNullable(resolve(iomObject, filter.orientation(), objectCache).getValue()).map(Double::parseDouble).orElse(null),
                        resolve(iomObject, filter.vAlign(), objectCache).getValue(),
                        resolve(iomObject, filter.hAlign(), objectCache).getValue(),
                        resolve(iomObject, filter.text(), objectCache).getValue(),
                        filter.mapping()));
    }

    private interface Filter {
        MatchResult matches(IomObject iomObject, Map<String, IomObject> objectCache);

        enum MatchResult {
            UNRESOLVED_REF,
            MATCH,
            NO_MATCH,
        }
    }

    private record Mapper(
            List<Filter> filter,
            LayerMapping mapping,
            List<PathElement> geometry,
            List<PathElement> orientation,
            List<PathElement> vAlign,
            List<PathElement> hAlign,
            List<PathElement> text
    ) {
    }

    private record TagFilter(String tag) implements Filter {
        @Override
        public MatchResult matches(IomObject iomObject, Map<String, IomObject> objectCache) {
            return iomObject.getobjecttag().equals(tag) ? MatchResult.MATCH : MatchResult.NO_MATCH;
        }
    }

    private record PathMatcher(List<PathElement> pathElements, List<String> values) implements Filter {
        @Override
        public MatchResult matches(IomObject iomObject, Map<String, IomObject> objectCache) {
            var value = resolve(iomObject, pathElements, objectCache);
            if (value.skipEvaluation()) {
                return MatchResult.UNRESOLVED_REF;
            } else if (value.isUndefined()) {
                return MatchResult.NO_MATCH;
            } else {
                if (values.contains(value.getValue()) || matchesEnumSubValue(values, value.getValue())) {
                    return MatchResult.MATCH;
                } else {
                    return MatchResult.NO_MATCH;
                }
            }
        }
    }

    private record PathElement(String name, Resolution resolution, AbstractLeafElement element) {
        public enum Resolution {
            ATTRIBUTE,
            STRUCTURE,
            REFERENCE,
        }
    }
}
