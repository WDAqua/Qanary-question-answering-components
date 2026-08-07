#!/bin/bash

# Build and push the Docker images of the Python Qanary components.
#
# Every step is checked: a component that cannot be built or pushed is collected and
# reported at the end, and the script exits non-zero. Without that the loop happily
# continued after a failed "docker build", so a broken component produced no image
# while the workflow step still reported success.

set -uo pipefail

# all Python component directories ...
mapfile -t candidates < <(ls -1 | grep -P "[qQ]anary-component.*Python-[a-zA-Z]+$")
# ... except the submodules, which are external component repositories built elsewhere.
# (an explicit membership test rather than "comm", which needs both inputs sorted the
# same way -- "ls" sorts by locale, "comm" compares bytewise)
mapfile -t submodules < <(git config --file .gitmodules --get-regexp path | awk '{ print $2 }')

components=()
for dir in "${candidates[@]}"
do
  is_submodule=false
  for submodule in "${submodules[@]}"
  do
    if [ "${dir}" = "${submodule}" ]
    then
      is_submodule=true
      break
    fi
  done

  if [ "${is_submodule}" = true ]
  then
    echo "skipping ${dir} (submodule, built in its own repository)"
  else
    components+=("${dir}")
  fi
done

if [ ${#components[@]} -eq 0 ]
then
  echo "No Python component directory found - refusing to report success"
  exit 2
fi

# the components moved from the app/ to the component/ layout, both are still in use
find_version() {
  local dir="$1"
  local file
  for file in "${dir}/component/__init__.py" "${dir}/app/__init__.py"
  do
    if [ -f "${file}" ]
    then
      local version
      version=$(grep -oP '(?<=version = ")[^"]*' "${file}")
      if [ -n "${version}" ]
      then
        echo "${version}"
        return 0
      fi
    fi
  done
  return 1
}

built=0
failed=()

for dir in "${components[@]}"
do
  image=$(echo "${dir}" | tr "[:upper:]" "[:lower:]")

  echo "::group::Building ${image}"

  version=$(find_version "${dir}")
  if [ -z "${version}" ]
  then
    echo "::endgroup::"
    echo "no version found for ${dir} (expected 'version = \"...\"' in component/__init__.py or app/__init__.py)"
    failed+=("${dir}: no version")
    continue
  fi

  latest_image_name="qanary/${image}:latest"
  versioned_image_name="qanary/${image}:${version}"

  # the directory is the build context, so no "cd" is needed and a wrong working
  # directory cannot make a later component build the wrong sources
  if ! docker build -t "${versioned_image_name}" -t "${latest_image_name}" "${dir}"
  then
    echo "::endgroup::"
    echo "docker build failed for ${dir}"
    failed+=("${dir}: build failed")
    continue
  fi
  echo "::endgroup::"

  echo "Pushing ${versioned_image_name} and ${latest_image_name}"
  if ! docker push "${versioned_image_name}" || ! docker push "${latest_image_name}"
  then
    echo "docker push failed for ${image}"
    failed+=("${dir}: push failed")
  else
    built=$((built + 1))
  fi

  # free the runner's disk again, the ML based images are large
  docker rmi -f "${versioned_image_name}" "${latest_image_name}" || true
done

echo "Successfully built and pushed ${built} of ${#components[@]} Python component image(s)"

if [ ${#failed[@]} -gt 0 ]
then
  echo "The following component(s) failed:"
  for entry in "${failed[@]}"
  do
    echo "  - ${entry}"
  done
  exit 3
fi

if [ "${built}" -eq 0 ]
then
  echo "No Python component image was built - refusing to report success"
  exit 4
fi
