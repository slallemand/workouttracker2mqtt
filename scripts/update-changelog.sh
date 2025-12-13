#!/bin/bash
# Script to update CHANGELOG.md when version changes
# Usage: ./scripts/update-changelog.sh [version] [changelog_entry]

set -e

ADDON_DIR="workouttracker2mqtt"
CHANGELOG_FILE="$ADDON_DIR/CHANGELOG.md"
CONFIG_FILE="$ADDON_DIR/config.yaml"

# Get version from argument or config.yaml
if [ -n "$1" ]; then
    VERSION="$1"
else
    VERSION=$(grep '^version:' "$CONFIG_FILE" | sed 's/version: *"\(.*\)"/\1/')
fi

if [ -z "$VERSION" ]; then
    echo "Error: Could not determine version"
    exit 1
fi

DATE=$(date +%Y-%m-%d)

# Get changelog entry from argument or generate from git
if [ -n "$2" ]; then
    CHANGELOG_ENTRY="$2"
else
    # Get recent commits (last 10, excluding merge commits)
    CHANGELOG_ENTRY=$(git log --pretty=format:"- %s (%h)" --no-merges -10 | head -10)
    if [ -z "$CHANGELOG_ENTRY" ]; then
        CHANGELOG_ENTRY="- Version bump to $VERSION"
    fi
fi

# Create new changelog entry
NEW_ENTRY="## [$VERSION] - $DATE

### Changed
$CHANGELOG_ENTRY

"

# Check if CHANGELOG.md exists
if [ ! -f "$CHANGELOG_FILE" ]; then
    echo "# Changelog

All notable changes to this add-on will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

" > "$CHANGELOG_FILE"
fi

# Read the header (first 3 lines)
HEADER=$(head -3 "$CHANGELOG_FILE")
# Read the rest
REST=$(tail -n +4 "$CHANGELOG_FILE" 2>/dev/null || echo "")

# Combine
{
    echo "$HEADER"
    echo ""
    echo "$NEW_ENTRY"
    echo "$REST"
} > "$CHANGELOG_FILE.tmp"

mv "$CHANGELOG_FILE.tmp" "$CHANGELOG_FILE"

echo "✓ Updated CHANGELOG.md with version $VERSION"
echo ""
echo "Preview:"
head -15 "$CHANGELOG_FILE"

