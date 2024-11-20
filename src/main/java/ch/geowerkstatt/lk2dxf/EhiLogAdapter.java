package ch.geowerkstatt.lk2dxf;

import ch.ehi.basics.logging.LogEvent;
import ch.ehi.basics.logging.LogListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An adapter that forwards log events from an EhiLogger to a log4j Logger.
 */
public final class EhiLogAdapter implements LogListener {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void logEvent(LogEvent logEvent) {
        switch (logEvent.getEventKind()) {
            case LogEvent.ERROR -> LOGGER.error("{}", logEvent.getEventMsg());
            case LogEvent.ADAPTION -> LOGGER.warn("{}", logEvent.getEventMsg());
            case LogEvent.DEBUG_TRACE, LogEvent.STATE_TRACE -> LOGGER.trace("{}", logEvent.getEventMsg());
            default -> LOGGER.info("{}", logEvent.getEventMsg());
        }
    }
}
