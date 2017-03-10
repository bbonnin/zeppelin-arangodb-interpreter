package io.millesabords.zeppelin.interpreter.arangodb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterContext;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.apache.zeppelin.interpreter.thrift.InterpreterCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * ArangoDB Interpreter for Zeppelin.
 *
 * @see https://docs.arangodb.com/Aql/index.html
 * @see https://www.arangodb.com/sql-aql-comparison/
 *
 * @author Bruno Bonnin
 */
public class ArangoDbInterpreter extends Interpreter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArangoDbInterpreter.class);

    private static final List<String> COMMANDS = Arrays.asList("FOR", "RETURN", "FILTER",
            "SORT", "LIMIT", "LET", "COLLECT", "INSERT", "UPDATE", "REPLACE", "REMOVE", "UPSERT");

   private static final List<InterpreterCompletion> SUGGESTIONS = new ArrayList<>();

    public static final String ARANGODB_HOST = "arangodb.host";
    public static final String ARANGODB_PORT = "arangodb.port";
    public static final String ARANGODB_DATABASE = "arangodb.database";
    public static final String ARANGODB_USER = "arangodb.user";
    public static final String ARANGODB_PWD = "arangodb.pwd";

    static {
        for (final String cmd: COMMANDS) {
            SUGGESTIONS.add(new InterpreterCompletion(cmd, cmd));
        }
    }


    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ArangoDB arango;

    public ArangoDbInterpreter(Properties property) {
        super(property);
    }

    @Override
    public void open() {
        try {
            final ArangoDB.Builder builder = new ArangoDB.Builder()
                    .host(getProperty(ARANGODB_HOST), Integer.parseInt(getProperty(ARANGODB_PORT)));

            if (!StringUtils.isEmpty(getProperty(ARANGODB_USER))) {
                builder.user(getProperty(ARANGODB_USER)).password(getProperty(ARANGODB_PWD));
            }

            arango = builder.build();
        }
        catch (final Exception e) {
            LOGGER.error("Open connection with ArangoDB", e);
        }
    }

    @Override
    public void close() {
        if (arango != null) {
            arango.shutdown();
        }
    }

    @Override
    public InterpreterResult interpret(String cmd, InterpreterContext interpreterContext) {
        LOGGER.info("Run AQL command '" + cmd + "'");

        try {
            return interpretAql(cmd);
        }
        catch (final ArangoDBException e) {
            return new InterpreterResult(InterpreterResult.Code.ERROR, "Error : " + e.getMessage());
        }
    }

    @Override
    public void cancel(InterpreterContext interpreterContext) {
        //Nothing to do
    }

    @Override
    public FormType getFormType() {
        return FormType.NATIVE;
    }

    @Override
    public int getProgress(InterpreterContext interpreterContext) {
        return 0;
    }

    @Override
    public List<InterpreterCompletion> completion(String s, int i) {

        final List<InterpreterCompletion> suggestions = new ArrayList<>();

        if (StringUtils.isEmpty(s)) {
            suggestions.addAll(SUGGESTIONS);
        }
        else {
            for (final String cmd : COMMANDS) {
                if (cmd.contains(s.toUpperCase())) {
                    suggestions.add(new InterpreterCompletion(cmd, cmd));
                }
            }
        }

        return suggestions;
    }

    /**
     * Interpret the AQL query.
     *
     * @param cmd Contains the AQL query
     * @return The result of the query
     * @throws ArangoDBException Raised by the Arango driver in case of problem
     */
    private InterpreterResult interpretAql(String cmd) throws ArangoDBException {

        final ArangoCursor<Object> cursor = db().query(cmd, null, null, Object.class);
        final List<Map<String, Object>> flattenDocs = new LinkedList<>();
        final Set<String> keys = new TreeSet<>();

        // First : get all the keys in order to build an ordered list of the values for each doc
        //
        while (cursor.hasNext()) {
            final Object item = cursor.next();
            Map<String, Object> flattenMap;

            if (item instanceof Map) {
                flattenMap = JsonFlattener.flattenAsMap(gson.toJson(item));
            }
            else {
                flattenMap = new HashMap<>();
                flattenMap.put("value", item);
            }

            flattenDocs.add(flattenMap);
            for (final String key : flattenMap.keySet()) {
                keys.add(key);
            }
        }

        // Next : build the header of the table
        //
        final StringBuffer buffer = new StringBuffer();
        for (final String key : keys) {
            buffer.append(key).append('\t');
        }

        if (buffer.length() > 0) {
            buffer.replace(buffer.lastIndexOf("\t"), buffer.lastIndexOf("\t") + 1, "\n");
        }
        else {
            return new InterpreterResult(Code.SUCCESS, InterpreterResult.Type.TEXT, "Empty result");
        }

        // Finally : build the result by using the key set
        //
        for (final Map<String, Object> hit : flattenDocs) {
            for (final String key : keys) {
                final Object val = hit.get(key);
                if (val != null) {
                    buffer.append(val);
                }
                buffer.append('\t');
            }
            buffer.replace(buffer.lastIndexOf("\t"), buffer.lastIndexOf("\t") + 1, "\n");
        }

        return new InterpreterResult(Code.SUCCESS, InterpreterResult.Type.TABLE, buffer.toString());
    }

    private ArangoDatabase db() {
        return arango.db(getProperty(ARANGODB_DATABASE));
    }

}
