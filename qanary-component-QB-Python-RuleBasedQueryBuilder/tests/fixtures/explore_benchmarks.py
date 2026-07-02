#!/usr/bin/env python3
"""Explore the QALD-10 and LC-QuAD benchmarks (development-time tool).

Downloads both datasets and prints the distribution of query forms
(SELECT/ASK/COUNT), triple-pattern counts, entity/predicate counts and the
dominant structural shapes (E = entity, P = predicate, ? = variable). The
results — summarised in ``benchmark_analysis.md`` — informed the rule set of
the component and the composition of ``questions.json``.

Needs network access; the test suites never run this.
"""
import io
import json
import re
import collections
import urllib.request

QALD10_URL = ("https://raw.githubusercontent.com/KGQA/QALD_10/main/"
              "data/qald_10/qald_10.json")
LCQUAD_URL = ("https://raw.githubusercontent.com/AskNowQA/LC-QuAD/data/"
              "train-data.json")


def fetch(url):
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.load(io.TextIOWrapper(response, encoding="utf-8"))


def classify(sparql):
    condensed = " ".join(sparql.split())
    form = ("ASK" if re.search(r"\bASK\b", condensed, re.I)
            else "COUNT" if re.search(r"COUNT\s*\(", condensed, re.I)
            else "SELECT")
    body = re.search(r"\{(.*)\}", condensed)
    triple_patterns = ([t for t in re.split(r"\s\.\s", body.group(1)) if t.strip()]
                       if body else [])
    entities = len(re.findall(
        r"<http://dbpedia\.org/resource/[^>]+>|wd:Q\d+", condensed))
    predicates = len(set(re.findall(
        r"<http://dbpedia\.org/(?:ontology|property)/[^>]+>|wdt:P\d+", condensed)))
    return form, len(triple_patterns), entities, predicates


def shape(sparql):
    condensed = " ".join(sparql.split())
    body = re.search(r"\{(.*)\}", condensed)
    if not body:
        return None
    def kind(term):
        if term.startswith("<http://dbpedia.org/resource"):
            return "E"
        return "?" if term.startswith("?") else "o"
    hops = []
    for pattern in re.split(r"\s\.\s*", body.group(1)):
        parts = pattern.split()
        if len(parts) >= 3:
            hops.append(f"{kind(parts[0])}-P-{kind(' '.join(parts[2:]))}")
    return "|".join(hops)


def report(name, questions, get_sparql, get_text):
    print(f"\n================ {name} ({len(questions)} questions) ================")
    forms = collections.Counter()
    entity_predicate = collections.Counter()
    shapes = collections.Counter()
    examples = {}
    for question in questions:
        sparql = get_sparql(question)
        form, n_triples, entities, predicates = classify(sparql)
        forms[(form, n_triples)] += 1
        entity_predicate[(min(entities, 3), min(predicates, 3))] += 1
        key = (form, shape(sparql))
        shapes[key] += 1
        examples.setdefault(key, get_text(question))
    print("query forms / triple counts:")
    for key in sorted(forms, key=lambda k: -forms[k])[:8]:
        print(f"  {key[0]:6s} triples={key[1]}  n={forms[key]}")
    print("entity/predicate counts (capped at 3):")
    for key in sorted(entity_predicate, key=lambda k: -entity_predicate[k])[:6]:
        print(f"  entities={key[0]} predicates={key[1]}  n={entity_predicate[key]}")
    print("dominant structural shapes:")
    for key in sorted(shapes, key=lambda k: -shapes[k])[:10]:
        print(f"  {key[0]:6s} {str(key[1]):24s} n={shapes[key]:5d}  "
              f"e.g. {str(examples[key])[:70]!r}")


def main():
    lcquad = fetch(LCQUAD_URL)
    report("LC-QuAD 1.0 train", lcquad,
           lambda q: q["sparql_query"], lambda q: q["corrected_question"])
    qald = fetch(QALD10_URL)["questions"]
    report("QALD-10", qald,
           lambda q: q["query"]["sparql"],
           lambda q: next((t["string"] for t in q["question"]
                           if t["language"] == "en"), ""))


if __name__ == "__main__":
    main()
