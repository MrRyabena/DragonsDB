package dragon.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dragon.Dragon;

public class JsonView implements View {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

    @Override
    public Stream<Dragon> fromView(OutputStream stream) {
        if (!(stream instanceof ByteArrayOutputStream)) {
            throw new IllegalArgumentException("JsonView.fromView expects a ByteArrayOutputStream");
        }

        byte[] bytes = ((ByteArrayOutputStream) stream).toByteArray();
        if (bytes == null || bytes.length == 0) {
            return Stream.empty();
        }

        var input = new ByteArrayInputStream(bytes);
        var reader = new java.io.InputStreamReader(input, core.Defaults.CHARSET);
        var jsonReader = new com.google.gson.stream.JsonReader(reader);

        try {
            jsonReader.beginArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        var iterator = new java.util.Iterator<Dragon>() {
            private Dragon next = null;
            private boolean finished = false;

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

        var spliterator = java.util.Spliterators.spliteratorUnknownSize(iterator,
                java.util.Spliterator.ORDERED | java.util.Spliterator.NONNULL);
        var streamResult = java.util.stream.StreamSupport.stream(spliterator, false);
        streamResult = streamResult.onClose(() -> {
            try {
                jsonReader.close();
            } catch (java.io.IOException ignored) {
            }
        });
        return streamResult;
    }
}
