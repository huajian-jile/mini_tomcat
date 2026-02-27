package minitomcat.config;

import minitomcat.container.Context;
import minitomcat.container.Wrapper;
import minitomcat.filter.Filter;
import minitomcat.filter.FilterDef;
import minitomcat.servlet.Servlet;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * web.xml 解析：servlet、servlet-mapping、filter、filter-mapping、
 * welcome-file-list、context-param、init-param（更接近 Tomcat 的部署描述符）。
 */
public class WebXmlParser {
    private final Context context;
    private final Map<String, String> servletClassByName = new HashMap<>();
    private final Map<String, Map<String, String>> servletInitParamsByName = new HashMap<>();
    private final Map<String, String> servletNameByUrlPattern = new HashMap<>();
    private final Map<String, FilterDef> filterDefByName = new HashMap<>();
    private final List<FilterMapping> filterMappings = new ArrayList<>();

    public WebXmlParser(Context context) {
        this.context = context;
    }

    public void parse(InputStream in) throws Exception {
        if (in == null) return;
        var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        doc.getDocumentElement().normalize();

        // context-param
        var ctxParams = doc.getElementsByTagName("context-param");
        for (int i = 0; i < ctxParams.getLength(); i++) {
            var node = ctxParams.item(i);
            String name = getText(node, "param-name");
            String value = getText(node, "param-value");
            if (name != null && value != null) context.setContextParam(name.trim(), value.trim());
        }

        // filter
        var filterNodes = doc.getElementsByTagName("filter");
        for (int i = 0; i < filterNodes.getLength(); i++) {
            var node = filterNodes.item(i);
            String name = getText(node, "filter-name");
            String clazz = getText(node, "filter-class");
            if (name == null || clazz == null) continue;
            Filter filter = loadFilter(clazz.trim());
            if (filter == null) continue;
            FilterDef fd = new FilterDef();
            fd.setName(name.trim());
            fd.setFilter(filter);
            addInitParams(node, fd.getInitParams());
            filterDefByName.put(name.trim(), fd);
        }

        // filter-mapping
        var fmNodes = doc.getElementsByTagName("filter-mapping");
        for (int i = 0; i < fmNodes.getLength(); i++) {
            var node = fmNodes.item(i);
            String name = getText(node, "filter-name");
            String pattern = getText(node, "url-pattern");
            if (name != null && pattern != null)
                filterMappings.add(new FilterMapping(name.trim(), pattern.trim()));
        }

        // servlet
        var servletNodes = doc.getElementsByTagName("servlet");
        for (int i = 0; i < servletNodes.getLength(); i++) {
            var node = servletNodes.item(i);
            String name = getText(node, "servlet-name");
            String clazz = getText(node, "servlet-class");
            if (name != null && clazz != null) {
                servletClassByName.put(name.trim(), clazz.trim());
                Map<String, String> initParams = new HashMap<>();
                addInitParams(node, initParams);
                servletInitParamsByName.put(name.trim(), initParams);
            }
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

        // 按 filter-mapping 为 filter 设置 url-pattern，并加入 Context（保持 filter-mapping 顺序）
        var order = new ArrayList<String>();
        for (FilterMapping fm : filterMappings) {
            FilterDef fd = filterDefByName.get(fm.filterName);
            if (fd != null) {
                fd.addUrlPattern(fm.urlPattern);
                if (!order.contains(fm.filterName)) order.add(fm.filterName);
            }
        }
        for (String name : order) {
            FilterDef fd = filterDefByName.get(name);
            if (fd != null) context.addFilterDef(fd);
        }

        // 创建 Wrapper 并关联 servlet
        for (Map.Entry<String, String> e : servletNameByUrlPattern.entrySet()) {
            String urlPattern = e.getKey();
            String servletName = e.getValue();
            String servletClass = servletClassByName.get(servletName);
            if (servletClass == null) continue;
            Wrapper w = new Wrapper();
            w.setName(servletName);
            w.setUrlPattern(urlPattern);
            w.setInitParams(servletInitParamsByName.getOrDefault(servletName, Map.of()));
            Servlet servlet = loadServlet(servletClass);
            if (servlet != null) w.setServlet(servlet);
            context.addWrapper(w);
        }
    }

    private void addInitParams(org.w3c.dom.Node parent, Map<String, String> params) {
        var inits = parent.getChildNodes();
        for (int i = 0; i < inits.getLength(); i++) {
            var n = inits.item(i);
            if (n.getNodeName().equals("init-param")) {
                String name = getText(n, "param-name");
                String value = getText(n, "param-value");
                if (name != null && value != null) params.put(name.trim(), value.trim());
            }
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

    private Servlet loadServlet(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (Servlet) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Failed to load servlet " + className + ": " + e.getMessage());
            return null;
        }
    }

    private Filter loadFilter(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (Filter) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Failed to load filter " + className + ": " + e.getMessage());
            return null;
        }
    }

    private static class FilterMapping { final String filterName; final String urlPattern; FilterMapping(String n, String p) { filterName = n; urlPattern = p; } }
}
