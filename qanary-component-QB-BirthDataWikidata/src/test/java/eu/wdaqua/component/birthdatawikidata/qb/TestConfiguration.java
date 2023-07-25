package eu.wdaqua.component.birthdatawikidata.qb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@org.springframework.boot.test.context.TestConfiguration
public class TestConfiguration {
    protected static final String FILENAME_ANNOTATIONS = "/queries/getAnnotation.rq";
    protected static final String FILENAME_ANNOTATIONS_FILTERED = "/queries/getAnnotationFiltered.rq";
    protected static final String FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON = "/queries/getQuestionAnswerFromWikidataByPerson.rq";
    protected static final String FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME = "/queries/getQuestionAnswerFromWikidataByFirstnameLastname.rq";

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