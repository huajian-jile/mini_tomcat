package minispring.servlet;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import minitomcat.http.Request;
import minitomcat.http.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * 适配器：将 mini_tomcat 的 Request/Response 适配为 HttpExchange
 */
public class MiniSpringExchange extends HttpExchange {

    private final Request request;
    private final Response response;
    private final String path;
    private final String method;
    private Headers requestHeaders = new Headers();
    private Headers responseHeaders = new Headers();
    private ByteArrayOutputStream bodyCache = new ByteArrayOutputStream();
    private boolean headersSent = false;

    public MiniSpringExchange(Request request, Response response) {
        this.request = request;
        this.response = response;
        String uri = request.getUri();
        // 去掉 /api 前缀，因为 servlet mapping 是 /api/*
        if (uri != null && uri.startsWith("/api")) {
            uri = uri.substring(4);
            if (uri.isEmpty()) uri = "/";
        }
        this.path = uri != null ? uri : "/";
        this.method = request.getMethod();
    }

    @Override
    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    @Override
    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public URI getRequestURI() {
        return URI.create(path);
    }

    @Override
    public String getRequestMethod() {
        return method;
    }

    @Override
    public InputStream getRequestBody() {
        return request.getInputStream();
    }

    @Override
    public OutputStream getResponseBody() {
        return bodyCache;
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
        String message = getStatusMessage(rCode);
        response.setStatus(rCode, message);
        headersSent = true;
    }

    private static String getStatusMessage(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "OK";
        };
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return new InetSocketAddress(8080);
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return new InetSocketAddress(9000);
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public void setStreams(InputStream in, OutputStream out) {
    }

    @Override
    public void setAttribute(String name, Object value) {
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public void close() {
        try {
            // 设置响应头
            for (var entry : responseHeaders.entrySet()) {
                for (String value : entry.getValue()) {
                    response.addHeader(entry.getKey(), value);
                }
            }
            // 设置缓存的响应体
            response.setBody(bodyCache.toByteArray());
            response.flush();
        } catch (IOException ignored) {}
    }

    @Override
    public HttpContext getHttpContext() {
        return null;
    }

    @Override
    public HttpPrincipal getPrincipal() {
        return null;
    }

    @Override
    public int getResponseCode() {
        return 200;
    }
}
