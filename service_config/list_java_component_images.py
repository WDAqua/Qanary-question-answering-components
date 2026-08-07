#!/usr/bin/env python3
"""List the Docker images to build for the Java Qanary components.

Prints one tab separated record per component that Maven actually produced a JAR for:

    <component directory>\t<image name with prefix>\t<version>\t<jar path>

Components without a JAR are reported on stderr and skipped: not every component
directory is part of the Maven reactor (see the profiles in the root pom.xml), so
their target/ directory stays empty and there is nothing to put into an image.
"""

import os
import re
import sys
from glob import glob


def read(path):
    with open(path, encoding="utf-8", errors="replace") as handle:
        return handle.read()


def property_of(pom, name):
    match = re.search(r"<%s>([^<]+)</%s>" % (name, name), pom)
    return match.group(1) if match else None


def project_version(pom):
    """The project's own <version>, i.e. the first one after the parent block."""
    _, _, after_parent = pom.partition("</parent>")
    match = re.search(r"<version>([^<]+)</version>", after_parent or pom)
    return match.group(1) if match else None


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else "."
    found = 0
    skipped = []

    for directory in sorted(glob(os.path.join(root, "qanary-component-*"))):
        pom_path = os.path.join(directory, "pom.xml")
        dockerfile = os.path.join(directory, "Dockerfile")
        # Python components have no pom.xml, they are handled by build_python_images.sh
        if not (os.path.isfile(pom_path) and os.path.isfile(dockerfile)):
            continue

        pom = read(pom_path)
        prefix = property_of(pom, "docker.image.prefix")
        name = property_of(pom, "docker.image.name")
        version = project_version(pom)
        final_name = property_of(pom, "finalName")

        if not (prefix and name and version and final_name):
            skipped.append("%s (incomplete pom metadata)" % directory)
            continue

        jar = os.path.join(directory, "target", "%s.jar" % final_name)
        if not os.path.isfile(jar):
            skipped.append("%s (no %s)" % (directory, os.path.basename(jar)))
            continue

        print("%s\t%s/%s\t%s\ttarget/%s.jar" % (directory, prefix, name, version, final_name))
        found += 1

    for entry in skipped:
        print("skipping %s" % entry, file=sys.stderr)
    print("%d component image(s) to build, %d skipped" % (found, len(skipped)), file=sys.stderr)

    # Never let the pipeline report success while silently producing nothing.
    if found == 0:
        print("ERROR: no component JARs found - did the Maven build run?", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
