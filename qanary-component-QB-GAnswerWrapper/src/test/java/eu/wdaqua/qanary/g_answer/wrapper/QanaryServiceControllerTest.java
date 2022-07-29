package eu.wdaqua.qanary.g_answer.wrapper;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.config.QanaryConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
class QanaryServiceControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(QanaryServiceControllerTest.class);

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    /**
     * initialize local controller enabled for tests
     *
     * @throws Exception
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
     * send and receive message a JSON message to
     * QanaryConfiguration.annotatequestion, check if the values are the same
     */
    @Test
    @Disabled
    // TODO this test cannot be executed as the triplestore needs to be mocked first
    void testMessageReceiveAndSend() {

        QanaryMessage requestMessage;
        try {
            requestMessage = new QanaryMessage(new URI(endpointKey), new URI(inGraphKey), new URI(outGraphKey));
            logger.info("Message {}" + requestMessage);
        } catch (URISyntaxException e) {
            fail(e.getMessage());
            return;
        }

        // check the response
        MvcResult res;
        try {
            res = mockMvc.perform( //
                            post(QanaryConfiguration.annotatequestion) //
                                    .content(requestMessage.asJsonString()) //
                                    .contentType(MediaType.APPLICATION_JSON))
                    // .andExpect(status().is2xxSuccessful()) //
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) //
                    .andReturn();
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }

        // check the values of all messages, should be equal if dummy
        // implementation is used
        QanaryMessage resultMessage;
        try {
            resultMessage = new QanaryMessage(res.getResponse().getContentAsString());
        } catch (Exception e) {
            fail(e.getMessage());
            return;
        }

        for (Entry<URI, URI> entry : requestMessage.getValues().entrySet()) {
            URI key = entry.getKey();
//			int compareResult = entry.getValue().toString().compareTo(resultMessage.getValues().get(key).toString());
//			assertTrue("check result vs. request: " + key, compareResult == 0);
            assertEquals(entry.getValue().toString(), resultMessage.getValues().get(key).toString(), "check result vs. request: " + key);
        }

    }

    /**
     * test correct message format
     */
    @Test
    void testMessageFromJson() {
        // create message from JSON string
        QanaryMessage message;
        try {
            message = new QanaryMessage(new URI(endpointKey), new URI(inGraphKey), new URI(outGraphKey));

            URI endpointKeyUrlFromMessage = message.getEndpoint();
            assertNotNull(endpointKeyUrlFromMessage);

            URI endpointKeyUrlFromHere = new URI(endpointKey);

            // TODO: more tests to ensure mechanism
            //assertTrue(endpointKeyUrlFromHere.toString().compareTo(endpointKeyUrlFromMessage.toString()) == 0);
            assertEquals(endpointKeyUrlFromHere.toString(), endpointKeyUrlFromMessage.toString());

        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

}