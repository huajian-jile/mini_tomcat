import minitomcat.config.ServerXmlParser;
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
 * 启动 Mini Tomcat：通过 conf/server.xml 配置应用
 * 类似真实 Tomcat 的部署方式
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Server server = new Server();

        // 解析 server.xml 配置
        File confDir = resolveConfDir();
        File serverXml = new File(confDir, "server.xml");
        File tomcatHome = confDir.getParentFile();
        ServerXmlParser.ServerConfig serverConfig = new ServerXmlParser().parse(serverXml);

        for (ServerXmlParser.ServiceConfig serviceConfig : serverConfig.services) {
            Service service = new Service();
            service.setName(serviceConfig.name);

            // 添加 Connector
            Connector connector = new Connector();
            connector.setPort(serviceConfig.port);
            service.addConnector(connector);

            // 添加 Host
            for (ServerXmlParser.HostConfig hostConfig : serviceConfig.hosts) {
                Host host = new Host();
                host.setName(hostConfig.name);

                // 加载配置的 Context
                for (ServerXmlParser.ContextConfig ctxConfig : hostConfig.contexts) {
                    File appDir = resolveDocBase(ctxConfig.docBase, tomcatHome);
                    if (appDir != null && appDir.isDirectory()) {
                        Context context = loadContext(ctxConfig.path, appDir, confDir);
                        if (context != null) {
                            host.addContext(context);
                        }
                    } else {
                        System.out.println("[Mini Tomcat] Warning: docBase not found: " + ctxConfig.docBase);
                    }
                }

                service.getEngine().addHost(host);
            }

            server.addService(service);
        }

        server.init();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { server.stop(); } catch (Exception e) { e.printStackTrace(); }
        }));
        System.out.println("Mini Tomcat started.");
    }

    /** 加载 Web 应用 */
    private static Context loadContext(String contextPath, File appDir, File confDir) throws Exception {
        Context context = new Context();
        context.setName(contextPath);
        context.setPath(contextPath);
        context.setDocBase(appDir.getAbsolutePath());

        System.out.println("[Mini Tomcat] Loading application: " + contextPath + " from " + appDir.getAbsolutePath());

        // 加载 WEB-INF/web.xml
        File webXml = new File(appDir, "WEB-INF/web.xml");
        if (webXml.isFile()) {
            new WebXmlParser(context).parse(webXml);
        } else {
            addDefaultWrapper(context);
        }

        return context;
    }

    /** 添加默认的静态资源 servlet */
    private static void addDefaultWrapper(Context context) {
        Wrapper wrapper = new Wrapper();
        wrapper.setName("default");
        wrapper.setUrlPattern("/");
        wrapper.setServlet(new DefaultServlet());
        context.addWrapper(wrapper);
    }

    /** 解析 conf 目录 */
    private static File resolveConfDir() {
        File fromUserDir = Paths.get(System.getProperty("user.dir"), "conf").toFile();
        if (fromUserDir.isDirectory()) return fromUserDir;
        try {
            File classDir = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File projectRoot = classDir.getParentFile();
            if (projectRoot != null) {
                File fromProject = new File(projectRoot, "conf");
                if (fromProject.isDirectory()) return fromProject;
            }
        } catch (Exception ignored) { }
        return fromUserDir;
    }

    /** 解析 docBase（支持绝对路径和相对路径） */
    private static File resolveDocBase(String docBase, File tomcatHome) {
        // 绝对路径
        File absPath = new File(docBase);
        if (absPath.isAbsolute() && absPath.isDirectory()) {
            return absPath;
        }

        // 相对于 Tomcat 根目录
        File relative = new File(tomcatHome, docBase);
        if (relative.isDirectory()) {
            return relative;
        }

        // 相对于当前工作目录
        File userDir = Paths.get(System.getProperty("user.dir"), docBase).toFile();
        if (userDir.isDirectory()) {
            return userDir;
        }

        return null;
    }
}
