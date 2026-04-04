package core;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

import dragon.Dragon;

public class Response implements Serializable {
    public enum Status {
        SUCCESS,
        ERROR,
        NEED_PARAMETER
    };

    public Status status;
    public Optional<Stream<Dragon>> data;
    public Optional<String> message;
}
