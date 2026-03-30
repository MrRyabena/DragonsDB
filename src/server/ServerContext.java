package server;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import collection.ApiCommand;
import dragon.Dragon;

public class ServerContext {
    public SocketAddress clientAddress;
    public ByteBuffer requestData;
    public ApiCommand command;
    public Stream<Dragon> stream;
    public ByteBuffer response;

    public ServerContext() {
        requestData = ByteBuffer.allocate(64 * 1024);
        response = ByteBuffer.allocate(0);
    }
}
