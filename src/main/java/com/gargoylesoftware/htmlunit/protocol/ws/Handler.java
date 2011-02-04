package com.gargoylesoftware.htmlunit.protocol.ws;

import com.gargoylesoftware.htmlunit.protocol.javascript.JavaScriptURLConnection;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(final URL url) {
        return new WebSocketURLConnection(url);
    }
}