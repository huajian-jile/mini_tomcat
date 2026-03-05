package minitomcat.config;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析 apps.json 配置文件
 */
public class AppConfig {

    public static class AppInfo {
        public String path = "/";
        public String docBase = "";
    }

    public static List<AppInfo> parse(File configFile) {
        List<AppInfo> apps = new ArrayList<>();

        if (configFile == null || !configFile.isFile()) {
            return apps;
        }

        try {
            String content = new String(java.nio.file.Files.readAllBytes(configFile.toPath()));
            content = content.replace("\n", "").replace("\r", "").replace(" ", "");

            // 简单解析 JSON 数组
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start == -1 || end == -1) return apps;

            String arrayContent = content.substring(start + 1, end);
            // 分割每个对象
            int braceCount = 0;
            int objStart = -1;
            for (int i = 0; i < arrayContent.length(); i++) {
                char c = arrayContent.charAt(i);
                if (c == '{') {
                    if (objStart == -1) objStart = i;
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0 && objStart != -1) {
                        String obj = arrayContent.substring(objStart, i + 1);
                        AppInfo info = parseObject(obj);
                        if (info != null && !info.docBase.isEmpty()) {
                            apps.add(info);
                        }
                        objStart = -1;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse apps.json: " + e.getMessage());
        }

        return apps;
    }

    private static AppInfo parseObject(String obj) {
        AppInfo info = new AppInfo();

        // 提取 path
        int pathStart = obj.indexOf("\"path\":\"");
        if (pathStart != -1) {
            int pathEnd = obj.indexOf("\"", pathStart + 8);
            if (pathEnd != -1) {
                info.path = obj.substring(pathStart + 8, pathEnd);
            }
        }

        // 提取 docBase
        int docStart = obj.indexOf("\"docBase\":\"");
        if (docStart != -1) {
            int docEnd = obj.indexOf("\"", docStart + 11);
            if (docEnd != -1) {
                info.docBase = obj.substring(docStart + 11, docEnd);
            }
        }

        return info;
    }
}
