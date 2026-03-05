package minitomcat.container;

import minitomcat.core.LifecycleBase;
import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.lifecycle.LifecycleException;

import java.util.ArrayList;
import java.util.List;

/**
 * 核心处理引擎，管理其下所有虚拟主机（Host）。
 * 持有 Pipeline 与 Host 列表，BasicValve 负责按 Host 分发。
 * 实现 Lifecycle，支持事件监听机制。
 */
public final class Engine extends LifecycleBase implements Container {
    private String name = "Engine";
    private Container parent;
    private final List<Host> hosts = new ArrayList<>();
    private final Pipeline pipeline = new PipelineBase();
    private String defaultHost = "localhost";
    private long startTime = 0;

    public Engine() {
        pipeline.setBasic(new EngineValve(this));
    }

    @Override
    protected void initInternal() throws LifecycleException {
        // 初始化所有 Host
        for (Host host : hosts) {
            host.init();
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        startTime = System.currentTimeMillis();
        // 启动所有 Host
        for (Host host : hosts) {
            host.start();
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        // 停止所有 Host
        for (Host host : hosts) {
            host.stop();
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        // 销毁所有 Host
        for (Host host : hosts) {
            host.destroy();
        }
    }

    @Override
    public void invoke(Request request, Response response) throws Exception {
        pipeline.invoke(request, response);
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setParent(Container parent) {
        this.parent = parent;
    }

    @Override
    public Container getParent() {
        return parent;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addHost(Host host) {
        host.setParent(this);
        hosts.add(host);
    }

    public void removeHost(Host host) {
        hosts.remove(host);
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public Host findHost(String name) {
        for (Host host : hosts) {
            if (host.getName().equalsIgnoreCase(name)) {
                return host;
            }
        }
        return null;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getUpTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Engine 的 BasicValve：按 Host 头选择虚拟主机，未匹配则用默认
     */
    private static class EngineValve implements Valve {
        private final Engine engine;

        EngineValve(Engine engine) {
            this.engine = engine;
        }

        @Override
        public void invoke(Request request, Response response, Valve next) throws Exception {
            String hostName = request.getHeader("Host");
            if (hostName != null) {
                int colon = hostName.indexOf(':');
                if (colon > 0) {
                    hostName = hostName.substring(0, colon).trim();
                } else {
                    hostName = hostName.trim();
                }
            }

            Host host = null;
            // 先精确匹配
            for (Host h : engine.getHosts()) {
                if (hostName != null && hostName.equalsIgnoreCase(h.getName())) {
                    host = h;
                    break;
                }
            }
            // 再模糊匹配（支持通配符域名）
            if (host == null) {
                for (Host h : engine.getHosts()) {
                    if (h.getName().startsWith("*.")) {
                        String suffix = h.getName().substring(1);
                        if (hostName != null && hostName.endsWith(suffix)) {
                            host = h;
                            break;
                        }
                    }
                }
            }
            // 使用默认主机
            if (host == null && !engine.getHosts().isEmpty()) {
                host = engine.findHost(engine.getDefaultHost());
                if (host == null) {
                    host = engine.getHosts().get(0);
                }
            }

            if (host != null) {
                host.invoke(request, response);
            } else {
                response.setStatus(500, "Internal Server Error");
                response.setBody("No Host configured.");
            }
        }
    }
}
