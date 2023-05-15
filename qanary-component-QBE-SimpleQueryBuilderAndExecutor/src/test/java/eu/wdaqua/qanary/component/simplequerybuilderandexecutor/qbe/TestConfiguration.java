package eu.wdaqua.qanary.component.simplequerybuilderandexecutor.qbe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@org.springframework.boot.test.context.TestConfiguration
public class TestConfiguration {
    protected static final String FILENAME_SELECT_CLASSES = "/queries/select_classes.rq";
    protected static final String FILENAME_SELECT_PROPERTIES = "/queries/select_properties.rq";
    protected static final String FILENAME_SELECT_ENTITIES = "/queries/select_entities.rq";
    protected static final String FILENAME_INSERT_SPARQL = "/queries/insert_sparql.rq";
    protected static final String FILENAME_INSERT_JSON = "/queries/insert_json.rq";

    /**
     * get the defined SPARQL query and remove all control characters (like newline)
     *
     * @return
     * @throws IOException
     */
    protected static String getTestQuery(String testQueryFilename) throws IOException {
        String path = TestConfiguration.class.getClassLoader().getResource(testQueryFilename).getPath();

        return new String(Files.readAllBytes(Paths.get(path)));
    }

}