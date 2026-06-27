#!/usr/bin/env python3
"""Aggregate the per-module JaCoCo ``jacoco.xml`` reports into one Markdown table.

The Java test run produces one ``<module>/target/site/jacoco/jacoco.xml`` per
component (the report-level ``<counter type="LINE">`` holds that module's totals).
This sums them into a per-component + overall line-coverage table, written to
``java-coverage-summary.md`` (used for the PR comment) and appended to the GitHub
Actions job summary. Run from the repository root after the tests.

A small dedicated parser is used instead of a third-party action because the
latter chokes ("Invalid report") when handed ~50 separate module reports.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

OUTFILE = "java-coverage-summary.md"
PREFIX = "Qanary component: "


def line_counter(root):
    """Return (missed, covered) from the report-level LINE counter, or None."""
    for counter in root.findall("counter"):  # direct children = report totals
        if counter.get("type") == "LINE":
            return int(counter.get("missed", "0")), int(counter.get("covered", "0"))
    return None


def main() -> int:
    rows = []
    total_valid = total_covered = 0

    for xml_path in sorted(glob.glob("**/target/site/jacoco/jacoco.xml", recursive=True)):
        try:
            root = ET.parse(xml_path).getroot()
        except (ET.ParseError, OSError):
            continue
        counts = line_counter(root)
        if counts is None:
            continue
        missed, covered = counts
        valid = missed + covered
        name = (root.get("name") or xml_path).removeprefix(PREFIX)
        pct = (covered / valid * 100.0) if valid else 0.0
        rows.append((name, valid, covered, pct))
        total_valid += valid
        total_covered += covered

    overall = (total_covered / total_valid * 100.0) if total_valid else 0.0

    lines = ["## ☕ Java component code coverage", ""]
    if rows:
        lines.append(
            f"**Overall: {overall:.1f}%** &nbsp; ({total_covered}/{total_valid} lines, "
            f"{len(rows)} components)"
        )
        lines += ["", "| Component | Lines | Covered | Coverage |", "|---|---:|---:|---:|"]
        for name, valid, covered, pct in rows:
            lines.append(f"| `{name}` | {valid} | {covered} | {pct:.1f}% |")
    else:
        lines.append("_No JaCoCo coverage data was produced._")
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
