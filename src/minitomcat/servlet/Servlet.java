package minitomcat.servlet;

import minitomcat.http.Request;
import minitomcat.http.Response;

/**
 * Servlet 接口：处理单次请求的核心抽象。
 */
public interface Servlet {
    void service(Request request, Response response) throws Exception;
}
