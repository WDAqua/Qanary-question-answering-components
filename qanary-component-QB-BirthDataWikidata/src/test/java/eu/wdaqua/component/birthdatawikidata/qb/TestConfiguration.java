package eu.wdaqua.component.birthdatawikidata.qb;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Collectors;

import eu.wdaqua.component.qb.birthdata.wikidata.BirthDataQueryBuilder;

@org.springframework.boot.test.context.TestConfiguration
public class TestConfiguration {

    protected static final String FILENAME_ANNOTATIONS = BirthDataQueryBuilder.FILENAME_ANNOTATIONS;
    protected static final String FILENAME_ANNOTATIONS_NAMED_ENTITY_FILTERED_FOR_WIKIDATA = BirthDataQueryBuilder.FILENAME_ANNOTATIONS_NAMED_ENTITY_FILTERED_FOR_WIKIDATA; 
    protected static final String FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON = BirthDataQueryBuilder.FILENAME_WIKIDATA_BIRTHDATA_QUERY_PERSON;
    protected static final String FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME = BirthDataQueryBuilder.FILENAME_WIKIDATA_BIRTHDATA_QUERY_FIRST_AND_LASTNAME;

    /**
     * get the defined SPARQL query and remove all control characters (like newline)
     *
     * @return
     * @throws IOException
     */
    protected static String getTestQuery(String testQueryFilename) throws IOException {
//        String path = TestConfiguration.class.getClassLoader().getResource(testQueryFilename).getPath();
//        File file = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + testQueryFilename);
//        return new String(Files.readAllBytes(file.toPath()));

        try (InputStream inputStream = TestConfiguration.class.getClassLoader().getResourceAsStream(testQueryFilename)) {
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                return reader.lines().collect(Collectors.joining("\n"));
            } else {
                throw new IOException("Resource not found: " + testQueryFilename);
            }
        }
    }
}