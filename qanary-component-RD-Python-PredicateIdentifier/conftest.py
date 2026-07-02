"""pytest hooks for the predicate-identification suites.

In verbose mode (``pytest -v``) each knowledge-graph test prints the resource
URI, the question and the computed predicate URI, trailed by an ok/failed
symbol, e.g.::

    ...::test_identify_predicate[Germany#1] http://dbpedia.org/resource/Germany  |  Tell me the capital of Germany.  ->  http://dbpedia.org/ontology/capital  ✔ ok

The test stashes the "resource | question -> predicate" line on its node (see
``tests/kg_cases.record_report_line``); these hooks turn it into the per-test
status word and append the symbol from the actual pass/fail outcome. Tests that
do not stash a line (e.g. the unit tests) keep pytest's default reporting.
"""
import pytest


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    report = outcome.get_result()
    if report.when == "call":
        report._kg_line = getattr(item, "_kg_report_line", None)


def pytest_report_teststatus(report, config):
    line = getattr(report, "_kg_line", None)
    if report.when == "call" and line:
        if report.passed:
            return "passed", ".", f"{line}  ✔ ok"
        if report.failed:
            return "failed", "F", f"{line}  ✘ failed"
    return None
