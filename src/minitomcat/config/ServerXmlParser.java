package minitomcat.config;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 server.xml 配置文件
 */
public class ServerXmlParser {

    public static class ServerConfig {
        public List<ServiceConfig> services = new ArrayList<>();
    }

    public static class ServiceConfig {
        public String name = "Catalina";
        public int port = 9000;
        public List<HostConfig> hosts = new ArrayList<>();
    }

    public static class HostConfig {
        public String name = "localhost";
        public List<ContextConfig> contexts = new ArrayList<>();
    }

    public static class ContextConfig {
        public String path = "/";
        public String docBase = "";
    }

    public ServerConfig parse(File file) throws Exception {
        ServerConfig serverConfig = new ServerConfig();

        if (file == null || !file.isFile()) {
            return getDefaultConfig();
        }

        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        doc.getDocumentElement().normalize();

        // 解析 Service
        var serviceNodes = doc.getElementsByTagName("Service");
        for (int i = 0; i < serviceNodes.getLength(); i++) {
            var serviceNode = serviceNodes.item(i);
            ServiceConfig service = new ServiceConfig();
            service.name = getAttribute(serviceNode, "name", "Catalina");

            // 解析 Connector
            var connectorNodes = ((org.w3c.dom.Element) serviceNode).getElementsByTagName("Connector");
            for (int j = 0; j < connectorNodes.getLength(); j++) {
                var connNode = connectorNodes.item(j);
                String port = getAttribute(connNode, "port", "9000");
                service.port = Integer.parseInt(port);
            }

            // 解析 Engine -> Host
            var engineNodes = ((org.w3c.dom.Element) serviceNode).getElementsByTagName("Engine");
            for (int j = 0; j < engineNodes.getLength(); j++) {
                var engineNode = engineNodes.item(j);
                var hostNodes = ((org.w3c.dom.Element) engineNode).getElementsByTagName("Host");
                for (int k = 0; k < hostNodes.getLength(); k++) {
                    var hostNode = hostNodes.item(k);
                    HostConfig host = new HostConfig();
                    host.name = getAttribute(hostNode, "name", "localhost");

                    // 解析 Context
                    var contextNodes = ((org.w3c.dom.Element) hostNode).getElementsByTagName("Context");
                    for (int m = 0; m < contextNodes.getLength(); m++) {
                        var contextNode = contextNodes.item(m);
                        ContextConfig context = new ContextConfig();
                        context.path = getAttribute(contextNode, "path", "/");
                        context.docBase = getAttribute(contextNode, "docBase", "");
                        if (!context.docBase.isEmpty()) {
                            host.contexts.add(context);
                        }
                    }

                    service.hosts.add(host);
                }
            }

            serverConfig.services.add(service);
        }

        if (serverConfig.services.isEmpty()) {
            return getDefaultConfig();
        }

        return serverConfig;
    }

    private ServerConfig getDefaultConfig() {
        ServerConfig serverConfig = new ServerConfig();
        ServiceConfig service = new ServiceConfig();
        service.name = "Catalina";
        service.port = 9000;
        HostConfig host = new HostConfig();
        host.name = "localhost";
        service.hosts.add(host);
        serverConfig.services.add(service);
        return serverConfig;
    }

    private String getAttribute(org.w3c.dom.Node node, String attrName, String defaultValue) {
        var attr = ((org.w3c.dom.Element) node).getAttribute(attrName);
        return attr != null && !attr.isEmpty() ? attr : defaultValue;
    }
}
