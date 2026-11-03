FROM eclipse-temurin:21.0.12.1_1-jdk-alpine@sha256:fd52084c35bbac78890d95d57707d31ffc9d7a3324199e89c06be2a9dc71bfa0 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew clean :contrast-mcp-stdio-app:bootJar -x test --no-daemon

FROM eclipse-temurin:21.0.12.1_1-jre-alpine@sha256:b48e02e0ea88d7bfb305335df94923bdef394905f4147dccd52e873cf17b1088
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
