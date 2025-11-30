#!/usr/bin/env bash
# Script to run the custom ASP.NET Core Minimal API generator
#
# Usage:
#   ./run-generator.sh [--additional-properties key=value,key2=value2]
#
# Examples:
#   ./run-generator.sh
#   ./run-generator.sh --additional-properties useMediatr=true
#   ./run-generator.sh --additional-properties useMediatr=false,useAuthentication=true

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GENERATOR_JAR="$SCRIPT_DIR/target/aspnet-minimalapi-openapi-generator-1.0.0.jar"
CLI_JAR="$HOME/.m2/repository/org/openapitools/openapi-generator-cli/7.17.0-SNAPSHOT/openapi-generator-cli-7.17.0-SNAPSHOT.jar"
OPENAPI_SPEC="$PROJECT_ROOT/petstore-tests/petstore.yaml"
OUTPUT_DIR="$PROJECT_ROOT/test-output"

# Check if generator JAR exists
if [ ! -f "$GENERATOR_JAR" ]; then
    echo "Error: Generator JAR not found at $GENERATOR_JAR"
    echo "Run 'cd generator && devbox run mvn clean package' first"
    exit 1
fi

# Check if CLI JAR exists
if [ ! -f "$CLI_JAR" ]; then
    echo "Error: OpenAPI Generator CLI not found at $CLI_JAR"
    echo "Expected version: 7.17.0-SNAPSHOT"
    exit 1
fi

# Check if OpenAPI spec exists
if [ ! -f "$OPENAPI_SPEC" ]; then
    echo "Error: OpenAPI spec not found at $OPENAPI_SPEC"
    exit 1
fi

# Default properties
DEFAULT_PROPS="packageName=PetstoreApi"

# Parse additional properties from command line
ADDITIONAL_PROPS=""
if [ "$1" = "--additional-properties" ] && [ -n "$2" ]; then
    ADDITIONAL_PROPS=",$2"
fi

# Run the generator using the CLI with custom generator on classpath
# See: https://github.com/OpenAPITools/openapi-generator/blob/master/docs/customization.md#use-your-new-generator-with-the-cli
echo "Generating code..."
echo "  Spec: $OPENAPI_SPEC"
echo "  Output: $OUTPUT_DIR"
echo "  Properties: $DEFAULT_PROPS$ADDITIONAL_PROPS"
echo ""

java -cp "$GENERATOR_JAR:$CLI_JAR" org.openapitools.codegen.OpenAPIGenerator generate \
    -g aspnetcore-minimalapi \
    -i "$OPENAPI_SPEC" \
    -o "$OUTPUT_DIR" \
    --additional-properties="$DEFAULT_PROPS$ADDITIONAL_PROPS"

echo ""
echo "✅ Code generation complete!"
