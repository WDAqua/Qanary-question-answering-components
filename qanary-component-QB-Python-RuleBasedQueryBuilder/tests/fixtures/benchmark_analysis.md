# Benchmark analysis: QALD-10 and LC-QuAD

The rule set of the component and the composition of the offline question bank
(`questions.json`) were derived from an analysis of two established KGQA
benchmarks (run `explore_benchmarks.py` to reproduce; needs network access):

* **QALD-10** ([KGQA/QALD_10](https://github.com/KGQA/QALD_10)), 394 questions
  over Wikidata
* **LC-QuAD 1.0** ([AskNowQA/LC-QuAD](https://github.com/AskNowQA/LC-QuAD)),
  4000 training questions over DBpedia

## Findings (2026-07-02)

Query-form distribution:

| Benchmark | SELECT | COUNT | ASK |
|---|---|---|---|
| QALD-10 | 237 (1 triple dominates) | 96 | 61 |
| LC-QuAD | 3180 (1–3 triples) | 535 | 285 |

Entity/predicate counts per question (LC-QuAD): **1–2 entities and 1–2(–3)
predicates cover the overwhelming majority** — exactly the space the rule-based
component supports.

Dominant structural shapes (E = entity, P = predicate, ? = variable):

| Shape | LC-QuAD n | Example phrasing | Component rule |
|---|---|---|---|
| `E-P-?` | 1157 | "What is the allegiance of John Kotelawala?" | forward |
| `?-P-E` | 129 (+ variants) | "Which city's founder is John Forbes?" | backward |
| `ASK E-P-E` | 285 | "Was Ganymede discovered by Galileo Galilei?" | ask (both orientations) |
| `COUNT ?-P-E` | 62 | "How many movies did Stanley Kubrick direct?" | count wrapper |
| `E-P-?y \| ?y-P-?` | 209 | "What was the university of the rugby player who coached …?" | chain (8 direction/assignment variants) |
| `?-P-E1 \| ?-P-E2` | 320+81 | "Whose former teams are Indianapolis Colts and Carolina Panthers?" | intersection (8 variants) / shared-predicate intersection |

## How this shaped the question bank

`questions.json` mirrors this distribution with 124 questions over the offline
knowledge graph (`offline_kg.ttl`):

| Type | n | Entities × predicates |
|---|---|---|
| forward | 25 | 1 × 1 |
| backward | 15 | 1 × 1 |
| ask (8 true / 6 false) | 14 | 2 × 1 |
| count (forward + backward) | 14 | 1 × 1 |
| chain | 18 | 1 × 2 |
| intersection | 14 | 2 × 2 |
| intersection_shared | 9 | 2 × 1 |
| superlative (`ORDER BY … LIMIT 1`, DESC + ASC) | 12 | 1 × 2 and 0 × 1 |
| aggregation (`SUM`/`AVG`) | 3 | 1 × 2 |

Phrasings follow the benchmark style ("Was … born in …?", "How many movies did
… direct?", "Which film was directed by … and stars …?", "What is the highest
mountain in …?" — superlatives/aggregations appear in QALD-10 e.g. as "What is
the highest mountain in Germany?" and in LC-QuAD via COUNT templates).
Comparatives, filters and ordering by aggregates (e.g. "who directed the most
films") remain out of scope for the rule-based approach and are documented as
such in the component README.
