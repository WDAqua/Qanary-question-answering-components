# A simple QueryBuilder component w.r.t. Superhero Names

This rule-based Qanary component is intended to create a SPARQL query that can be executed on DBpedia for the limited knowledge domain of superhero names.

## Example 

### Question of the Qanary process
```
What is the real name of Captain America?
```

### Expected SPARQL query to be stored in the Qanary triplestore
```
PREFIX dbr: <http://dbpedia.org/resource/>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT * WHERE {
  ?resource foaf:name ?answer .
  ?resource rdfs:label ?label .
  FILTER(LANG(?label) = "en") .
  ?resource dct:subject dbr:Category:Superhero_film_characters .
  FILTER(! strStarts(LCASE(?label), LCASE(?answer))).
  VALUES ?resource { <http://dbpedia.org/resource/Captain_America> } .
} 
ORDER BY ?resource
```

## Precondition 

 * The current question needs to follow the pattern `What is the real name of <superheroname>`.
   * a possible question mark is not demanded or will be ignored
 * This component expects an annotation of named entities within the Qanary triplestore. Hence, previously another component capable of recognizing and disambiguating (NER/NED) DBpedia named entities needs to be executed.
   * e.g., the NED component using [Qanary DBpedia Spotlight component](https://github.com/WDAqua/Qanary-question-answering-components/tree/master/qanary_component-NED-DBpedia-Spotlight)

## Result 

The component stores the result (the aforementioned SPARQL query) into the `body oa:hasBody` of an annotation of type `qa:AnnotationOfAnswerSPARQL`. 
The following SPARQL query to retrieve this information from the Qanary triplestore:
```sparql
PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
PREFIX qa: <http://www.wdaqua.eu/qa#> 

SELECT *
FROM <IRI-of-current-Qanary-question-analysis-process>
WHERE {
    ?s a qa:AnnotationOfAnswerSPARQL ;
       oa:hasBody ?sparqlQueryOnDBpedia ;
       oa:annotatedBy ?annotatingService .
}
```

## Build and Run

### Build with Maven

Like all Qanary component created using the [Qanary Maven archetype](https://github.com/WDAqua/Qanary/tree/master/qanary-component-archetype) it can be built using the following commands:
```
mvn package
```
To exclude the building of a corresponding Docker container use:
```
mvn package -DskipDockerBuild 
```

### Run 

To run the JAR file directly use (`X.Y.Z` refers to the current version of the component):
```
java -jar target/qanary_component-QB-SimpleRealNameOfSuperHero-X.Y.Z.jar
```
While using the Docker image, start the image `qanary_component-QB-SimpleRealNameOfSuperHero`.
