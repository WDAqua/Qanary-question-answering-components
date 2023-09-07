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

import java.util.ArrayList;
import java.util.List;

@Repository
public class KG2KGTranslateAnnotationsOfInstanceRepository {

    private final static String dbpediaSparqlEndpoint = "http://dbpedia.org/sparql";
    private final Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstanceRepository.class);
    private RDFConnection rdfConnection;

    public KG2KGTranslateAnnotationsOfInstanceRepository() {
        this.rdfConnection = RDFConnection.connect(dbpediaSparqlEndpoint);
    }

    public void setRdfConnection(Dataset dataset) {
        this.rdfConnection = RDFConnection.connect(dataset);
    }

    public List<RDFNode> fetchEquivalentResource(String executableQuery) throws RuntimeException {
        QueryExecution queryExecution = rdfConnection.query(executableQuery);
        List<RDFNode> equivalentResources = new ArrayList<>();
        ResultSet resultSet = queryExecution.execSelect();

        if (!resultSet.hasNext()) {
            throw new RuntimeException("No resource found");
        }
        while (resultSet.hasNext()) {
            QuerySolution querySolution = resultSet.next();
            RDFNode newResource = querySolution.getResource("resource");
            equivalentResources.add(newResource);
        }
        return equivalentResources;
    }

}
