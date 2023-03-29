package eu.wdaqua.qanary.component.chatgptwrapper.tqa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionResult;
import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.MyCompletionRequest;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.MyOpenAiApi;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.MissingTokenException;
import eu.wdaqua.qanary.component.chatgptwrapper.tqa.openai.api.exception.OpenApiUnreachableException;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class ChatGPTWrapper extends QanaryComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatGPTWrapper.class);
    private final String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_AnnotationOfAnswerJson.rq";
    private final String applicationName;
    private final RestTemplate myRestTemplate;
    private final CacheOfRestTemplateResponse myCacheOfResponses;

    private MyOpenAiApi openAiApi;
    private String model;

    public ChatGPTWrapper(
            @Value("${spring.application.name}") String applicationName,
            @Value("${tqa.chatgptwrapper.api.key}") String token,
            @Value("${chatgpt.api.live.test}") boolean doApiIsAliveCheck,
            @Value("${chatgpt.model}") String model,
            RestTemplate restTemplate,
            CacheOfRestTemplateResponse myCacheOfResponses
    ) throws MissingTokenException, URISyntaxException, OpenApiUnreachableException {

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);

        this.applicationName = applicationName;
        this.myRestTemplate = restTemplate;
        this.myCacheOfResponses = myCacheOfResponses;

        this.openAiApi = new MyOpenAiApi(token, doApiIsAliveCheck);
        this.model = model;
    }

    /**
     * implement this method encapsulating the functionality of your Qanary
     * component
     *
     * @throws Exception
     */
    @Override
    public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

        // STEP 1: get the required data from the Qanary triplestore (the global process memory)
        LOGGER.info("process: {}", myQanaryMessage);
        QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
        QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
        String myQuestion = myQanaryQuestion.getTextualRepresentation();
        LOGGER.info("Question: {}", myQuestion);

        // STEP 2: enriching of query and fetching data from the ChatGPT API

        MyCompletionRequest completionRequest = new MyCompletionRequest();
        completionRequest.setModel(model);
        completionRequest.setPrompt(myQuestion);

        CompletionResult completionResult = openAiApi.createCompletion(
                myRestTemplate,
                myCacheOfResponses,
                completionRequest
        );

        // STEP 3: Push the SPARQL query to the triplestore

        String sparql = createInsertQuery(myQanaryQuestion, completionResult);
        LOGGER.info("SPARQL insert for adding data to Qanary triplestore: {}", sparql);
        myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

        return myQanaryMessage;
    }

    public JsonObject creatJsonAnswer(CompletionResult completionResult){
        JsonObject jsonAnswer = new JsonObject();
        JsonArray choices = new JsonArray();

        for(CompletionChoice choice : completionResult.getChoices()) {
            JsonObject choiceJson = new JsonObject();

            if (choice.getText() != null) {
                choiceJson.addProperty("text", choice.getText());
            }
            if (choice.getIndex() != null) {
                choiceJson.addProperty("index", choice.getIndex());
            }
            if (choice.getLogprobs() != null) {
                choiceJson.addProperty("logprobs", choice.getLogprobs().toString());
            }
            if (choice.getFinish_reason() != null) {
                choiceJson.addProperty("finish_reason", choice.getFinish_reason());
            }

            choices.add(choiceJson);
        }

        jsonAnswer.add("choices", choices);

        return jsonAnswer;
    }

    public String createInsertQuery(
            QanaryQuestion<String> myQanaryQuestion,
            CompletionResult completionResult
    ) throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        // use here the variable names defined in method insertAnnotationOfAnswerSPARQL
        bindings.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
        bindings.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
        bindings.add("jsonAnswer", ResourceFactory.createStringLiteral(creatJsonAnswer(completionResult).toString()));
        bindings.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

        // get the template of the INSERT query
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_INSERT_ANNOTATION, bindings);
    }

}
