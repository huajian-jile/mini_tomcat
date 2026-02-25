package minitomcat.http;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理器：创建、按 ID 查找会话，内存存储。
 */
public class SessionManager {
    private static final String COOKIE_NAME = "JSESSIONID";
    private final Map<String, HttpSession> sessions = new ConcurrentHashMap<>();

    public String getCookieName() { return COOKIE_NAME; }

    public HttpSession createSession() {
        String id = UUID.randomUUID().toString().replace("-", "");
        HttpSession session = new HttpSession(id);
        sessions.put(id, session);
        return session;
    }

    public HttpSession getSession(String id) {
        return id == null ? null : sessions.get(id);
    }

    public void removeSession(String id) {
        if (id != null) sessions.remove(id);
    }
}
