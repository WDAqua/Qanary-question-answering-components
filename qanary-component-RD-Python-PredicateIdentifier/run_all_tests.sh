#!/usr/bin/env bash
#
# Run the full test suite for the RD Python Predicate Identifier component.
#
# Creates (or reuses) a local virtual environment, installs the requirements and
# runs pytest. The tests are offline and deterministic, so no running triplestore
# or network access is needed to execute them — only the first-time dependency
# install needs the network.
#
# Usage:
#   ./run_all_tests.sh                            # run every test
#   ./run_all_tests.sh -v                         # verbose: print
#                                                 #   resource | question -> predicate  ✔ ok / ✘ failed
#   ./run_all_tests.sh tests/test_kg_dbpedia.py   # run a single suite
#   ./run_all_tests.sh -k capital                 # any pytest arguments are forwarded
#   ./run_all_tests.sh --cov=. --cov-report=term  # with coverage
#
# Environment:
#   VENV_DIR   virtual environment directory to create/use (default: venv)
#
set -euo pipefail

# always operate from the component directory (this script's location)
cd "$(dirname "$0")"

VENV_DIR="${VENV_DIR:-venv}"

# create the virtual environment on first run
if [ ! -d "$VENV_DIR" ]; then
    echo ">> creating virtual environment in '$VENV_DIR'"
    python3 -m venv "$VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

# install dependencies once (pytest and pytest-env are listed in requirements.txt);
# delete the virtual environment to force a fresh install
if ! python -c "import pytest" >/dev/null 2>&1; then
    echo ">> installing dependencies from requirements.txt"
    python -m pip install --upgrade pip >/dev/null
    python -m pip install -r requirements.txt
fi

# run the tests, forwarding any arguments (e.g. -v, -k, a path) to pytest
echo ">> running tests"
exec python -m pytest "$@"
