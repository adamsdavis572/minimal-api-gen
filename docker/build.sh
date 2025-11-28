#!/bin/bash
# Build script for custom OpenAPI Generator Docker image

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Building Custom OpenAPI Generator Docker Image ==="

# Build the generator JAR if needed
if [ ! -f "$PROJECT_ROOT/generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar" ]; then
    echo "Building generator JAR..."
    cd "$PROJECT_ROOT/generator"
    devbox run mvn clean package -q
fi

# Build Docker image
echo "Building Docker image..."
cd "$PROJECT_ROOT"

docker build \
    --build-arg ARG_OPENAPI_GENERATOR_VERSION=7.10.0 \
    -t minimal-api-generator:latest \
    -f docker/Dockerfile \
    .

echo ""
echo "=== Build Complete ==="
echo "Image: minimal-api-generator:latest"
echo ""
echo "Usage:"
echo "  # Generate with MediatR (mount project root):"
echo "  docker run --rm -v \$(pwd):/workspace minimal-api-generator:latest \\"
echo "    generate -g aspnetcore-minimalapi -i /workspace/specs/petstore.yaml -o /workspace/output --additional-properties useMediatr=true"
echo ""
echo "  # Generate without MediatR:"
echo "  docker run --rm -v \$(pwd):/workspace minimal-api-generator:latest \\"
echo "    generate -g aspnetcore-minimalapi -i /workspace/specs/petstore.yaml -o /workspace/output"
