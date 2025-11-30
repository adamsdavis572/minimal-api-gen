#!/bin/bash
# Copy test handler implementations over generated stubs
# This ensures tests run against real handler implementations

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_HANDLERS_DIR="$PROJECT_ROOT/petstore-tests/TestHandlers"
OUTPUT_HANDLERS_DIR="$PROJECT_ROOT/test-output/src/PetstoreApi/Handlers"
OUTPUT_SERVICES_DIR="$PROJECT_ROOT/test-output/src/PetstoreApi/Services"

echo "Copying test handler implementations..."

# Copy handlers
if [ -d "$TEST_HANDLERS_DIR" ]; then
    # Ensure directories exist
    mkdir -p "$OUTPUT_SERVICES_DIR"
    
    cp "$TEST_HANDLERS_DIR/GetPetByIdQueryHandler.cs" "$OUTPUT_HANDLERS_DIR/"
    cp "$TEST_HANDLERS_DIR/AddPetCommandHandler.cs" "$OUTPUT_HANDLERS_DIR/"
    cp "$TEST_HANDLERS_DIR/UpdatePetCommandHandler.cs" "$OUTPUT_HANDLERS_DIR/"
    cp "$TEST_HANDLERS_DIR/DeletePetCommandHandler.cs" "$OUTPUT_HANDLERS_DIR/"
    
    # Copy test data store
    cp "$TEST_HANDLERS_DIR/InMemoryPetStore.cs" "$OUTPUT_SERVICES_DIR/"
    
    echo "✓ Test handlers copied successfully"
else
    echo "ERROR: Test handlers directory not found at $TEST_HANDLERS_DIR"
    exit 1
fi
