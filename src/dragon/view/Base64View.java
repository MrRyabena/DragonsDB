package dragon.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import dragon.Dragon;

/**
 * A view that serializes dragons to Base64-encoded binary format for text-based storage.
 * Wraps SerializedView and encodes/decodes the binary data to/from Base64 strings.
 */
public class Base64View implements View {

    private final SerializedView serializedView = new SerializedView();

    @Override
    public InputStream toView(Stream<Dragon> stream) {
        // First, serialize to binary using SerializedView
        InputStream binaryStream = serializedView.toView(stream);

        // Read the binary data
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            binaryStream.transferTo(baos);
        } catch (IOException e) {
            return InputStream.nullInputStream();
        }

        // Encode to Base64
        String base64 = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());

        // Return as InputStream containing the Base64 string
        return new ByteArrayInputStream(base64.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public Stream<Dragon> fromView(OutputStream stream) {
        if (!(stream instanceof ByteArrayOutputStream baos)) {
            throw new IllegalArgumentException("Base64View.fromView expects ByteArrayOutputStream");
        }

        // Get the Base64 string
        String base64 = baos.toString(java.nio.charset.StandardCharsets.UTF_8);

        // Decode from Base64
        byte[] binaryData;
        try {
            binaryData = java.util.Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return Stream.empty();
        }

        // Create a ByteArrayOutputStream with the binary data for SerializedView
        ByteArrayOutputStream binaryBaos = new ByteArrayOutputStream();
        try {
            binaryBaos.write(binaryData);
        } catch (IOException e) {
            return Stream.empty();
        }

        // Deserialize using SerializedView
        return serializedView.fromView(binaryBaos);
    }
}