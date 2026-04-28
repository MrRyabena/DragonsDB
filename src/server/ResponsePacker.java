package server;

import java.io.IOException;
import java.util.function.Consumer;

import core.WireFrame;

/**
 * Serializes {@link core.Response} into a binary wire frame before UDP sending.
 */
/**
 * Serializes `Response` objects into transport-ready byte arrays and attaches them to
 * the `ServerContext` for sending back to the client.
 *
 * <p>Responsible for converting in-memory response structures into the wire format
 * expected by clients and ensuring any session identifiers or metadata are preserved.
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
