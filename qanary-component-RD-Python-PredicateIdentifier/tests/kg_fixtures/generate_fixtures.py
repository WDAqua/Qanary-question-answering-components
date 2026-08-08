#!/usr/bin/env python3
"""Generate deterministic knowledge-graph fixtures for the predicate-identifier
test suites (``tests/test_kg_*.py``).

For each knowledge graph it fetches ~100 resources together with each resource's
*real* predicate inventory, then derives at least three
``(question, expected-predicate)`` cases per resource: a question is phrased
around each predicate's label and kept only when the component
(``select_best_predicate``) resolves it unambiguously to that predicate. The
resulting fixtures are therefore golden regression cases grounded in real KG
data — they lock in the ranking behaviour across four very different
vocabularies.

Data sources (see the module note; per the task, dumps/web data are used where
no SPARQL endpoint exists):

* DBpedia    -- public SPARQL endpoint (https://dbpedia.org/sparql)
* Wikidata   -- WDQS for the resource sample + the MediaWiki wbgetentities API
                for each resource's claims and property labels
* MusicBrainz-- MusicBrainz web-service (WS2) data mapped to Music-Ontology /
                FOAF / Dublin-Core predicate URIs (no live SPARQL/RDF exists)
* GeoNames   -- the ``cities15000`` dump for the resource sample + each
                feature's RDF document (``sws.geonames.org/{id}/about.rdf``)

Run once (needs network) to (re)build the committed JSON fixtures next to this
file; the test suites themselves are fully offline and deterministic.

Usage:  python generate_fixtures.py [dbpedia|wikidata|musicbrainz|geonames|all]
"""
import os
import sys
import json
import time
import zipfile
import io
import urllib.parse
import datetime
import xml.etree.ElementTree as ET

import requests

HERE = os.path.dirname(os.path.abspath(__file__))
COMPONENT_ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, COMPONENT_ROOT)

# the component reads its configuration at import time
os.environ.setdefault("PRODUCTION", "True")
os.environ.setdefault("SERVICE_NAME_COMPONENT", "RD-Python-PredicateIdentifier")
os.environ.setdefault("KNOWLEDGE_GRAPH_ENDPOINT", "https://dbpedia.org/sparql")
os.environ.setdefault("MIN_MATCH_SCORE", "0.5")

from component import rd_predicate_identifier as rd  # noqa: E402

UA = ("Qanary-RD-PredicateIdentifier/1.0 "
      "(https://github.com/WDAqua/Qanary-question-answering-components; "
      "predicate-identifier test-data generation)")

CACHE = os.path.join(HERE, "_cache")
os.makedirs(CACHE, exist_ok=True)

TARGET_RESOURCES = 100
CASES_PER_RESOURCE = 4      # try for four, keep resources that yield >= MIN_CASES
MIN_CASES = 3
THRESHOLD = float(os.environ["MIN_MATCH_SCORE"])

# question phrasings; embedding the predicate label as keywords and varying the
# surrounding function words exercises the tokenizer, not just one template
QUESTION_TEMPLATES = [
    "What is the {label} of {subject}?",
    "Which {label} is associated with {subject}?",
    "Tell me the {label} of {subject}.",
    "What {label} does {subject} have?",
]

# predicates that carry no askable, content-bearing relation
STRUCTURAL_LOCAL_NAMES = {
    "type", "label", "comment", "seealso", "sameas", "isdefinedby",
    "isprimarytopicof", "primarytopic", "depiction", "thumbnail", "abstract",
    "subject", "hypernym", "wikipageid", "wikipagerevisionid", "wikipagelength",
    "wikipagewikilink", "wikipageexternallink", "wikipageredirects",
    "wikipagedisambiguates", "wikipageusestemplate", "wikipageoutdegree",
    "wikipagewikilinktext", "wasderivedfrom", "homepage", "page", "rights",
    "license", "preflabel", "altlabel", "about", "description", "signature",
    "logo", "align", "id", "viewport", "individualisedgnd", "soundrecording",
}


