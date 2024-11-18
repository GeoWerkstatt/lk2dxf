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
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A reader for LKMap INTERLIS transfer files.
 */
public final class XtfStreamReader implements AutoCloseable {
    private static final ReaderFactory READER_FACTORY = new ReaderFactory();

    private final IoxReader reader;
    private LKMapXtfReaderState state = null;

    /**
     * Creates a new reader for LKMap INTERLIS transfer files.
     * @param xtfFile The file to read from.
     * @throws IoxException If an error occurs while creating the transfer file reader.
     */
    public XtfStreamReader(File xtfFile) throws IoxException {
        LogEventFactory logEventFactory = new LogEventFactory();
        Settings settings = new Settings();
        this.reader = READER_FACTORY.createReader(xtfFile, logEventFactory, settings);
    }

    /**
     * Reads the objects as a sequential stream.
     * Advancing the stream may throw an exception when reading invalid data.
     * @return A stream of objects contained in the xtf file.
     * @throws IllegalStateException If this method is called more than once.
     */
    public Stream<IomObject> readObjects() {
        if (state != null) {
            throw new IllegalStateException("readObjects() can only be called once");
        }
        state = LKMapXtfReaderState.INITIALIZED;

        return StreamSupport.stream(new XtfReaderSpliterator(), false);
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }

    private enum LKMapXtfReaderState {
        INITIALIZED,
        TRANSFER,
        BASKET,
        COMPLETED,
    }

    /**
     * A sequential spliterator for reading objects from the surrounding {@link XtfStreamReader}.
     * Advancing the spliterator will read from the xtf reader and may throw an exception when reading invalid data.
     */
    private class XtfReaderSpliterator implements Spliterator<IomObject> {
        @Override
        public boolean tryAdvance(Consumer<? super IomObject> action) {
            try {
                IoxEvent event = reader.read();
                while (event != null) {
                    switch (event) {
                        case StartTransferEvent ignored -> {
                            if (state != LKMapXtfReaderState.INITIALIZED) {
                                throw new IllegalStateException("Unexpected start transfer event in state: " + state);
                            }
                            state = LKMapXtfReaderState.TRANSFER;
                            System.out.println("Start transfer");
                        }
                        case StartBasketEvent startBasketEvent -> {
                            if (state != LKMapXtfReaderState.TRANSFER) {
                                throw new IllegalStateException("Unexpected start basket event in state: " + state);
                            }
                            state = LKMapXtfReaderState.BASKET;
                            System.out.println("Start basket \"" + startBasketEvent.getBid() + "\"");
                        }
                        case ObjectEvent objectEvent -> {
                            if (state != LKMapXtfReaderState.BASKET) {
                                throw new IllegalStateException("Unexpected object event in state: " + state);
                            }
                            action.accept(objectEvent.getIomObject());
                            return true;
                        }
                        case EndBasketEvent ignored -> {
                            if (state != LKMapXtfReaderState.BASKET) {
                                throw new IllegalStateException("Unexpected end basket event in state: " + state);
                            }
                            state = LKMapXtfReaderState.TRANSFER;
                            System.out.println("End basket");
                        }
                        case EndTransferEvent ignored -> {
                            if (state != LKMapXtfReaderState.TRANSFER) {
                                throw new IllegalStateException("Unexpected end transfer event in state: " + state);
                            }
                            state = LKMapXtfReaderState.COMPLETED;
                            System.out.println("End transfer");
                            return false;
                        }
                        default -> throw new IllegalStateException("Unexpected iox event: " + event);
                    }
                    event = reader.read();
                }

                throw new IllegalStateException("Unexpected end of file");
            } catch (IoxException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Spliterator<IomObject> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return IMMUTABLE | NONNULL;
        }
    }
}
