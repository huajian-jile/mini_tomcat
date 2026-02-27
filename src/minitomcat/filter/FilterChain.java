package minitomcat.filter;

import minitomcat.http.Request;
import minitomcat.http.Response;

/**
 * 过滤器链：依次调用 Filter，最后调用目标 Servlet（与 Tomcat 的 ApplicationFilterChain 对应）。
 */
public interface FilterChain {
    void doFilter(Request request, Response response) throws Exception;
}
