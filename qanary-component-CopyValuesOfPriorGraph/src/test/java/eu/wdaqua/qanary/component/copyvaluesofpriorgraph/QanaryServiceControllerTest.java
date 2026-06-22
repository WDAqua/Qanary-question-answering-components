package eu.wdaqua.qanary.component.copyvaluesofpriorgraph;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.description;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Full Spring-context test: boots the component and verifies the Qanary service
 * description endpoint provided by the qa.component framework is served. The SBA
 * client failing to reach a (non-running) pipeline is logged but non-fatal.
 */
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class QanaryServiceControllerTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
    }

    @Test
    void testDescriptionAvailable() throws Exception {
        mockMvc.perform(get(description)).andExpect(status().isOk());
    }
}
