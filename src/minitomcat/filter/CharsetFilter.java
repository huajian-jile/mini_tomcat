package minitomcat.filter;

import minitomcat.http.Request;
import minitomcat.http.Response;

/**
 * 示例 Filter：在 Servlet 之后为响应补充 charset（若 Content-Type 未含 charset）。
 */
public class CharsetFilter implements Filter {
    @Override
    public void doFilter(Request request, Response response, FilterChain chain) throws Exception {
        chain.doFilter(request, response);
        String ct = response.getContentType();
        if (ct != null && !ct.toLowerCase().contains("charset")) {
            response.setContentType(ct + "; charset=UTF-8");
        }
    }
}
