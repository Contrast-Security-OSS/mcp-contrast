FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean :contrast-mcp-stdio-app:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
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
