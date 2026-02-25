package minitomcat.container;

import minitomcat.http.Request;
import minitomcat.http.Response;

/**
 * 管道：持有 BasicValve，请求由 Connector 进入后经 Pipeline 传递到 BasicValve，由 BasicValve 决定交给子容器或 Servlet。
 */
public interface Pipeline {
    void invoke(Request request, Response response) throws Exception;
    void setBasic(Valve valve);
    Valve getBasic();
}
