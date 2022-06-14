# QB RuBQ wrapper component

## Description

This Qanary component fetches the SPARQL query for the submitted question.
The question text will be sent to the RuBQ API.
The result from the API will be processed in to SPARQL and stored in the triple store.

## Configuration

The component use a cache to minimise the processing time.
For the configuration you can change the following parameter as u need:

```
qanary.webservicecalls.cache.specs=maximumSize=10000,expireAfterAccess=3600s
```

For the RuBQ API you can change the following parameter as u need:

```
rubq.endpoint.url=
rubq.threshold=0.5
rubq.endpoint.language.default=en
rubq.endpoint.language.supported=en,ru
```

**rubq.endpoint.url**: The URL of the API endpoint,
make sure that the API accepts requests with the parameters
"question" for the question text.

**rubq.threshold**: The threshold for the answer.

**rubq.endpoint.language.default**: The default language of the API for
the query.

**rubq.endpoint.language.supported**: The supported languages of the API for
the query.
