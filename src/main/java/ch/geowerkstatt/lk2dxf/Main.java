package ch.geowerkstatt.lk2dxf;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.StdListener;
import ch.geowerkstatt.lk2dxf.mapping.ObjectMapper;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox_j.utility.IoxUtility;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class Main {
    private static final String OPTION_HELP = "help";
    private static final String OPTION_LOGFILE = "logfile";
    private static final String OPTION_PERIMETER = "perimeter";
    private static final String OPTION_TRACE = "trace";
    private static final String OPTION_VERSION = "version";

    private static final String VERSION;

    private static final Logger LOGGER = LogManager.getLogger();

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

        if (commandLine == null) {
            System.exit(1);
        } else if (commandLine.hasOption(OPTION_HELP)) {
            printUsage(cliOptions);
        } else if (commandLine.hasOption(OPTION_VERSION)) {
            System.out.println(VERSION);
        } else {
            Optional<LK2DxfOptions> options = parseLK2DxfOptions(commandLine);
            if (options.isEmpty()) {
                printUsage(cliOptions);
                System.exit(1);
            } else {
                configureLogging(options.get());
                if (!processFiles(options.get())) {
                    System.exit(1);
                }
            }
        }
    }

    private static boolean processFiles(LK2DxfOptions options) {
        Optional<Geometry> perimeter = options.parsePerimeter();

        ObjectMapper objectMapper;
        try {
            objectMapper = new ObjectMapper();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read layer mappings.", e);
        }

        try (var dxfWriter = new DxfWriter(options.dxfFile(), 3, objectMapper.getLayerMappings(), "lk2dxf " + Main.VERSION)) {
            for (String xtfFile : options.xtfFiles()) {
                try (XtfStreamReader reader = new XtfStreamReader(new File(xtfFile))) {
                    Stream<MappedObject> objects = objectMapper.mapObjects(reader.readObjects());

                    if (perimeter.isPresent()) {
                        objects = objects.filter(o -> perimeter.get().intersects(o.geometry()));
                    }

                    objects.forEach(o -> o.writeToDxf(dxfWriter));
                } catch (Exception e) {
                    LOGGER.error("Failed to process file: {}", xtfFile, e);
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to write DXF file: {}", options.dxfFile(), e);
            return false;
        }

        return true;
    }

    private static void configureLogging(LK2DxfOptions lk2DxfOptions) {
        Level logLevel = lk2DxfOptions.trace() ? Level.TRACE : Level.INFO;
        Configurator.setRootLevel(logLevel);

        if (lk2DxfOptions.logfile().isPresent()) {
            var layout = PatternLayout.newBuilder()
                    .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                    .build();
            var fileAppender = FileAppender.newBuilder()
                    .setName("Logfile")
                    .setLayout(layout)
                    .withFileName(lk2DxfOptions.logfile().get())
                    .withAppend(false)
                    .build();
            var rootLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            rootLogger.get().addAppender(fileAppender, logLevel, null);
            fileAppender.start();
        }

        EhiLogger.getInstance().addListener(new EhiLogAdapter());
        EhiLogger.getInstance().removeListener(StdListener.getInstance());

        LOGGER.info("lk2dxf version {}", VERSION);
        LOGGER.info("ili2c version {}", TransferDescription.getVersion());
        LOGGER.info("iox-ili version {}", IoxUtility.getVersion());
        LOGGER.info("Transfer files: {}", lk2DxfOptions.xtfFiles());
    }

    private static CommandLine parseCommandLine(Options options, String[] args) {
        try {
            DefaultParser parser = new DefaultParser();
            return parser.parse(options, args);
        } catch (ParseException e) {
            LOGGER.error("Error parsing command line arguments: {}", e.getMessage());
            printUsage(options);
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
