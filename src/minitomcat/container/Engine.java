package minitomcat.container;

import minitomcat.http.Request;
import minitomcat.http.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * 核心处理引擎，管理其下所有虚拟主机（Host）。
 * 持有 Pipeline 与 Host 列表，BasicValve 负责按 Host 分发（更接近 Tomcat）。
 */
public final class Engine implements Container {
    private String name = "Engine";
    private Container parent;
    private final List<Host> hosts = new ArrayList<>();
    private final Pipeline pipeline = new PipelineBase();

    public Engine() {
        pipeline.setBasic(new EngineValve(this));
    }

    @Override
    public void invoke(Request request, Response response) throws Exception {
        pipeline.invoke(request, response);
    }

    @Override
    public Pipeline getPipeline() { return pipeline; }

    @Override
    public String getName() { return name; }
    @Override
    public void setParent(Container parent) { this.parent = parent; }
    @Override
    public Container getParent() { return parent; }

    public void setName(String name) { this.name = name; }
    public void addHost(Host host) {
        host.setParent(this);
        hosts.add(host);
    }
    public List<Host> getHosts() { return hosts; }

    /** Engine 的 BasicValve：按 Host 头选择虚拟主机，未匹配则用第一个（更接近 Tomcat） */
    private static class EngineValve implements Valve {
        private final Engine engine;
        EngineValve(Engine engine) { this.engine = engine; }
        @Override
        public void invoke(Request request, Response response, Valve next) throws Exception {
            String hostName = request.getHeader("Host");
            if (hostName != null) {
                int colon = hostName.indexOf(':');
                if (colon > 0) hostName = hostName.substring(0, colon).trim();
                else hostName = hostName.trim();
            }
            Host host = null;
            for (Host h : engine.getHosts()) {
                if (hostName != null && hostName.equalsIgnoreCase(h.getName())) {
                    host = h;
                    break;
                }
            }
            if (host == null && !engine.getHosts().isEmpty()) {
                host = engine.getHosts().get(0);
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
