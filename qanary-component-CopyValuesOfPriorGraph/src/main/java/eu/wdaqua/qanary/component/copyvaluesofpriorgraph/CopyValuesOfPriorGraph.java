package eu.wdaqua.qanary.component.copyvaluesofpriorgraph;

import java.io.IOException;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class CopyValuesOfPriorGraph extends QanaryComponent {

	private static final String FILENAME_FETCH_REQUIRED_ANNOTATIONS = "/queries/fetchRequiredAnnotations.rq";
	private static final String FILENAME_ADD_DATA_TO_CURRENT_GRAPH = "/queries/addDataToCurrentGraph.rq";
	private static final String FILENAME_STORE_COMPUTED_ANNOTATIONS = "/queries/storeComputedAnnotations.rq";
	
	private static final Logger logger = LoggerFactory.getLogger(CopyValuesOfPriorGraph.class);

	private final String applicationName;

	public CopyValuesOfPriorGraph(@Value("${spring.application.name}") final String applicationName) {
		this.applicationName = applicationName;

		// here if the files are available and do contain content
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_FETCH_REQUIRED_ANNOTATIONS);
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_STORE_COMPUTED_ANNOTATIONS);
	}
	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component, some helping notes w.r.t. the typical 3 steps of implementing a
	 * Qanary component are included in the method (you might remove all of them)
	 * 
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// typical helpers 		
		QanaryUtils myQanaryUtils = this.getUtils();
		QanaryTripleStoreConnector connectorToQanaryTriplestore = myQanaryUtils.getQanaryTripleStoreConnector();
		
		// --------------------------------------------------------------------
		// STEP 1: get the required data from the Qanary triplestore (the global process memory)
		// --------------------------------------------------------------------
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion();
		// check if a prior conversation was annotated
		QuerySolutionMap bindingsForSelect = new QuerySolutionMap();
		bindingsForSelect.add("graph", ResourceFactory.createResource(myQanaryQuestion.getInGraph().toASCIIString()));
		String sparqlSelectQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILENAME_FETCH_REQUIRED_ANNOTATIONS, bindingsForSelect);		
		logger.info("generated SPARQL INSERT query: {}", sparqlSelectQuery);
        ResultSet resultset = connectorToQanaryTriplestore.select(sparqlSelectQuery);
		int p = 0;
		while (resultset.hasNext()) {
			// update the current graph with information from prior graph
			QuerySolution tuple = resultset.next();
			String priorGraph = tuple.get("priorGraph").asResource().getURI(); 
			// create an update query to copy values
			addDataToCurrentGraph(priorGraph, myQanaryQuestion, myQanaryUtils);
			logger.info("Copied data from prior graph {}", priorGraph);
			p++;
		}
		if (p == 0) {
			logger.warn("Component {} was called, but no prior graph could be found!", applicationName);
		} else if (p > 1) {
		logger.warn("Copied information from more than one prior graph! ({} total)", p);
		}

		return myQanaryMessage;
	}

	protected void addDataToCurrentGraph(String priorGraph, QanaryQuestion myQanaryQuestion, QanaryUtils myQanaryUtils) throws IOException, SparqlQueryFailed {
		QuerySolutionMap bindsForAdd = new QuerySolutionMap();
		bindsForAdd.add("currentGraph", 
				ResourceFactory.createResource(myQanaryQuestion.getInGraph().toString()));
		bindsForAdd.add("priorGraph",
				ResourceFactory.createResource(priorGraph));
		String sparqlAddQuery = QanaryTripleStoreConnector.readFileFromResourcesWithMap(
				FILENAME_ADD_DATA_TO_CURRENT_GRAPH, bindsForAdd);
		logger.info("generated SPARQL ADD query: {}", sparqlAddQuery);
		QanaryTripleStoreConnector connectorToQanaryTriplestore = myQanaryUtils.getQanaryTripleStoreConnector();
		connectorToQanaryTriplestore.update(sparqlAddQuery);
	}
}
