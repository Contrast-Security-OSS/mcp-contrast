FROM eclipse-temurin:21-jdk-alpine@sha256:4fb80de7aeb277ad949cfbe89b4f504e50bb34c57fd908c5825236473d71e986 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean :contrast-mcp-stdio-app:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine@sha256:704db3c40204a44f471191446ddd9cda5d60dab40f0e15c6507b815ed897238b
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
