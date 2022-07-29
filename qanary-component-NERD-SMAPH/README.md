# smaph-erd Wrapper for Qanary
============================

## Description

In order to use this component you need to install the [smaph-erd fork](https://github.com/WDAqua/smaph-erd) and run
smaph system first.

Alternatively the Dockerfile included will create a docker container running smaph and its wrapper for Qanary. Before
you will start the docker container you will need to:

* obtain a key of the Bing Search API [here](https://datamarket.azure.com/dataset/bing/search)
* edit **smaph-config.xml** replacing **BING_KEY** with your [Primary Account Key](https://datamarket.azure.com/account)

## Input specification

Not applicable as the textual question is a default parameter

## Output specification

Comment: no score in the output

```ttl
@prefix qa: <http://www.wdaqua.eu/qa#> .
@prefix oa: <http://www.w3.org/ns/openannotation/core/> .

<urn:qanary:output> a qa:AnnotationOfInstance .
<urn:qanary:output> oa:hasTarget [
    a   oa:SpecificResource;
        oa:hasSource    <urn:qanary:myQanaryQuestion> ;
        oa:hasSelector  [
            a oa:TextPositionSelector ;
            oa:start "0"^^xsd:nonNegativeInteger ;
            oa:end  "5"^^xsd:nonNegativeInteger
        ]
    ] .
<urn:qanary:output> oa:annotatedBy <urn:qanary:SmaphErd> ;
    oa:annotatedAt "2001-10-26T21:32:52"^^xsd:dateTime .
```
