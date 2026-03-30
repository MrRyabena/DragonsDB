package dragon.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import dragon.Dragon;

public class SerializedView implements View {

    @Override
    public InputStream toView(Stream<Dragon> stream) {
        try (var baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            stream.forEach(dragon -> {
                try {
                    oos.writeObject(dragon);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            oos.flush();
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return InputStream.nullInputStream();
    }

    @Override
    public Stream<Dragon> fromView(OutputStream stream) {
        if (!(stream instanceof ByteArrayOutputStream baos)) {
            throw new IllegalArgumentException("BinaryView.fromView expects ByteArrayOutputStream");
        }

        byte[] data = baos.toByteArray();
        if (data.length == 0) {
            return Stream.empty();
        }

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));

            Iterator<Dragon> iterator = new Iterator<>() {
                Dragon next = readNext();

                private Dragon readNext() {
                    try {
                        return (Dragon) ois.readObject();
                    } catch (EOFException e) {
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public Dragon next() {
                    Dragon current = next;
                    next = readNext();
                    return current;
                }
            };

            return StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                    false);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
