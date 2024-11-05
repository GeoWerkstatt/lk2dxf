package ch.geowerkstatt.lk2dxf;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

import java.util.List;
import java.util.Optional;

public record LK2DxfOptions(
        List<String> xtfFiles,
        String dxfFile,
        Optional<String> perimeterWkt,
        Optional<String> logfile,
        boolean trace) {

    /**
     * Parses the perimeter WKT string to a {@link Geometry}.
     * @return The parsed perimeter geometry, or an empty optional if the wkt was empty.
     * @throws IllegalStateException If the wkt could not be parsed.
     */
    public Optional<Geometry> parsePerimeter() {
        return perimeterWkt.map(wkt -> {
            try {
                return new WKTReader().read(wkt);
            } catch (com.vividsolutions.jts.io.ParseException e) {
                throw new IllegalStateException("Error parsing perimeter WKT.", e);
            }
        });
    }
}
