package ch.geowerkstatt.lk2dxf;

import ch.geowerkstatt.lk2dxf.mapping.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public final class Main {
    private static final String OPTION_HELP = "help";
    private static final String OPTION_LOGFILE = "logfile";
    private static final String OPTION_PERIMETER = "perimeter";
    private static final String OPTION_TRACE = "trace";
    private static final String OPTION_VERSION = "version";

    private static final String VERSION;

    static {
        String packageVersion = Main.class.getPackage().getImplementationVersion();
        VERSION = packageVersion != null ? packageVersion : "unknown";
    }

    private Main() { }

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        Options cliOptions = createCliOptions();
        CommandLine commandLine = parseCommandLine(cliOptions, args);

        if (commandLine.hasOption(OPTION_HELP)) {
            printUsage(cliOptions);
        } else if (commandLine.hasOption(OPTION_VERSION)) {
            System.out.println(VERSION);
        } else {
            Optional<LK2DxfOptions> options = parseLK2DxfOptions(commandLine);
            if (options.isEmpty()) {
                printUsage(cliOptions);
                System.exit(1);
            } else {
                processFiles(options.get());
            }
        }
    }

    private static void processFiles(LK2DxfOptions options) {
        Optional<Geometry> perimeter = options.parsePerimeter();
        AtomicInteger counter = new AtomicInteger();

        for (String xtfFile : options.xtfFiles()) {
            try (LKMapXtfReader reader = new LKMapXtfReader(new File(xtfFile))) {
                ObjectMapper mapper = new ObjectMapper();
                Stream<MappedObject> objects = mapper.mapObjects(reader.readObjects());

                if (perimeter.isPresent()) {
                    objects = objects.filter(o -> perimeter.get().intersects(o.geometry()));
                }

                objects.forEach(o -> System.out.println(counter.incrementAndGet() + ": " + o.iomObject().getobjectoid() + " -> " + o.layerMapping().layer()));
            } catch (Exception e) {
                System.err.println("Failed to process file: " + xtfFile);
                e.printStackTrace();
                return;
            }
        }
    }

    private static CommandLine parseCommandLine(Options options, String[] args) {
        try {
            DefaultParser parser = new DefaultParser();
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            printUsage(options);
            System.exit(1);
            return null;
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp("java -jar lk2dxf.jar [options] input.xtf [input2.xtf ...] output.dxf", options);
    }

    private static Optional<LK2DxfOptions> parseLK2DxfOptions(CommandLine commandLine) {
        List<String> remainingArgs = commandLine.getArgList();
        if (remainingArgs.size() < 2) {
            return Optional.empty();
        }

        String dxfFile = remainingArgs.removeLast();
        Optional<String> perimeterWkt = Optional.ofNullable(commandLine.getOptionValue(OPTION_PERIMETER));
        Optional<String> logfile = Optional.ofNullable(commandLine.getOptionValue(OPTION_LOGFILE));
        boolean trace = commandLine.hasOption(OPTION_TRACE);

        return Optional.of(new LK2DxfOptions(remainingArgs, dxfFile, perimeterWkt, logfile, trace));
    }

    private static Options createCliOptions() {
        Option help = Option.builder("h")
                .longOpt(OPTION_HELP)
                .desc("print this help message")
                .build();
        Option logfile = Option.builder()
                .longOpt(OPTION_LOGFILE)
                .desc("path to the log file")
                .argName("file")
                .hasArg()
                .build();
        Option perimeter = Option.builder()
                .longOpt(OPTION_PERIMETER)
                .desc("exclude all objects whose geometry is fully outside the specified perimeter")
                .argName("wkt")
                .hasArg()
                .build();
        Option trace = Option.builder()
                .longOpt(OPTION_TRACE)
                .desc("enable trace logging")
                .build();
        Option version = Option.builder()
                .longOpt(OPTION_VERSION)
                .desc("print the version of this application")
                .build();

        Options options = new Options();
        options.addOption(help);
        options.addOption(logfile);
        options.addOption(perimeter);
        options.addOption(trace);
        options.addOption(version);
        return options;
    }
}
