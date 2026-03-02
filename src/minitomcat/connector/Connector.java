package minitomcat.connector;

import minitomcat.container.Container;
import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.lifecycle.Lifecycle;

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
 * 实现 Lifecycle，支持 init/start/stop/destroy（更接近 Tomcat）。
 */
public class Connector implements Lifecycle, Runnable {
    private int port = 9000;
    private Container container;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private ExecutorService executor;

    public void setPort(int port) { this.port = port; }
    public int getPort() { return port; }
    public void setContainer(Container container) { this.container = container; }
    public Container getContainer() { return container; }

    @Override
    public void init() throws Exception {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Coyote-" + port + "-worker");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void start() throws Exception {
        if (running) return;
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptor = new Thread(this, "Coyote-Acceptor-" + port);
        acceptor.setDaemon(false);  // 非 daemon，保证 main 结束后 JVM 不退出、服务持续运行
        acceptor.start();
    }

    @Override
    public void stop() throws Exception {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        serverSocket = null;
        if (executor != null) executor.shutdown();
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    @Override
    public void run() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(20000);
                executor.submit(() -> process(socket));
            } catch (IOException e) {
                if (running) e.printStackTrace();
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
            } catch (IOException ignored) {}
        }
    }

    /** 客户端断开（刷新、关闭、取消）时抛出的异常，与 Tomcat 一致：静默处理，不打堆栈 */
    private static boolean isClientDisconnect(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketException) return true;
            String msg = t.getMessage();
            if (msg != null) {
                String lower = msg.toLowerCase();
                if (lower.contains("connection reset") || lower.contains("broken pipe")
                        || lower.contains("connection aborted") || lower.contains("中止")
                        || lower.contains("closed") || lower.contains("connection was")) {
                    return true;
                }
            }
        }
        return false;
    }
}
