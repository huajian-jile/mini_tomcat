package minitomcat.lifecycle;

/**
 * 生命周期状态枚举，与 Tomcat 一致。
 */
public enum LifecycleState {
    NEW(false),
    INITIALIZING(false),
    INITIALIZED(false),
    STARTING_PREP(false),
    STARTING(false),
    STARTED(true),
    STOPPING_PREP(false),
    STOPPING(false),
    STOPPED(false),
    DESTROYING(false),
    DESTROYED(false),
    FAILED(false);

    private final boolean available;

    LifecycleState(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }

    public static LifecycleState fromString(String state) {
        if (state == null) return null;
        try {
            return valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
