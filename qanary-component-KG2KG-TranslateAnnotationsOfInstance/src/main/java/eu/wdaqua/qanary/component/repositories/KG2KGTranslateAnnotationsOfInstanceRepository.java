package eu.wdaqua.qanary.component.repositories;

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

    public KG2KGTranslateAnnotationsOfInstanceRepository() {

    }

    public RDFNode fetchEquivalentResource(String executableQuery) throws RuntimeException {
        RDFConnection conn = RDFConnection.connect("http://dbpedia.org/sparql");
        QueryExecution queryExecution = conn.query(executableQuery);
        ResultSet resultSet = queryExecution.execSelect();
        if (resultSet.hasNext()) {
            QuerySolution querySolution = resultSet.next();
            logger.info(querySolution.get("resource").toString());
            return querySolution.get("resource");
        } else
            throw new RuntimeException();
    }
}
