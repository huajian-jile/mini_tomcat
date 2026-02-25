package minitomcat.config;

import minitomcat.container.Context;
import minitomcat.container.Wrapper;
import minitomcat.servlet.Servlet;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单 web.xml 解析：servlet、servlet-mapping、welcome-file-list（更接近 Tomcat 的部署描述符）。
 */
public class WebXmlParser {
    private final Context context;
    private final Map<String, String> servletClassByName = new HashMap<>();
    private final Map<String, String> servletNameByUrlPattern = new HashMap<>();

    public WebXmlParser(Context context) {
        this.context = context;
    }

    /** 从 classpath 或文件路径加载 web.xml 并解析，填充 Context 的 Wrapper 与 welcome 列表 */
    public void parse(InputStream in) throws Exception {
        if (in == null) return;
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        doc.getDocumentElement().normalize();

        // servlet
        var servletNodes = doc.getElementsByTagName("servlet");
        for (int i = 0; i < servletNodes.getLength(); i++) {
            var node = servletNodes.item(i);
            String name = getText(node, "servlet-name");
            String clazz = getText(node, "servlet-class");
            if (name != null && clazz != null) servletClassByName.put(name.trim(), clazz.trim());
        }

        // servlet-mapping
        var mappingNodes = doc.getElementsByTagName("servlet-mapping");
        for (int i = 0; i < mappingNodes.getLength(); i++) {
            var node = mappingNodes.item(i);
            String name = getText(node, "servlet-name");
            String pattern = getText(node, "url-pattern");
            if (name != null && pattern != null) servletNameByUrlPattern.put(pattern.trim(), name.trim());
        }

        // welcome-file-list
        var welcomeList = doc.getElementsByTagName("welcome-file-list");
        for (int i = 0; i < welcomeList.getLength(); i++) {
            var list = welcomeList.item(i);
            var files = list.getChildNodes();
            for (int j = 0; j < files.getLength(); j++) {
                var f = files.item(j);
                if (f.getNodeName().equals("welcome-file")) {
                    String text = f.getTextContent();
                    if (text != null && !text.isEmpty()) context.addWelcomeFile(text.trim());
                }
            }
        }

        // 为每个 url-pattern 创建 Wrapper 并关联 servlet 类
        for (Map.Entry<String, String> e : servletNameByUrlPattern.entrySet()) {
            String urlPattern = e.getKey();
            String servletName = e.getValue();
            String servletClass = servletClassByName.get(servletName);
            if (servletClass == null) continue;
            Wrapper w = new Wrapper();
            w.setName(servletName);
            w.setUrlPattern(urlPattern);
            Servlet servlet = loadServlet(servletClass);
            if (servlet != null) w.setServlet(servlet);
            context.addWrapper(w);
        }
    }

    public void parse(File file) throws Exception {
        if (file != null && file.isFile()) {
            try (InputStream in = new java.io.FileInputStream(file)) {
                parse(in);
            }
        }
    }

    private String getText(org.w3c.dom.Node parent, String tagName) {
        var list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            var n = list.item(i);
            if (n.getNodeName().equals(tagName)) {
                String t = n.getTextContent();
                return t == null ? null : t.trim();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Servlet loadServlet(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (Servlet) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Failed to load servlet " + className + ": " + e.getMessage());
            return null;
        }
    }
}
