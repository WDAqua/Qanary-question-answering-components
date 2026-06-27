#!/usr/bin/env python3
"""Aggregate the per-component Python coverage.xml files into one Markdown report.

Each Python component is tested in its own virtual environment, so coverage is
produced per component (``<component>/coverage.xml``, Cobertura format). This
script sums the line counts across all of them, writes a Markdown table to
``python-coverage-summary.md`` (used for the PR comment) and appends it to the
GitHub Actions job summary. Run from the repository root after the tests.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

OUTFILE = "python-coverage-summary.md"


def main() -> int:
    rows = []
    total_valid = total_covered = 0

    for xml_path in sorted(glob.glob("*/coverage.xml")):
        component = os.path.dirname(xml_path)
        try:
            root = ET.parse(xml_path).getroot()
            valid = int(root.get("lines-valid", "0"))
            covered = int(root.get("lines-covered", "0"))
        except (ET.ParseError, ValueError, OSError):
            continue
        pct = (covered / valid * 100.0) if valid else 0.0
        rows.append((component, valid, covered, pct))
        total_valid += valid
        total_covered += covered

    overall = (total_covered / total_valid * 100.0) if total_valid else 0.0

    lines = ["## 🐍 Python component code coverage", ""]
    if rows:
        lines.append(
            f"**Overall: {overall:.1f}%** &nbsp; ({total_covered}/{total_valid} lines, "
            f"{len(rows)} components)"
        )
        lines += ["", "| Component | Lines | Covered | Coverage |", "|---|---:|---:|---:|"]
        for component, valid, covered, pct in rows:
            lines.append(f"| `{component}` | {valid} | {covered} | {pct:.1f}% |")
    else:
        lines.append("_No Python coverage data was produced._")
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
