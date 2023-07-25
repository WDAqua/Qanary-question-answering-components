package eu.wdaqua.qanary.component.comiccharacteralteregoaimpledbpedia.qb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@org.springframework.boot.test.context.TestConfiguration
public class TestConfiguration {
    protected static final String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";
    protected static final String FILENAME_SELECT_ANNOTATION = "/queries/select_annotation.rq";
    protected static final String FILENAME_DBPEDIA_QUERY = "/queries/dbpedia_query.rq";

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