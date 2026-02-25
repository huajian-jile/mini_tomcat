package minitomcat.container;

import minitomcat.http.Request;
import minitomcat.http.Response;

/**
 * 容器体系顶层接口：负责处理请求并向下传递。
 * Engine -> Host -> Context -> Wrapper 形成层级。
 * 请求经 Pipeline/Valve 链最终到达 BasicValve，由 BasicValve 交给子容器或 Servlet。
 */
public interface Container {
    /** 处理请求：委托给 Pipeline */
    void invoke(Request request, Response response) throws Exception;

    /** 容器名称，用于匹配请求路径 */
    String getName();

    /** 设置父容器 */
    void setParent(Container parent);

    Container getParent();

    /** 获取管道，用于装配 Valve（Tomcat 中每层容器都有 Pipeline） */
    Pipeline getPipeline();
}
