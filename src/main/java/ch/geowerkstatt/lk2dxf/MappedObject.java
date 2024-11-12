package ch.geowerkstatt.lk2dxf;

import ch.geowerkstatt.lk2dxf.mapping.LayerMapping;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.itf.impl.jtsext.geom.JtsextGeometryFactory;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.jts.Iox2jtsext;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public record MappedObject(Geometry geometry, IomObject iomObject, LayerMapping layerMapping) {
    private static final GeometryFactory GEOMETRY_FACTORY = new JtsextGeometryFactory();

    /**
     * Creates a new geometry object from the given {@link IomObject}.
     * @param iomObject The {@link IomObject} to create the geometry object from.
     * @return A geometry object containing the {@link IomObject} and its extracted {@link Geometry}.
     * @throws IllegalArgumentException If the object tag is not supported.
     * @throws RuntimeException If an error occurs while extracting the geometry.
     */
    public static MappedObject create(IomObject iomObject, LayerMapping layerMapping) {
        try {
            Geometry geometry = switch (layerMapping.geometryType()) {
                case "Point" -> readPoint(iomObject, layerMapping.geometry());
                case "Line" -> readLine(iomObject, layerMapping.geometry());
                case "Surface" -> readSurface(iomObject, layerMapping.geometry());
                default -> throw new IllegalArgumentException("Unsupported object tag: " + iomObject.getobjecttag());
            };
            return new MappedObject(geometry, iomObject, layerMapping);
        } catch (IoxException e) {
            throw new RuntimeException("Error creating geometry for object with id \"" + iomObject.getobjectoid() + "\".", e);
        }
    }

    private static Geometry readPoint(IomObject iomObject, String attributeName) throws IoxException {
        IomObject position = iomObject.getattrobj(attributeName, 0);
        return GEOMETRY_FACTORY.createPoint(Iox2jtsext.coord2JTS(position));
    }

    private static Geometry readLine(IomObject iomObject, String attributeName) throws IoxException {
        IomObject line = iomObject.getattrobj(attributeName, 0);
        return Iox2jtsext.polyline2JTS(line, false, 0.0);
    }

    private static Geometry readSurface(IomObject iomObject, String attributeName) throws IoxException {
        IomObject surface = iomObject.getattrobj(attributeName, 0);
        return Iox2jtsext.surface2JTS(surface, 0.0);
    }
}
