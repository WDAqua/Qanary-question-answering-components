#!/usr/bin/env python3
"""Aggregate the per-module JaCoCo ``jacoco.xml`` reports into one Markdown table.

Lists *every* Java component built by the default Maven profile — components
without executable tests (no ``jacoco.exec`` -> no report) are shown as "no
tests" so coverage gaps are visible. The per-module report total is the
report-level ``<counter type="LINE">`` in ``<module>/target/site/jacoco/jacoco.xml``.
Writes ``java-coverage-summary.md`` (used for the PR comment) and appends to the
GitHub Actions job summary. Run from the repository root after the tests.

A small dedicated parser is used instead of a third-party action because the
latter chokes ("Invalid report") when handed ~50 separate module reports.
"""
import os
import sys
import xml.etree.ElementTree as ET

NS = "{http://maven.apache.org/POM/4.0.0}"
OUTFILE = "java-coverage-summary.md"


def active_modules(pom="pom.xml"):
    """Module directories of the default-active Maven profile (the set CI builds)."""
    try:
        root = ET.parse(pom).getroot()
    except (ET.ParseError, OSError):
        return []
    profiles = root.find(f"{NS}profiles")
    chosen = None
    if profiles is not None:
        for profile in profiles.findall(f"{NS}profile"):
            activation = profile.find(f"{NS}activation")
            default = activation is not None and (
                (activation.findtext(f"{NS}activeByDefault") or "").strip() == "true"
            )
            if default and profile.find(f"{NS}modules") is not None:
                chosen = profile
                break
    container = chosen if chosen is not None else root
    mods = container.find(f"{NS}modules")
    if mods is None:
        return []
    return [m.text.strip() for m in mods.findall(f"{NS}module") if m.text and m.text.strip()]


def line_total(jacoco_xml):
    """(missed, covered) from the report-level LINE counter, or None if unavailable."""
    try:
        root = ET.parse(jacoco_xml).getroot()
    except (ET.ParseError, OSError):
        return None
    for counter in root.findall("counter"):  # direct children = report totals
        if counter.get("type") == "LINE":
            return int(counter.get("missed", "0")), int(counter.get("covered", "0"))
    return None


def short(name):
    for prefix in ("qanary-component-", "Qanary-component-"):
        if name.startswith(prefix):
            return name[len(prefix):]
    return name


def main() -> int:
    modules = active_modules()
    rows = []          # (name, valid, covered, pct) for tested modules
    untested = []      # names without test execution
    total_valid = total_covered = 0

    for module in sorted(modules, key=str.lower):
        counts = line_total(os.path.join(module, "target", "site", "jacoco", "jacoco.xml"))
        if counts is None:
            untested.append(short(module))
            continue
        missed, covered = counts
        valid = missed + covered
        if valid == 0:
            untested.append(short(module))
            continue
        rows.append((short(module), valid, covered, covered / valid * 100.0))
        total_valid += valid
        total_covered += covered

    overall = (total_covered / total_valid * 100.0) if total_valid else 0.0
    tested = len(rows)
    total = tested + len(untested)

    lines = ["## ☕ Java component code coverage", ""]
    if total:
        lines.append(
            f"**Overall: {overall:.1f}%** &nbsp; ({total_covered}/{total_valid} lines) — "
            f"{tested}/{total} components have tests"
        )
        lines += ["", "| Component | Lines | Covered | Coverage |", "|---|---:|---:|---:|"]
        for name, valid, covered, pct in rows:
            lines.append(f"| `{name}` | {valid} | {covered} | {pct:.1f}% |")
        for name in untested:
            lines.append(f"| `{name}` | — | — | _no tests_ |")
    else:
        lines.append("_No Java components found._")
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
