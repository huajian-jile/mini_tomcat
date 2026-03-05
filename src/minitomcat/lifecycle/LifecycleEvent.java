package minitomcat.lifecycle;

/**
 * 生命周期事件，与 Tomcat 一致。
 * 在组件状态变化时触发，通知所有监听器。
 */
public class LifecycleEvent {
    private final Lifecycle lifecycle;
    private final String type;
    private final Object data;

    public LifecycleEvent(Lifecycle lifecycle, String type) {
        this(lifecycle, type, null);
    }

    public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
        this.lifecycle = lifecycle;
        this.type = type;
        this.data = data;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    // 事件类型常量
    public static final String BEFORE_INIT_EVENT = "before_init";
    public static final String AFTER_INIT_EVENT = "after_init";
    public static final String BEFORE_START_EVENT = "before_start";
    public static final String AFTER_START_EVENT = "after_start";
    public static final String BEFORE_STOP_EVENT = "before_stop";
    public static final String AFTER_STOP_EVENT = "after_stop";
    public static final String BEFORE_DESTROY_EVENT = "before_destroy";
    public static final String AFTER_DESTROY_EVENT = "after_destroy";
    public static final String PERIODIC_EVENT = "periodic";
}
