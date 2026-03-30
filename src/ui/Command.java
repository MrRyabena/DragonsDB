package ui;

import java.io.Serializable;

/**
 * A parsed user command with optional arguments.
 *
 * @param command command type
 * @param dragon  optional dragon argument (for commands operating on a dragon)
 * @param type    optional dragon type argument (for counting/filtering by type)
 * @param name    optional name argument (for filtering by name prefix)
 */
public record Command(Commands command, dragon.Dragon dragon, dragon.DragonType type, String name)
        implements Serializable {
}
