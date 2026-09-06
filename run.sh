#!/usr/bin/env bash
#
# Thin wrapper that accepts the project's short command line and forwards it to Maven.
#
#   ./run.sh clean verify -PTEST -user=User1 -suiteXmlFolder=Login \
#            -suiteXmlFile=testsuite.xml --headless -browser=chrome
#
# Maven itself only understands -Dkey=value, so `-user=User1`, `-browser=chrome`,
# `--headless` and friends are rewritten here. Everything Maven already knows
# (goals, phases, -P, -D, -o, -X, ...) is passed through untouched.
#
set -euo pipefail

MAVEN_ARGS=()
HAS_GOAL=0

for arg in "$@"; do
  case "$arg" in
    --headless)
      MAVEN_ARGS+=("-Dheadless=true")
      ;;
    --headed)
      MAVEN_ARGS+=("-Dheadless=false")
      ;;
    -D*|-P*)
      MAVEN_ARGS+=("$arg")
      ;;
    --*=*)
      # --user=User1  ->  -Duser=User1
      key="${arg%%=*}"; key="${key#--}"
      MAVEN_ARGS+=("-D${key}=${arg#*=}")
      ;;
    -*=*)
      # -user=User1  ->  -Duser=User1
      key="${arg%%=*}"; key="${key#-}"
      MAVEN_ARGS+=("-D${key}=${arg#*=}")
      ;;
    -*)
      MAVEN_ARGS+=("$arg")
      ;;
    *)
      HAS_GOAL=1
      MAVEN_ARGS+=("$arg")
      ;;
  esac
done

# No phase given? Run the usual one.
if [ "$HAS_GOAL" -eq 0 ]; then
  MAVEN_ARGS=("clean" "verify" "${MAVEN_ARGS[@]}")
fi

echo "mvn ${MAVEN_ARGS[*]}"
exec mvn "${MAVEN_ARGS[@]}"
