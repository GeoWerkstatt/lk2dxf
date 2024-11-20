package ch.geowerkstatt.lk2dxf;

import ch.geowerkstatt.lk2dxf.mapping.LayerMapping;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.itf.impl.jtsext.geom.JtsextGeometryFactory;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.jts.Iox2jtsext;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record MappedObject(
        String oid,
        Geometry geometry,
        IomObject iomGeometry,
        double orientation,
        String vAlign,
        String hAlign,
        String text,
        LayerMapping layerMapping) {
    private static final GeometryFactory GEOMETRY_FACTORY = new JtsextGeometryFactory();
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a new {@link MappedObject} that contains all information to process the object further.
     *
     * @throws RuntimeException If an error occurs while extracting the geometry.
     */
    public MappedObject(String oid, IomObject iomGeometry, Double orientation, String vAlign, String hAlign, String text, LayerMapping layerMapping) {
        this(oid, constructGeometry(iomGeometry, layerMapping.output(), oid), iomGeometry, orientation == null ? 90 : orientation, vAlign, hAlign, text, layerMapping);
    }

    private static Geometry constructGeometry(IomObject iomGeometry, LayerMapping.OutputType outputType, String oid) {
        try {
            return switch (outputType) {
                case TEXT, POINT -> GEOMETRY_FACTORY.createPoint(Iox2jtsext.coord2JTS(iomGeometry));
                case LINE -> Iox2jtsext.polyline2JTS(iomGeometry, false, 0.0);
                case SURFACE -> Iox2jtsext.surface2JTS(iomGeometry, 0.0);
            };
        } catch (IoxException e) {
            throw new RuntimeException("Error creating geometry for object with id \"" + oid + "\".", e);
        }
    }

    /**
     * Writes the object to a DXF file using the provided {@link DxfWriter}.
     */
    public void writeToDxf(DxfWriter dxfWriter) {
        try {
            if (iomGeometry == null) {
                throw new IllegalStateException("Cannot write object to dxf without geometry.");
            }

            switch (layerMapping().output()) {
                case SURFACE -> dxfWriter.writeHatch(layerMapping().layer(), iomGeometry);
                case LINE -> dxfWriter.writeLwPolyline(layerMapping().layer(), iomGeometry);
                case POINT ->
                        dxfWriter.writeBlockInsert(layerMapping().layer(), layerMapping().symbol(), orientation, iomGeometry);
                case TEXT ->
                        dxfWriter.writeText(layerMapping().layer(), layerMapping().font(), text, hAlign, vAlign, orientation, layerMapping().textsize(), iomGeometry);
                default -> throw new AssertionError("Unknown output type: " + layerMapping().output());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write object: {} to dxf.", oid(), e);
        }
    }
}
