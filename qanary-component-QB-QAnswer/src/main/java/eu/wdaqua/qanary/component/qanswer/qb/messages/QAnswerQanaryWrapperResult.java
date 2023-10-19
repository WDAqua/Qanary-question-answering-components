package eu.wdaqua.qanary.component.qanswer.qb.messages;

import java.util.List;

public class QAnswerQanaryWrapperResult {

    private final QAnswerResult result;
    private final String sparqlImprovedQuestion;
    private final List<String> sparqlQueryCandidates;

    public QAnswerQanaryWrapperResult(QAnswerResult result, String sparqlImprovedQuestion, List<String> sparqlQueryCandidates) {
        this.result = result;
        this.sparqlImprovedQuestion = sparqlImprovedQuestion;
        this.sparqlQueryCandidates = sparqlQueryCandidates;
    }

    public QAnswerResult getResult() {
        return result;
    }

    public String getSparqlImprovedQuestion() {
        return sparqlImprovedQuestion;
    }

    public List<String> getSparqlQueryCandidates() {
        return sparqlQueryCandidates;
    }
}
