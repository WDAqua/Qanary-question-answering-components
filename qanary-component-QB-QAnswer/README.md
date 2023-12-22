# A Query Builder and Query Candidate Fetcher for the QAnswer API

The component is fetching (typically 60) SPARQL queries from the QAnswer API which are considered to be possible
solutons for the given question (aka SPARQL Query Candidates).
This component is also taking into account the already annotated Named Entities.
If Named Entities are available in the Qanary triplestore, then their entity URIs are replaced in the text of the given
question, s.t., an *enriched question* is computed.
Hence, information annotated by previous Qanary components can be integrated into the request (see the examples below),
s.t., the ambiguity of the computed result might be reduced while activating Named Entity Recognizer or Named Entity
Disambiguator (Entity Linking) components in your Qanary process.
Consequently, your request to the QAnswer API might be more precise.

## Background

[QAnswer](https://www.qanswer.eu/) is an API capable of retrieving data from different knowledge graphs.

## Examples

### Example 1: What is the capital of Germany?

Given the question `What is the capital of Germany?` and the Named Entity `Germany` at position (23,30) with the
URI http://www.wikidata.org/entity/Q183 the following question is send to the QAnswer API:

```What is the capital of http://www.wikidata.org/entity/Q183 ?```

The result will be the following resource URI: http://www.wikidata.org/entity/Q64 (Berlin)

### Example 2: Is Berlin the capital of Germany

Given the question `Is Berlin the capital of Germany` and the Named Entity `Berlin` at position (3,9) with the
URI http://www.wikidata.org/entity/Q64 the following question is send to the QAnswer API:

```Is http://www.wikidata.org/entity/Q64 the capital of Germany```

The result would be the boolean literal `true`.

## Computed Data in the Qanary Triplestore

This Qanary component is storing the following information into the Qanary triplestore (that is the global process
memory of any Qanary Question Answering process).

## Configuration

The following attributes should be changed if required (typically in the file `application.properties`):

```ini
# define the API endpoint of the QAnswer API
qanswer.endpoint.url=https://qanswer-core1.univ-st-etienne.fr/api/qa/full

# define the minimum required confidence (property: `qa:score`) for named entities (otherwise they are ignored)
qanswer.qbe.namedentities.threshold=0.5

# define the language of the user input, the language is provided to the QAnswer API
qanswer.endpoint.language.default=en

# define the knowledge base ID that should be used by the QAnswer API
qanswer.endpoint.knowledgebase.default=wikidata

# define the user that should be used by the QAnswer API
qanswer.endpoint.user.default=open
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
  "qanswerEndpointUrl": "https://qanswer-core1.univ-st-etienne.fr/api/qa/full"
}
```

### API documentation

The OpenAPI (Swagger) documentation is available at `/swagger-ui.html` (Web page) and `/api-docs` (JSON format).
You might redefine the documentation endpoints in your application.properties file.

If you deploy this component locally using the default configuration, then the following documentation URLs are
provided:

* http://0.0.0.0:11023/swagger-ui.html
* http://0.0.0.0:11023/api-docs
