package minitomcat.server;

import minitomcat.container.Engine;
import minitomcat.connector.Connector;
import minitomcat.core.LifecycleBase;
import minitomcat.lifecycle.LifecycleException;
import minitomcat.naming.NamingResources;

import java.util.ArrayList;
import java.util.List;

/**
 * Service：将多个 Connector 与一个 Engine 关联起来。
 * 实现 Lifecycle，支持事件监听机制。
 */
public class Service extends LifecycleBase {
    private String name = "Catalina";
    private Server server;
    private Engine engine;
    private final List<Connector> connectors = new ArrayList<>();
    private NamingResources namingResources;

    public Service() {
        // 默认创建 Engine
        engine = new Engine();
        engine.setName("Catalina");
    }

    @Override
    protected void initInternal() throws LifecycleException {
        // 初始化 Engine
        if (engine != null) {
            engine.init();
        }
        // 初始化所有 Connector
        for (Connector connector : connectors) {
            connector.init();
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        // 启动 Engine
        if (engine != null) {
            engine.start();
        }
        // 启动所有 Connector
        for (Connector connector : connectors) {
            connector.start();
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        // 停止所有 Connector
        for (Connector connector : connectors) {
            connector.stop();
        }
        // 停止 Engine
        if (engine != null) {
            engine.stop();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        // 销毁所有 Connector
        for (Connector connector : connectors) {
            connector.destroy();
        }
        // 销毁 Engine
        if (engine != null) {
            engine.destroy();
        }
    }

    public void addConnector(Connector connector) {
        connector.setService(this);
        connectors.add(connector);
        // 关联 Engine
        if (engine != null) {
            connector.setContainer(engine);
        }
    }

    public void removeConnector(Connector connector) {
        connectors.remove(connector);
    }

    public Connector[] findConnectors() {
        return connectors.toArray(new Connector[0]);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Engine getContainer() {
        return engine;
    }

    public void setContainer(Engine engine) {
        this.engine = engine;
        // 重新关联所有 Connector
        for (Connector connector : connectors) {
            connector.setContainer(engine);
        }
    }

    public Engine getEngine() {
        return engine;
    }

    public NamingResources getNamingResources() {
        return namingResources;
    }

    public void setNamingResources(NamingResources namingResources) {
        this.namingResources = namingResources;
    }

    public List<Connector> getConnectors() {
        return connectors;
    }
}
