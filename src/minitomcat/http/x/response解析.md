这正是与刚才 Request 类配对的 HTTP 响应封装类，它模仿了 Tomcat 中 HttpServletResponse 的核心功能。

这个类的作用是：将 Java 中的数据组装成符合 HTTP 协议的字节流，并通过输出流写回给客户端（浏览器）。它负责构建状态行、响应头（包括 Cookie）、响应体，并处理了输出的细节（如字符编码、内容长度）。

以下是详细的代码分析：

核心功能与设计思路

这个 Response 类主要做了三件事：

数据封装：提供友好的方法（如 setStatus、addHeader、setBody）让业务逻辑层设置响应内容。
协议组装：在 flush 方法中，将状态、头、体按照 HTTP 协议格式拼接成字节流。
会话支持：通过 addCookie 方法支持写入 Set-Cookie，配合 Request 类实现完整的 Session 机制。

关键代码解析

响应头与 Cookie 管理
通用头管理：使用 List headerLines 存储所有的头信息。虽然使用 List 会保留顺序，但在实际 HTTP 协议中顺序通常不重要，使用 Map 会更便于后续的覆盖修改，不过对于简单的写回场景，List 已经足够。
Cookie 封装：addCookie 方法封装了 Cookie 的格式细节：
支持 Path 和 Max-Age 属性。
安全特性：默认添加了 HttpOnly 标志，这能有效防止 XSS 攻击（通过 JavaScript 无法读取该 Cookie），是一个很好的安全实践。

响应体与状态管理
灵活的 Body 设置：支持传入 String 或 byte[]，内部自动转换为字节流。
字符编码：统一使用 UTF-8 编码，保证了中文字符的正常显示。
状态行：默认状态为 200 OK，可通过 setStatus 修改。

核心输出 (flush 方法)
这是类的执行核心，负责将数据真正写入网络流。

防重入机制：使用 committed 标志位确保响应只能被写回一次。在 HTTP 通信中，一旦响应头发出，就不能再修改，这个设计符合规范。
自动补全头信息：
如果设置了 contentType 且未手动添加 Content-Type 头，则自动添加。
如果存在 body 且未手动添加 Content-Length，则自动计算并添加。这是非常重要的优化，避免了客户端一直等待数据。
协议格式化：
状态行格式：HTTP/1.1 200 OK。
头部格式：Name: Value + rn。
空行分隔：在头部结束后写入额外的 rn，标志着头部结束，接下来是正文。

潜在的改进点与建议

虽然代码功能完整，但在健壮性和性能上还有一些可以优化的地方：

Content-Length 的覆盖问题：
现状：代码通过 stream().anyMatch 检查是否已存在 Content-Length。但在实际应用中，如果开发者手动设置了错误的长度，可能会导致客户端解析错误。
建议：更严格的做法是，如果用户未设置，强制自动计算；或者提供一个方法让用户明确表示“不要自动计算”。

字符编码的灵活性：
现状：硬编码了 UTF-8。
建议：可以增加 charset 属性，允许设置其他编码（虽然现在几乎全是 UTF-8）。

资源竞争：
现状：flush 方法中直接操作了 outputStream。
注意：由于每个 Response 对象通常只由一个线程处理（一个请求一个线程），所以线程安全问题不大。但如果在异步处理中，需要确保 flush 只被调用一次。

错误处理：
现状：flush 抛出 IOException。
建议：在写入过程中如果发生异常（如客户端断开连接），可能需要记录日志或进行清理操作。

总结

这是一个简洁而强大的响应封装类。它准确地实现了 HTTP 响应的核心要素，并通过 flush 方法的自动补全逻辑（Content-Type 和 Content-Length），极大地简化了使用者的开发工作。特别是对 HttpOnly Cookie 的支持，体现了对安全细节的关注。配合之前的 Request 和 Connector，已经构成了一个完整的微型 Web 服务器核心。