package eu.wdaqua.qanary.component;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;

public class TestDataProvider {

    private Dataset dataset;

    public TestDataProvider() {
        initializeDataset();
    }

    public Dataset getDataset() {
        return dataset;
    }

    private void initializeDataset() {
        Dataset dataset = DatasetFactory.create();

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("owl", OWL.getURI());
        model.createStatement(
                ResourceFactory.createResource("http://dbpedia.org/resource/Leipzig"),
                ResourceFactory.createProperty(OWL.getURI(), "sameAs"),
                ResourceFactory.createResource("http://wikidata.org/resource/Q123")
        );

        dataset.setDefaultModel(model);
        this.dataset = dataset;
    }
}
