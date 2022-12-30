package eu.wdaqua.qanary.component.qanswer.qb;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.component.QanaryComponentDescriptionController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.inject.Inject;
import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * test the standard description endpoint of Qanary components
 *
 * @author anbo
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class QanaryDescriptionControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(QanaryDescriptionControllerTest.class);
    @Inject
    QanaryComponentDescriptionController descriptionController;
    @Autowired
    private Environment env;
    private MockMvc mockMvcDescription;

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

        mockMvcDescription = MockMvcBuilders.standaloneSetup(descriptionController).setViewResolvers(viewResolver).build();

        realEndpoint = new URI(env.getProperty("qanswer.endpoint.url"));
        assert (realEndpoint != null) : "qanswer.endpoint.url cannot be empty";
    }

    /**
     * test description interface
     *
     * @throws Exception
     */
    @Test
    void testDescriptionAvailable() throws Exception {
        mockMvcDescription.perform(get(QanaryConfiguration.description)) // fetch
                .andExpect(status().isOk()) // HTTP 200
                .andReturn(); //
    }
}