package ch.geowerkstatt.lk2dxf;

import ch.ehi.basics.settings.Settings;
import ch.interlis.iom.IomObject;
import ch.interlis.iox.EndBasketEvent;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox.ObjectEvent;
import ch.interlis.iox.StartBasketEvent;
import ch.interlis.iox.StartTransferEvent;
import ch.interlis.iox_j.logging.LogEventFactory;
import ch.interlis.iox_j.utility.ReaderFactory;

import java.io.File;
import java.util.function.Consumer;

/**
 * A reader for LKMap INTERLIS transfer files.
 */
public final class LKMapXtfReader implements AutoCloseable {
    private static final String BASKET_NAME = "SIA405_LKMap_2015_LV95.SIA405_LKMap";
    private static final ReaderFactory READER_FACTORY = new ReaderFactory();

    private final IoxReader reader;

    /**
     * Creates a new reader for LKMap INTERLIS transfer files.
     * @param xtfFile The file to read from.
     * @throws IoxException If an error occurs while creating the transfer file reader.
     */
    public LKMapXtfReader(File xtfFile) throws IoxException {
        LogEventFactory logEventFactory = new LogEventFactory();
        Settings settings = new Settings();
        this.reader = READER_FACTORY.createReader(xtfFile, logEventFactory, settings);
    }

    /**
     * Reads the objects streamed by the reader and passes them to the consumer.
     * @param consumer A consumer to process the objects.
     * @throws IoxException If an error occurs while reading the objects.
     * @throws IllegalStateException If the transfer file is not in the expected format.
     */
    public void readObjects(Consumer<IomObject> consumer) throws IoxException {
        IoxEvent event = reader.read();
        if (!(event instanceof StartTransferEvent)) {
            throw new IllegalStateException("Expected start transfer event, got: " + event);
        }

        event = reader.read();
        while (!(event instanceof EndTransferEvent)) {
            if (event instanceof StartBasketEvent startBasketEvent) {
                if (!BASKET_NAME.equals(startBasketEvent.getType())) {
                    throw new IllegalStateException("Invalid basket type: " + startBasketEvent.getType());
                }
            } else {
                throw new IllegalStateException("Expected start basket event, got: " + event);
            }

            event = reader.read();
            while (event instanceof ObjectEvent objectEvent) {
                consumer.accept(objectEvent.getIomObject());
                event = reader.read();
            }

            if (!(event instanceof EndBasketEvent)) {
                throw new IllegalStateException("Expected end basket event, got: " + event);
            }

            event = reader.read();
        }
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