# --------------------------------------------------------------------------- #
# small HTTP / caching helpers
# --------------------------------------------------------------------------- #
def _cache_path(name):
    return os.path.join(CACHE, name)


def cached_json(name, producer):
    """Return JSON from cache or compute+persist it (keeps re-runs cheap)."""
    path = _cache_path(name)
    if os.path.exists(path):
        with open(path, encoding="utf-8") as handle:
            return json.load(handle)
    value = producer()
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(value, handle)
    return value


def http_get(url, params=None, accept=None, tries=4, pause=1.0):
    headers = {"User-Agent": UA}
    if accept:
        headers["Accept"] = accept
    last = None
    for attempt in range(tries):
        try:
            response = requests.get(url, params=params, headers=headers, timeout=60)
            # Virtuoso (DBpedia) returns 206 Partial Content for anytime-query
            # results that are still valid SPARQL JSON
            if response.status_code in (200, 206):
                return response
            last = f"HTTP {response.status_code}"
        except requests.RequestException as error:
            last = str(error)
        time.sleep(pause * (attempt + 1))
    raise RuntimeError(f"GET {url} failed: {last}")


def sparql_select(endpoint, query, pause=1.0):
    response = http_get(endpoint, params={"query": query, "format": "json"},
                        accept="application/sparql-results+json", pause=pause)
    return response.json()["results"]["bindings"]


# --------------------------------------------------------------------------- #
# shared case construction
# --------------------------------------------------------------------------- #
def readable_label(label_text):
    """A natural-language rendering of a predicate label for the question."""
    return " ".join(rd.tokenize(label_text))


def build_cases(candidates, subject_label, eligible=None):
    """Derive >=MIN_CASES (question, expected-predicate) cases for one resource.

    ``candidates`` is the resource's full ``[(uri, label|None), ...]`` inventory
    (the competition the ranker must beat). Cases are only drawn from ``eligible``
    predicate URIs when given (e.g. to skip Wikidata external-ID properties),
    but ranking always runs against the full inventory. Only predicates the
    ranker resolves unambiguously back to themselves are kept.
    """
    cases = []
    used_keywords = set()
    for uri, label in sorted(candidates, key=lambda item: item[0]):
        if eligible is not None and uri not in eligible:
            continue
        if rd.local_name(uri).lower() in STRUCTURAL_LOCAL_NAMES:
            continue
        label_text = rd.predicate_label(uri, label)
        tokens = rd.tokenize(label_text)
        if not tokens or any(token in used_keywords for token in tokens):
            continue
        readable = readable_label(label_text)
        if not readable:
            continue
        template = QUESTION_TEMPLATES[len(cases) % len(QUESTION_TEMPLATES)]
        question = template.format(label=readable, subject=subject_label)
        predicate, score = rd.select_best_predicate(question, candidates)
        if predicate == uri and score >= THRESHOLD:
            cases.append({"question": question,
                          "expected_predicate": uri,
                          "score": round(score, 3)})
            used_keywords.update(tokens)
        if len(cases) >= CASES_PER_RESOURCE:
            break
    return cases if len(cases) >= MIN_CASES else None


