package ch.geowerkstatt.lk2dxf;

import java.util.List;
import java.util.Optional;

public record LK2DxfOptions(
        List<String> xtfFiles,
        String dxfFile,
        Optional<String> perimeterWkt,
        Optional<String> logfile,
        boolean trace) {
}
