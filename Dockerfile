FROM eclipse-temurin:21.0.13_9-jdk-alpine@sha256:b78a1d385da875588e21207f07dddf9a1418849971ae857fb24c8f3a7d441a72 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean :contrast-mcp-stdio-app:bootJar -x test --no-daemon

FROM eclipse-temurin:21.0.13_9-jre-alpine@sha256:82cf3d9ccbe1ed46252008d51b2f2329e6b9e8a2f4394c4bebefc3ea7f4b1372
WORKDIR /app

# Upgrade all packages to get security patches, then install shadow for user management
RUN apk upgrade --no-cache && apk add --no-cache shadow

# Create a non-root user to run the application
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/contrast-mcp-stdio-app/build/libs/mcp-contrast-*.jar app.jar

# Set ownership of the application files
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
