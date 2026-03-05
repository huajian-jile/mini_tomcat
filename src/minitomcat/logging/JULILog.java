package minitomcat.logging;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Tomcat JULI 日志系统的简化实现。
 * 支持配置文件和日志分级。
 */
public class JULILog {
    private final String name;
    private final Logger logger;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private JULILog(String name) {
        this.name = name;
        this.logger = Logger.getLogger(name);
        // 使用父Logger的配置，避免重复添加handler
        this.logger.setUseParentHandlers(true);
    }

    public static JULILog getLog(String name) {
        return new JULILog(name);
    }

    public void debug(String message) {
        logger.fine(message);
    }

    public void debug(String message, Throwable thrown) {
        logger.fine(message + ": " + thrown.getMessage());
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String message, Throwable thrown) {
        logger.info(message + ": " + thrown.getMessage());
    }

    public void warn(String message) {
        logger.warning(message);
    }

    public void warn(String message, Throwable thrown) {
        logger.warning(message + ": " + thrown.getMessage());
    }

    public void error(String message) {
        logger.severe(message);
    }

    public void error(String message, Throwable thrown) {
        logger.severe(message + ": " + thrown.getMessage());
    }

    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    /**
     * 初始化日志系统
     */
    public static void init() {
        // 配置根Logger
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);

        // 添加控制台处理器
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String timestamp = DATE_FORMAT.format(new Date(record.getMillis()));
                String level = record.getLevel().getName();
                String loggerName = record.getLoggerName();
                String message = record.getMessage();
                return String.format("[%s] [%s] [%s] %s%n", timestamp, level, loggerName, message);
            }
        });
        rootLogger.addHandler(consoleHandler);
    }

    /**
     * 配置日志文件
     */
    public static void configureFileHandler(String fileName, int limit, int count) throws IOException {
        Logger rootLogger = Logger.getLogger("");
        FileHandler fileHandler = new FileHandler(fileName, limit, count, true);
        fileHandler.setLevel(Level.ALL);
        fileHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String timestamp = DATE_FORMAT.format(new Date(record.getMillis()));
                String level = record.getLevel().getName();
                String loggerName = record.getLoggerName();
                String message = record.getMessage();
                if (record.getThrown() != message) {
                    message += "\n" + record.getThrown();
                }
                return String.format("[%s] [%s] [%s] %s%n", timestamp, level, loggerName, message);
            }
        });
        rootLogger.addHandler(fileHandler);
    }
}
