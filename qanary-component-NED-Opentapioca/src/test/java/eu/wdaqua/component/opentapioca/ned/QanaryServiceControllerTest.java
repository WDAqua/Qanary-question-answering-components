package eu.wdaqua.component.opentapioca.ned;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.inGraphKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.outGraphKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;

@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class QanaryServiceControllerTest {
    @SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(QanaryServiceControllerTest.class);
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext applicationContext;

    /**
     * initialize local controller enabled for tests
     */
    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
    }

    /**
     * test description interface
     *
     * @throws Exception
     */
    @Test
    void testDescriptionAvailable() throws Exception {
        mockMvc.perform(get(QanaryConfiguration.description)) // fetch
                .andExpect(status().isOk()) // HTTP 200
                .andReturn(); //
    }

    /**
     * test correct message format
     */
    @Test
    void testMessageFromJson() {
        // create message from json string
        QanaryMessage message;
        try {
            message = new QanaryMessage(new URI(endpointKey), new URI(inGraphKey), new URI(outGraphKey));

            URI endpointKeyUrlFromMessage = message.getEndpoint();
            assertNotNull(endpointKeyUrlFromMessage);

            URI endpointKeyUrlFromHere = new URI(endpointKey);

            // TODO: more tests to ensure mechanism
            assertEquals(endpointKeyUrlFromMessage.toString(), endpointKeyUrlFromHere.toString());

        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

}