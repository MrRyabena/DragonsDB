package dragon.view;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import dragon.Dragon;

/**
 * Converts a stream of {@link Dragon} objects to/from an I/O representation.
 */
public interface View {

    /**
     * Encodes the given stream into an input stream representation.
     *
     * @param stream stream of dragons (may be null depending on implementation)
     * @return encoded input stream
     */
    InputStream toView(Stream<Dragon> stream);

    /**
     * Decodes dragons from the provided output stream (typically a buffer produced by storage).
     *
     * @param stream source buffer stream
     * @return decoded stream of dragons
     */
    Stream<Dragon> fromView(OutputStream stream);

}
