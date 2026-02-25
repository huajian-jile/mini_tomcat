package minitomcat.server;

import minitomcat.lifecycle.Lifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Server：代表整个 Tomcat 实例，是最高级别的容器，持有多个 Service。
 * 实现 Lifecycle：init -> start -> stop -> destroy（更接近 Tomcat）。
 */
public class Server implements Lifecycle {
    private final List<Service> services = new ArrayList<>();
    private boolean started;

    @Override
    public void init() throws Exception {
        for (Service service : services) {
            service.init();
        }
    }

    @Override
    public void start() throws Exception {
        if (started) return;
        for (Service service : services) {
            service.start();
        }
        started = true;
    }

    @Override
    public void stop() throws Exception {
        if (!started) return;
        for (Service service : services) {
            service.stop();
        }
        started = false;
    }

    @Override
    public void destroy() throws Exception {
        for (Service service : services) {
            service.destroy();
        }
    }

    public void addService(Service service) {
        services.add(service);
    }

    public List<Service> getServices() {
        return services;
    }
}
