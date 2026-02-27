# Mini Tomcat 说明文档

本文档说明**完整 Tomcat 的功能与架构**、**本项目的实现**，以及**各包与 Java 文件的作用**及对比。

---

## 一、完整 Tomcat 的功能与架构

### 1.1 核心组件与职责

| 组件 | 在 Tomcat 中的职责 |
|------|-------------------|
| **Server** | 代表整个 Tomcat 实例，最顶层容器；管理多个 Service，负责整体生命周期（init/start/stop/destroy）。 |
| **Service** | 将一组 **Connector** 与一个 **Engine** 关联；一个 Server 可有多个 Service（如 Catalina、Coyote）。 |
| **Connector（连接器）** | 接收客户端请求、处理网络 I/O；将原始 HTTP 请求封装成 **Request**，把 **Response** 写回。Tomcat 中 HTTP 实现常称为 **Coyote**。 |
| **Engine** | 核心处理引擎；管理其下所有 **Host**，按请求的 Host 头选择虚拟主机。 |
| **Host** | 虚拟主机（对应一个域名）；可包含多个 **Context**（Web 应用）。 |
| **Context** | 一个 Web 应用（对应一个应用路径）；有 **docBase**（资源根目录）、**web.xml**、Session 等；是 Servlet 运行的直接环境。 |
| **Wrapper** | 最底层容器；管理**单个 Servlet** 的生命周期，调用其 `service()`。 |
| **Pipeline / Valve** | 每层容器有一条 **Pipeline**，上有多个 **Valve**；请求依次经过 Valve，最后到 **BasicValve**，由它交给下一层容器或 Servlet。 |
| **Jasper** | JSP 引擎；将 JSP 编译为 Java 源码，再编译为 class 由 JVM 执行。 |
| **Lifecycle** | 统一生命周期：`init()` → `start()` → `stop()` → `destroy()`；Server、Service、Connector、Engine、Host、Context、Wrapper 等均实现该接口。 |

### 1.2 请求处理流程（Tomcat）

1. Connector 在端口接受连接，解析 HTTP，构造 **Request** / **Response**。
2. 请求进入 **Engine** 的 Pipeline → Engine 的 BasicValve 按 **Host** 头选 **Host**。
3. Host 的 Pipeline → Host 的 BasicValve 按 URI 选 **Context**，设置 `request.setContext()`、`contextPath`。
4. Context 的 Pipeline → Context 的 BasicValve 按路径选 **Wrapper**（含 welcome-file 等），设置 `servletPath`。
5. Wrapper 的 Pipeline → Wrapper 的 BasicValve 调用 **Servlet.service(request, response)**。
6. 若为静态资源，由 **DefaultServlet** 从 Context 的 **docBase** 读取并返回。

### 1.3 其他重要能力（Tomcat 有，本实现简化或未实现）

- **web.xml**：servlet、servlet-mapping、welcome-file-list、filter、listener 等。
- **Session**：HttpSession、JSESSIONID Cookie、集群/持久化等。
- **Filter**：过滤器链。
- **Listener**：生命周期、Session 等监听器。
- **JNDI、连接池、安全管理、集群** 等企业特性。

---

## 二、本项目（Mini Tomcat）的实现与对比

### 2.1 整体对比

| 能力 | 完整 Tomcat | Mini Tomcat 实现情况 |
|------|-------------|----------------------|
| Server / Service / Connector | ✅ 完整 | ✅ 有，结构一致 |
| Engine / Host / Context / Wrapper | ✅ 完整 | ✅ 有，层级与职责一致 |
| Pipeline / Valve / BasicValve | ✅ 每层一条 Pipeline | ✅ 每层一个 BasicValve，逻辑等价 |
| Request / Response | ✅ 完整（参数、属性、Cookie 等） | ✅ 有参数、属性、Cookie、Session 绑定 |
| Request POST body 解析 | ✅ application/x-www-form-urlencoded | ✅ 已支持 |
| Lifecycle（init/start/stop/destroy） | ✅ 全组件 | ✅ Server、Service、Connector |
| 按 Host 头选 Host | ✅ | ✅ EngineValve 中实现 |
| docBase、静态资源、welcome-file | ✅ | ✅ Context + DefaultServlet |
| web.xml | ✅ 完整解析 | ✅ servlet、servlet-mapping、filter、filter-mapping、welcome-file-list、context-param、init-param |
| Session（HttpSession、JSESSIONID） | ✅ | ✅ SessionManager + Request.getSession() |
| Filter（FilterChain） | ✅ | ✅ Filter、FilterChain、FilterConfig、FilterDef |
| JSP（Jasper） | ✅ 编译与执行 | ⚠️ 仅占位类，不编译不执行 |
| Listener / 集群 / JNDI 等 | ✅ | ❌ 未实现 |

