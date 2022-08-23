package eu.wdaqua.qanary.component.qanswer.qb;

import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerRequest;
import eu.wdaqua.qanary.component.qanswer.qb.messages.QAnswerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * test the custom endpoint of this component
 *
 * @author anbo
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class QanaryCustomControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(QanaryCustomControllerTest.class);
    @Inject
    QAnswerQueryBuilderAndSparqlResultFetcherController customController;
    @Autowired
    private Environment env;
    private MockMvc mockMvcCustom;

    private URI realEndpoint;

    /**
     * initialize local controller enabled for tests
     *
     * @throws Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/jsp/view/");
        viewResolver.setSuffix(".jsp");

        mockMvcCustom = MockMvcBuilders.standaloneSetup(customController).setViewResolvers(viewResolver).build();

        realEndpoint = new URI(env.getProperty("qanswer.endpoint.url"));
        assert (realEndpoint != null) : "qanswer.endpoint.url cannot be empty";
    }

    @Test
    void testDemoEndpoint() throws MalformedURLException, URISyntaxException {
        String question = "What is the capital of Spain";
        String lang = "en";
        String kb = "wikidata";

        QAnswerResult result0 = customController.requestQAnswerWebService(this.realEndpoint, question, lang, kb);
        assertTrue(result0.getValues().size() > 0, "the number of fetched results should be > 0, but  was " + result0.getValues().size());
        assertTrue(result0.getValues().size() <= 60, "the number of fetched results should be <= 60, but  was " + result0.getValues().size());

        QAnswerRequest requestMessage = new QAnswerRequest(question, lang, kb);

        MvcResult res;
        try {
            res = mockMvcCustom.perform( //
                            post(QAnswerQueryBuilderAndSparqlResultFetcherController.DEMO) //
                                    .content(requestMessage.asJsonString()) //
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.endpoint").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.knowledgebaseId").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.language").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.question").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.result.values").exists()) //
                    .andReturn();

        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }


        // TODO: check the content of the response fields being equal to the previous result
        try {
            res = mockMvcCustom.perform( //
                            post(QAnswerQueryBuilderAndSparqlResultFetcherController.API) //
                                    .content(requestMessage.asJsonString()) //
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.endpoint").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.knowledgebaseId").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.language").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.question").exists()) //
                    .andExpect(MockMvcResultMatchers.jsonPath("$.values").exists()) //
                    .andReturn();
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }


    }

}