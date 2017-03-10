package io.millesabords.zeppelin.interpreter.arangodb;

import java.util.Properties;

import org.junit.AfterClass;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;

/**
 * Base class for the tests.
 *
 * @author Bruno Bonnin
 *
 */
public abstract class AbstractArangoDbInterpreterTest {

    private static final String HOST = "localhost";
    private static final int PORT = 8529;
    private static final String USERNAME = "root";
    private static final String PASSWORD = "arango";

    private static String dbName;

    protected static ArangoDB arango;
    protected static ArangoDbInterpreter interpreter;

    protected static void init(String dbName) {

        AbstractArangoDbInterpreterTest.dbName = dbName;

        arango = new ArangoDB.Builder()
                .host(HOST, PORT)
                .user(USERNAME).password(PASSWORD)
                .build();

        try {
            arango.db(dbName).drop();
        }
        catch (final ArangoDBException e) {}

        arango.createDatabase(dbName);

        final Properties props = new Properties();
        props.put(ArangoDbInterpreter.ARANGODB_HOST, HOST);
        props.put(ArangoDbInterpreter.ARANGODB_PORT, "" + PORT);
        props.put(ArangoDbInterpreter.ARANGODB_DATABASE, dbName);
        props.put(ArangoDbInterpreter.ARANGODB_USER, USERNAME);
        props.put(ArangoDbInterpreter.ARANGODB_PWD, PASSWORD);
        interpreter = new ArangoDbInterpreter(props);
        interpreter.open();
    }

    protected static ArangoDatabase db() {
        return arango.db(dbName);
    }

    @AfterClass
    public static void clean() {
        if (arango != null) {
            try {
                arango.db(dbName).drop();
            }
            catch (final ArangoDBException e) {}

            arango.shutdown();
        }

        if (interpreter != null) {
            interpreter.close();
        }
    }
}
