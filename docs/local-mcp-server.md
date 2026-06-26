# Local MCP Server guide

Reference material for the [Local MCP Server](../README.md#local-mcp-server). For installation, see the [README quick start](../README.md#quick-start) and the per-client [installation guides](installation-guides/).

## Getting the JAR File

If you're using JAR deployment (instead of Docker), you'll need the JAR file:

### Download (Recommended)

Download the latest pre-built JAR from [GitHub Releases](https://github.com/Contrast-Security-OSS/mcp-contrast/releases/latest).

The JAR file will be named `mcp-contrast-X.X.X.jar`.

### Verifying the Download

Each release JAR is signed with a [GitHub build provenance attestation](https://docs.github.com/en/actions/security-guides/using-artifact-attestations-to-establish-provenance-for-builds), which proves the JAR was built by this repository's release workflow. Verify a downloaded JAR with the [GitHub CLI](https://cli.github.com/):

```bash
gh attestation verify mcp-contrast-X.X.X.jar --repo Contrast-Security-OSS/mcp-contrast
```

A successful check confirms the JAR was produced by the official release workflow and has not been altered. No key import or fingerprint comparison is required. The GitHub CLI validates the attestation against the public Sigstore transparency log.

### Build from Source

Alternatively, you can build from source if you need the latest development version. Requires Java 21+:

```bash
./gradlew :contrast-mcp-stdio-app:bootJar
```

The built JAR will be located at `contrast-mcp-stdio-app/build/libs/mcp-contrast-X.X.X-SNAPSHOT.jar`

## Proxy Configuration

If you're behind a corporate firewall or proxy, you'll need to configure proxy settings for the MCP server to reach your Contrast instance. The configuration differs depending on whether you're using Docker or JAR deployment.

### Java Process (JAR Deployment)

Choose ONE of the following based on how you're running the JAR:

#### Direct Java Command

Use this if you're running the JAR directly from the command line or a script.

Add these two system properties to your `java` command:

```
-Dhttp_proxy_host=proxy.example.com
-Dhttp_proxy_port=8080
```

**Complete example:**
```bash
java \
  -Dhttp_proxy_host=proxy.example.com \
  -Dhttp_proxy_port=8080 \
  -jar /path/to/mcp-contrast-X.X.X.jar \
  --CONTRAST_HOST_NAME=example.contrastsecurity.com \
  --CONTRAST_API_KEY=example \
  --CONTRAST_SERVICE_KEY=example \
  --CONTRAST_USERNAME=example@example.com \
  --CONTRAST_ORG_ID=example
```

#### MCP Configuration File

Use this if you're running the JAR through an MCP host (IntelliJ, Claude Desktop, Cline, etc.).

Add these two lines to the beginning of your `args` array:

```json
"-Dhttp_proxy_host=proxy.example.com",
"-Dhttp_proxy_port=8080",
```

**Complete example using IntelliJ's `mcp.json`:**
```json
{
  "servers": {
    "contrast": {
      "command": "java",
      "args": [
        "-Dhttp_proxy_host=proxy.example.com",
        "-Dhttp_proxy_port=8080",
        "-jar",
        "/path/to/mcp-contrast-X.X.X.jar",
        "--CONTRAST_HOST_NAME=example.contrastsecurity.com",
        "--CONTRAST_API_KEY=example",
        "--CONTRAST_SERVICE_KEY=example",
        "--CONTRAST_USERNAME=example@example.com",
        "--CONTRAST_ORG_ID=example"
      ]
    }
  }
}
```

### Docker (Docker Deployment)

Choose ONE of the following based on how you're running Docker:

#### Direct Docker Run Command

Use this if you're running Docker directly from the command line.

Add these two environment variables to your `docker run` command:

```bash
-e http_proxy_host="proxy.example.com" \
-e http_proxy_port="8080" \
```

**Complete example:**
```bash
docker run \
  -e http_proxy_host="proxy.example.com" \
  -e http_proxy_port="8080" \
  -e CONTRAST_HOST_NAME=example.contrastsecurity.com \
  -e CONTRAST_API_KEY=example \
  -e CONTRAST_SERVICE_KEY=example \
  -e CONTRAST_USERNAME=example \
  -e CONTRAST_ORG_ID=example \
  -i --rm \
  contrast/mcp-contrast:latest \
  -t stdio
```

#### MCP Configuration File

Use this if you're running Docker through an MCP host (IntelliJ, VS Code, Claude Desktop, Cline, etc.).

Add these proxy settings:

Add to the `args` array (after the Contrast credentials):
```json
"-e", "http_proxy_host",
"-e", "http_proxy_port",
```

Add to the `env` object:
```json
"http_proxy_host": "proxy.example.com",
"http_proxy_port": "8080"
```

**Complete example using IntelliJ's `mcp.json`:**
```json
{
  "servers": {
    "contrast": {
      "command": "docker",
      "args": [
        "run",
        "-e", "CONTRAST_HOST_NAME",
        "-e", "CONTRAST_API_KEY",
        "-e", "CONTRAST_SERVICE_KEY",
        "-e", "CONTRAST_USERNAME",
        "-e", "CONTRAST_ORG_ID",
        "-e", "http_proxy_host",
        "-e", "http_proxy_port",
        "-i", "--rm",
        "contrast/mcp-contrast:latest",
        "-t", "stdio"
      ],
      "env": {
        "CONTRAST_HOST_NAME": "example.contrastsecurity.com",
        "CONTRAST_API_KEY": "example",
        "CONTRAST_SERVICE_KEY": "example",
        "CONTRAST_USERNAME": "example@example.com",
        "CONTRAST_ORG_ID": "example",
        "http_proxy_host": "proxy.example.com",
        "http_proxy_port": "8080"
      }
    }
  }
}
```

For VS Code with input variables, see the [VS Code Installation Guide](installation-guides/install-vscode.md).

## Common Issues
If you are experiencing issues with the MCP server, here are some common troubleshooting steps:
### Review Log
A log will be created, by default under `/tmp/mcp-contrast.log` either locally or within the Docker container. You can view this log to see if there are any errors or issues with the MCP server.

### Enable Debug Logging
To enable debug logging you can add the following flag to the command line arguments when running the MCP server:
`--logging.level.root=DEBUG`
This can be added at this part of the docker command
```
        "--rm",
        "contrast/mcp-contrast:latest",
        "-t",
        "--logging.level.root=DEBUG",
        "stdio"
        ],
```

### Certificate Issues
If the SSL Certificate for the Teamserver URL is not trusted, you may see the following error:
```
Failed to list applications: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```
If this occurs you will need to add the certificate to the Java Truststore and then add the following to the command line arguments when running the MCP server:
`-Djavax.net.ssl.trustStore=/location/to/mcp-truststore.jks, -Djavax.net.ssl.trustStorePassword=yourpassword`
More details on how to do this can be found in the [Java documentation](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html). Or ask your LLM to help you with this.
