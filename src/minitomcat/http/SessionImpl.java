package minitomcat.http;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session 实现类，与 Tomcat 一致。
 */
public class SessionImpl implements Session {
    private final String id;
    private long creationTime;
    private long lastAccessedTime;
    private int maxInactiveInterval = 30 * 60; // 30 minutes
    private boolean isValid = true;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private SessionManager manager;
    private boolean isNew = true;

    public SessionImpl(String id) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public Object getAttribute(String name) {
        if (!isValid) {
            throw new IllegalStateException("Session is not valid");
        }
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        if (!isValid) {
            throw new IllegalStateException("Session is not valid");
        }
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (!isValid) {
            throw new IllegalStateException("Session is not valid");
        }
        if (value == null) {
            removeAttribute(name);
        } else {
            attributes.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        if (!isValid) {
            throw new IllegalStateException("Session is not valid");
        }
        attributes.remove(name);
    }

    @Override
    public void invalidate() {
        isValid = false;
        attributes.clear();
        if (manager != null) {
            manager.removeSession(id);
        }
    }

    @Override
    public boolean isValid() {
        if (!isValid) {
            return false;
        }
        if (maxInactiveInterval > 0) {
            long elapsed = System.currentTimeMillis() - lastAccessedTime;
            if (elapsed >= maxInactiveInterval * 1000) {
                invalidate();
                return false;
            }
        }
        return true;
    }

    @Override
    public void access() {
        lastAccessedTime = System.currentTimeMillis();
        isNew = false;
    }

    @Override
    public void passivate() {
        // Session 钝化（序列化等）
    }

    @Override
    public void activate() {
        // Session 激活
    }

    public void setManager(SessionManager manager) {
        this.manager = manager;
    }

    public SessionManager getManager() {
        return manager;
    }

    public boolean isNew() {
        return isNew;
    }
}
