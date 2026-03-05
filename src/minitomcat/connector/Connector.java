package minitomcat.connector;

import minitomcat.container.Container;
import minitomcat.container.Engine;
import minitomcat.core.LifecycleBase;
import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.lifecycle.LifecycleException;
import minitomcat.server.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 连接器（Coyote 角色）：接收 HTTP 请求，封装为 Request，交给容器，写回 Response。
 * 实现 Lifecycle，支持事件监听机制。
 */
public class Connector extends LifecycleBase implements Runnable {
    private int port = 8080;
    private String protocol = "HTTP/1.1";
    private Container container;
    private Service service;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private ExecutorService executor;
    private int maxThreads = 200;
    private int minSpareThreads = 10;
    private int connectionTimeout = 20000;

    public Connector() {
    }

    public Connector(int port) {
        this.port = port;
    }

    @Override
    protected void initInternal() throws LifecycleException {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Coyote-" + port + "-worker");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    protected void startInternal() throws LifecycleException {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            Thread acceptor = new Thread(this, "Coyote-Acceptor-" + port);
            acceptor.setDaemon(false);
            acceptor.start();
        } catch (IOException e) {
            throw new LifecycleException("Failed to start connector on port " + port, e);
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        serverSocket = null;
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        stopInternal();
    }

    @Override
    public void run() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(connectionTimeout);
                executor.submit(() -> process(socket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Accept failed: " + e.getMessage());
                }
            }
        }
    }

    private void process(Socket socket) {
        try (InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            Request request = new Request(in);
            Response response = new Response(out);
            request.setResponse(response);
            request.parse();

            if (container != null) {
                container.invoke(request, response);
            } else {
                response.setStatus(503, "Service Unavailable");
                response.setBody("No container.");
            }

            response.flush();
        } catch (Exception e) {
            if (!isClientDisconnect(e)) {
                e.printStackTrace();
            }
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isClientDisconnect(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketException) return true;
            String msg = t.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("connection reset") || lower.contains("broken pipe")
                        || lower.contains("connection aborted") || lower.contains("abort")
                        || lower.contains("closed") || lower.contains("connection was")) {
                    return true;
                }
            }
        }
        return false;
    }

    // Getters and Setters
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
        // 自动从 Service 获取 Engine
        if (service != null && service.getEngine() != null) {
            this.container = service.getEngine();
        }
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMinSpareThreads() {
        return minSpareThreads;
    }

    public void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
}
