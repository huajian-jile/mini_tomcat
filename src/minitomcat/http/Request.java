package minitomcat.http;

import minitomcat.container.Context;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 代表一次 HTTP 请求，由 Connector 从网络流中解析并封装。
 * 支持请求参数、属性、Session（更接近 Tomcat 的 Request）。
 */
public class Request {
    private String method;
    private String uri;
    private String queryString;
    private String protocol;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private InputStream inputStream;
    private byte[] postBody;  // POST 表单 body，解析后保留供 getInputStream 重放

    private String contextPath = "";
    private String servletPath = "";
    private Context context;
    private Response response;
    private HttpSession session;
    private boolean sessionRequested;

    public Request(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /** 解析请求行、请求头，并解析 query string（更接近 Tomcat） */
    public void parse() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) return;

        String[] parts = requestLine.split("\\s+", 3);
        if (parts.length >= 1) method = parts[0];
        if (parts.length >= 2) {
            uri = parts[1];
            int q = uri.indexOf('?');
            if (q >= 0) {
                queryString = uri.substring(q + 1);
                parseQueryString(queryString);
                uri = uri.substring(0, q);
            }
        }
        if (parts.length >= 3) protocol = parts[2];

        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                headers.put(name, value);
            }
        }

        // POST 表单 body 解析（application/x-www-form-urlencoded）
        if ("POST".equalsIgnoreCase(method)) {
            String ct = headers.get("Content-Type");
            if (ct != null && ct.toLowerCase().contains("application/x-www-form-urlencoded")) {
                String cl = headers.get("Content-Length");
                int len = -1;
                if (cl != null) try { len = Integer.parseInt(cl.trim()); } catch (NumberFormatException ignored) {}
                if (len > 0 && len <= 1024 * 1024) {  // 限制 1MB
                    byte[] buf = new byte[len];
                    int off = 0;
                    while (off < len) {
                        int n = inputStream.read(buf, off, len - off);
                        if (n <= 0) break;
                        off += n;
                    }
                    postBody = buf;
                    String bodyStr = new String(buf, 0, off, StandardCharsets.UTF_8);
                    parseQueryString(bodyStr);
                }
            }
        }
    }

    private void parseQueryString(String qs) {
        if (qs == null || qs.isEmpty()) return;
        for (String pair : qs.split("&")) {
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                String key = decode(pair.substring(0, eq));
                String value = decode(pair.substring(eq + 1));
                parameters.put(key, value);
            } else if (!pair.isEmpty()) {
                parameters.put(decode(pair), "");
            }
        }
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    /** 从 Cookie 头中解析指定 name 的值 */
    public String getCookieValue(String name) {
        String cookie = headers.get("Cookie");
        if (cookie == null) return null;
        for (String part : cookie.split(";")) {
            part = part.trim();
            int eq = part.indexOf('=');
            if (eq > 0 && part.substring(0, eq).trim().equalsIgnoreCase(name)) {
                return part.substring(eq + 1).trim();
            }
        }
        return null;
    }

    public String getMethod() { return method; }
    public String getUri() { return uri; }
    public String getQueryString() { return queryString; }
    public String getProtocol() { return protocol; }
    public String getHeader(String name) { return headers.get(name); }
    public InputStream getInputStream() {
        if (postBody != null) return new ByteArrayInputStream(postBody);
        return inputStream;
    }

    public String getParameter(String name) { return parameters.get(name); }
    public Map<String, String> getParameterMap() { return Collections.unmodifiableMap(parameters); }

    public void setAttribute(String name, Object value) { attributes.put(name, value); }
    public Object getAttribute(String name) { return attributes.get(name); }

    public String getContextPath() { return contextPath; }
    public void setContextPath(String contextPath) { this.contextPath = contextPath; }

    public String getServletPath() { return servletPath; }
    public void setServletPath(String servletPath) { this.servletPath = servletPath; }

    public Context getContext() { return context; }
    public void setContext(Context context) { this.context = context; }

    public void setResponse(Response response) { this.response = response; }

    /** 获取当前会话，若无则根据 Context 的 SessionManager 创建并写 Cookie */
    public HttpSession getSession() {
        sessionRequested = true;
        if (session != null) return session;
        if (context == null) return null;
        SessionManager manager = context.getSessionManager();
        if (manager == null) return null;
        String id = getCookieValue(manager.getCookieName());
        session = manager.getSession(id);
        if (session == null) {
            session = manager.createSession();
            if (response != null) {
                response.addCookie(manager.getCookieName(), session.getId(), "/", -1);
            }
        }
        return session;
    }

    public boolean isSessionRequested() { return sessionRequested; }
}
