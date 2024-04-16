package eu.wdaqua.qanary.component.qanswer.qb.messages;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.swagger.v3.oas.annotations.Hidden;

public class QAnswerResult {
    private static final Logger logger = LoggerFactory.getLogger(QAnswerResult.class);
    @Hidden
    private com.google.gson.JsonParser jsonParser;
    private URI endpoint;
    private String knowledgebaseId;
    private String user;
    private String language;
    private String question;
    private List<QAnswerQueryCandidate> queryCandidates;

    public QAnswerResult(JsonObject json, String question, URI endpoint, String language, String knowledgebaseId, String user)
            throws URISyntaxException {

        this.question = question;
        this.language = language;
        this.knowledgebaseId = knowledgebaseId;
        this.user = user;
        this.endpoint = endpoint;
        this.queryCandidates = new LinkedList<QAnswerQueryCandidate>();

        initData(json);
    }

    /**
     * init the fields while parsing the JSON data
     *
     * @param parsedJsonObject
     * @throws URISyntaxException
     */
    private void initData(JsonObject parsedJsonObject) throws URISyntaxException {

        JsonArray queryCandidatesArray = parsedJsonObject.getAsJsonArray("queries").getAsJsonArray();

        for (JsonElement queryCandidate : queryCandidatesArray) {
            JsonObject queryCandidateObject = queryCandidate.getAsJsonObject(); 
            String query = queryCandidateObject.get("query").getAsString();
            float score = queryCandidateObject.get("confidence").getAsFloat();
            QAnswerQueryCandidate candidate = new QAnswerQueryCandidate(query, score);
            queryCandidates.add(candidate);
        }

        logger.debug("fetched {} query candidates", this.queryCandidates.size());
    }

    public List<QAnswerQueryCandidate> getQueryCandidates() {
        return queryCandidates;
    }

    public String getKnowledgebaseId() {
        return knowledgebaseId;
    }

    public String getUser() {
        return user;
    }

    public String getLanguage() {
        return language;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public String getQuestion() {
        return question;
    }

    public class QAnswerQueryCandidate {
        private String query;
        private float score;

        public QAnswerQueryCandidate(String query, float score) {
            this.query = query;
            this.score = score;
        }

        public String getQueryString() {
            return this.query;
        }

        public float getScore() {
            return this.score;
        }
    }

}
