package server;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import core.Request;

/**
 * Deserializes incoming request data from the client.
 * Reads the command line and optionally script content (for execute_script).
 */
public class RequestReader implements Consumer<ServerContext> {

    public RequestReader() {
        logger.info("RequestReader initialized.");
    }

    @Override
    public void accept(ServerContext context) {
        if (context.requestData == null || context.requestData.remaining() == 0) {
            logger.warn("Received empty request data");
            return;
        }

        byte[] data = new byte[context.requestData.remaining()];
        context.requestData.get(data);

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));

            // Read the command line
            context.request = (Request) ois.readObject();
            logger.debug("Received input.");           

        } catch (Exception e) {
            logger.error("Failed to decode request", e);
            context.request = new Request();
        }
    }

    static {
        logger = Logger.getLogger(RequestReader.class);
    }

    static private Logger logger;
}
