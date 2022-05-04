# QB g Answer wrapper component

## Description

This Qanary component fetches the SPARQL query for the submitted question.
The question text attribute will be sent to the g Answer API.
The result from the API will be processed in to SPARQL and stored in the triple store.

## Configuration

The component use a cache to minimise the processing time.
For the configuration you can change the following parameter as u need:

```
qanary.webservicecalls.cache.specs=maximumSize=10000,expireAfterAccess=3600s
```

For the g Answer API you can change the following parameter as u need:

```
g_answer.endpoint.url=
g_answer.threshold=0.5
g_answer.endpoint.language.default=en
g_answer.endpoint.language.supported=en
```

**g_answer.endpoint.url**: The URL of the API endpoint,
make sure that the API accepts requests with the parameter
"question" for the question text.

**g_answer.threshold**: The threshold for the answer.

**g_answer.endpoint.language.default**: The default language of the API for
the query.

**g_answer.endpoint.language.supported**: The supported languages of the API for
the query.
