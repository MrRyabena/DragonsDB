package server;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import core.Request;
import core.Response;

/**
 * Lightweight request/response context carried through server pipeline stages.
 *
 * <p>Holds incoming client request, computed response, session and connection metadata.
 * Instances are created per incoming client request and passed between `RequestReader`,
 * `CommandsHandler`, `ResponsePacker` and `ResponseSender` components.
 */
public class ServerContext {
    public SocketAddress clientAddress;
    public ByteBuffer requestData;
    public Request request;
    public Response response;
    public ByteBuffer responseData;
    public long requestId;
    public long sessionId;
    public boolean skipCommandHandling;

    public ServerContext() {
        requestData = ByteBuffer.allocate(64 * 1024);
        responseData = ByteBuffer.allocate(64 * 1024);
        requestId = 0;
        sessionId = 0;
        skipCommandHandling = false;
    }
}
