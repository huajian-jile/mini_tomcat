package minitomcat.server;

import minitomcat.core.LifecycleBase;
import minitomcat.lifecycle.*;
import minitomcat.naming.NamingResources;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Server：代表整个 Tomcat 实例，是最高级别的容器，持有多个 Service。
 * 实现 Lifecycle，支持事件监听机制。
 */
public class Server extends LifecycleBase {
    private final List<Service> services = new ArrayList<>();
    private int port = 8005;
    private String shutdownCommand = "SHUTDOWN";
    private ServerSocket serverSocket;
    private volatile boolean stopped = false;
    private Thread shutdownThread;
    private NamingResources namingResources;

    public Server() {
        // 默认端口 8005 用于接收关闭命令
    }

    @Override
    protected void initInternal() throws LifecycleException {
        // 初始化命名资源
        if (namingResources != null) {
            namingResources.init();
        }
        // 初始化所有 Service
        for (Service service : services) {
            service.init();
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        // 启动所有 Service
        for (Service service : services) {
            service.start();
        }
        // 启动关闭监听器
        startShutdownListener();
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        // 停止所有 Service
        for (Service service : services) {
            service.stop();
        }
        // 停止关闭监听器
        stopped = true;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        // 销毁所有 Service
        for (Service service : services) {
            service.destroy();
        }
        // 销毁命名资源
        if (namingResources != null) {
            namingResources.destroy();
        }
    }

    private void startShutdownListener() {
        stopped = false;
        shutdownThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                while (!stopped) {
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout(10000);
                    try {
                        java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(socket.getInputStream()));
                        String command = reader.readLine();
                        if (shutdownCommand.equals(command)) {
                            stop();
                        }
                    } finally {
                        socket.close();
                    }
                }
            } catch (IOException e) {
                // Server socket closed
            }
        }, "Shutdown-Port-" + port);
        shutdownThread.setDaemon(false);
        shutdownThread.start();
    }

    public void addService(Service service) {
        service.setServer(this);
        services.add(service);
    }

    public void removeService(Service service) {
        services.remove(service);
    }

    public List<Service> getServices() {
        return services;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getShutdownCommand() {
        return shutdownCommand;
    }

    public void setShutdownCommand(String shutdownCommand) {
        this.shutdownCommand = shutdownCommand;
    }

    public NamingResources getNamingResources() {
        return namingResources;
    }

    public void setNamingResources(NamingResources namingResources) {
        this.namingResources = namingResources;
    }

    /**
     * 关闭 Server
     */
    public void shutdown() throws Exception {
        stop();
    }
}
