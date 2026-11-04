#!/bin/sh
set -eu

if ! command -v zip >/dev/null 2>&1; then
    echo "package.sh: 'zip' is required but was not found on PATH" >&2
    exit 1
fi

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PLUGIN_DIR="$SCRIPT_DIR/bytechef-dev"
SKILLS_DIR="$PLUGIN_DIR/skills"
AGENTS_DIR="$SCRIPT_DIR/agents"

if [ $# -ge 1 ]; then
    case "$1" in
        /*) OUT_DIR="$1" ;;
        *) OUT_DIR="$(pwd)/$1" ;;
    esac
else
    REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
    OUT_DIR="$REPO_ROOT/build/claude-code-plugin"
fi

rm -rf "$OUT_DIR/claude-code" "$OUT_DIR/claude" "$OUT_DIR/agents"
mkdir -p "$OUT_DIR/claude-code" "$OUT_DIR/claude" "$OUT_DIR/agents/skills"

cp -R "$PLUGIN_DIR" "$OUT_DIR/claude-code/bytechef-dev"

for skill in "$SKILLS_DIR"/*/; do
    name=$(basename "$skill")
    (cd "$SKILLS_DIR" && zip -r -X -q "$OUT_DIR/claude/$name.zip" "$name")
    cp -R "$SKILLS_DIR/$name" "$OUT_DIR/agents/skills/$name"
done

cp "$AGENTS_DIR/README.md" "$OUT_DIR/agents/README.md"
cp "$AGENTS_DIR/codex-config.toml.example" "$OUT_DIR/agents/codex-config.toml.example"

echo "Packaged ByteChef skills into $OUT_DIR"
