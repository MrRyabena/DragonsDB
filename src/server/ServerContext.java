package server;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import core.Request;
import core.Response;

public class ServerContext {
    public SocketAddress clientAddress;
    public ByteBuffer requestData;
    public Request request;
    public Response response;
    public ByteBuffer responseData;
    public long sessionId;

    public ServerContext() {
        requestData = ByteBuffer.allocate(64 * 1024);
        responseData = ByteBuffer.allocate(64 * 1024);
        sessionId = 0;
    }
}
