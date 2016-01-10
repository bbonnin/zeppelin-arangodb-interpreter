package io.millesabords.zeppelin.interpreter.arangodb;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Simulates an ArangoDB server for test purpose.
 *
 * Default connection : http://127.0.0.1:8529
 *
 * @author Bruno Bonnin
 *
 */
@SuppressWarnings("restriction")
public class MockArangoServer {

    private final HttpServer server;

    private final Map<String, ArangoResponse> responses = new HashMap<>();

    public MockArangoServer() throws IOException {
        this(8529);
    }

    public MockArangoServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                final String req = exchange.getRequestMethod() + " " + exchange.getRequestURI();
                final ArangoResponse response = responses.get(req);
                if (response != null) {
                    exchange.sendResponseHeaders(response.status,
                            response.body != null ? response.body.length() : 0);
                    if (response.body != null) {
                        final OutputStream out = exchange.getResponseBody();
                        out.write(response.body.getBytes());
                        out.close();
                    }
                }
            }
        });
        server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public void addResponse(String request, int responseStatus, String responseBody) {
        responses.put(request, new ArangoResponse(responseStatus, responseBody));
    }

    public void initResponses(String db, String coll) {
        addResponse("POST /_api/database", 201, "{\"result\":true,\"error\":false,\"code\":201}");
        addResponse("DELETE /_api/database/" + db, 200, "{\"result\":true,\"error\":false,\"code\":200}");
        addResponse("POST /_db/" + db + "/_api/collection", 200, "{\"id\":\"7984136251\",\"name\":\"" + coll + "\",\"waitForSync\":false,\"isVolatile\":false,\"isSystem\":false,\"status\":3,\"type\":2,\"error\":false,\"code\":200}");
        addResponse("POST /_api/document?collection=" + coll, 202, "{\"error\":false,\"_id\":\"" + coll + "/1\",\"_rev\":\"1300562966\",\"_key\":\"1\"}");
//        mockServer.addResponse("POST /_api/cursor", 201, "{\"result\":[{"b":47,"name":"Homer","_id":"firstCollection/5","_rev":"1302266902","_key":"5"},{"b":48,"name":"Homer","_id":"firstCollection/6","_rev":"1302463510","_key":"6"},{"b":45,"name":"Homer","_id":"firstCollection/3","_rev":"1301873686","_key":"3"},{"b":46,"name":"Homer","_id":"firstCollection/4","_rev":"1302070294","_key":"4"},{"b":43,"name":"Homer","_id":"firstCollection/1","_rev":"1301480470","_key":"1"},{"b":51,"name":"Homer","_id":"firstCollection/9","_rev":"1303053334","_key":"9"},{"b":44,"name":"Homer","_id":"firstCollection/2","_rev":"1301677078","_key":"2"},{"b":49,"name":"Homer","_id":"firstCollection/7","_rev":"1302660118","_key":"7"},{"b":42,"name":"Homer","_id":"firstCollection/0","_rev":"1301283862","_key":"0"},{"b":50,"name":"Homer","_id":"firstCollection/8","_rev":"1302856726","_key":"8"}],"hasMore":false,"count":10,"cached":false,"extra":{"stats":{"writesExecuted":0,"writesIgnored":0,"scannedFull":10,"scannedIndex":0,"filtered":0},"warnings":[]},"error":false,"code":201}

    }



    static class ArangoResponse {
        public int status;
        public String body;
        public ArangoResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

}
