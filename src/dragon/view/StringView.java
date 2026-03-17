package dragon.view;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import dragon.Dragon;

/**
 * Human-readable text {@link View} implementation.
 *
 * <p>Formats each dragon as a multi-line block with basic indentation for nested objects.
 */
public class StringView implements View {

    /**
     * Formats a stream of dragons into a text representation.
     *
     * @param stream stream of dragons
     * @return input stream containing formatted text
     */
    @Override
    public InputStream toView(Stream<Dragon> stream) {
        StringBuilder sb = new StringBuilder();

        stream.forEach(dragon -> {
            // Root-level fields
            appendField(sb, "id:           ", String.valueOf(dragon.getId()), 0);
            appendField(sb, "name:         ", dragon.getName(), 0);
            appendField(sb, "creationDate: ", String.valueOf(dragon.getCreationDate()), 0);
            appendField(sb, "age:          ", String.valueOf(dragon.getAge()), 0);
            appendField(sb, "weight:       ", String.valueOf(dragon.getWeight()), 0);
            appendField(sb, "speaking:     ", String.valueOf(dragon.isSpeaking()), 0);

            // Nested fields
            appendField(sb, "coordinates:  ", null, 0);
            if (dragon.getCoordinates() != null) {
                appendField(sb, "x:            ", String.valueOf(dragon.getCoordinates().getX()), 1);
                appendField(sb, "y:            ", String.valueOf(dragon.getCoordinates().getY()), 1);
            }

            appendField(sb, "type:           ", dragon.getType() == null ? "null" : dragon.getType().name(), 0);

            appendField(sb, "head:", null, 0);
            if (dragon.getHead() != null) {
                appendField(sb, "size:         ", String.valueOf(dragon.getHead().getSize()), 1);
                appendField(sb, "toothCount:   ", String.valueOf(dragon.getHead().getToothCount()), 1);
            }

            // Separator between dragons
            sb.append(System.lineSeparator());
        });

        return new java.io.ByteArrayInputStream(sb.toString().getBytes(core.Defaults.CHARSET));
    }

    /**
     * Appends a single formatted field with indentation.
     *
     * @param sb target string builder
     * @param name field label (including alignment spaces if desired)
     * @param value value string or null to omit value after the label
     * @param indentLevel indentation level (each level adds two spaces)
     */
    private static void appendField(StringBuilder sb, String name, String value, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            sb.append("  ");
        }

        sb.append(name);

        if (value != null) {
            sb.append(' ').append(value);
        }

        sb.append(System.lineSeparator());
    }

    /**
     * Parsing is not implemented for this view.
     *
     * @param stream ignored
     * @return empty stream
     */
    @Override
    public Stream<Dragon> fromView(OutputStream stream) {
        return Stream.empty();
    }

}
