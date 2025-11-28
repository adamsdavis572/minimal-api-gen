# Docker Image for Custom OpenAPI Generator

This directory contains Docker configuration for running the custom ASP.NET Core Minimal API generator in a containerized environment.

## Quick Start

```bash
# Build the image
./build.sh

# Test the image
./test.sh

# Use the image (from project root)
docker run --rm -v $(pwd):/workspace minimal-api-generator:latest \
  generate -g aspnetcore-minimalapi \
  -i /workspace/specs/petstore.yaml \
  -o /workspace/output \
  --additional-properties useMediatr=true
```

## Files

- `Dockerfile` - Image definition
- `build.sh` - Build script with helpful output
- `test.sh` - Validation script that tests generation

## Building

### Default Build

```bash
./build.sh
```

This builds `minimal-api-generator:latest` using OpenAPI Generator CLI 7.10.0.

### Custom OpenAPI Generator Version

```bash
docker build \
  --build-arg ARG_OPENAPI_GENERATOR_VERSION=7.9.0 \
  -t minimal-api-generator:7.9.0 \
  -f Dockerfile \
  ..
```

### Manual Build Steps

If you prefer to build manually:

```bash
# 1. Build the generator JAR
cd ../generator
devbox run mvn clean package

# 2. Build Docker image
cd ..
docker build \
  --build-arg ARG_OPENAPI_GENERATOR_VERSION=7.10.0 \
  -t minimal-api-generator:latest \
  -f docker/Dockerfile \
  .
```

## Usage

### Basic Generation

```bash
# Run from your project root directory
docker run --rm \
  -v $(pwd):/workspace \
  minimal-api-generator:latest \
  generate \
  -g aspnetcore-minimalapi \
  -i /workspace/specs/your-spec.yaml \
  -o /workspace/output
```

### With Additional Properties

```bash
docker run --rm \
  -v $(pwd):/workspace \
  minimal-api-generator:latest \
  generate \
  -g aspnetcore-minimalapi \
  -i /workspace/specs/your-spec.yaml \
  -o /workspace/output \
  --additional-properties useMediatr=true,packageName=MyApi
```

### Volume Mounting

The `-v` flag mounts your local directory into the container:

- `-v $(pwd):/workspace` - Mounts current directory as `/workspace` in container
- Source files (OpenAPI specs) must be accessible via the mount
- Output directory must also be within the mount

**Example Structure:**

```
my-project/
├── specs/
│   └── api.yaml
└── generated/
    └── (output goes here)
```

```bash
cd my-project
docker run --rm -v $(pwd):/workspace minimal-api-generator:latest \
  generate -g aspnetcore-minimalapi \
  -i /workspace/specs/api.yaml \
  -o /workspace/generated \
  --additional-properties useMediatr=true
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Generate API Code

on:
  push:
    paths:
      - 'specs/**'

jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Generate API
        run: |
          docker run --rm \
            -v ${{ github.workspace }}:/local \
            minimal-api-generator:latest \
            generate -g aspnetcore-minimalapi \
            -i /local/specs/api.yaml \
            -o /local/src/Generated \
            --additional-properties useMediatr=true
      
      - name: Commit changes
        run: |
          git config user.name "GitHub Actions"
          git config user.email "actions@github.com"
          git add src/Generated
          git commit -m "Regenerate API code" || echo "No changes"
          git push
```

### GitLab CI

```yaml
generate-api:
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker run --rm 
        -v $CI_PROJECT_DIR:/workspace 
        minimal-api-generator:latest
        generate -g aspnetcore-minimalapi
        -i /workspace/specs/api.yaml
        -o /workspace/generated
        --additional-properties useMediatr=true
  artifacts:
    paths:
      - generated/
```

## Troubleshooting

### "No such file or directory" errors

Ensure your OpenAPI spec path is relative to the mounted directory:

```bash
# ❌ Wrong - absolute path outside container
-i /Users/you/project/spec.yaml

# ✅ Correct - path within mounted volume
-v $(pwd):/workspace
-i /workspace/specs/spec.yaml
```

### Permission issues with generated files

Generated files are owned by the container user. To fix ownership:

```bash
# After generation
sudo chown -R $(id -u):$(id -g) ./output
```

Or run container with your user ID:

```bash
docker run --rm \
  -u $(id -u):$(id -g) \
  -v $(pwd):/workspace \
  minimal-api-generator:latest \
  generate -g aspnetcore-minimalapi \
  -i /workspace/specs/your-spec.yaml \
  -o /workspace/output
```

### Image not found

Ensure you've built the image:

```bash
# Check if image exists
docker images | grep minimal-api-generator

# Build if missing
./build.sh
```

## Attribution

Dockerfile structure based on [Stack Overflow answer](https://stackoverflow.com/q/78887848) by Arturo Martínez Díaz, modified by community (CC BY-SA 4.0). Retrieved 2025-11-28.
