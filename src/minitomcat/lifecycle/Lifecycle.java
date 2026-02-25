package minitomcat.lifecycle;

/**
 * 生命周期接口，与 Tomcat 一致：init -> start -> stop -> destroy。
 */
public interface Lifecycle {
    void init() throws Exception;
    void start() throws Exception;
    void stop() throws Exception;
    void destroy() throws Exception;
}
