package dragon.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dragon.Dragon;

/**
 * JSON-based {@link View} implementation using Gson.
 *
 * <p>Serializes/deserializes a stream of dragons as a JSON array.
 */
public class JsonView implements View {
    private static final Logger logger = Logger.getLogger(JsonView.class);

    /** Gson instance configured for pretty printing and LocalDateTime support. */
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(
                    java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>)
                            (src, typeOfSrc, context) ->
                                    new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(
                    java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>)
                            (json, typeOfT, context) -> java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    /**
     * Serializes a stream of dragons to JSON.
     *
     * @param stream stream of dragons (may be null)
     * @return input stream containing JSON array
     */
    @Override
    public InputStream toView(Stream<Dragon> stream) {
        if (stream == null) {
            return new ByteArrayInputStream("[]".getBytes(core.Defaults.CHARSET));
        }

        var baos = new ByteArrayOutputStream();
        try (var writer = new java.io.OutputStreamWriter(baos, core.Defaults.CHARSET);
                var jsonWriter = gson.newJsonWriter(writer)) {
            jsonWriter.beginArray();
            stream.forEach(dragon -> gson.toJson(dragon, Dragon.class, jsonWriter));
            jsonWriter.endArray();
            jsonWriter.flush();
        } catch (java.io.IOException e) {
            // ByteArrayOutputStream does not throw on close, but we keep
            // this to satisfy the compile-time checked exception.
            throw new RuntimeException(e);
        }

        return new ByteArrayInputStream(baos.toByteArray());
    }

    /**
     * Deserializes dragons from a byte buffer.
     *
     * <p>Requires {@link ByteArrayOutputStream} as input container.
     *
     * @param stream output stream containing serialized JSON bytes
     * @return stream of decoded dragons
     * @throws IllegalArgumentException if the given stream is not a {@link ByteArrayOutputStream}
     */
    @Override
    public Stream<Dragon> fromView(OutputStream stream) {
        if (!(stream instanceof ByteArrayOutputStream)) {
            throw new IllegalArgumentException("JsonView.fromView expects a ByteArrayOutputStream");
        }

        byte[] bytes = ((ByteArrayOutputStream) stream).toByteArray();
        if (bytes == null || bytes.length == 0) {
            return Stream.empty();
        }

        try {

            var input = new ByteArrayInputStream(bytes);
            var reader = new java.io.InputStreamReader(input, core.Defaults.CHARSET);
            var jsonReader = new com.google.gson.stream.JsonReader(reader);

            jsonReader.beginArray();
            // } catch (java.io.IOException e) {
            //     throw new RuntimeException(e);
            // }

            var iterator =
                    new java.util.Iterator<Dragon>() {
                        private Dragon next = null;
                        private boolean finished = false;

                        private void finishQuietly() {
                            finished = true;
                            try {
                                jsonReader.close();
                            } catch (java.io.IOException ignored) {
                            }
                        }

                        private void advance() {
                            if (finished || next != null) {
                                return;
                            }

                            try {
                                if (!jsonReader.hasNext()) {
                                    finished = true;
                                    jsonReader.endArray();
                                    jsonReader.close();
                                    return;
                                }
                                next = gson.fromJson(jsonReader, Dragon.class);
                            } catch (java.io.IOException e) {
                                throw new RuntimeException(e);
                            } catch (com.google.gson.JsonSyntaxException e) {
                                // Includes MalformedJsonException as a cause for broken JSON.
                                logger.error("Error parsing JSON (the data is lost): " + e.getMessage());
                                finishQuietly();
                            } catch (com.google.gson.JsonIOException e) {
                                logger.error("Error reading JSON (the data is lost): " + e.getMessage());
                                finishQuietly();
                            } catch (com.google.gson.JsonParseException e) {
                                logger.error("Error parsing JSON (the data is lost): " + e.getMessage());
                                finishQuietly();
                            }
                        }

                        @Override
                        public boolean hasNext() {
                            advance();
                            return next != null;
                        }

                        @Override
                        public Dragon next() {
                            advance();
                            if (next == null) {
                                throw new java.util.NoSuchElementException();
                            }
                            var result = next;
                            next = null;
                            return result;
                        }
                    };

            var spliterator =
                    java.util.Spliterators.spliteratorUnknownSize(
                            iterator,
                            java.util.Spliterator.ORDERED | java.util.Spliterator.NONNULL);
            var streamResult = java.util.stream.StreamSupport.stream(spliterator, false);
            streamResult =
                    streamResult.onClose(
                            () -> {
                                try {
                                    jsonReader.close();
                                } catch (java.io.IOException ignored) {
                                }
                            });
            return streamResult;
        } catch (java.io.IOException e) {
            return Stream.empty();
        } catch (com.google.gson.JsonIOException e) {
            logger.error("Error reading JSON: " + e.getMessage());
            return Stream.empty();
        } catch (com.google.gson.JsonSyntaxException e) {
            logger.error("Error parsing JSON: " + e.getMessage());
            return Stream.empty();
        } catch (com.google.gson.JsonParseException e) {
            logger.error("Error parsing JSON: " + e.getMessage());
            return Stream.empty();
        }
    }
}
