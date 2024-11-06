package ch.geowerkstatt.lk2dxf;

import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.itf.impl.jtsext.geom.JtsextGeometryFactory;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.jts.Iox2jtsext;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public record GeometryObject(Geometry geometry, IomObject iomObject) {
    private static final String BASKET_NAME = "SIA405_LKMap_2015_LV95.SIA405_LKMap";
    private static final GeometryFactory GEOMETRY_FACTORY = new JtsextGeometryFactory();

    /**
     * Creates a new geometry object from the given {@link IomObject}.
     * @param iomObject The {@link IomObject} to create the geometry object from.
     * @return A geometry object containing the {@link IomObject} and its extracted {@link Geometry}.
     * @throws IllegalArgumentException If the object tag is not supported.
     * @throws RuntimeException If an error occurs while extracting the geometry.
     */
    public static GeometryObject create(IomObject iomObject) {
        try {
            Geometry geometry = switch (iomObject.getobjecttag()) {
                case BASKET_NAME + ".LKPunkt" -> readPoint(iomObject, "SymbolPos");
                case BASKET_NAME + ".LKLinie" -> readLine(iomObject, "Linie");
                case BASKET_NAME + ".LKFlaeche" -> readSurface(iomObject, "Flaeche");
                case BASKET_NAME + ".LKObjekt_Text" -> readPoint(iomObject, "TextPos");
                default -> throw new IllegalArgumentException("Unsupported object tag: " + iomObject.getobjecttag());
            };
            return new GeometryObject(geometry, iomObject);
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
