#!/usr/bin/env python3
"""Verify that a deployment actually reached the deployment server.

A green build step proves nothing: the image build silently published nothing for
about seven months while every run reported success, and LD-Shuyo crash-looped on
the server without anybody noticing. This script turns "the pipeline is green" into
"the components are running the code we just built" by asking the deployment itself:

1. the Qanary pipeline's registry has to be reachable and know about components,
2. every registered component has to report UP,
3. every registered component has to have restarted after the deployment was
   triggered (otherwise the updater did not roll the new images out),
4. every Java component that is deployed and serves an OpenAPI description has to
   report the version this repository currently declares in its pom.xml.

Check 4 is best effort: components whose port cannot be reached from the runner are
reported as "not checked" rather than failing the build, because not every port of
the deployment host is reachable from outside. A component that answers with the
*wrong* version is always a failure.

Exit code 0 means the deployment was verified, anything else means it was not.
"""

import argparse
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone
from glob import glob

DEFAULT_REGISTRY = "http://demos.swe.htwk-leipzig.de:40111/instances"


def fetch_json(url, timeout):
    request = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def parse_timestamp(value):
    """Parse an ISO 8601 timestamp; Virtuoso/Spring emit up to 9 fractional digits."""
    if not value:
        return None
    text = value.strip().replace("Z", "+00:00")
    # datetime.fromisoformat accepts at most 6 fractional digits
    text = re.sub(r"(\.\d{6})\d+", r"\1", text)
    try:
        stamp = datetime.fromisoformat(text)
    except ValueError:
        return None
    return stamp if stamp.tzinfo else stamp.replace(tzinfo=timezone.utc)


def read(path):
    with open(path, encoding="utf-8", errors="replace") as handle:
        return handle.read()


def summarize(entries, limit=4):
    """Keep the polling output readable when many components are affected."""
    if len(entries) <= limit:
        return ", ".join(entries)
    return "%s, ... (%d more)" % (", ".join(entries[:limit]), len(entries) - limit)


def component_versions(root):
    """Map the Docker image name of every Java component to its declared version."""
    versions = {}
    for directory in sorted(glob(os.path.join(root, "qanary-component-*"))):
        pom_path = os.path.join(directory, "pom.xml")
        if not os.path.isfile(pom_path):
            continue
        pom = read(pom_path)
        name = re.search(r"<docker\.image\.name>([^<]+)</docker\.image\.name>", pom)
        _, _, after_parent = pom.partition("</parent>")
        version = re.search(r"<version>([^<]+)</version>", after_parent or pom)
        if name and version:
            versions[name.group(1)] = version.group(1)
    return versions


def deployed_services(service_config_path):
    """Yield (host port, image name) for every service of the deployment."""
    services = []
    if not os.path.isfile(service_config_path):
        return services
    config = json.loads(read(service_config_path))
    for service in config.get("services", []):
        image = str(service.get("image", "")).split("/")[-1]
        ports = str(service.get("port", ""))
        for mapping in ports.split(","):
            host_port = mapping.split(":")[0].strip()
            if host_port.isdigit() and image:
                services.append((host_port, image))
                break
    return services


def check_registry(instances, since):
    """Return (not_up, not_restarted) for the registered components."""
    not_up, not_restarted = [], []
    for instance in instances:
        registration = instance.get("registration", {})
        name = registration.get("name", "<unnamed>")
        status = instance.get("statusInfo", {}).get("status")
        if status != "UP":
            not_up.append("%s (%s)" % (name, status))
        startup = parse_timestamp(registration.get("metadata", {}).get("startup"))
        if since is not None and (startup is None or startup < since):
            not_restarted.append("%s (startup %s)" % (name, startup))
    return not_up, not_restarted


