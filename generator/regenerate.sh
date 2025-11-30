#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Complete Regeneration Workflow ==="
echo ""

# Step 1: Delete old generated code
echo "Step 1: Deleting old generated code..."
rm -rf ../test-output
echo "✓ Old code deleted"
echo ""

# Step 2: Regenerate code
echo "Step 2: Regenerating code..."
./run-generator.sh --additional-properties useMediatr=true
echo ""

# Step 3: Copy test project
echo "Step 3: Copying test project..."
mkdir -p ../test-output/tests
cp -r ../petstore-tests/PetstoreApi.Tests ../test-output/tests/
echo "✓ Test project copied"
echo ""

# Step 4: Copy test handlers
echo "Step 4: Copying test handlers..."
./copy-test-handlers.sh
echo ""

# Step 5: Run tests (optional)
if [ "$1" == "--test" ]; then
    echo "Step 5: Running tests..."
    devbox run dotnet test ../test-output/tests/PetstoreApi.Tests/PetstoreApi.Tests.csproj --verbosity normal
else
    echo "Step 5: Skipping tests (use --test flag to run tests)"
    echo "To run tests later: ./test.sh"
fi

echo ""
echo "=== Regeneration Complete ==="