def assemble(kg_name, endpoint, resource_iterator):
    """Drive ``resource_iterator`` into a fixture.

    The iterator yields ``(uri, label, candidates)`` or
    ``(uri, label, candidates, eligible_predicate_uris)``.
    """
    resources = []
    for item in resource_iterator:
        uri, subject_label, candidates = item[0], item[1], item[2]
        eligible = item[3] if len(item) > 3 else None
        if len(resources) >= TARGET_RESOURCES:
            break
        if not candidates:
            continue
        cases = build_cases(candidates, subject_label, eligible)
        if not cases:
            continue
        resources.append({
            "resource": uri,
            "resource_label": subject_label,
            "candidates": [[c_uri, c_label] for c_uri, c_label in
                           sorted(candidates, key=lambda item: item[0])],
            "cases": cases,
        })
        print(f"  [{len(resources):3d}/{TARGET_RESOURCES}] {uri} "
              f"({len(candidates)} predicates, {len(cases)} cases)")
    fixture = {
        "knowledge_graph": kg_name,
        "endpoint": endpoint,
        "generated_at": datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
        "generator": "tests/kg_fixtures/generate_fixtures.py",
        "resource_count": len(resources),
        "case_count": sum(len(r["cases"]) for r in resources),
        "resources": resources,
    }
    out = os.path.join(HERE, f"{kg_name.lower()}.json")
    with open(out, "w", encoding="utf-8") as handle:
        json.dump(fixture, handle, ensure_ascii=False, indent=1)
    print(f"wrote {out}: {fixture['resource_count']} resources, "
          f"{fixture['case_count']} cases")
    return fixture


def subject_from_uri(uri):
    local = rd.local_name(uri)
    return urllib.parse.unquote(local).replace("_", " ").strip()


# --------------------------------------------------------------------------- #
# DBpedia (SPARQL endpoint)
# --------------------------------------------------------------------------- #
DBPEDIA_ENDPOINT = "https://dbpedia.org/sparql"
# class -> a property typical of that class; requiring it keeps the sample to
# real, well-described entities (and away from mistyped/stub resources)
DBPEDIA_CLASSES = {
    "Country": "capital", "City": "country", "Person": "birthDate",
    "Film": "director", "Company": "foundingYear", "University": "country",
    "Book": "author", "Band": "genre", "Mountain": "elevation",
    "River": "length", "Lake": "areaTotal", "Airport": "runwayLength",
    "Software": "developer", "Automobile": "manufacturer", "Museum": "location",
}


def dbpedia_resources():
    per_class = 10
    labels = {}
    ordered = []
    for cls, typical in DBPEDIA_CLASSES.items():
        # bound the base set with an inner LIMIT (keeps ORDER BY RAND() cheap),
        # then shuffle for a diverse sample across the class
        query = f"""
            PREFIX dbo: <http://dbpedia.org/ontology/>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            SELECT ?s ?l WHERE {{
                {{ SELECT ?s WHERE {{ ?s a dbo:{cls} ; dbo:{typical} ?x }} LIMIT 3000 }}
                ?s rdfs:label ?l . FILTER(LANG(?l) = "en")
            }} ORDER BY RAND() LIMIT {per_class}
        """
        bindings = cached_json(f"dbpedia_class_{cls}.json",
                               lambda q=query: sparql_select(DBPEDIA_ENDPOINT, q))
        for binding in bindings:
            uri = binding["s"]["value"]
            if uri not in labels:
                labels[uri] = binding.get("l", {}).get("value") or subject_from_uri(uri)
                ordered.append(uri)
    for uri in ordered:
        candidates = cached_json(
            "dbpedia_pred_" + urllib.parse.quote(uri, safe="") + ".json",
            lambda u=uri: rd.fetch_candidate_predicates(DBPEDIA_ENDPOINT, u))
        yield uri, labels[uri], [tuple(c) for c in candidates]


# --------------------------------------------------------------------------- #
# Wikidata (WDQS sample + wbgetentities API)
# --------------------------------------------------------------------------- #
WDQS = "https://query.wikidata.org/sparql"
WD_API = "https://www.wikidata.org/w/api.php"
WD_DIRECT = "http://www.wikidata.org/prop/direct/"
WD_CLASSES = {"Q515": "city", "Q6256": "country", "Q5": "human",
              "Q11424": "film", "Q3918": "university", "Q571": "book",
              "Q482994": "album", "Q4830453": "company", "Q11446": "ship",
              "Q8502": "mountain", "Q4022": "river", "Q23397": "lake"}


