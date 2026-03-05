package minitomcat.lifecycle;

/**
 * 生命周期监听器接口，与 Tomcat 一致。
 * 监听组件的生命周期事件。
 */
public interface LifecycleListener {
    /**
     * 处理生命周期事件
     */
    void lifecycleEvent(LifecycleEvent event);
}
