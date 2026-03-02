package minispring.servlet;

import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.servlet.Servlet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * MiniSpring 的 Servlet 适配器
 */
public class MiniSpringServlet implements Servlet {

    private static liu.spring.webmvc.DispatcherServlet dispatcherServlet;
    private static boolean initialized = false;

    @Override
    public void service(Request request, Response response) throws Exception {
        System.out.println("[MiniSpring] service called: " + request.getUri() + " method: " + request.getMethod());

        if (!initialized) {
            init();
            initialized = true;
        }

        minispring.servlet.MiniSpringExchange exchange = new minispring.servlet.MiniSpringExchange(request, response);
        try {
            dispatcherServlet.handle(exchange);
            System.out.println("[MiniSpring] handle completed");
        } catch (Exception e) {
            System.out.println("[MiniSpring] handle error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            exchange.close();
        }
    }

    private synchronized void init() throws Exception {
        if (dispatcherServlet != null) return;

        System.out.println("[MiniSpring] Initializing...");

        liu.spring.webmvc.RequestMappingHandlerMapping handlerMapping =
            new liu.spring.webmvc.RequestMappingHandlerMapping();

        try {
            // 创建 SqlSession
            Class<?> sqlSessionClass = Class.forName("liu.db.SqlSession");
            Object sqlSession = sqlSessionClass.getDeclaredConstructor().newInstance();

            // 创建 UserMapper
            Class<?> userMapperClass = Class.forName("liu.mapper.UserMapper");
            Object userMapper = sqlSessionClass.getMethod("getMapper", Class.class).invoke(sqlSession, userMapperClass);

            // 创建 UserServiceImp
            Class<?> userServiceClass = Class.forName("liu.service.UserServiceImp");
            Object userServiceImp = userServiceClass.getDeclaredConstructor().newInstance();

            // 注入 userMapper
            for (Field f : userServiceClass.getDeclaredFields()) {
                if (f.getName().equals("userMapper")) {
                    f.setAccessible(true);
                    f.set(userServiceImp, userMapper);
                }
            }

            // 创建 UserController
            Class<?> userControllerClass = Class.forName("liu.Controller.UserController");
            Object userController = userControllerClass.getDeclaredConstructor().newInstance();

            // 注入 userService
            for (Field f : userControllerClass.getDeclaredFields()) {
                if (f.getName().equals("userService")) {
                    f.setAccessible(true);
                    f.set(userController, userServiceImp);
                }
            }

            handlerMapping.registerHandler(userController, userControllerClass);
            System.out.println("[MiniSpring] Registered: UserController, UserService, UserMapper");
        } catch (Exception e) {
            System.out.println("[MiniSpring] Warning: " + e.getMessage());
            e.printStackTrace();
        }

        liu.spring.webmvc.RequestMappingHandlerAdapter handlerAdapter =
            new liu.spring.webmvc.RequestMappingHandlerAdapter();

        List<liu.spring.webmvc.HandlerMapping> mappings = new ArrayList<>();
        mappings.add(handlerMapping);
        List<liu.spring.webmvc.HandlerAdapter> adapters = new ArrayList<>();
        adapters.add(handlerAdapter);

        dispatcherServlet = new liu.spring.webmvc.DispatcherServlet(mappings, adapters);
        System.out.println("[MiniSpring] Initialized successfully");
    }
}
