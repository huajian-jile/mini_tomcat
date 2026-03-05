package minitomcat.lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * 生命周期事件管理器，与 Tomcat 一致。
 * 管理监听器并触发事件。
 */
public class LifecycleSupport {
    private final Lifecycle lifecycle;
    private final List<LifecycleListener> listeners = new ArrayList<>();

    public LifecycleSupport(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void addLifecycleListener(LifecycleListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public LifecycleListener[] findLifecycleListeners() {
        return listeners.toArray(new LifecycleListener[0]);
    }

    public void fireLifecycleEvent(String type) {
        fireLifecycleEvent(type, null);
    }

    public void fireLifecycleEvent(String type, Object data) {
        LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
        for (LifecycleListener listener : listeners) {
            try {
                listener.lifecycleEvent(event);
            } catch (Exception e) {
                System.err.println("LifecycleListener exception: " + e.getMessage());
            }
        }
    }
}
