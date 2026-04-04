package server;

import java.util.function.Consumer;

public class ResponsePacker implements Consumer<ServerContext> {
    public ResponsePacker() {}

    @Override
    public void accept(ServerContext context) {
        if (context.response == null) {
            return;
        }
        try {
            context.responseData.clear();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
            oos.writeObject(context.response);
            oos.flush();
            byte[] responseBytes = baos.toByteArray();
            context.responseData.put(responseBytes);
            context.responseData.flip();
        } catch (Exception e) {
            logger.error("Failed to serialize response", e);
            context.responseData.clear();
        }
    }

    private static org.apache.log4j.Logger logger =
            org.apache.log4j.Logger.getLogger(ResponsePacker.class);
}
