package minitomcat.http;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Session 管理器，与 Tomcat 一致。
 * 支持 Session 创建、销毁、持久化。
 */
public class SessionManager {
    private final Map<String, Session> sessions = new HashMap<>();
    private int maxActiveSessions = -1;
    private long sessionTimeout = 30 * 60; // 30 minutes
    private long lastId = 0;
    private boolean distributable = false;

    public SessionManager() {
        // 启动 Session 过期检查线程
        startExpirationThread();
    }

    private void startExpirationThread() {
        Thread expirationThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 每分钟检查一次
                    expireSessions();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Session-Expirer");
        expirationThread.setDaemon(true);
        expirationThread.start();
    }

    private void expireSessions() {
        long now = System.currentTimeMillis();
        List<Session> toRemove = new ArrayList<>();
        synchronized (sessions) {
            for (Session session : sessions.values()) {
                if (session.getMaxInactiveInterval() > 0) {
                    long elapsed = now - session.getLastAccessedTime();
                    if (elapsed >= session.getMaxInactiveInterval() * 1000) {
                        toRemove.add(session);
                    }
                }
            }
            for (Session session : toRemove) {
                removeSession(session.getId());
            }
        }
    }

    public synchronized Session createSession() {
        if (maxActiveSessions > 0 && sessions.size() >= maxActiveSessions) {
            return null;
        }
        String id = generateSessionId();
        Session session = new Session(id);
        session.setManager(this);
        sessions.put(id, session);
        return session;
    }

    public synchronized Session getSession(String id) {
        return sessions.get(id);
    }

    public synchronized void removeSession(String id) {
        Session session = sessions.remove(id);
        if (session != null) {
            session.invalidate();
        }
    }

    public synchronized void addSession(Session session) {
        sessions.put(session.getId(), session);
    }

    public synchronized void removeSession(Session session) {
        sessions.remove(session.getId());
    }

    public synchronized int getActiveSessions() {
        return sessions.size();
    }

    public synchronized void expireSession(String id) {
        removeSession(id);
    }

    private synchronized String generateSessionId() {
        long id = ++lastId;
        return "ID-" + System.currentTimeMillis() + "-" + id + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public int getMaxActiveSessions() {
        return maxActiveSessions;
    }

    public void setMaxActiveSessions(int maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public boolean isDistributable() {
        return distributable;
    }

    public void setDistributable(boolean distributable) {
        this.distributable = distributable;
    }

    public Collection<Session> getSessions() {
        return sessions.values();
    }
}
