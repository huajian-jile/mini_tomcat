import minitomcat.config.AppConfig;
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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 启动 Mini Tomcat：通过 apps.json 配置应用
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Server server = new Server();

        // 解析 apps.json 配置
        File configFile = resolveConfigFile();
        File tomcatHome = configFile.getParentFile();

        // 解析 server.xml 获取端口
        File confDir = resolveConfDir();
        File serverXml = new File(confDir, "server.xml");
        ServerXmlParser.ServerConfig serverConfig = new ServerXmlParser().parse(serverXml);
        int port = 8099;
        if (!serverConfig.services.isEmpty()) {
            port = serverConfig.services.get(0).port;
        }

        Service service = new Service();
        service.setName("Catalina");

        Connector connector = new Connector();
        connector.setPort(port);
        service.addConnector(connector);

        Host host = new Host();
        host.setName("localhost");

        // 扫描 webapps 目录，自动解压 WAR 包并更新 apps.json
        File webappsDir = new File(tomcatHome, "webapps");
        if (webappsDir.isDirectory()) {
            scanAndDeployWars(webappsDir, configFile);
        }

        // 加载 apps.json 配置的应用
        AppConfig.AppInfo[] apps = null;
        try {
            apps = AppConfig.parse(configFile).toArray(new AppConfig.AppInfo[0]);
        } catch (Exception e) {
            System.err.println("Failed to load apps.json: " + e.getMessage());
        }

        if (apps != null) {
            for (AppConfig.AppInfo app : apps) {
                File appDir = resolveDocBase(app.docBase, tomcatHome);
                if (appDir != null && appDir.isDirectory()) {
                    Context context = loadContext(app.path, appDir, confDir);
                    if (context != null) {
                        host.addContext(context);
                    }
                } else {
                    System.out.println("[Mini Tomcat] Warning: docBase not found: " + app.docBase);
                }
            }
        }

        service.getEngine().addHost(host);
        server.addService(service);

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
        }

        // 如果没有注册任何 servlet，使用默认静态资源 servlet
        if (context.getWrappers().isEmpty()) {
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

    /** 解析 apps.json 配置文件，如果不存在则自动创建 */
    private static File resolveConfigFile() throws Exception {
        // 优先使用当前目录下的 apps.json
        File userDir = new File(System.getProperty("user.dir"));
        File configFile = new File(userDir, "apps.json");

        if (configFile.isFile()) {
            System.out.println("[Mini Tomcat] Using config: " + configFile.getAbsolutePath());
            return configFile;
        }

        // 如果不存在，在当前运行目录创建默认配置
        System.out.println("[Mini Tomcat] No apps.json found, creating default config in: " + userDir.getAbsolutePath());
        createDefaultConfig(userDir);
        return configFile;
    }

    /** 创建默认的 apps.json 和目录结构 */
    private static void createDefaultConfig(File userDir) throws Exception {
        // 创建 webapps 目录
        File webappsDir = new File(userDir, "webapps");
        if (!webappsDir.exists()) {
            webappsDir.mkdirs();
            // 创建示例 ROOT 应用
            File rootDir = new File(webappsDir, "ROOT");
            rootDir.mkdirs();
            File webInfDir = new File(rootDir, "WEB-INF");
            webInfDir.mkdirs();
            // 创建默认的 index.html
            File indexFile = new File(rootDir, "index.html");
            try (FileWriter fw = new FileWriter(indexFile)) {
                fw.write("<!DOCTYPE html>\n");
                fw.write("<html>\n<head><title>Mini Tomcat</title></head>\n");
                fw.write("<body><h1>Welcome to Mini Tomcat</h1><p>Server is running!</p></body>\n</html>");
            }
        }

        // 创建 apps.json
        File configFile = new File(userDir, "apps.json");
        String defaultConfig = "[\n" +
            "    {\n" +
            "        \"path\": \"/\",\n" +
            "        \"docBase\": \"webapps/ROOT\"\n" +
            "    }\n" +
            "]";
        try (FileWriter fw = new FileWriter(configFile)) {
            fw.write(defaultConfig);
        }
        System.out.println("[Mini Tomcat] Created apps.json with default config");
        System.out.println("[Mini Tomcat] Place your WAR packages in: " + webappsDir.getAbsolutePath());
    }

    /** 解析 conf 目录 */
    private static File resolveConfDir() throws Exception {
        File userDir = new File(System.getProperty("user.dir"));
        File confDir = new File(userDir, "conf");

        if (confDir.isDirectory()) return confDir;

        // 如果不存在，在用户目录创建默认 conf
        System.out.println("[Mini Tomcat] Creating default conf directory");
        confDir.mkdirs();
        File serverXml = new File(confDir, "server.xml");
        String defaultServerXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<Server>\n" +
            "    <Service name=\"Catalina\">\n" +
            "        <Connector port=\"9000\"/>\n" +
            "        <Engine name=\"Catalina\">\n" +
            "            <Host name=\"localhost\"/>\n" +
            "        </Engine>\n" +
            "    </Service>\n" +
            "</Server>";
        try (FileWriter fw = new FileWriter(serverXml)) {
            fw.write(defaultServerXml);
        }
        System.out.println("[Mini Tomcat] Created default server.xml");
        return confDir;
    }

    /** 解析 docBase（支持绝对路径和相对路径） */
    private static File resolveDocBase(String docBase, File tomcatHome) {
        // 统一路径分隔符
        docBase = docBase.replace("\\", "/");

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
        File userDir = new File(System.getProperty("user.dir"), docBase);
        if (userDir.isDirectory()) {
            return userDir;
        }

        return null;
    }

    /** 扫描 webapps 目录，解压 WAR 包并更新 apps.json */
    private static void scanAndDeployWars(File webappsDir, File configFile) throws Exception {
        File[] files = webappsDir.listFiles();
        if (files == null) return;

        // 收集已部署的应用
        java.util.List<AppConfig.AppInfo> deployedApps = new java.util.ArrayList<>();

        for (File file : files) {
            String appName = file.getName();
            String warName = appName;
            if (appName.endsWith(".war")) {
                warName = appName.substring(0, appName.length() - 4);
            }

            // 目录名或 WAR 名（去掉 .war 后缀）作为 path
            String contextPath = "/" + warName;
            if ("ROOT".equals(warName)) {
                contextPath = "/";
            }

            File appDir = new File(webappsDir, warName);

            // 如果是 WAR 文件，解压
            if (file.getName().endsWith(".war")) {
                // 检查是否需要解压（目录不存在或 WAR 文件更新）
                boolean needExtract = !appDir.exists() ||
                    file.lastModified() > appDir.lastModified();

                if (needExtract) {
                    if (appDir.exists()) {
                        deleteDir(appDir);
                    }
                    appDir.mkdirs();
                    extractWar(file, appDir);
                    System.out.println("[Mini Tomcat] Extracted WAR: " + file.getName());
                }
            }

            // 添加到已部署列表（如果是目录或已解压的 WAR）
            if (appDir.isDirectory()) {
                AppConfig.AppInfo info = new AppConfig.AppInfo();
                info.path = contextPath;
                info.docBase = "webapps/" + warName;
                deployedApps.add(info);
                System.out.println("[Mini Tomcat] Found app: " + contextPath + " -> " + warName);
            }
        }

        // 更新 apps.json
        if (!deployedApps.isEmpty()) {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < deployedApps.size(); i++) {
                AppConfig.AppInfo app = deployedApps.get(i);
                sb.append("    {\n");
                sb.append("        \"path\": \"").append(app.path).append("\",\n");
                sb.append("        \"docBase\": \"").append(app.docBase).append("\"\n");
                sb.append("    }");
                if (i < deployedApps.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            try (FileWriter fw = new FileWriter(configFile)) {
                fw.write(sb.toString());
            }
            System.out.println("[Mini Tomcat] Updated apps.json with " + deployedApps.size() + " applications");
        }
    }

    /** 解压 WAR 文件 */
    private static void extractWar(File warFile, File destDir) throws Exception {
        byte[] buffer = new byte[4096];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(warFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                // 安全检查：防止 zip slip 漏洞
                if (!newFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                    continue;
                }

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /** 删除目录 */
    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDir(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}
