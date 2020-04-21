/**
 *
 * To write the logs in file
 *
 */

import java.io.IOException;
import java.util.*;
import java.util.logging.*;

public class LogWriter {

    static Logger logger;

    public static Logger getLogger(Integer peerId) {
        logger = Logger.getLogger(LogWriter.class.getName());
        logger.setLevel(Level.INFO);

        FileHandler fh = null;
        try {
            fh = new FileHandler("log_peer_" + peerId + ".log");
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fh.setFormatter(new SimpleFormatter() {
            private static final String f = "[%1$tF %1$tT] %2$-7s %n";

            @Override
            public synchronized String format(LogRecord log_record) {
                return String.format(f,
                        new Date(log_record.getMillis()),
                        log_record.getMessage()
                );
            }
        });

        logger.addHandler(fh);
        return logger;
    }

}
