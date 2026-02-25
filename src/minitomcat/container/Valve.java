package minitomcat.container;

import minitomcat.http.Request;
import minitomcat.http.Response;

/**
 * 阀门接口：请求经过 Pipeline 时依次经过多个 Valve，最后到达 BasicValve 交给下一级容器或 Servlet。
 */
public interface Valve {
    void invoke(Request request, Response response, Valve next) throws Exception;
}
