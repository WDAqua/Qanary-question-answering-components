package eu.wdaqua.qanary.component.dateofdeathrealpersons.qb.controller;

import eu.wdaqua.qanary.component.dateofdeathrealpersons.qb.QueryBuilderDateOfDeathDBpedia;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
public class QueryBuilderDateOfDeathDBpediaController {

    @Autowired
    private QueryBuilderDateOfDeathDBpedia queryBuilderDateOfDeathDBpedia;

    @GetMapping("/getdbpediaquery/{dbpediaResource}")
    @Operation(summary = "Send a request to receive the computed query for a given resource.",
            description = "A URI is required."
                    + "The resource should link to a person with an existing dbo:deathDate property."
                    + "Otherwise the execution of this query will bring no result."
                    + "Example: http://dbpedia.org/resource/Stephen_Hawking (http%3A%2F%2Fdbpedia.org%2Fresource%2FStephen_Hawking)"
    )
    public ResponseEntity<?> getDbpediaQuery(@PathVariable("dbpediaResource") String dbpediaResource) throws IOException {
        try {
            String decodedResource = URLDecoder.decode(dbpediaResource, StandardCharsets.UTF_8);
            String result = queryBuilderDateOfDeathDBpedia.getDbpediaQuery(decodedResource);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}
