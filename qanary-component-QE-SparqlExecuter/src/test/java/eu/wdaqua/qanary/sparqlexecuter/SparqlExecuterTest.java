package eu.wdaqua.qanary.sparqlexecuter;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
class SparqlExecuterTest {

    private static final Logger logger = LoggerFactory.getLogger(SparqlExecuterTest.class);

    @Value("${knowledgegraph.endpoint.wikidata}")
    private String knowledgegraphEndpointWikidata;

    String knownValidResponseBody = "{ \"head\": { \"vars\": [ \"Who\" ] } , \"results\": { \"bindings\": [ { \"Who\": { \"type\": \"uri\" , \"value\": \"http://dbpedia.org/resource/Rembrandt\" } } ] } }";
    String knownValidSelectQuery = "SELECT DISTINCT ?o1 WHERE {  <http://www.wikidata.org/entity/Q183>  <http://www.wikidata.org/prop/direct/P36>  ?o1 .  }  LIMIT 1000";

    private SparqlExecuter mockedSparqlExecuter;
    private QanaryQuestion mockedQanaryQuestion;
    private QanaryUtils mockedQanaryUtils;
    private QanaryTripleStoreConnector mockedQanaryTriplestoreConnector;
    private RDFNode mockedRDFNode;

    @BeforeEach
    public void init() throws QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed, IOException, eu.wdaqua.qanary.sparqlexecuter.exception.SparqlQueryFailed {
        this.mockedSparqlExecuter = Mockito.mock(SparqlExecuter.class);
        this.mockedQanaryQuestion = Mockito.mock(QanaryQuestion.class);
        this.mockedQanaryUtils = Mockito.mock(QanaryUtils.class);
        this.mockedQanaryTriplestoreConnector = Mockito.mock(QanaryTripleStoreConnector.class);
        Mockito.when(this.mockedQanaryQuestion.getOutGraph()).thenReturn(new URI(endpointKey));
        Mockito.when(this.mockedQanaryQuestion.getUri()).thenReturn(new URI("targetquestion"));
        Mockito.when(this.mockedSparqlExecuter.getSparqlInsertQuery(any(String.class), any(QanaryQuestion.class))).thenCallRealMethod();
        Mockito.when(this.mockedQanaryUtils.getQanaryTripleStoreConnector()).thenReturn(this.mockedQanaryTriplestoreConnector);
        Mockito.when(this.mockedSparqlExecuter.getResultSparqlQuery(any(QanaryUtils.class), any(QanaryQuestion.class))).thenCallRealMethod();
        Mockito.when(this.mockedSparqlExecuter.getQueryResultsAsJson(any(String.class), any(String.class))).thenCallRealMethod();

        ResultSet mockedResultSet = Mockito.mock(ResultSet.class);
        QuerySolution mockedQuerySolution = Mockito.mock(QuerySolution.class);

        this.mockedRDFNode = Mockito.mock(RDFNode.class);
        Mockito.when(mockedResultSet.hasNext()).thenReturn(true).thenReturn(false); // return only one result
        Mockito.when(mockedResultSet.next()).thenReturn(mockedQuerySolution); Mockito.when(mockedQuerySolution.get(any(String.class))).thenReturn(mockedRDFNode);

        Mockito.when(this.mockedQanaryTriplestoreConnector.select(any(String.class)))
            .thenReturn(mockedResultSet);

    }

    @Test
    void testGetSparqlInsertQuery() throws IOException, QanaryExceptionNoOrMultipleQuestions, URISyntaxException, SparqlQueryFailed {
        String json = knownValidResponseBody;
        String sparql = this.mockedSparqlExecuter.getSparqlInsertQuery(json, this.mockedQanaryQuestion);
        assertNotNull(sparql); 
        assertNotEquals(0, sparql.length());
    }

    @Test
    void testGetResultSparqlQuery() throws SparqlQueryFailed, IOException {
        // This test exists only to ensure that SparqlExecuter.getResultSparqlQuery() 
        // can handle the return type org.apache.jena.query.ResultSet.
        //
        // The actual query is not relevant!


        Mockito.when(mockedRDFNode.toString()).thenReturn(this.knownValidSelectQuery);


        String resultSparqlQuery = this.mockedSparqlExecuter.getResultSparqlQuery(
                this.mockedQanaryUtils, this.mockedQanaryQuestion);

        assertNotNull(resultSparqlQuery);
        assertNotEquals(0, resultSparqlQuery.length());
    }

    @Test
    void testGetQueryResultsAsJson() throws eu.wdaqua.qanary.sparqlexecuter.exception.SparqlQueryFailed, UnsupportedEncodingException, URISyntaxException {
        
        String jsonString = this.mockedSparqlExecuter.getQueryResultsAsJson(
                this.knownValidSelectQuery, this.knowledgegraphEndpointWikidata);
        assertNotNull(jsonString);
        assertNotEquals(0, jsonString.length());

    }

}