### 2.2 请求处理流程（本项目）

与 Tomcat 一致：**Connector → Engine(Pipeline) → Host(Pipeline) → Context(Pipeline) → Wrapper(Pipeline) → Servlet.service()**；静态资源由 DefaultServlet 从 Context 的 docBase 读取，welcome-file 在 ContextValve 中处理。

---

## 三、包与 Java 文件说明

### 3.1 根入口

| 文件 | 作用 |
|------|------|
| **Main.java** | 程序入口。组装 Server → Service → Connector、Engine → Host → Context；解析 webapp 目录与 web.xml；设置 docBase；调用 `server.init()`、`server.start()`；注册 ShutdownHook 调用 `server.stop()`。 |

---

### 3.2 包：`minitomcat.server`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **Server.java** | 代表整个 Tomcat 实例；持有多个 Service；实现 **Lifecycle**（init/start/stop/destroy），在 start/stop 中调用各 Service。 | org.apache.catalina.Server |
| **Service.java** | 将多个 Connector 与一个 **Engine** 关联；实现 Lifecycle，init 时给 Connector 设置 container（Engine），start/stop 时启停 Connector。 | org.apache.catalina.Service |

---

### 3.3 包：`minitomcat.connector`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **Connector.java** | **连接器**：绑定端口、接受 Socket；将输入流封装为 **Request**、输出流封装为 **Response**；解析请求后调用 **Container.invoke(request, response)**；实现 Lifecycle；接受线程设为非 daemon 以保持进程运行。 | Coyote 连接器 / ProtocolHandler |

---

### 3.4 包：`minitomcat.container`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **Container.java** | 容器顶层接口：`invoke(request, response)`、`getName()`、`setParent/getParent()`、`getPipeline()`。 | org.apache.catalina.Container |
| **Pipeline.java** | 管道接口：`invoke(request, response)`、`setBasic/getBasic(Valve)`。请求经 Pipeline 进入 BasicValve。 | org.apache.catalina.Pipeline |
| **Valve.java** | 阀门接口：`invoke(request, response, next)`。可串联多个 Valve，本项目每层仅用 BasicValve。 | org.apache.catalina.Valve |
| **PipelineBase.java** | Pipeline 的默认实现；持有一个 BasicValve，invoke 时直接调用该 Valve。 | StandardPipeline 等 |
| **Engine.java** | 引擎；持有一个 Pipeline（BasicValve 为 **EngineValve**）；管理多个 Host；EngineValve 按 **Host** 头选 Host 并调用其 invoke。 | org.apache.catalina.Engine |
| **Host.java** | 虚拟主机；持有一个 Pipeline（BasicValve 为 **HostValve**）；管理多个 Context；HostValve 按 URI 选 Context，设置 `request.setContext()`、`setContextPath()` 后调用 Context.invoke。 | org.apache.catalina.Host |
| **Context.java** | Web 应用；持有一个 Pipeline（BasicValve 为 **ContextValve**）；**docBase**、**welcomeFiles**、**SessionManager**、**filterDefs**、**contextParams**；`getResourceAsStream(path)`、`resourceExists(path)`；ContextValve 处理 welcome-file、按路径选 Wrapper、构建 **FilterChain**（匹配的 Filter + Servlet）并调用 chain.doFilter。 | org.apache.catalina.core.StandardContext |
| **Wrapper.java** | 最底层容器；持有一个 Pipeline（BasicValve 为 **WrapperValve**）；管理单个 **Servlet**、urlPattern、**initParams**（来自 web.xml init-param）；WrapperValve 调用 `servlet.service(request, response)`。 | org.apache.catalina.Wrapper / StandardWrapper |

---

