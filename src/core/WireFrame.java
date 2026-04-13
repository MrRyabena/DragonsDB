package core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Transport envelope for UDP packets.
 *
 * <p>The frame is intentionally separate from Request/Response so transport metadata can evolve
 * independently from business payloads. The payload itself is still serialized Java objects.
 */
public final class WireFrame implements Serializable {
    public static final int MAGIC = 0x44524231; // 'DRB1'
    public static final short VERSION = 1;
    public static final int MAX_PACKET_SIZE = 64 * 1024;
    private static final int HEADER_SIZE = Integer.BYTES + Short.BYTES + Byte.BYTES + Byte.BYTES + Long.BYTES + Long.BYTES + Integer.BYTES;

    public enum Kind {
        REQUEST((byte) 1),
        RESPONSE((byte) 2);

        private final byte code;

        Kind(byte code) {
            this.code = code;
        }

        public byte code() {
            return code;
        }

        public static Kind fromCode(byte code) {
            for (Kind kind : values()) {
                if (kind.code == code) {
                    return kind;
                }
            }
            throw new IllegalArgumentException("Unknown frame kind: " + code);
        }
    }

    public final int magic;
    public final short version;
    public final Kind kind;
    public final long requestId;
    public final long sessionId;
    public final byte flags;
    public final byte[] payload;

    public WireFrame(Kind kind, long requestId, long sessionId, byte flags, byte[] payload) {
        this(MAGIC, VERSION, kind, requestId, sessionId, flags, payload);
    }

    public WireFrame(
            int magic, short version, Kind kind, long requestId, long sessionId, byte flags, byte[] payload) {
        this.magic = magic;
        this.version = version;
        this.kind = kind;
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.flags = flags;
        this.payload = payload == null ? new byte[0] : payload;
    }

    public byte[] toBytes() throws IOException {
        if (payload.length + HEADER_SIZE > MAX_PACKET_SIZE) {
            throw new IOException("WireFrame payload exceeds UDP packet limit");
        }

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payload.length).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(magic);
        buffer.putShort(version);
        buffer.put(kind.code());
        buffer.put(flags);
        buffer.putLong(requestId);
        buffer.putLong(sessionId);
        buffer.putInt(payload.length);
        buffer.put(payload);
        return buffer.array();
    }

    public static WireFrame fromBytes(byte[] raw, int length) throws IOException {
        if (raw == null || length <= 0) {
            throw new IOException("Empty wire frame");
        }
        if (length < HEADER_SIZE) {
            throw new IOException("Wire frame is too small");
        }

        ByteBuffer buffer = ByteBuffer.wrap(raw, 0, length).order(ByteOrder.BIG_ENDIAN);
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            throw new IOException(String.format("Invalid wire frame magic: 0x%08X", magic));
        }

        short version = buffer.getShort();
        if (version != VERSION) {
            throw new IOException("Unsupported wire frame version: " + version);
        }

        Kind kind = Kind.fromCode(buffer.get());
        byte flags = buffer.get();
        long requestId = buffer.getLong();
        long sessionId = buffer.getLong();
        int payloadLength = buffer.getInt();
        if (payloadLength < 0 || payloadLength > length - HEADER_SIZE) {
            throw new IOException("Invalid payload length: " + payloadLength);
        }

        byte[] payload = new byte[payloadLength];
        buffer.get(payload);
        return new WireFrame(magic, version, kind, requestId, sessionId, flags, payload);
    }

    public static WireFrame wrapRequest(Request request, long requestId) throws IOException {
        return new WireFrame(Kind.REQUEST, requestId, request == null ? 0 : request.sessionId, (byte) 0, serialize(request));
    }

    public static WireFrame wrapResponse(Response response, long requestId) throws IOException {
        return new WireFrame(Kind.RESPONSE, requestId, response == null ? 0 : response.sessionId, (byte) 0, serialize(response));
    }

    public Request unwrapRequest() throws IOException {
        return deserialize(payload, Request.class);
    }

    public Response unwrapResponse() throws IOException {
        return deserialize(payload, Response.class);
    }

    public int payloadLength() {
        return payload.length;
    }

    private static byte[] serialize(Serializable object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            return baos.toByteArray();
        }
    }

    private static <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object object = ois.readObject();
            return type.cast(object);
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new IOException("Failed to deserialize " + type.getSimpleName(), e);
        }
    }
}
