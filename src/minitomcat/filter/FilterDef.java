package minitomcat.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter 定义：名称、实例、URL 模式、init 参数。
 */
public class FilterDef {
    private String name;
    private Filter filter;
    private final List<String> urlPatterns = new ArrayList<>();
    private final Map<String, String> initParams = new HashMap<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Filter getFilter() { return filter; }
    public void setFilter(Filter filter) { this.filter = filter; }
    public List<String> getUrlPatterns() { return urlPatterns; }
    public void addUrlPattern(String pattern) { urlPatterns.add(pattern); }
    public Map<String, String> getInitParams() { return initParams; }
    public void addInitParam(String name, String value) { initParams.put(name, value); }

    public boolean matches(String path) {
        for (String p : urlPatterns) {
            if (matchPattern(p, path)) return true;
        }
        return false;
    }
    private static boolean matchPattern(String pattern, String path) {
        if ("/*".equals(pattern)) return true;
        if (pattern.equals(path)) return true;
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return path.startsWith(prefix) && (path.length() == prefix.length() || path.charAt(prefix.length()) == '/');
        }
        return false;
    }
}
