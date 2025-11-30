#!/bin/bash
# Run full test suite with handler copy
# Usage: ./test.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

echo "=== Running Test Suite ==="
echo ""

# Step 1: Copy test handlers over generated stubs
echo "Step 1: Copying test handlers..."
./copy-test-handlers.sh
echo ""

# Step 2: Run tests
echo "Step 2: Running tests..."
devbox run dotnet test ~/scratch/git/minimal-api-gen/test-output/tests/PetstoreApi.Tests/PetstoreApi.Tests.csproj --verbosity normal
