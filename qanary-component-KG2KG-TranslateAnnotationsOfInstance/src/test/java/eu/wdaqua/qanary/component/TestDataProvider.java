package eu.wdaqua.qanary.component;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;

public class TestDataProvider {

    private Dataset dataset;

    public TestDataProvider() {
        initializeDataset();
    }

    public Dataset getDataset() {
        return dataset;
    }

    private void initializeDataset() {

        /*
        Resource resourceA = ResourceFactory.createResource("http://dbpedia.org/resource/Leipzig");
        Resource resourceB = ResourceFactory.createResource("http://wikidata.org/resource/Q123");

        Model model = ModelFactory.createDefaultModel();
        //  model.setNsPrefix("owl", OWL.NS);
        model.setNsPrefix("sameAs", OWL.getURI());

        model.add(resourceA, model.createProperty("sameAs", OWL.getURI()), resourceB);
        */

        this.dataset = RDFDataMgr.loadDataset("testDataset.rdf");
    }
}
