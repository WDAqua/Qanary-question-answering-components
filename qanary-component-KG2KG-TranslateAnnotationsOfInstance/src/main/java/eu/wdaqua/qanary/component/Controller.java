package eu.wdaqua.qanary.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class Controller {

    @Autowired
    private KG2KGTranslateAnnotationsOfInstance kg2KGTranslateAnnotationsOfInstance;
    private static final String DBPEDIA_TO_WIKIDATA_QUERY = "queries/dbpediaToWikidata.rq";
    private static final String WIKIDATA_TO_DBPEDIA_QUERY = "queries/wikidataToDbpedia.rq";


    @GetMapping("/query/{resource}")
    public ResponseEntity<String> getCounterResource(@PathVariable String resource) throws IOException {
        return new ResponseEntity<>(kg2KGTranslateAnnotationsOfInstance.getCounterResource(DBPEDIA_TO_WIKIDATA_QUERY, "http://dbpedia.org/resource/Stephen_Hawking"), HttpStatus.OK);
    }


}
