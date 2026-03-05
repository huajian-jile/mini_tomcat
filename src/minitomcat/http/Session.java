package minitomcat.http;

import java.util.*;

/**
 * Session 接口，与 Tomcat 一致。
 */
public interface Session {
    public String getId();
    public long getCreationTime();
    public long getLastAccessedTime();
    public int getMaxInactiveInterval();
    public void setMaxInactiveInterval(int interval);
    public Object getAttribute(String name);
    public Enumeration<String> getAttributeNames();
    public void setAttribute(String name, Object value);
    public void removeAttribute(String name);
    public void invalidate();
    public boolean isValid();
    public void access();
    public void passivate();
    public void activate();
}
