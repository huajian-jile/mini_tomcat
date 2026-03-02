package minitomcat.adapter;

import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.servlet.Servlet;
import liu.spring.webmvc.*;

import java.util.ArrayList;
import java.util.List;

/**
 * MiniSpring 的 Servlet 适配器：将 mini_tomcat 的请求适配给 minispring 的 DispatcherServlet
 */
public class MiniSpringServlet implements Servlet {

    private static DispatcherServlet dispatcherServlet;
    private static boolean initialized = false;

    @Override
    public void service(Request request, Response response) throws Exception {
        // 初始化 minispring
        if (!initialized) {
            init();
            initialized = true;
        }

        // 创建适配器
        MiniTomcatExchange exchange = new MiniTomcatExchange(request, response);

        // 使用 minispring 的逻辑处理请求
        try {
            dispatcherServlet.handle(exchange);
        } finally {
            exchange.close();
        }
    }

    private synchronized void init() throws Exception {
        if (dispatcherServlet != null) return;

        System.out.println("[MiniSpring] Initializing via mini_tomcat...");

        // 创建 HandlerMapping
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();

        // 直接手动注册控制器（简化版）
        try {
            // 获取 UserController 实例
            Class<?> userControllerClass = Class.forName("liu.Controller.UserController");
            Object userController = userControllerClass.getDeclaredConstructor().newInstance();

            // 获取 UserServiceImp 实例
            Class<?> userServiceClass = Class.forName("liu.service.UserServiceImp");
            Object userServiceImp = userServiceClass.getDeclaredConstructor().newInstance();

            // 注入依赖（简化版：通过字段注入）
            for (Field f : userControllerClass.getDeclaredFields()) {
                if (f.getName().equals("userService")) {
                    f.setAccessible(true);
                    f.set(userController, userServiceImp);
                }
            }

            // 注册处理器
            handlerMapping.registerHandler(userController, userControllerClass);

            System.out.println("[MiniSpring] Registered: UserController");
        } catch (Exception e) {
            System.out.println("[MiniSpring] Warning: " + e.getMessage());
        }

        // 创建 HandlerAdapter
        RequestMappingHandlerAdapter handlerAdapter = new RequestMappingHandlerAdapter();

        // 创建列表
        List<HandlerMapping> mappings = new ArrayList<>();
        mappings.add(handlerMapping);
        List<HandlerAdapter> adapters = new ArrayList<>();
        adapters.add(handlerAdapter);

        // 创建 DispatcherServlet
        dispatcherServlet = new DispatcherServlet(mappings, adapters);

        System.out.println("[MiniSpring] Initialized successfully");
    }
}
