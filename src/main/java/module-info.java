module dcapture.io {
    requires java.sql;
    requires java.json;
    requires java.ws.rs;
    requires javax.inject;
    requires javax.servlet.api;
    requires org.apache.commons.io;
    requires commons.fileupload;
    requires commons.csv;
    requires org.glassfish.java.json;
    requires jetty.io;
    requires jetty.util;
    requires jetty.server;
    requires jetty.servlet;
    requires pustike.inject;
    exports dcapture.io;
}