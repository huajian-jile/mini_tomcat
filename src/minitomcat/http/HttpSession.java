package minitomcat.http;

import java.util.HashMap;
import java.util.Map;

/**
 * 会话对象，与 Tomcat 的 HttpSession 对应，用于在多次请求间保存用户数据。
 */
public class HttpSession {
    private final String id;
    private final long creationTime = System.currentTimeMillis();
    private final Map<String, Object> attributes = new HashMap<>();

    public HttpSession(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public long getCreationTime() { return creationTime; }

    public Object getAttribute(String name) { return attributes.get(name); }

    public void setAttribute(String name, Object value) { attributes.put(name, value); }

    public void removeAttribute(String name) { attributes.remove(name); }

    public void invalidate() { attributes.clear(); }
}
