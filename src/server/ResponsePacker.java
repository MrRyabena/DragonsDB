package server;

import java.io.IOException;
import java.util.function.Consumer;

import core.WireFrame;

public class ResponsePacker implements Consumer<ServerContext> {
    public ResponsePacker() {}

    @Override
    public void accept(ServerContext context) {
        if (context.response == null) {
            return;
        }
        try {
            context.responseData.clear();
            context.response.sessionId = context.sessionId;
            byte[] responseBytes = WireFrame.wrapResponse(context.response, context.requestId).toBytes();
            context.responseData.put(responseBytes);
            context.responseData.flip();
        } catch (IOException e) {
            logger.error("Failed to serialize response", e);
            context.responseData.clear();
        }
    }

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ResponsePacker.class);
}
