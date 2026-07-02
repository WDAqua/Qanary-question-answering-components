"""Shared helpers to drive the component pipeline in tests.

Used by both the offline E2E suite and the live QALD-9 quality suite: seed the
(offline) Qanary triplestore with the annotations upstream components would
produce, and read back the query the component stored.
"""


def seed_annotations(server, graph, question_id, question_text,
                     entities, predicates):
    """Seed the Qanary triplestore as upstream NED/REL components would."""
    server.add_question(question_id, question_text)
    question_uri = server.question_uri(question_id)
    triples = [f"<{question_uri}> a qa:Question ."]
    for index, entity in enumerate(entities):
        triples.append(f"""
            <urn:ann:entity:{question_id}:{index}> a qa:AnnotationOfInstance ;
                oa:hasBody <{entity}> ; qa:score {0.9 - 0.1 * index} .""")
    for index, predicate in enumerate(predicates):
        triples.append(f"""
            <urn:ann:relation:{question_id}:{index}> a qa:AnnotationOfRelation ;
                oa:hasBody <{predicate}> ; qa:score {0.9 - 0.1 * index} .""")
    server.dataset.update(f"""
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        INSERT DATA {{ GRAPH <{graph}> {{ {' '.join(triples)} }} }}
    """)
    return question_uri


def read_best_query(server, graph):
    """The highest-scored qa:AnnotationOfAnswerSPARQL, as a QE component would read it."""
    result = server.dataset.query(f"""
        PREFIX qa: <http://www.wdaqua.eu/qa#>
        PREFIX oa: <http://www.w3.org/ns/openannotation/core/>
        SELECT ?query ?score
        FROM <{graph}>
        WHERE {{
            ?annotation a qa:AnnotationOfAnswerSPARQL ;
                        oa:hasBody ?query ;
                        qa:score ?score .
        }}
        ORDER BY DESC(?score) LIMIT 1
    """)
    rows = list(result)
    return str(rows[0]["query"]) if rows else None
