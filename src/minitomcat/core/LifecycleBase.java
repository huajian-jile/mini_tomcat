package minitomcat.core;

import minitomcat.lifecycle.*;

/**
 * 生命周期基础实现类，与 Tomcat 的 LifecycleBase 一致。
 * 提供生命周期状态管理和事件触发的基础实现。
 */
public abstract class LifecycleBase implements Lifecycle {
    private static final String NAME = "LifecycleBase";

    private LifecycleState state = LifecycleState.NEW;
    private LifecycleSupport lifecycleSupport = new LifecycleSupport(this);

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycleSupport.addLifecycleListener(listener);
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycleSupport.removeLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycleSupport.findLifecycleListeners();
    }

    @Override
    public synchronized LifecycleState getState() {
        return state;
    }

    @Override
    public synchronized void setState(LifecycleState state) throws LifecycleException {
        setState(state, null);
    }

    @Override
    public synchronized void setState(LifecycleState state, Object data) throws LifecycleException {
        if (state == null) {
            return;
        }

        if (!(this instanceof Lifecycle)) {
            throw new LifecycleException("Component does not implement Lifecycle");
        }

        // 状态转换验证
        if (this.state == state) {
            return;
        }

        // 触发事件
        switch (state) {
            case STARTING_PREP:
                fireLifecycleEvent(LifecycleEvent.BEFORE_START_EVENT, data);
                break;
            case STARTED:
                fireLifecycleEvent(LifecycleEvent.AFTER_START_EVENT, data);
                break;
            case STOPPING_PREP:
                fireLifecycleEvent(LifecycleEvent.BEFORE_STOP_EVENT, data);
                break;
            case STOPPED:
                fireLifecycleEvent(LifecycleEvent.AFTER_STOP_EVENT, data);
                break;
            case INITIALIZED:
                fireLifecycleEvent(LifecycleEvent.AFTER_INIT_EVENT, data);
                break;
            case DESTROYED:
                fireLifecycleEvent(LifecycleEvent.AFTER_DESTROY_EVENT, data);
                break;
            default:
                break;
        }

        this.state = state;
    }

    protected void fireLifecycleEvent(String type) {
        lifecycleSupport.fireLifecycleEvent(type);
    }

    protected void fireLifecycleEvent(String type, Object data) {
        lifecycleSupport.fireLifecycleEvent(type, data);
    }

    // 模板方法模式，子类实现具体逻辑
    protected abstract void initInternal() throws LifecycleException;
    protected abstract void startInternal() throws LifecycleException;
    protected abstract void stopInternal() throws LifecycleException;
    protected abstract void destroyInternal() throws LifecycleException;

    @Override
    public final void init() throws LifecycleException {
        if (state == LifecycleState.INITIALIZED || state ==LifecycleState.STARTED) {
            return;
        }

        try {
            setState(LifecycleState.INITIALIZING);
            initInternal();
            setState(LifecycleState.INITIALIZED);
        } catch (Throwable t) {
            handleException(t);
        }
    }

    @Override
    public final void start() throws LifecycleException {
        if (state == LifecycleState.STARTED || state == LifecycleState.STARTING) {
            return;
        }

        try {
            setState(LifecycleState.STARTING_PREP);
            startInternal();
            setState(LifecycleState.STARTED);
        } catch (Throwable t) {
            handleException(t);
        }
    }

    @Override
    public final void stop() throws LifecycleException {
        if (state == LifecycleState.STOPPED || state == LifecycleState.STOPPING) {
            return;
        }

        try {
            setState(LifecycleState.STOPPING_PREP);
            stopInternal();
            setState(LifecycleState.STOPPED);
        } catch (Throwable t) {
            handleException(t);
        }
    }

    @Override
    public final void destroy() throws LifecycleException {
        if (state == LifecycleState.DESTROYED) {
            return;
        }

        try {
            setState(LifecycleState.DESTROYING);
            destroyInternal();
            setState(LifecycleState.DESTROYED);
        } catch (Throwable t) {
            handleException(t);
        }
    }

    private void handleException(Throwable t) throws LifecycleException {
        setState(LifecycleState.FAILED);
        if (t instanceof LifecycleException) {
            throw (LifecycleException) t;
        }
        throw new LifecycleException(t);
    }
}
