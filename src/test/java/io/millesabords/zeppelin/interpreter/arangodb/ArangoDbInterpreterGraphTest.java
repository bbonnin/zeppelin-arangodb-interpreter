package io.millesabords.zeppelin.interpreter.arangodb;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;
import com.arangodb.entity.BaseEdgeDocument;
import com.arangodb.entity.CollectionType;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.CollectionCreateOptions;

/**
 * Tests for graph queries.
 *
 * The data are coming from : https://docs.arangodb.com/3.1/cookbook/Graph/ExampleActorsAndMovies.html
 *
 * @author Bruno Bonnin
 *
 */
public class ArangoDbInterpreterGraphTest extends AbstractArangoDbInterpreterTest {

    private static final String DB_NAME = "cinema";

    private static Object createMovie(String key, String title, int released, String tagline) {
        final BaseDocument doc = new BaseDocument(key);
        doc.addAttribute("title", title);
        doc.addAttribute("released", released);
        doc.addAttribute("tagline", tagline);
        return doc;
    }

    private static Object createActor(String key, String name, int born) {
        final BaseDocument doc = new BaseDocument(key);
        doc.addAttribute("name", name);
        doc.addAttribute("born", born);
        return doc;
    }

    private static Object createActsIn(String actor, String movie, int year, String... roles) {
        final BaseEdgeDocument doc = new BaseEdgeDocument();
        doc.setFrom(actor);
        doc.setTo(movie);
        doc.addAttribute("roles", roles);
        doc.addAttribute("year", year);
        return doc;
    }

    @BeforeClass
    public static void populate() throws ArangoDBException, IOException {
        init(DB_NAME);

        db().createCollection("actors");
        db().createCollection("movies");
        db().createCollection("actsIn", new CollectionCreateOptions().type(CollectionType.EDGES));

        final Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
        final EdgeDefinition edgeDefinition = new EdgeDefinition()
                .collection("actsIn")
                .from("actors")
                .to("movies");
        edgeDefinitions.add(edgeDefinition);
        db().createGraph("moviesGraph", edgeDefinitions, null);

        final String theMatrix = db().collection("movies").insertDocument(
                createMovie("TheMatrix", "The Matrix", 1999, "Welcome to the Real World")).getId();
        final String theDevilsAdvocate = db().collection("movies").insertDocument(
                createMovie("TheDevilsAdvocate", "The Devil's Advocate", 1997, "Evil has its winning ways")).getId();

        final String keanu = db().collection("actors").insertDocument(
                createActor("Keanu", "Keanu Reeves", 1964)).getId();
        final String carrie = db().collection("actors").insertDocument(
                createActor("Carrie", "Carrie-Anne Moss", 1967)).getId();

        db().collection("actsIn").insertDocument(
                createActsIn(keanu, theMatrix, 1999, "Neo"));
        db().collection("actsIn").insertDocument(
                createActsIn(keanu, theDevilsAdvocate, 1997, "Kevin Lomax"));
        db().collection("actsIn").insertDocument(
                createActsIn(carrie, theMatrix, 1999, "Trinity"));
    }

    @Test
    public void testGraphQueries() {
        InterpreterResult res = interpreter.interpret("FOR x IN actsIn COLLECT actor = x._from WITH COUNT INTO counter FILTER counter >= 2 RETURN {actor: actor, movies: counter}", null);
        assertEquals(Code.SUCCESS, res.code());

        res = interpreter.interpret("FOR x IN actsIn COLLECT movie = x._to WITH COUNT INTO counter FILTER counter == 1 RETURN movie", null);
        assertEquals(Code.SUCCESS, res.code());

        res = interpreter.interpret("FOR x IN actsIn COLLECT movie = x._to WITH COUNT INTO counter RETURN {movie: movie, actors: counter}", null);
        assertEquals(Code.SUCCESS, res.code());

        //res = interpreter.interpret("FOR x IN ANY 'movies/TheMatrix' actsIn OPTIONS {bfs: true, uniqueVertices: 'global'} RETURN x._id", null);
        res = interpreter.interpret("FOR x IN UNION_DISTINCT ("
            + "(FOR y IN ANY 'movies/TheMatrix' actsIn OPTIONS {bfs: true, uniqueVertices: 'global'} RETURN y._id), "
            + "(FOR y IN ANY 'movies/TheDevilsAdvocate' actsIn OPTIONS {bfs: true, uniqueVertices: 'global'} RETURN y._id)"
            + ") RETURN x", null);
        assertEquals(Code.SUCCESS, res.code());

        res = interpreter.interpret("FOR x IN INTERSECTION ("
            + "(FOR y IN ANY 'movies/TheMatrix' actsIn OPTIONS {bfs: true, uniqueVertices: 'global'} RETURN y._id), "
            + "(FOR y IN ANY 'movies/TheDevilsAdvocate' actsIn OPTIONS {bfs: true, uniqueVertices: 'global'} RETURN y._id)) RETURN x", null);
        System.out.println(res.message());
        assertEquals(Code.SUCCESS, res.code());
    }

}
