package server;

import java.io.IOException;
import java.util.function.Consumer;

import core.WireFrame;

/**
 * Serializes {@link core.Response} into a binary wire frame before UDP sending.
 */
public class ResponsePacker implements Consumer<ServerContext> {
    public ResponsePacker() {}

    @Override
    public void accept(ServerContext context) {
        if (context.response == null) {
            return;
        }
        try {
            context.responseData.clear();
            // Preserve session continuity on the client side.
            context.response.sessionId = context.sessionId;
            // Keep requestId so client can match response to the request.
            byte[] responseBytes = WireFrame.wrapResponse(context.response, context.requestId).toBytes();
            context.responseData.put(responseBytes);
            context.responseData.flip();
        } catch (IOException e) {
            logger.error("Failed to serialize response", e);
            context.responseData.clear();
        }
    }

    private static final org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ResponsePacker.class);
}