def wd_api(params):
    params = dict(params, format="json")
    return http_get(WD_API, params=params).json()


def wikidata_resources():
    ids = []
    for qid in WD_CLASSES:
        query = (f"SELECT ?s WHERE {{ ?s wdt:P31 wd:{qid} . "
                 f"?s wdt:P18 ?img }} LIMIT 12")
        bindings = cached_json(f"wd_class_{qid}.json",
                               lambda q=query: sparql_select(WDQS, q, pause=2.0))
        got = sorted({b["s"]["value"].rsplit("/", 1)[-1] for b in bindings})
        ids.extend(got)
        time.sleep(1.0)
    seen, ordered = set(), []
    for qid in ids:
        if qid not in seen:
            seen.add(qid)
            ordered.append(qid)

    for qid in ordered:
        entity = cached_json(f"wd_entity_{qid}.json",
                             lambda i=qid: wd_api({"action": "wbgetentities",
                                                   "ids": i, "props": "claims|labels",
                                                   "languages": "en"}))
        ent = entity.get("entities", {}).get(qid, {})
        subject_label = (ent.get("labels", {}).get("en", {}).get("value")
                         or subject_from_uri(qid))
        claims = ent.get("claims", {})
        props = sorted(claims.keys())
        if not props:
            yield f"http://www.wikidata.org/entity/{qid}", subject_label, []
            continue
        labels = wd_property_labels(props)
        candidates = [(WD_DIRECT + p, labels.get(p)) for p in props]
        # keep the whole inventory as ranking competition, but only ask about
        # content properties — external identifiers ("VIAF ID", "Data Commons
        # ID", ...) make for uninteresting questions
        eligible = {WD_DIRECT + p for p in props
                    if _wd_datatype(claims[p]) not in WD_SKIP_DATATYPES}
        yield (f"http://www.wikidata.org/entity/{qid}", subject_label,
               candidates, eligible)
        time.sleep(0.2)


# Wikidata property datatypes that do not make good question targets
WD_SKIP_DATATYPES = {"external-id", "commonsMedia", "url", "geo-shape",
                     "tabular-data", "math", "musical-notation", "wikibase-lexeme"}


def _wd_datatype(statements):
    for statement in statements:
        datatype = statement.get("mainsnak", {}).get("datatype")
        if datatype:
            return datatype
    return None


def wd_property_labels(pids):
    labels = {}
    for start in range(0, len(pids), 50):
        chunk = pids[start:start + 50]
        key = "wd_plabels_" + "_".join(chunk[:1]) + f"_{start}_{len(chunk)}.json"
        data = cached_json(key, lambda c=chunk: wd_api(
            {"action": "wbgetentities", "ids": "|".join(c),
             "props": "labels", "languages": "en"}))
        for pid, ent in data.get("entities", {}).items():
            label = ent.get("labels", {}).get("en", {}).get("value")
            if label:
                labels[pid] = label
    return labels


# --------------------------------------------------------------------------- #
# MusicBrainz (WS2 web service mapped to Music-Ontology / FOAF / DC predicates)
# --------------------------------------------------------------------------- #
MB = "https://musicbrainz.org/ws/2"
FOAF = "http://xmlns.com/foaf/0.1/"
DCT = "http://purl.org/dc/terms/"
MO = "http://purl.org/ontology/mo/"
MBO = "http://musicbrainz.org/ontology#"


def mb_get(path, params):
    params = dict(params, fmt="json")
    response = http_get(f"{MB}/{path}", params=params, pause=1.5)
    time.sleep(1.1)  # MusicBrainz asks for <= 1 request/second
    return response.json()


def _first_name(value):
    if isinstance(value, dict):
        return value.get("name") or value.get("title")
    return value


