package minitomcat.servlet;

import minitomcat.container.Context;
import minitomcat.http.Request;
import minitomcat.http.Response;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认 Servlet：优先从 Context 的 docBase 提供静态资源，否则返回欢迎页（更接近 Tomcat 的 DefaultServlet）。
 */
public class DefaultServlet implements Servlet {
    private static final Map<String, String> MIME_TYPES = new HashMap<>();
    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("txt", "text/plain; charset=UTF-8");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("svg", "image/svg+xml");
    }

    @Override
    public void service(Request request, Response response) throws Exception {
        Context ctx = request.getContext();
        String path = request.getServletPath();
        if (path == null) path = "/";

        // 尝试从 docBase 提供静态资源
        if (ctx != null && ctx.getDocBase() != null) {
            InputStream in = ctx.getResourceAsStream(path);
            if (in != null) {
                byte[] body = readAll(in);
                response.setContentType(getContentType(path));
                response.setBody(body);
                return;
            }
        }

        // 否则返回欢迎页
        response.setContentType("text/html; charset=UTF-8");
        String body = "<!DOCTYPE html><html><head><meta charset='UTF-8'/><title>Mini Tomcat</title></head>" +
                "<body><h1>Mini Tomcat</h1><p>Request: " + request.getMethod() + " " + request.getUri() + "</p></body></html>";
        response.setBody(body);
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private static String getContentType(String path) {
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            String ext = path.substring(dot + 1).toLowerCase();
            String mime = MIME_TYPES.get(ext);
            if (mime != null) return mime;
        }
        return "application/octet-stream";
    }
}
