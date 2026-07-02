# Knowledge-graph test fixtures

These JSON files drive the per-knowledge-graph predicate-identification suites
(`tests/test_kg_dbpedia.py`, `tests/test_kg_wikidata.py`,
`tests/test_kg_musicbrainz.py`, `tests/test_kg_geonames.py`). Each fixture is a
**100-resource subset** of a real knowledge graph together with every
resource's real predicate inventory and **≥3 `(question, expected-predicate)`
cases**.

The suites are fully **offline and deterministic** — they only run the
component's ranking logic (`select_best_predicate`) against the stored data.

| Fixture | Resources | Cases | Source |
|---|---|---|---|
| `dbpedia.json` | 100 | 400 | DBpedia SPARQL endpoint (`https://dbpedia.org/sparql`) |
| `wikidata.json` | 100 | 400 | WDQS sample + MediaWiki `wbgetentities` API (claims + property labels) |
| `musicbrainz.json` | 100 | 392 | MusicBrainz web service (WS2) mapped to Music-Ontology / FOAF / DC predicates |
| `geonames.json` | 100 | 400 | `cities15000` dump (sample) + per-feature RDF (`sws.geonames.org/{id}/about.rdf`) |

Per the task, SPARQL endpoints are used where available (DBpedia, Wikidata);
MusicBrainz and GeoNames have no live SPARQL/RDF query endpoint, so their data
comes from the web dumps / web service.

## How the cases are built

`generate_fixtures.py` phrases a question around each predicate's label (e.g.
the `dbo:capital` predicate → *"…the capital of Germany?"*) and keeps the case
only when the component resolves that question **unambiguously back to that
predicate** out of the resource's full inventory. The cases are therefore
*golden regression cases* grounded in real KG data: they lock in the ranking
behaviour (tokenisation, camelCase splitting, the ontology-namespace tie-break,
Wikidata property-label resolution) across four very different vocabularies. A
change that regresses any of these flips many cases red.

Only content predicates are asked about; Wikidata external-ID properties and
GeoNames Creative-Commons/Dublin-Core provenance metadata are excluded as
question targets but **remain in each resource's candidate list as ranking
competition**.

## Regenerating

The fixtures are committed; the suites never touch the network. To refresh them
against current KG data (needs network):

```bash
pip install -r ../../requirements.txt
python generate_fixtures.py all          # or: dbpedia | wikidata | musicbrainz | geonames
```

Raw responses are cached under `_cache/` (git-ignored) so re-runs are cheap.
`generate_fixtures.py` is a development-time tool and is not collected by pytest.
