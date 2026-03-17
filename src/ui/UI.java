package ui;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

import dragon.view.JsonView;
import dragon.view.StringView;
import dragon.view.View;
import storage.Storage;

public class UI {

    public UI(collection.API collection, Storage storage, OutputStreamWriter out, View storageView, View outView) {
        this.collection = collection;
        this.storage = storage;
        this.out = out;

        this.storageView = storageView;
        this.outView = outView;
    }

    public UI(collection.API collection, Storage storage, OutputStreamWriter out) {
        this(collection, storage, out, new JsonView(), new StringView());
    }

    public void execute(Command command) {
        var execute = executes.get(command.command().getCode());
        execute.execute(command, this);
        addToHistory(command);
    }

    private void addToHistory(Command command) {
        if (history.size() == 8) {
            history.removeFirst();
        }
        history.addLast(command);
        try {
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private collection.API collection;
    private Storage storage;
    private OutputStreamWriter out;

    private View storageView;
    private View outView;

    private final Deque<Command> history = new ArrayDeque<>(8);

    private static final HashMap<Integer, Executable> executes;

    static {
        executes = new HashMap();

        executes.put(Commands.ADD.getCode(), (command, ui) -> ui.collection.add(command.dragon()));
        executes.put(Commands.UPDATE_BY_ID.getCode(), (command, ui) -> ui.collection.updateById(command.dragon()));
        executes.put(Commands.REMOVE_BY_ID.getCode(),
                (command, ui) -> ui.collection.removeIf(x -> x.getId() == command.dragon().getId()));
        executes.put(Commands.CLEAR.getCode(), (command, ui) -> ui.collection.clear());
        executes.put(Commands.SAVE.getCode(),
                (command, ui) -> ui.storage.save(ui.storageView.toView(ui.collection.getStream())));
        executes.put(Commands.REMOVE_GREATER.getCode(),
                (command, ui) -> ui.collection.removeIf(x -> x.compareTo(command.dragon()) > 0));
        executes.put(Commands.REMOVE_LOWER.getCode(),
                (command, ui) -> ui.collection.removeIf(x -> x.compareTo(command.dragon()) < 0));
        executes.put(Commands.COUNT_BY_TYPE.getCode(),
                (command, ui) -> {
                    try {
                        ui.out
                                .write(Integer
                                        .valueOf(ui.collection.countIf(x -> x.getType() == command.type()))
                                        .toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        executes.put(Commands.COUNT_GREATER_THAN_TYPE.getCode(),
                (command, ui) -> {
                    try {
                        ui.out.write(
                                Integer.valueOf(
                                        ui.collection
                                                .countIf(x -> x.getType().compareTo(command.type()) > 0))
                                        .toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                });

        executes.put(Commands.FILTER_STARTS_WITH_NAME.getCode(),
                (command, ui) -> {
                    try (var isr = new InputStreamReader(ui.outView
                            .toView(ui.collection.getStream().filter(e -> e.getName().startsWith(command.name()))))) {
                        isr.transferTo(ui.out);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        executes.put(Commands.SHOW.getCode(), (command, ui) -> {
            try (var isr = new InputStreamReader(ui.outView.toView(ui.collection.getStream()))) {
                isr.transferTo(ui.out);

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        executes.put(Commands.INFO.getCode(), (command, ui) -> {
            var str = new StringBuilder();
            str.append("Collection type: HashSet<Dragon>\n");
            str.append("Date created: ");
            var date = ui.storage.getDateCreated();
            str.append(date != null ? date.toString() : "null");
            str.append("\nDate modified: ");
            var dateModified = ui.storage.getDateModified();
            str.append(dateModified != null ? dateModified.toString() : "null");
            str.append("\nNumber of elements: ");
            str.append(ui.collection.countIf(e -> true));
            str.append("\n");

            try {
                ui.out.write(str.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        executes.put(Commands.HISTORY.getCode(), (command, ui) -> {
            ui.history.stream().forEach(e -> {
                try {
                    ui.out.write(e.command().toString());
                    ui.out.write("\n");
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
        });

    }

}
