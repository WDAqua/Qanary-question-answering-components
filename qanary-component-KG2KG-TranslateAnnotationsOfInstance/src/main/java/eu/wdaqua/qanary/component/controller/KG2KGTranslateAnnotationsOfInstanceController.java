package eu.wdaqua.qanary.component.controller;

import eu.wdaqua.qanary.component.KG2KGTranslateAnnotationsOfInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
public class KG2KGTranslateAnnotationsOfInstanceController {

    @Autowired
    private KG2KGTranslateAnnotationsOfInstance kg2KGTranslateAnnotationsOfInstance;
    private Logger logger = LoggerFactory.getLogger(KG2KGTranslateAnnotationsOfInstanceController.class);

    @GetMapping("/equivalentresources/{resource}")
    public ResponseEntity<String> getCounterResource(@PathVariable("resource") String resource) throws Exception {
        String decodedResource = URLDecoder.decode(resource, StandardCharsets.ISO_8859_1);
        return new ResponseEntity<>(kg2KGTranslateAnnotationsOfInstance.computeEquivalentResource(decodedResource).toString(), HttpStatus.OK);
    }


}
