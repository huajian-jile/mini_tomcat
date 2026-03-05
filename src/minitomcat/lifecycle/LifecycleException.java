package minitomcat.lifecycle;

/**
 * 生命周期异常，与 Tomcat 一致。
 */
public class LifecycleException extends Exception {
    private static final long serialVersionUID = 1L;

    public LifecycleException() {
        super();
    }

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleException(Throwable cause) {
        super(cause);
    }
}
