package minitomcat.container;

import minitomcat.http.Request;
import minitomcat.http.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟主机，对应一个域名，可包含多个 Web 应用（Context）。
 * 使用 Pipeline + HostValve 选择 Context 并设置 request 的 contextPath/context。
 */
public class Host implements Container {
    private String name = "localhost";
    private Container parent;
    private final List<Context> contexts = new ArrayList<>();
    private final Pipeline pipeline = new PipelineBase();

    public Host() {
        pipeline.setBasic(new HostValve(this));
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
    public void addContext(Context context) {
        context.setParent(this);
        contexts.add(context);
    }
    public List<Context> getContexts() { return contexts; }

    private static class HostValve implements Valve {
        private final Host host;
        HostValve(Host host) { this.host = host; }
        @Override
        public void invoke(Request request, Response response, Valve next) throws Exception {
            String uri = request.getUri();
            if (uri == null) uri = "/";
            String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
            Context matched = null;
            String contextPath = "";
            // 选择最长匹配
            for (Context ctx : host.getContexts()) {
                String cp = ctx.getPath();
                if (path.equals(cp) || path.startsWith(cp + "/")) {
                    if (cp.length() > contextPath.length()) {
                        matched = ctx;
                        contextPath = cp;
                    }
                }
            }
            // 如果没有匹配到，使用 ROOT ("/")
            if (matched == null) {
                for (Context ctx : host.getContexts()) {
                    if ("/".equals(ctx.getPath())) {
                        matched = ctx;
                        break;
                    }
                }
            }
            if (matched != null) {
                request.setContext(matched);
                request.setContextPath(matched.getPath());
                matched.invoke(request, response);
            } else {
                response.setStatus(404, "Not Found");
                response.setBody("No context for: " + path);
            }
        }
    }
}
