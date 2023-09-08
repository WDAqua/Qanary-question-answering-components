package eu.wdaqua.qanary.component;

import eu.wdaqua.qanary.component.pojos.AnnotationOfInstancePojo;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;

import java.util.ArrayList;
import java.util.List;

public class TestDataProvider {

    private Dataset dataset;
    private List<AnnotationOfInstancePojo> annotationOfInstanceCompletePojoList;
    private List<AnnotationOfInstancePojo> annotationOfInstanceMissingNewResourcePojoList;

    public TestDataProvider() {
        initializeDataset();
        initializePojoLists();
    }

    public Dataset getDataset() {
        return dataset;
    }

    private void initializePojoLists() {
        this.annotationOfInstanceMissingNewResourcePojoList = new ArrayList<>();
        AnnotationOfInstancePojo obj1 = new AnnotationOfInstancePojo("annoID1", "http://dbpedia.org/resource/Leipzig", "targetQuestion1", 1, 2, 0.5d);
        AnnotationOfInstancePojo obj2 = new AnnotationOfInstancePojo("annoID2", "http://dbpedia.org/resource/Hulk", "targetQuestion2", 3, 4, 0.6d);
        AnnotationOfInstancePojo obj3 = new AnnotationOfInstancePojo("annoID3", "http://dbpedia.org/resource/Batman", "targetQuestion3", 5, 6, 0.7d);
        this.annotationOfInstanceMissingNewResourcePojoList.add(obj1);
        this.annotationOfInstanceMissingNewResourcePojoList.add(obj2);
        this.annotationOfInstanceMissingNewResourcePojoList.add(obj3);
        this.initializeCompletePojoList();
    }

    private void initializeCompletePojoList() {
        this.annotationOfInstanceCompletePojoList = this.annotationOfInstanceMissingNewResourcePojoList;
        annotationOfInstanceCompletePojoList.get(0).setNewResources(new ArrayList<>() {{
            add(ResourceFactory.createResource("http://wikidata.org/entity/Q2079"));
            add(ResourceFactory.createResource("http://wikidata.org/entity/Q3677461"));
            add(ResourceFactory.createResource("http://wikidata.org/entity/Q113624612"));
        }});
        annotationOfInstanceCompletePojoList.get(1).setNewResources(new ArrayList<>() {{
            add(ResourceFactory.createResource("http://www.wikidata.org/entity/Q188760"));
        }});
        annotationOfInstanceCompletePojoList.get(2).setNewResources(new ArrayList<>() {{
            add(ResourceFactory.createResource("http://www.wikidata.org/entity/Q2695156"));
        }});
    }

    public List<AnnotationOfInstancePojo> getAnnotationOfInstanceMissingNewResourcePojoList() {
        return annotationOfInstanceMissingNewResourcePojoList;
    }

    public List<AnnotationOfInstancePojo> getAnnotationOfInstanceCompletePojoList() {
        return annotationOfInstanceCompletePojoList;
    }

    private void initializeDataset() {
        this.dataset = RDFDataMgr.loadDataset("testDataset.rdf");
    }
}
