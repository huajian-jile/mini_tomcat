package minitomcat.filter;

import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.servlet.Servlet;

import java.util.List;

/**
 * 过滤器链实现：按顺序调用 Filter，最后调用 Servlet。
 */
public class ApplicationFilterChain implements FilterChain {
    private final List<Filter> filters;
    private final Servlet servlet;
    private int index = 0;

    public ApplicationFilterChain(List<Filter> filters, Servlet servlet) {
        this.filters = filters != null ? filters : List.of();
        this.servlet = servlet;
    }

    @Override
    public void doFilter(Request request, Response response) throws Exception {
        if (index < filters.size()) {
            Filter f = filters.get(index++);
            f.doFilter(request, response, this);
        } else if (servlet != null) {
            servlet.service(request, response);
        }
    }
}
