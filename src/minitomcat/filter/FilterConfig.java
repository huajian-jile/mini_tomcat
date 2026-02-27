package minitomcat.filter;

import java.util.Collections;
import java.util.Map;

/**
 * Filter 配置，持有 init-param 等（与 Tomcat 的 FilterConfig 对应）。
 */
public class FilterConfig {
    private final String filterName;
    private final Map<String, String> initParameters;

    public FilterConfig(String filterName, Map<String, String> initParameters) {
        this.filterName = filterName;
        this.initParameters = initParameters != null ? initParameters : Map.of();
    }

    public String getFilterName() { return filterName; }
    public String getInitParameter(String name) { return initParameters.get(name); }
    public Map<String, String> getInitParameterMap() { return Collections.unmodifiableMap(initParameters); }
}
