package io.millesabords.zeppelin.interpreter.arangodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;

/**
 * Tests for basic queries.
 *
 * @author Bruno Bonnin
 *
 */
public class ArangoDbInterpreterTest extends AbstractArangoDbInterpreterTest {

    private static final String[] METHODS = { "GET", "PUT", "DELETE", "POST" };
    private static final String[] STATUS = { "200", "404", "500", "403" };
    private static final String DB_NAME = "monitoring";
    private static final String COLL_NAME = "logs";


    @BeforeClass
    public static void populate() throws ArangoDBException, IOException {
        init(DB_NAME);

        arango.db(DB_NAME).createCollection(COLL_NAME);

        for (Integer i = 0; i < 50; i++) {
            final BaseDocument log = new BaseDocument();
            log.setKey(i.toString());
            log.addAttribute("date", new Date());
            log.addAttribute("status", STATUS[RandomUtils.nextInt(STATUS.length)]);
            log.addAttribute("content_length", RandomUtils.nextInt(2000));

            final Map<String, Object> req = new HashMap<>();
            req.put("method", METHODS[RandomUtils.nextInt(METHODS.length)]);
            req.put("url", "/zeppelin/" + UUID.randomUUID().toString());
            req.put("headers", Arrays.asList("Accept: *.*", "Host: apache.org"));
            log.addAttribute("request", req);

            arango.db(DB_NAME).collection(COLL_NAME).insertDocument(log);
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
        assertTrue(Code.SUCCESS.equals(res.code())
                && res.message().size() == 1
                && "Empty result".equals(res.message().get(0).getData()));
    }

}
