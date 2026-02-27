package minitomcat.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 代表一次 HTTP 响应，由 Connector 写入到客户端。
 * 支持 Content-Type、Cookie 等（更接近 Tomcat 的 Response）。
 */
public class Response {
    private final OutputStream outputStream;
    private int status = 200;
    private String statusMessage = "OK";
    private String contentType;
    private final List<String> headerLines = new ArrayList<>();
    private byte[] body;
    private boolean committed;

    public Response(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setStatus(int status, String message) {
        this.status = status;
        this.statusMessage = message;
    }

    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentType() { return contentType; }

    public void addHeader(String name, String value) {
        headerLines.add(name + ": " + value);
    }

    /** 添加 Set-Cookie（用于 Session 等） */
    public void addCookie(String name, String value, String path, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        if (path != null) sb.append("; Path=").append(path);
        if (maxAge >= 0) sb.append("; Max-Age=").append(maxAge);
        sb.append("; HttpOnly");
        headerLines.add("Set-Cookie: " + sb.toString());
    }

    public void setBody(String content) {
        this.body = content.getBytes(StandardCharsets.UTF_8);
    }

    public void setBody(byte[] content) {
        this.body = content;
    }

    /** 将响应写回客户端（仅可写一次） */
    public void flush() throws IOException {
        if (committed) return;
        committed = true;

        if (contentType != null) {
            addHeader("Content-Type", contentType);
        }
        if (body != null) {
            boolean hasLength = headerLines.stream().anyMatch(h -> h.toLowerCase().startsWith("content-length:"));
            if (!hasLength) {
                addHeader("Content-Length", String.valueOf(body.length));
            }
        }

        String statusLine = "HTTP/1.1 " + status + " " + statusMessage + "\r\n";
        outputStream.write(statusLine.getBytes(StandardCharsets.UTF_8));
        for (String line : headerLines) {
            outputStream.write(line.getBytes(StandardCharsets.UTF_8));
            outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
        if (body != null) {
            outputStream.write(body);
        }
        outputStream.flush();
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public boolean isCommitted() { return committed; }
}
