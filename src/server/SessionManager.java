package server;

import java.net.SocketAddress;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * Manages active client sessions.
 * Handles session creation, lookup, cleanup, and timeout management.
 */
public class SessionManager {
    private static final Logger logger = Logger.getLogger(SessionManager.class);
    private final Map<SocketAddress, ClientSession> sessions = Collections.synchronizedMap(new WeakHashMap<>());
    private final Timer cleanupTimer;

    public SessionManager() {
        this.cleanupTimer = new Timer("SessionCleanup", true);
        // Cleanup expired sessions every minute
        cleanupTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                cleanupExpiredSessions();
            }
        }, 60000, 60000);
    }

    /**
     * Gets or creates a session for the given client address.
     */
    public ClientSession getOrCreateSession(SocketAddress clientAddress) {
        return sessions.computeIfAbsent(clientAddress, addr -> {
            ClientSession session = new ClientSession(addr);
            logger.info("Created new session: " + session);
            return session;
        });
    }

    /**
     * Gets an existing session without creating one.
     */
    public ClientSession getSession(SocketAddress clientAddress) {
        return sessions.get(clientAddress);
    }

    /**
     * Removes a session.
     */
    public void removeSession(SocketAddress clientAddress) {
        ClientSession session = sessions.remove(clientAddress);
        if (session != null) {
            logger.info("Removed session: " + session);
        }
    }

    /**
     * Cleans up expired sessions.
     */
    private void cleanupExpiredSessions() {
        List<SocketAddress> expiredAddresses = new ArrayList<>();
        for (Map.Entry<SocketAddress, ClientSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredAddresses.add(entry.getKey());
            }
        }
        for (SocketAddress addr : expiredAddresses) {
            removeSession(addr);
            logger.info("Cleaned up expired session for: " + addr);
        }
    }

    /**
     * Gets all active sessions (for monitoring/debugging).
     */
    public Collection<ClientSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    /**
     * Shuts down the session manager and cleanup timer.
     */
    public void shutdown() {
        cleanupTimer.cancel();
        sessions.clear();
        logger.info("SessionManager shut down");
    }
}
