package eu.wdaqua.qanary.component.annotationofspotclass.qb;

import eu.wdaqua.qanary.component.QanaryServiceController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class AnnotationOfSpotClassTest {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationOfSpotClassTest.class);

    @Inject
    QanaryServiceController controller;
    @Value("${annotationOfSpotClass.url}")
    String url;

    private MockMvc mockMvc;

    @Test
    void testRunCurlPostWithParam() throws Exception {
        String contentType = "application/json";

        String output = AnnotationofSpotClass.runCurlPOSTWithParam(url, "{  \"string\": \"Which river flows through Seoul?\",  \"language\": \"en\"}", contentType);

        assertNotNull(output);

        String expected = "[{\"query\":\"SELECT ?v2 WHERE { ?v2 ?v8 ?v7 ; ?v9 ?v5 . } \",\"slots\":[{\"s\":\"v5\",\"p\":\"is\",\"o\":\"rdf:Class|rdf:Resource\"},{\"s\":\"v5\",\"p\":\"verbalization\",\"o\":\"Seoul\"},{\"s\":\"v8\",\"p\":\"is\",\"o\":\"<http://lodqa.org/vocabulary/sort_of>\"},{\"s\":\"v9\",\"p\":\"is\",\"o\":\"rdf:Property\"},{\"s\":\"v9\",\"p\":\"verbalization\",\"o\":\"flows\"},{\"s\":\"v7\",\"p\":\"is\",\"o\":\"rdf:Class\"},{\"s\":\"v7\",\"p\":\"verbalization\",\"o\":\"river\"}],\"score\":\"1.0\"}]";
        assertEquals(expected, output);
    }
}
