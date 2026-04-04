package server;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import core.ParameterRequest;
import ui.Commands;

/**
 * Represents an active session with a connected client. Maintains dialog state, collected
 * parameters, and pending parameter requests.
 */
public class ClientSession {

    public ClientSession(SocketAddress clientAddress) {
        synchronized (ClientSession.class) {
            this.sessionId = nextSessionId++;
        }
        this.clientAddress = clientAddress;
        this.createdAt = System.currentTimeMillis();
        this.lastActivity = createdAt;
        this.pendingParameters = new LinkedList<>();
        this.collectedParameters = new HashMap<>();
    }

    public long getSessionId() {
        return sessionId;
    }

    public SocketAddress getClientAddress() {
        return clientAddress;
    }

    public void updateActivity() {
        this.lastActivity = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - lastActivity) > SESSION_TIMEOUT;
    }

    public Commands getCurrentCommand() {
        return currentCommand;
    }

    public void setCurrentCommand(Commands command) {
        this.currentCommand = command;
        this.collectedParameters.clear();
        this.pendingParameters.clear();
    }

    public Queue<ParameterRequest> getPendingParameters() {
        return pendingParameters;
    }

    public Map<String, Object> getCollectedParameters() {
        return collectedParameters;
    }

    public ParameterRequest peekNextParameter() {
        return pendingParameters.peek();
    }

    public ParameterRequest pollNextParameter() {
        return pendingParameters.poll();
    }

    public void addParameterRequest(ParameterRequest param) {
        pendingParameters.offer(param);
    }

    public void addParameter(String name, Object value) {
        collectedParameters.put(name, value);
    }

    public Object getParameter(String name) {
        return collectedParameters.get(name);
    }

    public boolean hasMoreParameters() {
        return !pendingParameters.isEmpty();
    }

    public void clearDialog() {
        currentCommand = null;
        pendingParameters.clear();
        collectedParameters.clear();
    }

    @Override
    public String toString() {
        return String.format(
                "Session[%d, %s, cmd=%s, params=%d]",
                sessionId, clientAddress, currentCommand, pendingParameters.size());
    }

    private static long nextSessionId = 1;
    private final long sessionId;
    private final SocketAddress clientAddress;
    private final long createdAt;
    private long lastActivity;

    // Dialog state
    private Commands currentCommand;
    private Queue<ParameterRequest> pendingParameters;
    private Map<String, Object> collectedParameters;

    // Timeout in milliseconds (default 5 minutes)
    private static final long SESSION_TIMEOUT = 5 * 60 * 1000;
}
