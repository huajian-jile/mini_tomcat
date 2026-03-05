package minitomcat.valves;

import minitomcat.container.Valve;
import minitomcat.http.Request;
import minitomcat.http.Response;
import minitomcat.logging.JULILog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 访问日志阀门，与 Tomcat 的 AccessLogValve 一致。
 * 记录每个请求的访问日志。
 */
public class AccessLogValve implements Valve {
    private static final JULILog log = JULILog.getLog(AccessLogValve.class);

    private String directory = "logs";
    private String prefix = "access_log.";
    private String suffix = ".log";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private volatile FileWriter writer;
    private String fileDate = "";
    private boolean enabled = true;
    private boolean asyncSupported = true;

    public AccessLogValve() {
    }

    @Override
    public void invoke(Request request, Response response, Valve next) throws Exception {
        long startTime = System.currentTimeMillis();
        try {
            next.invoke(request, response);
        } finally {
            if (enabled) {
                log(request, response, System.currentTimeMillis() - startTime);
            }
        }
    }

    private void log(Request request, Response response, long time) {
        try {
            String date = dateFormat.format(new Date());
            if (!date.equals(fileDate)) {
                rotateLog(date);
            }

            if (writer != null) {
                String logLine = formatLogLine(request, response, time);
                writer.write(logLine);
                writer.flush();
            }
        } catch (IOException e) {
            log.error("Failed to write access log", e);
        }
    }

    private String formatLogLine(Request request, Response response, long time) {
        String remoteAddr = request.getRemoteAddr();
        String method = request.getMethod();
        String uri = request.getUri();
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            uri = uri + "?" + queryString;
        }
        int status = response.getStatus();
        long bytes = response.getContentLength();
        long timestamp = System.currentTimeMillis();

        return String.format("%s - - [%s] \"%s %s\" %d %d %d%n",
            remoteAddr,
            new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z").format(new Date(timestamp)),
            method,
            uri,
            status,
            bytes,
            time
        );
    }

    private synchronized void rotateLog(String date) throws IOException {
        if (writer != null) {
            writer.close();
        }
        fileDate = date;

        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = directory + File.separator + prefix + date + suffix;
        writer = new FileWriter(fileName, true);
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
