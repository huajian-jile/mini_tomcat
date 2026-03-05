# Mini Tomcat 项目说明文档

## 项目简介

Mini Tomcat 是一个精简版的 Tomcat 实现，用于学习 Tomcat 核心原理。该项目参考了 Apache Tomcat 源码架构，实现了 Servlet 容器的基本功能。

## 项目结构

```
mini_tomcat/
├── src/
│   └── minitomcat/
│       ├── adapter/          # 适配器
│       ├── async/            # 异步 Servlet 支持
│       ├── config/           # 配置解析
│       │   ├── AppConfig.java
│       │   ├── ServerXmlParser.java
│       │   └── WebXmlParser.java
│       ├── connector/        # 连接器（对应 Coyote）
│       ├── container/        # 容器层次
│       │   ├── Engine.java      # 引擎
│       │   ├── Host.java        # 虚拟主机
│       │   ├── Context.java     # Web 应用
│       │   ├── Wrapper.java     # Servlet 包装
│       │   ├── Container.java  # 容器接口
│       │   ├── Pipeline.java    # 管道
│       │   └── Valve.java       # 阀门
│       ├── core/             # 核心基础类
│       │   └── LifecycleBase.java
│       ├── filter/           # 过滤器
│       ├── http/             # HTTP 处理
│       │   ├── Request.java
│       │   ├── Response.java
│       │   ├── Session.java
│       │   ├── SessionImpl.java
│       │   └── SessionManager.java
│       ├── jsp/              # JSP 支持（预留）
│       ├── lifecycle/        # 生命周期管理
│       │   ├── Lifecycle.java
│       │   ├── LifecycleState.java
│       │   ├── LifecycleEvent.java
│       │   ├── LifecycleListener.java
│       │   ├── LifecycleSupport.java
│       │   └── LifecycleException.java
│       ├── logging/          # 日志系统
│       │   └── JULILog.java
│       ├── naming/           # JNDI 资源管理
│       │   └── NamingResources.java
│       ├── servlet/          # Servlet 接口
│       │   ├── Servlet.java
│       │   └── DefaultServlet.java
│       ├── server/           # 服务器核心
│       │   ├── Server.java
│       │   └── Service.java
│       └── valves/           # 特殊阀门
│           └── AccessLogValve.java
├── docs/                    # 文档
└── README.md
```

## 核心架构

### 1. 容器层级

```
Server (整个 Tomcat 实例)
    │
    └─► Service (一个服务)
            │
            ├─► Connector × N (监听端口，接受请求)
            │
            └─► Engine (核心引擎)
                    │
                    └─► Host × N (虚拟主机)
                            │
                            └─► Context × N (Web 应用)
                                    │
                                    └─► Wrapper × N (Servlet)
```

### 2. 组件说明

| 组件 | 说明 |
|------|------|
| **Server** | 整个 Tomcat 实例，管理所有 Service |
| **Service** | 将 Connector 与 Engine 关联 |
| **Connector** | 监听端口，接收 HTTP 请求 |
| **Engine** | 核心处理引擎，选择虚拟主机 |
| **Host** | 虚拟主机，对应域名 |
| **Context** | Web 应用，对应一个项目 |
| **Wrapper** | Servlet 包装器 |

### 3. 请求处理流程

```
1. Connector.accept() → 接收 Socket
         │
2. 解析 HTTP 请求 → Request 对象
         │
3. container.invoke(request, response)
         │
4. Engine → Pipeline → EngineValve
         │    选择 Host
         ▼
5. Host → Pipeline → HostValve
         │    选择 Context
         ▼
6. Context → Pipeline → ContextValve
         │    选择 Servlet
         ▼
7. Wrapper → Servlet.service()
         │
8. Response 写出到客户端
```

## 新增功能

### 1. 生命周期事件机制

参考 Tomcat 的 Lifecycle 接口，实现了完整的事件驱动机制：

- **Lifecycle** - 生命周期接口
- **LifecycleState** - 状态枚举（NEW, INITIALIZING, STARTING, STARTED, STOPPING 等）
- **LifecycleEvent** - 事件对象
- **LifecycleListener** - 监听器接口
- **LifecycleSupport** - 事件管理类
- **LifecycleBase** - 基础实现类

**使用示例**：

