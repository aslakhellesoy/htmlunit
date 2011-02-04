/*
 * Copyright (c) 2002-2011 Gargoyle Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.htmlunit.javascript.host;

import com.gargoylesoftware.htmlunit.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import webbit.WebServer;
import webbit.WebSocketConnection;
import webbit.WebSocketHandler;
import webbit.netty.NettyWebServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Thread.sleep;

@RunWith(BrowserRunner.class)
public class WebSocketTest extends WebServerTestCase {
    private WebServer webServer;

    @Test
    public void canSendAndReceiveData() throws Exception {
        final String html =
              "<html>\n"
            + "  <head>\n"
            + "    <title>XMLHttpRequest Test</title>\n"
            + "    <script>\n"
            + "      var ws;\n"
            + "      function testWebsocket() {\n"
            + "        if (window.WebSocket) {\n"
            + "          ws = new WebSocket('ws://localhost:" + PORT + "/websocket');\n"
            + "          ws.onopen = function() {\n"
            + "            ws.send('Hello Websocket');\n"
            + "          };\n"
            + "          ws.onmessage = function(message) {\n"
            + "            alert('onmessage:'+message);\n "
            + "            ws.close();\n"
            + "          };\n"
            + "          ws.onclose = function(message) {\n"
            + "            alert('closed');\n "
            + "          };\n"
            + "          alert('OK');\n"
            + "        } else {\n"
            + "          alert('KO');\n"
            + "        };\n"
            + "      }\n"
            + "    </script>\n"
            + "  </head>\n"
            + "  <body onload='testWebsocket()'>\n"
            + "  </body>\n"
            + "</html>";

        final WebClient client = getWebClient();
        killIfStillAliveAfter(client, 2000);

        final List<String> collectedAlerts = Collections.synchronizedList(new ArrayList<String>());
        client.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
        final MockWebConnection conn = new MockWebConnection();
        conn.setResponse(URL_FIRST, html);
        client.setWebConnection(conn);
        client.getPage(URL_FIRST);

        assertEquals(0, client.waitForBackgroundJavaScriptStartingBefore(1000));
        final String[] alerts = {"OK", "onmessage:HELLO WEBSOCKET"};
        assertEquals(alerts, collectedAlerts);
    }

    private void killIfStillAliveAfter(final WebClient client, final int millis) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    sleep(millis);
                } catch (InterruptedException e) {
                }
                client.closeAllWindows();
            }
        }).start();
    }

    @Before
    public void startWebbit() throws IOException, InterruptedException {
        webServer = new NettyWebServer(PORT);
        webServer.add("/websocket", new WebSocketHandler() {
            private WebSocketConnection connection;

            public void onOpen(WebSocketConnection connection) throws Exception {
                this.connection = connection;
            }

            public void onMessage(WebSocketConnection connection, String msg) throws Exception {
                connection.send(msg.toUpperCase());
            }

            public void onClose(WebSocketConnection connection) throws Exception {
                throw new UnsupportedOperationException("TODO");
            }
        });
        webServer.start();
    }

    @After
    public void stopWebbit() throws IOException, InterruptedException {
        webServer.stop();
        webServer.join();
    }
}
