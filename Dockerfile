FROM maven:3.9-eclipse-temurin-17@sha256:036d1a6f2965e4368157bb87f02cd31652a96918a26f7eb5ae45a0aa33f2cb8e AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine@sha256:c707c0d18cb9e8556380719f80d96a7529d0746fbb42143893949b98ed2f8943
WORKDIR /app

# Upgrade all packages to get security patches, then install shadow for user management
RUN apk upgrade --no-cache && apk add --no-cache shadow

# Create a non-root user to run the application
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/target/mcp-contrast-*.jar app.jar

# Set ownership of the application files
RUN chown -R appuser:appgroup /app

# Switch to the non-root user
USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
