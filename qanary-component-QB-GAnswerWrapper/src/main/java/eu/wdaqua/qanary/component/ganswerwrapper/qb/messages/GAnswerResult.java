package eu.wdaqua.qanary.component.ganswerwrapper.qb.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Hidden;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class GAnswerResult {
    private static final Logger logger = LoggerFactory.getLogger(GAnswerResult.class);

    private com.google.gson.JsonParser jsonParser;

    private URI endpoint;
    private String language;
    private String question;

    private String sparql;
    private List<String> values;
    private String type;
    private URI datatype;
    private double confidence;

    @Hidden
    public final URI RESOURCETYPEURI;
    @Hidden
    public final URI BOOLEANTYPEURI;
    @Hidden
    public final URI STRINGTYPEURI;

    public GAnswerResult(JSONObject json, String question, URI endpoint, String language) throws URISyntaxException {
        jsonParser = new JsonParser();
        JsonArray parsedJsonArray = jsonParser.parse(json.toJSONString()).getAsJsonObject().getAsJsonArray("queries").getAsJsonArray();

        this.question = question;
        this.language = language;
        this.endpoint = endpoint;

        this.RESOURCETYPEURI = new URI("http://www.w3.org/2001/XMLSchema#anyURI");
        this.BOOLEANTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#boolean");
        this.STRINGTYPEURI = new URI("http://www.w3.org/2001/XMLSchema#string");

        initData(parsedJsonArray);
    }

    /**
     * init the fields while parsing the JSON data
     *
     * @param answers
     * @throws URISyntaxException
     */
    private void initData(JsonArray answers) throws URISyntaxException {
        logger.debug("responseQuestion: {}", answers);

        logger.debug("0. sparql: {}", answers.get(0).getAsString());
        this.sparql = answers.get(0).getAsString();

    }

    private String wikidataPrefixes() {
        String prefixes = "PREFIX wd: <http://www.wikidata.org/entity/>" + "PREFIX wds: <http://www.wikidata.org/entity/statement/>" + "PREFIX wdv: <http://www.wikidata.org/value/>" + "PREFIX wdt: <http://www.wikidata.org/prop/direct/>" + "PREFIX wikibase: <http://wikiba.se/ontology#>" + "PREFIX p: <http://www.wikidata.org/prop/>" + "PREFIX ps: <http://www.wikidata.org/prop/statement/>" + "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>" + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" + "PREFIX bd: <http://www.bigdata.com/rdf#>" + "PREFIX wdref: <http://www.wikidata.org/reference/>" + "PREFIX psv: <http://www.wikidata.org/prop/statement/value/>" + "PREFIX psn: <http://www.wikidata.org/prop/statement/value-normalized/>" + "PREFIX pqv: <http://www.wikidata.org/prop/qualifier/value/>" + "PREFIX pqn: <http://www.wikidata.org/prop/qualifier/value-normalized/>" + "PREFIX pr: <http://www.wikidata.org/prop/reference/>" + "PREFIX prv: <http://www.wikidata.org/prop/reference/value/>" + "PREFIX prn: <http://www.wikidata.org/prop/reference/value-normalized/>" + "PREFIX wdno: <http://www.wikidata.org/prop/novalue/>" + "PREFIX wdata: <http://www.wikidata.org/wiki/Special:EntityData/>" + "PREFIX schema: <http://schema.org/>" + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + "PREFIX owl: <http://www.w3.org/2002/07/owl#>" + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>" + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" + "PREFIX prov: <http://www.w3.org/ns/prov#>" + "PREFIX bds: <http://www.bigdata.com/rdf/search#>" + "PREFIX gas: <http://www.bigdata.com/rdf/gas#>" + "PREFIX hint: <http://www.bigdata.com/queryHints#>";

        return prefixes;
    }

    public JsonParser getJsonParser() {
        return jsonParser;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public String getLanguage() {
        return language;
    }

    public String getQuestion() {
        return question;
    }

    public String getSparql() {
        return sparql;
    }

    public List<String> getValues() {
        return values;
    }

    public String getType() {
        return type;
    }

    public URI getDatatype() {
        return datatype;
    }

    public double getConfidence() {
        return confidence;
    }
}
