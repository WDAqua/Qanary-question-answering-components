package eu.wdaqua.qanary.component.chatgptwrapper.tqa;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import eu.wdaqua.qanary.component.chatgptwrapper.tqa.controller.QuestionRestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
@TestPropertySource(properties = { //
		"CHATGPT.API.KEY=DUMMY-KEY-TO-ALLOW-COMPONENT-TO-START-AND-MOCK-TEST-TO-BE-EXECUTED", //
		"CHATGPT.API.LIVE.TEST.ACTIVE=false" //
})
class ChatGPTRestControllerTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    /**
     * initialize local application for tests
     */
    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext).build();
    }


    @Test
    void testGetResponse() throws Exception {
        mockMvc.perform(post("/"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @EnabledIf(
            expression = "#{environment['chatgpt.api.live.test.active'] == 'true'}", //
            loadContext = true
    )
    void testPostResponse() throws Exception {
        this.mockMvc.perform(post(QuestionRestController.ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"this is a test\"}"))
                .andExpect(status().isOk());

    }

    @Test
    void testPostResponseWithoutQuestion() throws Exception {
        this.mockMvc.perform(post(QuestionRestController.ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPutResponse() throws Exception {
        mockMvc.perform(put(QuestionRestController.ENDPOINT))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testDeleteResponse() throws Exception {
        mockMvc.perform(delete(QuestionRestController.ENDPOINT))
                .andExpect(status().isMethodNotAllowed());
    }
}
