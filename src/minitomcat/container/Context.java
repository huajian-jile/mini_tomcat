package minitomcat.container;

import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.http.SessionManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 代表一个 Web 应用（对应一个项目路径），是 Servlet 运行的直接环境。
 * 支持 docBase（静态资源根目录）、welcome-file-list、SessionManager。
 */
public class Context implements Container {
    private String name = "ROOT";
    private String path = "/";
    private String docBase;  // 应用根目录，用于静态资源与 welcome 文件
    private Container parent;
    private final List<Wrapper> wrappers = new ArrayList<>();
    private final List<String> welcomeFiles = new ArrayList<>();
    private final SessionManager sessionManager = new SessionManager();
    private final Pipeline pipeline = new PipelineBase();

    public Context() {
        welcomeFiles.add("index.html");
        welcomeFiles.add("index.htm");
        pipeline.setBasic(new ContextValve(this));
    }

    @Override
    public void invoke(Request request, Response response) throws Exception {
        pipeline.invoke(request, response);
    }

    @Override
    public Pipeline getPipeline() { return pipeline; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getDocBase() { return docBase; }
    public void setDocBase(String docBase) { this.docBase = docBase; }

    public List<String> getWelcomeFiles() { return welcomeFiles; }
    public void addWelcomeFile(String name) {
        if (!welcomeFiles.contains(name)) welcomeFiles.add(name);
    }

    public SessionManager getSessionManager() { return sessionManager; }

    /** 根据相对路径获取资源流（从 docBase 读取），用于 DefaultServlet 等 */
    public InputStream getResourceAsStream(String path) throws IOException {
        if (docBase == null || path == null) return null;
        if (path.startsWith("/")) path = path.substring(1);
        Path file = Paths.get(docBase, path.replace("/", File.separator)).normalize();
        if (!file.startsWith(Paths.get(docBase).normalize())) return null; // 防止路径穿越
        if (!Files.isRegularFile(file)) return null;
        return Files.newInputStream(file);
    }

    public boolean resourceExists(String path) {
        if (docBase == null || path == null) return false;
        if (path.startsWith("/")) path = path.substring(1);
        Path file = Paths.get(docBase, path.replace("/", File.separator)).normalize();
        if (!file.startsWith(Paths.get(docBase).normalize())) return false;
        return Files.isRegularFile(file);
    }

    @Override
    public String getName() { return name; }
    @Override
    public void setParent(Container parent) { this.parent = parent; }
    @Override
    public Container getParent() { return parent; }
    public void setName(String name) { this.name = name; }

    public void addWrapper(Wrapper w) {
        w.setParent(this);
        wrappers.add(w);
    }
    public List<Wrapper> getWrappers() { return wrappers; }

    private static class ContextValve implements Valve {
        private final Context context;
        ContextValve(Context context) { this.context = context; }
        @Override
        public void invoke(Request request, Response response, Valve next) throws Exception {
            String uri = request.getUri();
            if (uri == null) uri = "/";
            String pathInfo = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
            if (context.getPath().length() > 1 && pathInfo.startsWith(context.getPath())) {
                pathInfo = pathInfo.substring(context.getPath().length());
                if (pathInfo.isEmpty()) pathInfo = "/";
            } else if ("/".equals(context.getPath()) && pathInfo.startsWith("/")) {
                // pathInfo 保持为 / 或 /xxx
            } else {
                pathInfo = "/";
            }

            // Welcome file：若 pathInfo 以 / 结尾或是 /，则尝试 welcome 文件
            if ("/".equals(pathInfo) || pathInfo.endsWith("/")) {
                for (String welcome : context.getWelcomeFiles()) {
                    String tryPath = pathInfo.endsWith("/") ? pathInfo + welcome : pathInfo + "/" + welcome;
                    if (context.resourceExists(tryPath)) {
                        pathInfo = tryPath;
                        break;
                    }
                }
            }

            Wrapper wrapper = null;
            for (Wrapper w : context.getWrappers()) {
                if (matchPattern(w.getUrlPattern(), pathInfo)) {
                    wrapper = w;
                    break;
                }
            }
            if (wrapper == null && !context.getWrappers().isEmpty()) {
                wrapper = context.getWrappers().get(0);
            }
            if (wrapper != null) {
                request.setServletPath(pathInfo);
                wrapper.invoke(request, response);
            } else {
                response.setStatus(404, "Not Found");
                response.setBody("No servlet for: " + pathInfo);
            }
        }

        private boolean matchPattern(String pattern, String path) {
            if ("/".equals(pattern)) return path.startsWith("/");
            if (pattern.equals(path)) return true;
            if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                return path.startsWith(prefix) && (path.length() == prefix.length() || path.charAt(prefix.length()) == '/');
            }
            return false;
        }
    }
}
