import minitomcat.config.WebXmlParser;
import minitomcat.container.Context;
import minitomcat.container.Host;
import minitomcat.connector.Connector;
import minitomcat.server.Server;
import minitomcat.server.Service;
import minitomcat.servlet.DefaultServlet;
import minitomcat.container.Wrapper;

import java.io.File;
import java.nio.file.Paths;

/**
 * 启动 Mini Tomcat：组装 Server -> Service -> Connector + Engine -> Host -> Context，
 * 支持 docBase、web.xml、Lifecycle（init -> start）。
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Server server = new Server();
        Service service = new Service();
        service.setName("Catalina");

        Connector connector = new Connector();
        connector.setPort(9000);
        service.addConnector(connector);

        Host host = new Host();
        host.setName("localhost");

        // === ROOT 应用（静态资源）===
        Context context = new Context();
        context.setName("ROOT");
        context.setPath("/");

        File webappDir = resolveWebappDir();
        if (webappDir != null && webappDir.isDirectory()) {
            String docBase = webappDir.getAbsolutePath();
            context.setDocBase(docBase);
            System.out.println("[Mini Tomcat] docBase = " + docBase);
            File webXml = new File(webappDir, "WEB-INF/web.xml");
            if (webXml.isFile()) {
                new WebXmlParser(context).parse(webXml);
            } else {
                addDefaultWrapper(context);
            }
        } else {
            addDefaultWrapper(context);
        }

        // 最后添加 minispring 的 servlet（更具体的路径）
        try {
            Wrapper apiWrapper = new Wrapper();
            apiWrapper.setName("api");
            apiWrapper.setUrlPattern("/api/*");
            apiWrapper.setServlet(new minispring.servlet.MiniSpringServlet());
            context.addWrapper(apiWrapper);
            System.out.println("[Mini Tomcat] minispring 应用已部署到 /api");
        } catch (Exception e) {
            System.out.println("[Mini Tomcat] minispring 部署失败: " + e.getMessage());
        }
        host.addContext(context);

        service.getEngine().addHost(host);
        server.addService(service);

        server.init();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.stop(); } catch (Exception e) { e.printStackTrace(); }
        }));
        System.out.println("Mini Tomcat started. Open http://localhost:9000/");
        System.out.println("MiniSpring API: http://localhost:9000/api/user/list");
    }

    /** 解析 webapp 目录 */
    private static File resolveWebappDir() {
        File fromUserDir = Paths.get(System.getProperty("user.dir"), "webapp").toFile();
        if (fromUserDir.isDirectory()) return fromUserDir;
        try {
            File classDir = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File projectRoot = classDir.getParentFile();
            if (projectRoot != null) {
                File fromProject = new File(projectRoot, "webapp");
                if (fromProject.isDirectory()) return fromProject;
            }
        } catch (Exception ignored) { }
        return fromUserDir;
    }

    private static void addDefaultWrapper(Context context) {
        Wrapper wrapper = new Wrapper();
        wrapper.setName("default");
        wrapper.setUrlPattern("/");
        wrapper.setServlet(new DefaultServlet());
        context.addWrapper(wrapper);
    }
}
