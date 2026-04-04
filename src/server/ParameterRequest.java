package server;

import java.io.Serializable;

/**
 * Describes a parameter that the server is requesting from the client.
 * Used in interactive commands that require step-by-step input.
 */
public class ParameterRequest implements Serializable {
    public enum ParameterType {
        STRING,
        INTEGER,
        LONG,
        FLOAT,
        DRAGON_TYPE,
        DRAGON,
        ID
    }

    /** Unique identifier for this parameter in the dialog */
    public String parameterName;

    /** Human-readable prompt to display to user */
    public String prompt;

    /** Type of parameter being requested */
    public ParameterType type;

    /** Whether this parameter is required */
    public boolean required;

    public ParameterRequest(String parameterName, String prompt, ParameterType type, boolean required) {
        this.parameterName = parameterName;
        this.prompt = prompt;
        this.type = type;
        this.required = required;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]%s", prompt, type, required ? " *" : "");
    }
}
