package eu.wdaqua.qanary.component.controller;

import eu.wdaqua.qanary.component.KG2KGTranslateAnnotationsOfInstance;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class KG2KGTranslateAnnotationsOfInstanceController {

    private final Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstanceController.class);
    @Autowired
    private KG2KGTranslateAnnotationsOfInstance kg2KGTranslateAnnotationsOfInstance;

    @GetMapping("/equivalentresources/{resource}")
    @Operation(
            summary = "Request equivalent dbpedia or wikidata resource.",
            description = """
                    Pass a encoded resource as path variable to get the equivalent\s
                    dbpedia or wikidata resource, depending on your input.
                    For example: '/http%3A%2F%2Fdbpedia.org%2Fresource%2FLeipzig' should return 3 equivalent wikidata resources.
                    """
    )
    public ResponseEntity<String> getEquivalentResource(@PathVariable("resource") String resource) throws Exception {
        String decodedResource = URLDecoder.decode(resource, StandardCharsets.ISO_8859_1);
        List<RDFNode> newResources = kg2KGTranslateAnnotationsOfInstance.computeEquivalentResource(decodedResource);
        if (newResources != null) {
            return new ResponseEntity<>(newResources.toString(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("There's no equivalent resource", HttpStatus.BAD_REQUEST);
        }
    }


}
