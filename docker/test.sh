#!/bin/bash
# Test script for custom OpenAPI Generator Docker image

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Testing Custom OpenAPI Generator Docker Image ==="

# Create test output directory
TEST_OUTPUT="$PROJECT_ROOT/docker-test-output"
rm -rf "$TEST_OUTPUT"
mkdir -p "$TEST_OUTPUT"

echo "Running generator with MediatR support..."
docker run --rm \
    -v "$PROJECT_ROOT:/workspace" \
    minimal-api-generator:latest \
    generate \
    -g aspnetcore-minimalapi \
    -i /workspace/specs/petstore.yaml \
    -o /workspace/docker-test-output \
    --additional-properties useMediatr=true

echo ""
echo "=== Test Complete ==="
echo "Generated files in: $TEST_OUTPUT"
echo ""

# Check if key files were generated
if [ -f "$TEST_OUTPUT/src/PetstoreApi/Program.cs" ]; then
    echo "✅ Program.cs generated"
else
    echo "❌ Program.cs NOT found"
    exit 1
fi

if [ -f "$TEST_OUTPUT/src/PetstoreApi/Commands/AddPetCommand.cs" ]; then
    echo "✅ MediatR Commands generated"
else
    echo "❌ MediatR Commands NOT found"
    exit 1
fi

echo ""
echo "Success! Docker image is working correctly."
