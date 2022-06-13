package eu.wdaqua.qanary.component.qanswer.qb;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import eu.wdaqua.qanary.component.QanaryComponentDescriptionController;

/**
 * test the standard description endpoint of Qanary components
 *
 * @author anbo
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class TestQanaryDescriptionController {

    private static final Logger logger = LoggerFactory.getLogger(TestQanaryDescriptionController.class);
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
    @Before
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
    public void testDescriptionAvailable() throws Exception {
        mockMvcDescription.perform(get(QanaryConfiguration.description)) // fetch
                .andExpect(status().isOk()) // HTTP 200
                .andReturn(); //
    }
}