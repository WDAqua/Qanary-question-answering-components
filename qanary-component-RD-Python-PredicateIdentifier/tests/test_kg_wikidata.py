"""Predicate identification over a 100-resource subset of Wikidata.

Fixture: ``kg_fixtures/wikidata.json`` (resource sample from WDQS, claims and
property labels from the MediaWiki wbgetentities API). Each case gives a
natural-language question and a resource URI; the expected output is the
predicate URI — an opaque ``wdt:P…`` direct-claim URI, so this suite also
exercises the component's Wikidata property-label resolution. Run ``pytest -v``
to see, per case, ``resource | question -> predicate`` with an ok/failed symbol.
"""
import pytest

from tests import kg_cases

KG = "wikidata"
component = kg_cases.load_component()
CANDIDATES, CASES, IDS = kg_cases.load_kg(KG)


def test_fixture_covers_100_resources_with_enough_cases():
    resource_count, case_count = kg_cases.fixture_summary(KG)
    assert resource_count == 100
    assert case_count >= 300  # at least three cases per resource


@pytest.mark.parametrize("resource,question,expected", CASES, ids=IDS)
def test_identify_predicate(resource, question, expected, request):
    # input: a natural-language question + the resource URI
    predicate, score = kg_cases.identify_predicate(component, CANDIDATES, resource, question)
    # output: the identified predicate URI (printed with ok/failed in verbose mode)
    kg_cases.record_report_line(request, resource, question, predicate)
    assert predicate == expected, kg_cases.failure_message(
        resource, question, expected, predicate, score)
    assert score >= component.MIN_MATCH_SCORE
