package server;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.apache.log4j.Logger;

import collection.ApiCommand;
import dragon.Dragon;

public class RequestReader implements Consumer<ServerContext> {

    public RequestReader() {
        logger.info("Started.");
    }

    static {
        logger = Logger.getLogger(RequestReader.class);
    }

    @Override
    public void accept(ServerContext context) {
        byte[] data = new byte[context.requestData.remaining()];
        context.requestData.get(data);

        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));

            // 1. Читаем команду
            context.command = (ApiCommand) ois.readObject();

            // 2. Создаём ленивый Stream<Dragon>, который читает оставшиеся объекты
            Iterator<Dragon> iterator = new Iterator<>() {
                Dragon next = readNext();

                private Dragon readNext() {
                    try {
                        Object obj = ois.readObject();
                        if (!(obj instanceof Dragon dragon)) {
                            throw new IllegalArgumentException("Request payload contains non-Dragon object");
                        }
                        return dragon;
                    } catch (EOFException e) {
                        return null; // конец потока
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

            context.stream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                    false);

        } catch (Exception e) {
            logger.error("Failed to decode request", e);
            context.stream = java.util.stream.Stream.empty();
        }
    }

    static private Logger logger;
}
