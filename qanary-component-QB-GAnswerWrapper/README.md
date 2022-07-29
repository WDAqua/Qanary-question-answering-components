# gAnswer wrapper component

## Description

The main task of this component is to manage the communication between the Qanary pipeline and the gAnswer API and to
prepare the data. To do this, the component fetches the submitted question and sends the text attribute to the
gAnswer API. The result from the API is processed, stored in the triple store and is then available in the Qanary
pipeline.

## Configuration

The component uses a cache to minimise the processing time.
For the configuration you can change the following parameter as you need:

```
qanary.webservicecalls.cache.specs=maximumSize=10000,expireAfterAccess=3600s
```

For the TeBaQA API you can change the following parameter as you need:

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

**g_answer.endpoint.language.default**: The default language of the API.

**g_answer.endpoint.language.supported**: The list of supported languages of the API,
e.g. `tebaqa.endpoint.language.supported=en,fr,ru`.

# Further references

- [How to start a standard java Qanary component](https://github.com/WDAqua/Qanary/wiki//How-to-start-a-standard-java-Qanary-component)
