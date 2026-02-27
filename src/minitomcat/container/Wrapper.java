package minitomcat.container;

import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.servlet.Servlet;

import java.util.HashMap;
import java.util.Map;

/**
 * 最底层容器，负责管理单个 Servlet 的生命周期并调用其 service。
 * 使用 Pipeline + WrapperValve 调用 Servlet。
 */
public class Wrapper implements Container {
    private String name = "default";
    private String urlPattern = "/";
    private Container parent;
    private Servlet servlet;
    private Map<String, String> initParams = new HashMap<>();
    private final Pipeline pipeline = new PipelineBase();

    public Wrapper() {
        pipeline.setBasic(new WrapperValve(this));
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

    public String getUrlPattern() { return urlPattern; }
    public void setUrlPattern(String urlPattern) { this.urlPattern = urlPattern; }
    public void setServlet(Servlet servlet) { this.servlet = servlet; }
    public Servlet getServlet() { return servlet; }
    public void setInitParams(Map<String, String> params) { this.initParams = params != null ? new HashMap<>(params) : new HashMap<>(); }
    public String getInitParameter(String name) { return initParams.get(name); }
    public void setName(String name) { this.name = name; }

    private static class WrapperValve implements Valve {
        private final Wrapper wrapper;
        WrapperValve(Wrapper wrapper) { this.wrapper = wrapper; }
        @Override
        public void invoke(Request request, Response response, Valve next) throws Exception {
            if (wrapper.getServlet() != null) {
                wrapper.getServlet().service(request, response);
            } else {
                response.setStatus(503, "Service Unavailable");
                response.setBody("Servlet not loaded.");
            }
        }
    }
}
