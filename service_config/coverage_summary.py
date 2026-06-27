#!/usr/bin/env python3
"""Aggregate the per-component Python coverage.xml files into one Markdown report.

Lists *every* Python component the CI tests (the ``[qQ]anary-component…Python-…``
dirs, minus git submodules) — components without tests (no ``coverage.xml``) are
shown as "no tests" so coverage gaps are visible. Each tested component is run in
its own virtual environment and produces ``<component>/coverage.xml`` (Cobertura
format); this sums the line counts, writes ``python-coverage-summary.md`` (used
for the PR comment) and appends to the GitHub Actions job summary. Run from the
repository root after the tests.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

OUTFILE = "python-coverage-summary.md"
COMPONENT_RE = re.compile(r"[qQ]anary-component.*Python-[a-zA-Z]+$")


def submodule_paths():
    paths = set()
    try:
        with open(".gitmodules", encoding="utf-8") as fh:
            for line in fh:
                line = line.strip()
                if line.startswith("path") and "=" in line:
                    paths.add(line.split("=", 1)[1].strip())
    except OSError:
        pass
    return paths


def python_components():
    subs = submodule_paths()
    return sorted(
        (d for d in os.listdir(".")
         if os.path.isdir(d) and COMPONENT_RE.search(d) and d not in subs),
        key=str.lower,
    )


def short(name):
    for prefix in ("qanary-component-", "Qanary-component-"):
        if name.startswith(prefix):
            return name[len(prefix):]
    return name


def line_total(coverage_xml):
    """(valid, covered) from a Cobertura coverage.xml, or None if unavailable."""
    try:
        root = ET.parse(coverage_xml).getroot()
        return int(root.get("lines-valid", "0")), int(root.get("lines-covered", "0"))
    except (ET.ParseError, ValueError, OSError):
        return None


def main() -> int:
    rows = []        # (component, valid, covered, pct)
    untested = []    # components without coverage data
    total_valid = total_covered = 0

    for component in python_components():
        counts = line_total(os.path.join(component, "coverage.xml"))
        if counts is None or counts[0] == 0:
            untested.append(component)
            continue
        valid, covered = counts
        rows.append((component, valid, covered, covered / valid * 100.0))
        total_valid += valid
        total_covered += covered

    overall = (total_covered / total_valid * 100.0) if total_valid else 0.0
    tested = len(rows)
    total = tested + len(untested)

    lines = ["## 🐍 Python component code coverage", ""]
    if total:
        lines.append(
            f"**Overall: {overall:.1f}%** &nbsp; ({total_covered}/{total_valid} lines) — "
            f"{tested}/{total} components have tests"
        )
        lines += ["", "| Component | Lines | Covered | Coverage |", "|---|---:|---:|---:|"]
        for component, valid, covered, pct in rows:
            lines.append(f"| `{short(component)}` | {valid} | {covered} | {pct:.1f}% |")
        for component in untested:
            lines.append(f"| `{short(component)}` | — | — | _no tests_ |")
    else:
        lines.append("_No Python components found._")
    markdown = "\n".join(lines) + "\n"

    with open(OUTFILE, "w", encoding="utf-8") as fh:
        fh.write(markdown)
    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as fh:
            fh.write(markdown)
    sys.stdout.write(markdown)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
