"""Predicate identification over a 100-resource subset of GeoNames.

Fixture: ``kg_fixtures/geonames.json``. GeoNames has no SPARQL endpoint, so the
subset is built from the ``cities15000`` dump (resource sample) plus each
feature's RDF document (``sws.geonames.org/{id}/about.rdf``) for the real
``gn:`` / ``wgs84_pos:`` predicate inventory. Each case gives a natural-language
question and a resource URI; the component must return the expected predicate
URI (ranked on the meaningful URI local names, as GeoNames offers no predicate
``rdfs:label``). Run ``pytest -v`` to see, per case,
``resource | question -> predicate`` with an ok/failed symbol.
"""
import pytest

from tests import kg_cases

KG = "geonames"
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
