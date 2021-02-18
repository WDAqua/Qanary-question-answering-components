# A out-of-the-box Qanary Language Classifier Component

The component is classifying the queston of the current process (already stored in the Qanary triplestore) using the Shuyo language model.
The component is creating a new annotation of the type `qa:AnnotationOfQuestionLanguage`. 

## Data 

To retrieve all information computed by this Qanary component from the Qanary triplestore you might use the following SPARQL query:

```sparql
PREFIX qa: <http://www.wdaqua.eu/qa#> 
SELECT * 
FROM <YOURCURRENTGRAPHID> 
WHERE { 
    ?a a qa:AnnotationOfQuestionLanguage . 
    ?a ?p ?o .
}
```

## Additional Web Interfaces

An additional QAnswer API wrapper is provided: `POST` method at `/api`.
You should provide the following data in a JSON format:

* question (required)

Examples:

```json
{
  "question": "What is the capital of Germany?"
}
```

### API documentation

The OpenAPI (Swagger) documentation is available at `/swagger-ui.html` (Web page) and `/api-docs` (JSON format).
You might redefine the documentation endpoints in your application.properties file.

If you deploy this component locally using the default configuration, then the following documentation URLs are provided:

* http://0.0.0.0:5555/swagger-ui.html
* http://0.0.0.0:5555/api-docs