def check_versions(host, services, versions, timeout, api_docs_path):
    """Compare the version each deployed component serves with the declared one."""
    mismatched, unreachable, unreported, verified = [], [], [], []
    for port, image in services:
        expected = versions.get(image)
        if expected is None:
            continue  # not a Java component of this repository (e.g. the Python ones)
        url = "http://%s:%s%s" % (host, port, api_docs_path)
        try:
            running = fetch_json(url, timeout).get("info", {}).get("version")
        except Exception as error:  # noqa: BLE001 - any transport problem means "unknown"
            unreachable.append("%s:%s (%s)" % (image, port, type(error).__name__))
            continue
        # Components without a customOpenAPI bean serve springdoc's placeholder ("v0"),
        # which says nothing about the deployed image - do not read that as a mismatch.
        if not running or not re.match(r"^\d+\.\d+", str(running)):
            unreported.append("%s:%s (serves %r)" % (image, port, running))
        elif running != expected:
            mismatched.append("%s: deployed %s, repository declares %s" % (image, running, expected))
        else:
            verified.append("%s %s" % (image, running))
    return mismatched, unreachable, unreported, verified


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", default=DEFAULT_REGISTRY)
    parser.add_argument("--since", help="ISO 8601 time the deployment was triggered")
    parser.add_argument("--timeout-minutes", type=float, default=15.0)
    parser.add_argument("--poll-seconds", type=float, default=20.0)
    parser.add_argument("--root", default=".")
    parser.add_argument("--service-config", default=None)
    parser.add_argument("--api-docs-path", default="/api-docs")
    parser.add_argument("--http-timeout", type=float, default=20.0)
    args = parser.parse_args()

    since = parse_timestamp(args.since) if args.since else None
    if args.since and since is None:
        print("Could not parse --since %r" % args.since, file=sys.stderr)
        return 2
    if since is not None:
        # tolerate small clock differences between the runner and the server
        since -= timedelta(minutes=2)
        print("Requiring every component to have restarted after %s" % since.isoformat())

    deadline = time.monotonic() + args.timeout_minutes * 60
    instances, last_problem = None, "the registry was never reached"

    while True:
        try:
            instances = fetch_json(args.registry, args.http_timeout)
        except Exception as error:  # noqa: BLE001
            last_problem = "registry %s not reachable (%s)" % (args.registry, error)
            instances = None
        else:
            if not instances:
                last_problem = "the registry knows no component at all"
            else:
                not_up, not_restarted = check_registry(instances, since)
                if not not_up and not not_restarted:
                    break
                problems = []
                if not_up:
                    problems.append("%d not UP: %s" % (len(not_up), summarize(not_up)))
                if not_restarted:
                    problems.append("%d did not restart: %s" % (len(not_restarted), summarize(not_restarted)))
                last_problem = "; ".join(problems)

        if time.monotonic() >= deadline:
            print("\nDEPLOYMENT NOT VERIFIED after %g minutes" % args.timeout_minutes, file=sys.stderr)
            print("  %s" % last_problem, file=sys.stderr)
            if instances is None:
                print(
                    "  If the deployment host is not reachable from this runner, this job "
                    "cannot verify anything and should be disabled rather than ignored.",
                    file=sys.stderr,
                )
            return 1

        print("waiting for the deployment: %s" % last_problem)
        time.sleep(args.poll_seconds)

    print("\n%d registered component(s), all UP:" % len(instances))
    for instance in sorted(instances, key=lambda i: i.get("registration", {}).get("name", "")):
        registration = instance.get("registration", {})
        print("  %-58s %s" % (registration.get("name"), registration.get("metadata", {}).get("startup")))

    # the versions the components actually serve
    host = re.sub(r"^\w+://", "", args.registry).split(":")[0].split("/")[0]
    service_config = args.service_config or os.path.join(args.root, "service_config", "service_config.json")
    mismatched, unreachable, unreported, verified = check_versions(
        host, deployed_services(service_config), component_versions(args.root), args.http_timeout, args.api_docs_path
    )

    if verified:
        print("\nversion verified for %d component(s):" % len(verified))
        for entry in verified:
            print("  %s" % entry)
    if unreported:
        print("\n%d component(s) do not report their version (no customOpenAPI bean):" % len(unreported))
        for entry in unreported:
            print("  %s" % entry)
    if unreachable:
        print("\nversion not checked for %d component(s) (port not reachable from here):" % len(unreachable))
        for entry in unreachable:
            print("  %s" % entry)
    if mismatched:
        print("\nDEPLOYED VERSION DOES NOT MATCH THIS REPOSITORY:", file=sys.stderr)
        for entry in mismatched:
            print("  %s" % entry, file=sys.stderr)
        return 1

    print("\nDeployment verified.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
