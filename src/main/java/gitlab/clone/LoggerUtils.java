package gitlab.clone;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerUtils {

    public static final String FULL = "FULL";
    public static final String DEFAULT = "DEFAULT";

    public static void disableFullAppender() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        ConsoleAppender<ILoggingEvent> appender =
                (ConsoleAppender) lc.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(FULL);
        appender.stop();
    }

    public static void disableDefaultAppender() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        ConsoleAppender<ILoggingEvent> appender =
                (ConsoleAppender) lc.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(DEFAULT);
        appender.stop();
    }
}
