package core;

import java.io.Serializable;

public class Request implements Serializable {
    public enum Status {
        INIT,
        GET,
        RESPONSE,
        ERROR
    };

    public Status status;
    public String request;
}
