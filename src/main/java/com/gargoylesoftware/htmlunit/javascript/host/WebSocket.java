package com.gargoylesoftware.htmlunit.javascript.host;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine;
import com.gargoylesoftware.htmlunit.javascript.SimpleScriptable;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJob;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.ContextAction;
import net.sourceforge.htmlunit.corejs.javascript.ContextFactory;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

// https://github.com/adamac/Java-WebSocket-client/blob/master/src/com/sixfire/websocket/WebSocket.java
// http://en.wikipedia.org/wiki/WebSockets
// http://dev.w3.org/html5/websockets/
public class WebSocket extends SimpleScriptable {
    static {
        System.setProperty("java.protocol.handler.pkgs", System.getProperty("java.protocol.handler.pkgs") + "|com.gargoylesoftware.htmlunit.protocol");
    }

    public static final int STATE_CONNECTING = 0;
    public static final int STATE_OPEN = 1;
    public static final int STATE_CLOSING = 2;
    public static final int STATE_CLOSED = 3;

    private WebSocketImpl ws;
    private Function onopen;
    private Function onmessage;
    private Function onerror;
    private Function onclose;
    private int readyState;
    private long bufferedAmount;
    private int readerId;
    private HtmlPage page;
    private JavaScriptEngine jsEngine;
    private Object urlParam;

    public void jsConstructor(Object urlParam) throws IOException, URISyntaxException {
        this.urlParam = urlParam;
        page = (HtmlPage) getWindow().getWebWindow().getEnclosedPage();
        jsEngine = page.getWebClient().getJavaScriptEngine();
        final URI url = page.getFullyQualifiedUrl(Context.toString(urlParam)).toURI();
        connect(url);
    }

    private void connect(URI url) {
        ws = new WebSocketImpl(url);
        final ContextAction action = new ContextAction() {
            public Object run(final Context cx) {
                try {
                    ws.connect();
                    fireEvent(cx, jsxGet_onopen(), ArrayUtils.EMPTY_OBJECT_ARRAY);

                    // TODO: do a while true here? It seems to block the rest though. Maybe start a thread and communicate via a queue.
                    while (true) {
                        String message = ws.recv();
                        fireEvent(cx, jsxGet_onmessage(), new Object[]{message});
                    }
                } catch (SocketException e) {
                    // Happens on close.
                } catch (IOException e) {
                }
                return null;
            }
        };

        final ContextFactory cf = jsEngine.getContextFactory();
        final JavaScriptJob job = new JavaScriptJob() {
            public void run() {
                cf.call(action);
            }

            @Override
            public String toString() {
                return "WebSocket Job " + getId();
            }
        };
        readerId = getWindow().getWebWindow().getJobManager().addJob(job, page);
    }

    public Object jsxGet_url() {
        return urlParam;
    }

    public int jsxGet_readyState() {
        return readyState;
    }

    public long jsxGet_bufferedAmount() {
        return bufferedAmount;
    }

    public void jsxSet_onopen(final Function onopen) {
        this.onopen = onopen;
    }

    public Function jsxGet_onopen() {
        return onopen;
    }

    public void jsxSet_onmessage(final Function onmessage) {
        this.onmessage = onmessage;
    }

    public Function jsxGet_onmessage() {
        return onmessage;
    }

    public void jsxSet_onerror(final Function onerror) {
        this.onerror = onerror;
    }

    public Function jsxGet_onerror() {
        return onerror;
    }

    public void jsxSet_onclose(final Function onclose) {
        this.onclose = onclose;
    }

    public Function jsxGet_onclose() {
        return onclose;
    }

    public String jsxGet_protocol() {
        return null;
    }

    public void jsxFunction_send(final String content) throws IOException {
        try {
            ws.send(content);
        } catch (IOException e) {
            final JavaScriptEngine jsEngine = page.getWebClient().getJavaScriptEngine();
//            jsEngine.callFunction(page, onerror, context, this, onerror.getParentScope(), ArrayUtils.EMPTY_OBJECT_ARRAY);
        }
    }

    public void jsxFunction_close() {
        try {
            ws.close();
        } catch (IOException ignore) {
        } finally {
            getWindow().getWebWindow().getJobManager().stopJob(readerId);
        }
    }

    private void fireEvent(Context cx, Function eventFunc, Object[] args) {
        jsEngine.callFunction(page, eventFunc, cx, eventFunc.getParentScope(), this, args);
    }
}
