package minitomcat.async;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步上下文实现，与 Tomcat 一致。
 * 支持异步请求处理。
 */
public class AsyncContextImpl implements AsyncContext {
    private final ServletRequest request;
    private final ServletResponse response;
    private final List<AsyncListener> listeners = new ArrayList<>();
    private volatile boolean asyncCompleted = false;
    private volatile boolean timeout = false;
    private ExecutorService executor;
    private long timeoutMs = 30000;
    private Runnable dispatchTarget;

    public AsyncContextImpl(ServletRequest request, ServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public ServletRequest getRequest() {
        return request;
    }

    @Override
    public ServletResponse getResponse() {
        return response;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return true;
    }

    @Override
    public void start(Runnable run) {
        if (executor == null) {
            executor = Executors.newCachedThreadPool();
        }
        executor.submit(() -> {
            try {
                run.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void dispatch() {
        // 简化实现：直接完成
        complete();
    }

    @Override
    public void dispatch(String path) {
        // 简化实现
        complete();
    }

    @Override
    public void complete() {
        if (asyncCompleted) return;
        asyncCompleted = true;
        fireOnComplete();
    }

    @Override
    public void setTimeout(long timeoutmillis) {
        this.timeoutMs = timeoutmillis;
    }

    @Override
    public long getTimeout() {
        return timeoutMs;
    }

    @Override
    public void addListener(AsyncListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void addListener(AsyncListener listener, ServletRequest request, ServletResponse response) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void fireOnStartAsync(AsyncEvent event) {
        for (AsyncListener listener : listeners) {
            try {
                listener.onStartAsync(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fireOnComplete(AsyncEvent event) {
        for (AsyncListener listener : listeners) {
            try {
                listener.onComplete(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fireOnComplete() {
        fireOnComplete(new AsyncEvent(this, request, response));
    }

    public void fireOnError(Throwable throwable) {
        AsyncEvent event = new AsyncEvent(this, request, response, throwable);
        for (AsyncListener listener : listeners) {
            try {
                listener.onError(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fireOnTimeout() {
        timeout = true;
        AsyncEvent event = new AsyncEvent(this, request, response);
        for (AsyncListener listener : listeners) {
            try {
                listener.onTimeout(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isAsyncComplete() {
        return asyncCompleted;
    }

    public boolean isTimeout() {
        return timeout;
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
