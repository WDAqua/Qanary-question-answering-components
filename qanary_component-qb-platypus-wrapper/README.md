# QB Platypus wrapper component

## Description
This Qanary component fetches the SPARQL query for the submitted question.
The question text and a language attribute will be sent to the Platypus API.
The result from the API will be processed in to SPARQL and stored in the triple store.

## Configuration
The component use a cache to minimise the processing time. 
For the configuration you can change the following parameter as u need:
```
qanary.webservicecalls.cache.specs=maximumSize=10000,expireAfterAccess=3600s
```

For the platypus API you can change the following parameter as u need:
```
platypus.endpoint.url=
platypus.threshold=0.5
platypus.endpoint.language.default=en
platypus.endpoint.language.supported=en,fr,es
```
**platypus.endpoint.url**: The URL of the API endpoint, 
make sure that the API accepts requests with the parameters 
"question" for the question text and "lang" for the language attribute.

**platypus.threshold**: The threshold for the answer.

**platypus.endpoint.language.default**: The default language of the API for 
the query.

**platypus.endpoint.language.supported**: The supported languages of the API for 
the query.
