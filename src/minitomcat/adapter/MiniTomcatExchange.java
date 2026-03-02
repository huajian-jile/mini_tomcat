package minitomcat.adapter;

import com.sun.net.httpserver.*;

import minitomcat.http.Request;
import minitomcat.http.Response;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 适配器：将 mini_tomcat 的 Request/Response 适配为 HttpExchange，
 * 供 minispring 的 DispatcherServlet 使用。
 */
public class MiniTomcatExchange implements HttpExchange {

    private final Request request;
    private final Response response;
    private String requestMethod;
    private URI requestURI;
    private Map<String, String> requestHeaders = new HashMap<>();
    private Map<String, String> responseHeaders = new HashMap<>();
    private InputStream requestBody;
    private OutputStream responseBody;
    private int responseCode = 200;

    public MiniTomcatExchange(Request request, Response response) throws IOException {
        this.request = request;
        this.response = response;
        this.requestMethod = request.getMethod();
        String uri = request.getUri();
        this.requestURI = URI.create(uri != null ? uri : "/");
        this.requestBody = request.getInputStream();
        this.responseBody = response.getOutputStream();
    }

    public Request getRequest() { return request; }
    public Response getResponse() { return response; }

    @Override
    public Headers getRequestHeaders() {
        return new Headers() {
            @Override
            public String getFirst(String key) {
                return requestHeaders.get(key.toLowerCase());
            }
        };
    }

    @Override
    public Headers getResponseHeaders() {
        return new Headers() {
            @Override
            public String getFirst(String key) {
                return responseHeaders.get(key.toLowerCase());
            }

            @Override
            public String put(String key, String value) {
                return responseHeaders.put(key.toLowerCase(), value);
            }
        };
    }

    @Override
    public URI getRequestURI() {
        return requestURI;
    }

    @Override
    public String getRequestMethod() {
        return requestMethod;
    }

    @Override
    public InputStream getRequestBody() {
        return requestBody;
    }

    @Override
    public OutputStream getResponseBody() {
        return responseBody;
    }

    @Override
    public void sendResponseHeaders(int rCode, long responseLength) throws IOException {
        this.responseCode = rCode;
        response.setStatus(rCode, "");
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
    public boolean keepAlive() {
        return true;
    }

    @Override
    public void close() {
        try {
            response.flush();
        } catch (IOException ignored) {}
    }

    @Override
    public HttpContext getHttpContext() {
        return null;
    }
}