```java
public class MyComponent extends LifecycleBase {
    @Override
    protected void initInternal() throws LifecycleException {
        // 初始化逻辑
    }

    @Override
    protected void startInternal() throws LifecycleException {
        // 启动逻辑
    }
}

// 添加监听器
component.addLifecycleListener(event -> {
    System.out.println("Event: " + event.getType());
});
```

### 2. JNDI 资源管理

支持环境条目和资源（DataSource 等）的配置：

```java
NamingResources resources = new NamingResources();

// 添加环境条目
resources.addEnvironment("app.name", "MyApp");

// 添加资源
Resource resource = new Resource();
resource.setName("jdbc/DB");
resource.setType("javax.sql.DataSource");
resources.addResource(resource);
```

### 3. Session 管理增强

完整的 Session 实现，支持：

- Session 创建/销毁
- 属性管理
- 超时自动失效
- 并发安全

```java
SessionManager manager = new SessionManager();
Session session = manager.createSession();
session.setAttribute("user", userObject);
session.invalidate();
```

### 4. 异步 Servlet 支持

实现 AsyncContext 接口：

```java
AsyncContext asyncContext = request.startAsync();
asyncContext.setTimeout(30000);
asyncContext.start(() -> {
    // 异步处理
    asyncContext.complete();
});
```

### 5. 日志系统

简化的 JULI 日志实现：

```java
JULILog log = JULILog.getLog(MyClass.class);
log.info("Application started");
log.error("Error occurred", exception);
```

### 6. 访问日志

AccessLogValve 记录请求访问日志：

```java
AccessLogValve accessLog = new AccessLogValve();
accessLog.setDirectory("logs");
accessLog.setPrefix("access_log");
engine.getPipeline().addValve(accessLog);
```

## 部署 Web 应用

### 1. 目录结构

```
webapps/
└── myapp/
    ├── WEB-INF/
    │   ├── web.xml        # 部署描述符（必须）
    │   ├── classes/       # 编译后的类
    │   └── lib/           # 依赖 JAR
    └── index.html         # 静态资源
```

### 2. web.xml 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app>
    <!-- Servlet 配置 -->
    <servlet>
        <servlet-name>HelloServlet</servlet-name>
        <servlet-class>com.example.HelloServlet</servlet-class>
    </servlet>

    <!-- URL 映射 -->
    <servlet-mapping>
        <servlet-name>HelloServlet</servlet-name>
        <url-pattern>/hello</url-pattern>
    </servlet-mapping>

    <!-- 欢迎页面 -->
    <welcome-file-list>
        <welcome-file>index.html</welcome-file>
    </welcome-file-list>

    <!-- Session 配置 -->
    <session-config>
        <session-timeout>30</session-timeout>
    </session-config>
</web-app>
```

### 3. apps.json 配置

```json
[
    {
        "path": "/myapp",
        "docBase": "webapps/myapp"
    }
]
```

## 与 Tomcat 源码对比

| 功能 | Mini Tomcat | Tomcat 源码 |
|------|-------------|------------|
| 生命周期 | ✅ LifecycleBase | ✅ LifecycleBase |
| 事件机制 | ✅ LifecycleEvent | ✅ LifecycleEvent |
| JNDI | ✅ NamingResources | ✅ NamingResources |
| Session | ✅ SessionManager | ✅ Manager |
| 异步 Servlet | ✅ AsyncContext | ✅ AsyncContext |
| 日志 | ✅ JULILog | ✅ JULI |
| 访问日志 | ✅ AccessLogValve | ✅ AccessLogValve |
| WebSocket | ❌ 预留 | ✅ WebSocket |
| JSP | ❌ 预留 | ✅ Jasper |

## 开发计划

- [x] 基础容器架构
- [x] HTTP 请求处理
- [x] Servlet 映射
- [x] Filter 过滤器
- [x] 生命周期事件机制
- [x] JNDI 资源管理
- [x] Session 管理
- [x] 异步 Servlet
- [x] 日志系统
- [ ] WebSocket 支持
- [ ] JSP 编译支持
- [ ] 注解配置支持
- [ ] WebSocket 支持

## 参考资料

- [Apache Tomcat 源码](https://github.com/apache/tomcat)
- [Servlet 规范](https://jakarta.ee/specifications/servlet/)
- [Tomcat 架构文档](https://tomcat.apache.org/architecture/)

## 许可证

本项目仅用于学习交流。
