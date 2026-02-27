package minitomcat.filter;

import minitomcat.http.Request;
import minitomcat.http.Response;

/**
 * 过滤器接口：在 Servlet 之前对请求/响应进行处理（与 Tomcat 的 javax.servlet.Filter 对应）。
 */
public interface Filter {
    void doFilter(Request request, Response response, FilterChain chain) throws Exception;
}
