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

/**
 * Application facade that wires together collection, storage and command
 * execution.
 *
 * <p>
 * Contains a registry of supported commands and their implementations and
 * maintains a history
 * of recently executed commands.
 */
public class UI {

    /**
     * Creates a UI instance with explicitly provided views.
     *
     * @param collection  collection API implementation
     * @param storage     persistence implementation
     * @param out         output writer for user-visible messages
     * @param storageView view used to serialize collection into storage format
     * @param outView     view used to render collection to user-visible format
     */
    public UI(collection.API collection, Storage storage, OutputStreamWriter out, View storageView, View outView) {
        this.collection = collection;
        this.storage = storage;
        this.out = out;

        this.storageView = storageView;
        this.outView = outView;
    }

    /**
     * Creates a UI instance with default views: JSON for storage and text for
     * output.
     *
     * @param collection collection API implementation
     * @param storage    persistence implementation
     * @param out        output writer for user-visible messages
     */
    public UI(collection.API collection, Storage storage, OutputStreamWriter out) {
        this(collection, storage, out, new JsonView(), new StringView());
    }

    /**
     * Executes a command using the command registry and records it in history.
     *
     * @param command command to execute
     */
    public void execute(Command command) {
        var execute = executes.get(command.command().getCode());
        execute.execute(command, this);
        addToHistory(command);
    }

    public void setOutputStream(OutputStreamWriter out) {
        this.out = out;
    }

    public void setOutputView(View view) {
        outView = view;
    }

    /**
     * Adds the command to the fixed-size history and flushes output.
     *
     * @param command command to record
     */
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

    /** Collection API used by commands. */
    private collection.API collection;

    /** Storage implementation used for save/load operations. */
    private Storage storage;

    /** Output destination for user-visible messages. */
    private OutputStreamWriter out;

    /** View used to serialize the collection for storage. */
    private View storageView;

    /** View used to render output for the user. */
    private View outView;

    /** Circular history buffer of last 8 commands. */
    private final Deque<Command> history = new ArrayDeque<>(8);

    /** Mapping from command code to executable implementation. */
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
                                        .toString() + "\n");
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
                                        .toString() + "\n");
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
                    ui.out.write(e.command().getText());
                    ui.out.write("\n");
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            });
        });

    }

}