def mb_artist_candidates(entity):
    fields = [
        (FOAF + "name", "name", entity.get("name")),
        (MBO + "type", "type", entity.get("type")),
        (FOAF + "gender", "gender", entity.get("gender")),
        (MBO + "country", "country", entity.get("country")),
        (MBO + "area", "area", _first_name(entity.get("area"))),
        (MBO + "beginArea", "begin area", _first_name(entity.get("begin-area"))),
        (MO + "activity_start", "begin date",
         (entity.get("life-span") or {}).get("begin")),
        (MO + "activity_end", "end date",
         (entity.get("life-span") or {}).get("end")),
        (MBO + "disambiguation", "disambiguation", entity.get("disambiguation")),
    ]
    if entity.get("aliases"):
        fields.append((MBO + "alias", "alias", entity["aliases"][0].get("name")))
    if entity.get("tags"):
        fields.append((MO + "tag", "tag", entity["tags"][0].get("name")))
    if entity.get("genres"):
        fields.append((MO + "genre", "genre", entity["genres"][0].get("name")))
    return fields


def mb_release_candidates(entity):
    fields = [
        (DCT + "title", "title", entity.get("title")),
        (DCT + "date", "date", entity.get("date")),
        (MBO + "country", "country", entity.get("country")),
        (MBO + "status", "status", entity.get("status")),
        (MBO + "packaging", "packaging", entity.get("packaging")),
        (MBO + "barcode", "barcode", entity.get("barcode")),
        (MBO + "disambiguation", "disambiguation", entity.get("disambiguation")),
    ]
    if entity.get("artist-credit"):
        fields.append((FOAF + "maker", "artist",
                       entity["artist-credit"][0].get("name")))
    if entity.get("label-info"):
        label = (entity["label-info"][0].get("label") or {}).get("name")
        fields.append((MO + "label", "label", label))
    if entity.get("media"):
        fields.append((MO + "format", "format", entity["media"][0].get("format")))
    return fields


def mb_recording_candidates(entity):
    fields = [
        (DCT + "title", "title", entity.get("title")),
        (MO + "duration", "duration", entity.get("length")),
        (MBO + "firstReleaseDate", "first release date",
         entity.get("first-release-date")),
        (MBO + "disambiguation", "disambiguation", entity.get("disambiguation")),
    ]
    if entity.get("artist-credit"):
        fields.append((FOAF + "maker", "artist",
                       entity["artist-credit"][0].get("name")))
    if entity.get("tags"):
        fields.append((MO + "tag", "tag", entity["tags"][0].get("name")))
    return fields


def mb_label_candidates(entity):
    return [
        (FOAF + "name", "name", entity.get("name")),
        (MBO + "type", "type", entity.get("type")),
        (MBO + "labelCode", "label code", entity.get("label-code")),
        (MBO + "country", "country", entity.get("country")),
        (MBO + "area", "area", _first_name(entity.get("area"))),
        (MBO + "disambiguation", "disambiguation", entity.get("disambiguation")),
    ]


MB_TYPES = {
    "artist": (["rock", "jazz", "electronic", "pop", "hip"],
               "aliases+tags+genres", mb_artist_candidates),
    "release": (["love", "live", "greatest", "symphony", "blue"],
                "labels+recordings+artist-credits+media", mb_release_candidates),
    "recording": (["light", "night", "heart", "time", "dream"],
                  "artist-credits+tags", mb_recording_candidates),
    "label": (["records", "music", "sound", "recordings", "entertainment"],
              "tags", mb_label_candidates),
}


