package eu.wdaqua.qanary.component.repositories;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class KG2KGTranslateAnnotationsOfInstanceRepository {

    private final Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstanceRepository.class);
    private String dbpediaSparqlEndpoint = "http://dbpedia.org/sparql";
    private RDFConnection rdfConnection;

    public KG2KGTranslateAnnotationsOfInstanceRepository() {
        this.rdfConnection = RDFConnection.connect(dbpediaSparqlEndpoint);
    }

    public void setRdfConnection(Dataset dataset) {
        this.rdfConnection = RDFConnection.connect(dataset);
    }

    public RDFNode fetchEquivalentResource(String executableQuery) throws RuntimeException {
        QueryExecution queryExecution = rdfConnection.query(executableQuery);
        ResultSet resultSet = queryExecution.execSelect();
        if (resultSet.hasNext()) {
            QuerySolution querySolution = resultSet.next();
            logger.info(querySolution.get("resource").toString());
            return querySolution.get("resource");
        } else
            throw new RuntimeException();
    }
}