### 3.5 包：`minitomcat.http`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **Request.java** | 封装 HTTP 请求：解析请求行、请求头、**query 参数**、**POST body**（application/x-www-form-urlencoded）；**Cookie**（getCookieValue）；**attributes**；**contextPath/servletPath**；**getSession()**。 | Request / Coyote Request |
| **Response.java** | 封装 HTTP 响应：status、**Content-Type**、**addHeader**、**addCookie**（Set-Cookie）；**setBody**；**flush()** 写回状态行、头、体。 | Response / Coyote Response |
| **HttpSession.java** | 会话对象：id、creationTime、**getAttribute/setAttribute/removeAttribute**、invalidate。 | HttpSession |
| **SessionManager.java** | 会话管理：**createSession()**、**getSession(id)**；内存存储；Cookie 名 JSESSIONID。 | Manager / Session 管理器 |

---

### 3.6 包：`minitomcat.filter`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **Filter.java** | 过滤器接口：`doFilter(request, response, chain)`。 | javax.servlet.Filter |
| **FilterChain.java** | 过滤器链接口：`doFilter(request, response)`。 | ApplicationFilterChain |
| **ApplicationFilterChain.java** | 过滤器链实现：按序调用 Filter，最后调用 Servlet。 | ApplicationFilterChain |
| **FilterConfig.java** | Filter 配置：filterName、initParameter。 | javax.servlet.FilterConfig |
| **FilterDef.java** | Filter 定义：name、filter 实例、urlPatterns、initParams；`matches(path)` 匹配 URL。 | FilterDef |
| **CharsetFilter.java** | 示例 Filter：为响应补充 charset。 | 示例 |

### 3.7 包：`minitomcat.servlet`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **Servlet.java** | Servlet 接口：`service(Request, Response)`。 | javax.servlet.Servlet |
| **DefaultServlet.java** | 默认 Servlet：优先从 Context 的 **docBase** 按 **servletPath** 提供静态文件（**MIME 类型**）；找不到则返回简单欢迎页。 | DefaultServlet |

---

### 3.8 包：`minitomcat.config`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **WebXmlParser.java** | **web.xml** 解析：`<servlet>`、`<servlet-mapping>`、`<filter>`、`<filter-mapping>`、`<welcome-file-list>`、`<context-param>`、`<init-param>`；反射创建 Servlet/Filter，按 url-pattern 创建 Wrapper/FilterDef 并加入 Context。 | 部署描述符解析 / ContextConfig 等 |

---

### 3.9 包：`minitomcat.lifecycle`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **Lifecycle.java** | 生命周期接口：`init()`、`start()`、`stop()`、`destroy()`。 | org.apache.catalina.Lifecycle |

---

### 3.10 包：`minitomcat.jsp`

| 文件 | 作用 | 对应 Tomcat |
|------|------|-------------|
| **Jasper.java** | JSP 引擎**占位**；仅提供 `isJspResource(path)` 等占位方法；**不实现** JSP 编译与执行。 | Jasper / JSP 编译与执行 |

---

## 四、目录结构一览

```
mini_tomcat/
├── TOMCAT与实现说明.md     # 本文档
├── src/
│   ├── Main.java                           # 入口：组装并启动 Server
│   └── minitomcat/
│       ├── server/       Server, Service   # 顶层与服务
│       ├── connector/    Connector         # 连接器（Coyote）
│       ├── container/    Container, Pipeline, Valve, PipelineBase,
│       │                  Engine, Host, Context, Wrapper   # 容器与管道
│       ├── http/         Request, Response, HttpSession, SessionManager  # 请求/响应/会话
│       ├── filter/        Filter, FilterChain, ApplicationFilterChain,
│       │                  FilterConfig, FilterDef, CharsetFilter   # 过滤器
│       ├── servlet/       Servlet, DefaultServlet           # Servlet 接口与默认实现
│       ├── config/        WebXmlParser                      # web.xml 解析
│       ├── lifecycle/     Lifecycle                         # 生命周期接口
│       └── jsp/           Jasper                            # JSP 占位
└── webapp/                 # 示例应用
    ├── index.html
    └── WEB-INF/
        └── web.xml
```

---

## 五、运行方式

- **编译**：`javac -encoding UTF-8 -sourcepath src -d out src/Main.java`
- **启动**：`java -cp out Main`（工作目录建议为项目根目录，以便找到 `webapp`）
- **访问**：http://localhost:9000/（端口在 `Connector` 与 `Main` 中配置为 9000）

启动成功会打印 `[Mini Tomcat] docBase = ...` 和 `Mini Tomcat started. Open http://localhost:9000/`，根路径会按 web.xml 与 welcome-file 提供 `webapp/index.html`。
