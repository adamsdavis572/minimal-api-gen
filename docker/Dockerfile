# Source - https://stackoverflow.com/q/78887848
# Posted by Arturo Martínez Díaz, modified by community. See post 'Timeline' for change history
# Retrieved 2025-11-28, License - CC BY-SA 4.0

FROM eclipse-temurin:11-jre-jammy

USER root

RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

ARG ARG_OPENAPI_GENERATOR_VERSION=7.10.0
ENV OPENAPI_GENERATOR_VERSION=${ARG_OPENAPI_GENERATOR_VERSION}

WORKDIR /app

# Download OpenAPI Generator CLI
RUN wget https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/${ARG_OPENAPI_GENERATOR_VERSION}/openapi-generator-cli-${ARG_OPENAPI_GENERATOR_VERSION}.jar -O openapi-generator-cli.jar

# Copy custom generator JAR
COPY generator/target/aspnet-minimalapi-openapi-generator-1.0.0.jar ./aspnet-minimalapi-generator.jar

# The custom generator must be first in the classpath to override built-in generators
ENTRYPOINT ["java", "-cp", "/app/aspnet-minimalapi-generator.jar:/app/openapi-generator-cli.jar", "org.openapitools.codegen.OpenAPIGenerator"]
