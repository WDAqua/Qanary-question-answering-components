package eu.wdaqua.qanary.component.dateofdeathrealpersons.qb.controller;

import eu.wdaqua.qanary.component.dateofdeathrealpersons.qb.QueryBuilderDateOfDeathDBpedia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class DefaultController {

    @Autowired
    private QueryBuilderDateOfDeathDBpedia queryBuilderDateOfDeathDBpedia;

    @GetMapping("/getdbpediaquery")
    public ResponseEntity<?> getDbpediaQuery(@RequestParam String dbpediaResource) throws IOException {
        try {
            String result = queryBuilderDateOfDeathDBpedia.getDbpediaQuery(dbpediaResource);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch(Exception e) {
            return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
        }
    }

}
