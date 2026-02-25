package minitomcat.jsp;

/**
 * Jasper：JSP 引擎占位。
 * 真实 Tomcat 中负责将 JSP 编译为 Java 源码再编译为 class 供 JVM 执行。
 * 此处仅保留接口，不实现编译与执行。
 */
public final class Jasper {
    private Jasper() {}

    /** 占位：是否支持 JSP（本实现不支持） */
    public static boolean isJspResource(String path) {
        return path != null && path.endsWith(".jsp");
    }
}
