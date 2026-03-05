package minitomcat.naming;

import minitomcat.core.LifecycleBase;
import minitomcat.lifecycle.LifecycleException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * JNDI 命名资源管理，与 Tomcat 一致。
 * 支持环境条目、DataSource 等资源的配置和管理。
 */
public class NamingResources extends LifecycleBase {
    private Context context;
    private final Map<String, String> environment = new HashMap<>();
    private final Map<String, Resource> resources = new HashMap<>();

    public NamingResources() {
    }

    @Override
    protected void initInternal() throws LifecycleException {
        try {
            context = new InitialContext();
        } catch (NamingException e) {
            throw new LifecycleException("Failed to create InitialContext", e);
        }
    }

    @Override
    protected void startInternal() throws LifecycleException {
        // 绑定环境条目
        try {
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                context.bind(entry.getKey(), entry.getValue());
            }
            // 绑定资源
            for (Map.Entry<String, Resource> entry : resources.entrySet()) {
                context.bind(entry.getKey(), entry.getValue().getObject());
            }
        } catch (NamingException e) {
            throw new LifecycleException("Failed to bind naming resources", e);
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        // 解绑资源
        try {
            for (String key : environment.keySet()) {
                context.unbind(key);
            }
            for (String key : resources.keySet()) {
                context.unbind(key);
            }
        } catch (NamingException e) {
            // ignore
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        context = null;
    }

    /**
     * 添加环境条目
     */
    public void addEnvironment(String name, String value) {
        environment.put(name, value);
    }

    /**
     * 移除环境条目
     */
    public void removeEnvironment(String name) {
        environment.remove(name);
    }

    /**
     * 获取环境条目
     */
    public String getEnvironment(String name) {
        return environment.get(name);
    }

    /**
     * 添加资源
     */
    public void addResource(Resource resource) {
        resources.put(resource.getName(), resource);
    }

    /**
     * 移除资源
     */
    public void removeResource(String name) {
        resources.remove(name);
    }

    /**
     * 获取资源
     */
    public Resource getResource(String name) {
        return resources.get(name);
    }

    public Context getContext() {
        return context;
    }

    public Map<String, String> getEnvironments() {
        return environment;
    }

    public Map<String, Resource> getResources() {
        return resources;
    }

    /**
     * 资源定义
     */
    public static class Resource {
        private String name;
        private String type;
        private Object object;
        private String auth;
        private String description;
        private Map<String, String> properties = new HashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperty(String key, String value) {
            properties.put(key, value);
        }
    }
}
