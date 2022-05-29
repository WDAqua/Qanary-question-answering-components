# A Query Builder and Executor to fetch Results from the QAnswer API

The component is fetching results from the QAnswer API.
This component is also taking into account the already annotated Named Entities.
If Named Entities are available in the Qanary triplestore, then their entity URIs are replaced in the text of the given question, s.t., an *enriched question* is computed.
Hence, information annotated by previous Qanary components can be integrated into the request (see the examples below), s.t., the ambiguity of the computed result might be reduced while activating Named Entity Recognizer or Named Entity Disambiguator (Entity Linking) components in your Qanary process.
Consequently, your request to the QAnswer API might be more precise.

## Background

[QAnswer](https://www.qanswer.eu/) is an API capable of retrieving data from different knowledge graphs.
An example is available at [https://qanswer-frontend.univ-st-etienne.fr/](https://qanswer-frontend.univ-st-etienne.fr/).

## Examples

### Example 1: What is the capital of Germany?

Given the question `What is the capital of Germany?` and the Named Entity `Germany` at position (23,30) with the URI http://www.wikidata.org/entity/Q183 the following question is send to the QAnswer API:

```What is the capital of http://www.wikidata.org/entity/Q183 ?```

The result will be the following resource URI: http://www.wikidata.org/entity/Q64 (Berlin)

### Example 2: Is Berlin the capital of Germany

Given the question `Is Berlin the capital of Germany` and the Named Entity `Berlin` at position (3,9) with the URI http://www.wikidata.org/entity/Q64 the following question is send to the QAnswer API:

```Is http://www.wikidata.org/entity/Q64 the capital of Germany```

The result would be the boolean literal `true`.

## Computed Data in the Qanary Triplestore

This Qanary component is storing the following information into the Qanary triplestore (that is the global process memory of any Qanary Question Answering process).

The stored information is reflecting the process inside of the component:

* Instance of `qa:ImprovedQuestion` containing as `rdf:value` the enriched question (see above).
  * E.g., `What is the capital of http://www.wikidata.org/entity/Q183 ?`
  * The relation to the `qa:Question` entity of your Qanary process is expressed via an annotation of the type `qa:AnnotationOfImprovedQuestion`.
* Instance of `qa:SparqlQuery` containing as `rdf:value` the SPARQL SELECT query computed by QAnswer that is used to retrieve information from the defined knowledge graph.
  * E.g., `SELECT DISTINCT ?o1 WHERE  { <http://www.wikidata.org/entity/Q183> <http://www.wikidata.org/prop/direct/P36> ?o1 . }  LIMIT 1000`
  * The relation to the `qa:Question` entity of your Qanary process is expressed via an annotation of the type `qa:AnnotationOfAnswerSPARQL`.
* Instance of `qa:Answer` containing as `rdf:value` a list of computed results as returned from the QAnswer API as `rdf:Seq`.
  * E.g., `[ a rdf:Seq; rdf:_1 <http://www.wikidata.org/entity/Q64> ]`)
  * The relation to the `qa:Question` entity of your Qanary process is expressed via an annotation of the type `qa:AnnotationAnswer`.
* Instance of `qa:AnswerType` containing as `rdf:value` a URI providing the datatype of the answer entities (see above)
  * E.g., `http://www.w3.org/2001/XMLSchema#anyURI`, `http://www.w3.org/2001/XMLSchema#boolean`, or `http://www.w3.org/2001/XMLSchema#decimal`
  * The relation to the `qa:Question` entity of your Qanary process is expressed via an annotation of the type `qa:AnnotationOfAnswerType`.

To retrieve all information computed by this Qanary component from the Qanary triplestore you might use the following SPARQL query:

```sparql
SELECT * FROM <YOURGRAPHURI> WHERE {
    ?s ?p ?o ; 
        a ?type. 
    VALUES ?t { 
        qa:AnnotationOfAnswerSPARQL qa:SparqlQuery
        qa:AnnotationOfImprovedQuestion qa:ImprovedQuestion 
        qa:AnnotationAnswer qa:Answer 
        qa:AnnotationOfAnswerType qa:AnswerType 
    }
}
ORDER BY ?type
```

## Configuration

The following attributes should be changed if required (typically in the file `application.properties`):

```ini
# define the API endpoint of the QAnswer API
qanswer.endpoint.url=http://qanswer-core1.univ-st-etienne.fr/api/gerbil

# define the minimum required confidence (property: `qa:score`) for named entities (otherwise they are ignored)
qanswer.qbe.namedentities.threshold=0.5

# define the language of the user input, the language is provided to the QAnswer API
qanswer.endpoint.language.default=en

# define the knowledge base ID that should be used by the QAnswer API
qanswer.endpoint.knowledgebase.default=wikidata
```

## Additional Web Interfaces

An additional QAnswer API wrapper is provided: `POST` method at `/api`.
You should provide the following data in a JSON format:

* question (required)
* language
* knowledgeBaseId
* qanswerEndpointUrl

Examples:

```json
{
  "question": "What is the capital of Germany?"
}
```

```json
{
  "question": "What is the capital of http://www.wikidata.org/entity/Q183 ?",
  "knowledgeBaseId": "wikidata"
}
```

```json
{
  "question": "What is the capital of Germany?",
  "language": "en",
  "knowledgeBaseId": "wikidata",
  "qanswerEndpointUrl": "http://qanswer-core1.univ-st-etienne.fr/api/gerbil"
}
```

### API documentation

The OpenAPI (Swagger) documentation is available at `/swagger-ui.html` (Web page) and `/api-docs` (JSON format).
You might redefine the documentation endpoints in your application.properties file.

If you deploy this component locally using the default configuration, then the following documentation URLs are provided:

* http://0.0.0.0:11022/swagger-ui.html
* http://0.0.0.0:11022/api-docs
