package io.millesabords.zeppelin.interpreter.arangodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoDriver;
import com.arangodb.ArangoException;
import com.arangodb.ArangoHost;
import com.arangodb.entity.BaseDocument;

public class ArangoDbInterpreterTest {

    private static final String[] METHODS = { "GET", "PUT", "DELETE", "POST" };
    private static final String[] STATUS = { "200", "404", "500", "403" };
    private static final String HOST = "localhost";
    private static final int PORT = 8529;
    private static final String DB_NAME = "monitoring";
    private static final String COLL_NAME = "logs";

    private static ArangoConfigure configure;
    private static ArangoDriver driver;
    private static ArangoDbInterpreter interpreter;

    @BeforeClass
    public static void populate() throws ArangoException, IOException {
        configure = new ArangoConfigure();
        configure.setArangoHost(new ArangoHost(HOST, PORT));
        configure.setDefaultDatabase(DB_NAME);
        configure.init();

        driver = new ArangoDriver(configure);

        //driver.createDatabase(DB_NAME);
        //driver.createCollection(COLL_NAME);

        for (Integer i = 0; i < 50; i++) {
            final BaseDocument log = new BaseDocument();
            log.setDocumentKey(i.toString());
            log.addAttribute("date", new Date());
            log.addAttribute("status", STATUS[RandomUtils.nextInt(STATUS.length)]);
            log.addAttribute("content_length", RandomUtils.nextInt(2000));

            final Map<String, Object> req = new HashMap<>();
            req.put("method", METHODS[RandomUtils.nextInt(METHODS.length)]);
            req.put("url", "/zeppelin/" + UUID.randomUUID().toString());
            req.put("headers", Arrays.asList("Accept: *.*", "Host: apache.org"));
            log.addAttribute("request", req);

            driver.createDocument(COLL_NAME, log);
        }

        final Properties props = new Properties();
        props.put(ArangoDbInterpreter.ARANGODB_HOST, HOST);
        props.put(ArangoDbInterpreter.ARANGODB_PORT, "" + PORT);
        props.put(ArangoDbInterpreter.ARANGODB_DATABASE, DB_NAME);
        interpreter = new ArangoDbInterpreter(props);
        interpreter.open();
    }


    @AfterClass
    public static void stop() {
        if (driver != null) {
            /*try {
                driver.deleteDatabase(DB_NAME);
            }
            catch (final ArangoException e) {
            }*/
        }
        if (configure != null) {
            configure.shutdown();
        }
        if (interpreter != null) {
            interpreter.close();
        }
    }

    @Test
    public void testBadQueries() {
        final InterpreterResult res = interpreter.interpret("nimportequoi", null);
        assertEquals(Code.ERROR, res.code());
    }

    @Test
    public void testGoodQueries() {
        InterpreterResult res = interpreter.interpret("FOR l IN logs RETURN l", null);
        assertEquals(Code.SUCCESS, res.code());

        res = interpreter.interpret("FOR l IN logs FILTER l.status == '200' RETURN l", null);
        assertEquals(Code.SUCCESS, res.code());

        res = interpreter.interpret("FOR l IN logs FILTER l.status == '200' RETURN l.content_length", null);
        assertEquals(Code.SUCCESS, res.code());

        res = interpreter.interpret("FOR l IN logs FILTER l.status == '404' RETURN { not_found_url : l.request.url }", null);
        assertEquals(Code.SUCCESS, res.code());

        res = interpreter.interpret("FOR l IN logs FILTER l.status == '499' RETURN l", null);
        assertTrue(Code.SUCCESS.equals(res.code()) && "Empty result".equals(res.message()));
    }

}
