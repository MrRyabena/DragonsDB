package server;

import java.util.function.Consumer;

import org.apache.log4j.Logger;

import core.Request;
import core.WireFrame;

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
            WireFrame frame = WireFrame.fromBytes(data, data.length);
            if (frame.kind != WireFrame.Kind.REQUEST) {
                logger.warn("Received non-request frame: " + frame.kind);
                context.request = new Request();
                return;
            }

            context.requestId = frame.requestId;
            context.sessionId = frame.sessionId;
            context.request = frame.unwrapRequest();
            logger.debug("Received input. requestId=" + context.requestId);

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
