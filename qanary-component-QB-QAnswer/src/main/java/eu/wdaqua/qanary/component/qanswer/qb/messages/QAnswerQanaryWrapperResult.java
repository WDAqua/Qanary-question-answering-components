package eu.wdaqua.qanary.component.qanswer.qb.messages;

public class QAnswerQanaryWrapperResult {

    private final QAnswerResult result;
    private final String sparqlQuery;

    public QAnswerQanaryWrapperResult(QAnswerResult result, String sparqQuery) {
        this.result = result;
        this.sparqlQuery = sparqQuery;
    }

    public QAnswerResult getResult() {
        return result;
    }

    public String getSparqlQuery() {
        return sparqlQuery;
    }
}
