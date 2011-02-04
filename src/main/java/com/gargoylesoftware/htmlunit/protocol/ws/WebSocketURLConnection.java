package com.gargoylesoftware.htmlunit.protocol.ws;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class WebSocketURLConnection extends URLConnection {
    @Override
    public void connect() throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    public WebSocketURLConnection(URL url) {
        super(url);
    }
}