def musicbrainz_resources():
    per_type = 30
    for mb_type, (queries, inc, mapper) in MB_TYPES.items():
        collected = []
        for query in queries:
            data = cached_json(
                f"mb_search_{mb_type}_{query}.json",
                lambda t=mb_type, q=query: mb_get(t, {"query": q, "limit": 12}))
            key = mb_type + "s" if mb_type != "release" else "releases"
            for item in data.get(key, data.get(mb_type + "s", [])):
                collected.append(item["id"])
        seen, ids = set(), []
        for mbid in collected:
            if mbid not in seen:
                seen.add(mbid)
                ids.append(mbid)
        for mbid in ids[:per_type]:
            entity = cached_json(
                f"mb_{mb_type}_{mbid}.json",
                lambda t=mb_type, i=mbid: mb_get(f"{t}/{i}", {"inc": inc}))
            uri = f"http://musicbrainz.org/{mb_type}/{mbid}"
            subject_label = entity.get("name") or entity.get("title") or mbid
            candidates = [(p_uri, p_label) for p_uri, p_label, value in mapper(entity)
                          if value]
            yield uri, subject_label, candidates


# --------------------------------------------------------------------------- #
# GeoNames (cities15000 dump for the sample + per-feature RDF documents)
# --------------------------------------------------------------------------- #
GEONAMES_DUMP = "https://download.geonames.org/export/dump/cities15000.zip"


def geonames_ids():
    path = _cache_path("cities15000.txt")
    if not os.path.exists(path):
        response = http_get(GEONAMES_DUMP)
        with zipfile.ZipFile(io.BytesIO(response.content)) as archive:
            data = archive.read("cities15000.txt").decode("utf-8")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(data)
    with open(path, encoding="utf-8") as handle:
        rows = [line.split("\t") for line in handle if line.strip()]
    rows.sort(key=lambda row: int(row[0]))
    # strided, deterministic sample across the whole (population>15000) file
    step = max(1, len(rows) // 200)
    return [(row[0], row[1]) for row in rows[::step]][:200]


def geonames_predicates(geoname_id):
    xml_text = cached_text(f"geonames_{geoname_id}.rdf",
                           lambda: http_get(
                               f"https://sws.geonames.org/{geoname_id}/about.rdf",
                               pause=1.0).text)
    time.sleep(0.4)
    predicates = set()
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError:
        return []
    for feature in root:
        for child in feature:
            tag = child.tag  # "{namespace}localname"
            if tag.startswith("{"):
                namespace, local = tag[1:].split("}", 1)
                predicates.add(namespace + local)
    return sorted(predicates)


def cached_text(name, producer):
    path = _cache_path(name)
    if os.path.exists(path):
        with open(path, encoding="utf-8") as handle:
            return handle.read()
    value = producer()
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(value)
    return value


GEONAMES_CONTENT_NAMESPACES = ("http://www.geonames.org/ontology#",
                               "http://www.w3.org/2003/01/geo/wgs84_pos#")


def geonames_resources():
    for geoname_id, name in geonames_ids():
        predicates = geonames_predicates(geoname_id)
        # GeoNames has no SPARQL endpoint, so no rdfs:label lookup is possible;
        # the component falls back to the (meaningful) URI local names
        candidates = [(uri, None) for uri in predicates]
        # ask only about the geographic content predicates (gn:/wgs84), not the
        # Creative-Commons / Dublin-Core provenance metadata every feature carries
        eligible = {uri for uri in predicates
                    if uri.startswith(GEONAMES_CONTENT_NAMESPACES)}
        yield f"https://sws.geonames.org/{geoname_id}/", name, candidates, eligible


# --------------------------------------------------------------------------- #
GENERATORS = {
    "dbpedia": lambda: assemble("DBpedia", DBPEDIA_ENDPOINT, dbpedia_resources()),
    "wikidata": lambda: assemble("Wikidata", WDQS, wikidata_resources()),
    "musicbrainz": lambda: assemble("MusicBrainz", MB, musicbrainz_resources()),
    "geonames": lambda: assemble("GeoNames",
                                 "https://sws.geonames.org/{id}/about.rdf",
                                 geonames_resources()),
}


def main():
    which = sys.argv[1] if len(sys.argv) > 1 else "all"
    targets = list(GENERATORS) if which == "all" else [which]
    for name in targets:
        print(f"\n=== generating {name} fixture ===")
        GENERATORS[name]()


if __name__ == "__main__":
    main()
