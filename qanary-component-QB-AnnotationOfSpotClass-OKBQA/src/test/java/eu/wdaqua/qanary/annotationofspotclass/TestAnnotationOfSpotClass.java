package eu.wdaqua.qanary.annotationofspotclass;

import eu.wdaqua.qanary.component.QanaryServiceController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TestAnnotationOfSpotClass {

    private static final Logger logger = LoggerFactory.getLogger(TestAnnotationOfSpotClass.class);

    @Inject
    QanaryServiceController controller;
    @Value("${annotationOfSpotClass.url}")
    String url;

    private MockMvc mockMvc;

    @Test
    public void testRunCurlPostWithParam() throws Exception {
        String contentType = "application/json";

        String output = AnnotationofSpotClass.runCurlPOSTWithParam(url, "{  \"string\": \"Which river flows through Seoul?\",  \"language\": \"en\"}", contentType);

        assertNotNull(output);

        String expected = "[{\"query\":\"SELECT ?v2 WHERE { ?v2 ?v8 ?v7 ; ?v9 ?v5 . } \",\"slots\":[{\"s\":\"v5\",\"p\":\"is\",\"o\":\"rdf:Class|rdf:Resource\"},{\"s\":\"v5\",\"p\":\"verbalization\",\"o\":\"Seoul\"},{\"s\":\"v8\",\"p\":\"is\",\"o\":\"<http://lodqa.org/vocabulary/sort_of>\"},{\"s\":\"v9\",\"p\":\"is\",\"o\":\"rdf:Property\"},{\"s\":\"v9\",\"p\":\"verbalization\",\"o\":\"flows\"},{\"s\":\"v7\",\"p\":\"is\",\"o\":\"rdf:Class\"},{\"s\":\"v7\",\"p\":\"verbalization\",\"o\":\"river\"}],\"score\":\"1.0\"}]";
        assertEquals(expected, output);
    }
}
