package ch.geowerkstatt.lk2dxf;

import java.io.File;
import java.util.List;

public final class Main {
    private Main() { }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        processFiles(List.of(args));
    }

    private static void processFiles(List<String> xtfFiles) {
        for (String xtfFile : xtfFiles) {
            try (LKMapXtfReader reader = new LKMapXtfReader(new File(xtfFile))) {
                reader.readObjects(iomObject -> {
                    System.out.println(iomObject.getobjectoid());
                });
            } catch (Exception e) {
                System.err.println("Failed to process file: " + xtfFile);
                e.printStackTrace();
                return;
            }
        }
    }
}
