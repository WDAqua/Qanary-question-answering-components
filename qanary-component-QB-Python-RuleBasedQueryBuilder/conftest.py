"""pytest hooks for the rule-based query-builder suites.

Two responsibilities:

1. **Verbose per-case lines** — in verbose mode (``pytest -v``) each E2E test
   prints the input entities, the question and the generated SPARQL query,
   trailed by an ok/failed symbol, e.g.::

    ...::test_e2e_question[q001-forward] http://dbpedia.org/resource/Germany  |  What is the capital of Germany?  ->  SELECT DISTINCT ?answer WHERE { ... }  ✔ ok

   A test stashes its line on the node as ``_kg_report_line``; tests without a
   line keep pytest's default reporting.

2. **Complete run report in a temporary file** — after every run a full report
   (summary counts plus one line per executed test, including the stashed
   question/query lines) is written to the system temp directory (override the
   location with the ``QB_TEST_REPORT_FILE`` environment variable). The path is
   printed at the end of the pytest output.
"""
import os
import time
import tempfile

import pytest

_results = []
_report_path = None


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item, call):
    outcome = yield
    report = outcome.get_result()
    if report.when == "call":
        report._kg_line = getattr(item, "_kg_report_line", None)
        _results.append({
            "nodeid": report.nodeid,
            "outcome": report.outcome,
            "duration": report.duration,
            "detail": report._kg_line,
        })
    elif report.when == "setup" and report.skipped:
        # tests skipped before their call phase (e.g. the live QALD-9 suite
        # without QALD9_LIVE_TEST_ACTIVE) still appear in the report
        _results.append({
            "nodeid": report.nodeid,
            "outcome": "skipped",
            "duration": report.duration,
            "detail": None,
        })


def pytest_report_teststatus(report, config):
    line = getattr(report, "_kg_line", None)
    if report.when == "call" and line:
        if report.passed:
            return "passed", ".", f"{line}  ✔ ok"
        if report.failed:
            return "failed", "F", f"{line}  ✘ failed"
    return None


def pytest_sessionfinish(session, exitstatus):
    global _report_path
    if not _results:
        return
    _report_path = os.environ.get("QB_TEST_REPORT_FILE") or os.path.join(
        tempfile.gettempdir(),
        f"qanary-qb-rulebased-test-report-{time.strftime('%Y%m%d-%H%M%S')}"
        f"-{os.getpid()}.txt")
    counts = {}
    for result in _results:
        counts[result["outcome"]] = counts.get(result["outcome"], 0) + 1
    with open(_report_path, "w", encoding="utf-8") as handle:
        handle.write("Qanary QB-Python-RuleBasedQueryBuilder — test run report\n")
        handle.write(f"finished: {time.strftime('%Y-%m-%d %H:%M:%S')}  "
                     f"exit status: {exitstatus}\n")
        handle.write("summary:  " + "  ".join(
            f"{outcome}={count}" for outcome, count in sorted(counts.items()))
            + f"  total={len(_results)}\n")
        handle.write("=" * 78 + "\n")
        symbols = {"passed": "✔ ok", "failed": "✘ failed", "skipped": "○ skipped"}
        for result in _results:
            handle.write(f"[{symbols.get(result['outcome'], result['outcome'])}] "
                         f"{result['nodeid']}  ({result['duration']:.3f}s)\n")
            if result["detail"]:
                handle.write(f"    {result['detail']}\n")


def pytest_terminal_summary(terminalreporter):
    if _report_path:
        terminalreporter.write_sep(
            "-", f"complete test report written to {_report_path}")
