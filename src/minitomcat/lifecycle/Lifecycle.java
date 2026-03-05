package minitomcat.lifecycle;

/**
 * 生命周期接口，与 Tomcat 一致：init -> start -> stop -> destroy。
 * 支持事件监听机制。
 */
public interface Lifecycle {
    /**
     * 添加生命周期监听器
     */
    void addLifecycleListener(LifecycleListener listener);

    /**
     * 移除生命周期监听器
     */
    void removeLifecycleListener(LifecycleListener listener);

    /**
     * 获取所有生命周期监听器
     */
    LifecycleListener[] findLifecycleListeners();

    /**
     * 初始化
     */
    void init() throws Exception;

    /**
     * 启动
     */
    void start() throws Exception;

    /**
     * 停止
     */
    void stop() throws Exception;

    /**
     * 销毁
     */
    void destroy() throws Exception;

    /**
     * 获取当前状态
     */
    LifecycleState getState();

    /**
     * 设置状态
     */
    void setState(LifecycleState state) throws LifecycleException;

    /**
     * 设置状态（带事件）
     */
    void setState(LifecycleState state, Object data) throws LifecycleException;
}
