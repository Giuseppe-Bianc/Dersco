#!/bin/bash
# Auto-formats Java files via Gradle Spotless after Edit/Write.
# PostToolUse hook for Edit|Write.
# Silent on success. Stdout/stderr redirected; non-zero exit is treated as
# "do not fail the tool call" so formatting trouble never blocks the edit.

if ! command -v jq >/dev/null 2>&1; then
  exit 0
fi

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

[ -z "$FILE_PATH" ] && exit 0
[ ! -f "$FILE_PATH" ] && exit 0

case "$FILE_PATH" in
  *.java) ;;
  *) exit 0 ;;
esac

# Walk up to find the project root (directory with ./gradlew).
find_project_root() {
  local dir="$PWD"
  while [ "$dir" != "/" ]; do
    if [ -f "$dir/gradlew" ]; then
      echo "$dir"
      return
    fi
    dir=$(dirname "$dir")
  done
  echo "$PWD"
}

ROOT=$(find_project_root)
[ ! -x "$ROOT/gradlew" ] && exit 0

(cd "$ROOT" && ./gradlew spotlessApply -q >/dev/null 2>&1) || true

exit 0
