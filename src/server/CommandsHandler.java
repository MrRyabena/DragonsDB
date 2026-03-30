package server;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import collection.API;
import storage.Storage;
import ui.UI;

public class CommandsHandler implements Consumer<ServerContext> {

    public CommandsHandler(API collection, Storage storage) {
        ui = new UI(collection, storage, null);
    }

    @Override
    public void accept(ServerContext context) {
        ByteArrayOutputStream str = new ByteArrayOutputStream();
        ui.setOutputStream(new OutputStreamWriter(str));
        ui.execute(context.requestCommand);

        context.response = ByteBuffer.wrap(str.toByteArray());
    }

    private UI ui;
}
