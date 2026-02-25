package minitomcat.server;

import minitomcat.container.Engine;
import minitomcat.connector.Connector;
import minitomcat.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Service：将多个 Connector 与一个 Engine 关联起来。
 * 实现 Lifecycle，并负责将 Engine 设置到每个 Connector（更接近 Tomcat）。
 */
public class Service implements Lifecycle {
    private String name = "Catalina";
    private final Engine engine = new Engine();
    private final List<Connector> connectors = new ArrayList<>();
    private boolean started;

    @Override
    public void init() throws Exception {
        engine.setParent(null);
        for (Connector c : connectors) {
            c.setContainer(engine);
            c.init();
        }
    }

    @Override
    public void start() throws Exception {
        if (started) return;
        for (Connector c : connectors) {
            c.start();
        }
        started = true;
    }

    @Override
    public void stop() throws Exception {
        if (!started) return;
        for (Connector c : connectors) {
            c.stop();
        }
        started = false;
    }

    @Override
    public void destroy() throws Exception {
        for (Connector c : connectors) {
            c.destroy();
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Engine getEngine() { return engine; }
    public void addConnector(Connector connector) {
        connector.setContainer(engine);
        connectors.add(connector);
    }
    public List<Connector> getConnectors() { return connectors; }
}
