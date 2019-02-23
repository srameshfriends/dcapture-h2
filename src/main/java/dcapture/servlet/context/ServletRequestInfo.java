package dcapture.servlet.context;

import dcapture.api.support.UrlBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ServletRequestInfo
 */
public class ServletRequestInfo {
    private String os, browser, browserVersion;
    private String remoteHost, remoteAddress;
    private int remotePort;
    private Boolean mobileDevice;

    public ServletRequestInfo(HttpServletRequest request) {
        setRemoteParam(request.getRemoteHost(), request.getRemoteAddr(), request.getRemotePort());
        String userAgent = request.getHeader("User-Agent");
        userAgent = userAgent == null ? "" : userAgent.toLowerCase();
        setOS(userAgent);
        setBrowser(userAgent);
        setMobile(userAgent);
    }

    private void setRemoteParam(String remoteHost, String remoteAddress, int remotePort) {
        try {
            if (remoteAddress.equals("0:0:0:0:0:0:0:1")) {
                InetAddress localIp = InetAddress.getLocalHost();
                remoteAddress = localIp.getHostAddress();
                remoteHost = localIp.getHostName();
            }
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.remoteAddress = remoteAddress;
        } catch (UnknownHostException ex) {
            // ignore exception
        }
    }

    private void setOS(String userAgent) {
        if (userAgent.contains("windows")) {
            os = "windows";
        } else if (userAgent.contains("mac")) {
            os = "mac";
        } else if (userAgent.contains("x11")) {
            os = "unix";
        } else if (userAgent.contains("android")) {
            os = "android";
        } else if (userAgent.contains("iphone")) {
            os = "iphone";
        } else {
            os = "unKnown";
        }
    }

    private void setBrowser(String userAgent) {
        if (userAgent.contains("msie")) {
            String subsString = userAgent.substring(userAgent.indexOf("msie"));
            String[] info = (subsString.split(";")[0]).split(" ");
            browser = info[0];
            browserVersion = info[1];
        } else if (userAgent.contains("firefox")) {
            String subsString = userAgent.substring(userAgent.indexOf("firefox"));
            String[] info = (subsString.split(" ")[0]).split("/");
            browser = info[0];
            browserVersion = info[1];
        } else if (userAgent.contains("chrome")) {
            String subsString = userAgent.substring(userAgent.indexOf("chrome"));
            String[] info = (subsString.split(" ")[0]).split("/");
            browser = info[0];
            browserVersion = info[1];
        } else if (userAgent.contains("opera")) {
            String subsString = userAgent.substring(userAgent.indexOf("opera"));
            String[] info = (subsString.split(" ")[0]).split("/");
            browser = info[0];
            browserVersion = info[1];
        } else if (userAgent.contains("safari")) {
            String subsString = userAgent.substring(userAgent.indexOf("safari"));
            String[] info = (subsString.split(" ")[0]).split("/");
            browser = info[0];
            browserVersion = info[1];
        } else {
            browser = "unknown";
            browserVersion = "";
        }
    }

    private void setMobile(String userAgent) {
        mobileDevice = userAgent.contains("mobile");
    }

    public String getOs() {
        return os;
    }

    public String getBrowser() {
        return browser;
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public Boolean getMobileDevice() {
        return mobileDevice;
    }

    @Override
    public String toString() {
        UrlBuilder builder = new UrlBuilder();
        builder.add("OS", os);
        builder.add("BROWSER", browser);
        builder.add("VERSION", browserVersion);
        builder.add("HOST", remoteHost);
        builder.add("ADDRESS", remoteAddress);
        builder.add("PORT", remotePort + "");
        builder.add("IS_MOBILE", mobileDevice + "");
        return builder.toString();
    }
}
