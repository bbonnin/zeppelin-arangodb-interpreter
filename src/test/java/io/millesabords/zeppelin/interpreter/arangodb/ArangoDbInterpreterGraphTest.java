package io.millesabords.zeppelin.interpreter.arangodb;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arangodb.ArangoConfigure;
import com.arangodb.ArangoException;
import com.arangodb.ArangoHost;

/**
 * Tests for graph queries.
 * 
 * The data are coming from : https://docs.arangodb.com/cookbook/GraphExampleActorsAndMovies.html
 * 
 * @author Bruno Bonnin
 *
 */
public class ArangoDbInterpreterGraphTest {

    private static final String HOST = "localhost";
    private static final int PORT = 8529;
    private static final String DB_NAME = "cinema";

    private static ArangoConfigure configure;
    private static ArangoDbInterpreter interpreter;

    @BeforeClass
    public static void populate() throws ArangoException, IOException {
        configure = new ArangoConfigure();
        configure.setArangoHost(new ArangoHost(HOST, PORT));
        configure.setDefaultDatabase(DB_NAME);
        configure.init();

        final Properties props = new Properties();
        props.put(ArangoDbInterpreter.ARANGODB_HOST, HOST);
        props.put(ArangoDbInterpreter.ARANGODB_PORT, "" + PORT);
        props.put(ArangoDbInterpreter.ARANGODB_DATABASE, DB_NAME);
        interpreter = new ArangoDbInterpreter(props);
        interpreter.open();
    }

    @AfterClass
    public static void stop() {
        if (configure != null) {
            configure.shutdown();
        }
        if (interpreter != null) {
            interpreter.close();
        }
    }

    @Test
    public void testGraphQueries() {
        InterpreterResult res = interpreter.interpret("FOR x IN actsIn COLLECT actor = x._from WITH COUNT INTO counter FILTER counter >= 3 RETURN {actor: actor, movies: counter}", null);
        assertEquals(Code.SUCCESS, res.code());
        
        res = interpreter.interpret("FOR x IN actsIn COLLECT movie = x._to WITH COUNT INTO counter FILTER counter == 6 RETURN movie", null);
        assertEquals(Code.SUCCESS, res.code());
        
        res = interpreter.interpret("FOR x IN actsIn COLLECT movie = x._to WITH COUNT INTO counter RETURN {movie: movie, actors: counter}", null);
        assertEquals(Code.SUCCESS, res.code());
        
        res = interpreter.interpret("RETURN NEIGHBORS(movies, actsIn, 'TheMatrix', 'any')", null);
        assertEquals(Code.SUCCESS, res.code());
    }

}
