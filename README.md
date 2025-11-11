# Contrast MCP Server

[![Java CI with Maven](https://github.com/Contrast-Labs/mcp-contrast/actions/workflows/build.yml/badge.svg)](https://github.com/Contrast-Labs/mcp-contrast/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The Contrast MCP Server allows you to connect Contrast Security to your AI coding agent to automatically remediate vulnerabilities, update insecure libraries, and analyze security coverage—all through natural language prompts.

- Remediate vulnerabilities directly from Contrast Assess data
- Identify and update insecure third-party libraries with Contrast SCA insights
- Review route coverage, Protect/ADR findings, and other security metadata on demand

> [!WARNING]
> **CRITICAL SECURITY WARNING:** Exposing Contrast vulnerability data to an AI service that trains on your prompts can leak sensitive information. Only use mcp-contrast with environments that contractually guarantee data isolation and prohibit model training on your inputs.
>
> **Verify AI Data Privacy:** Confirm that your service agreement prevents model training on your prompts and consult your security team before sharing Contrast data.
>
> **UNSAFE:** Public consumer LLM sites (e.g., free ChatGPT, Gemini, Claude) that use prompts for training.
>
> **POTENTIALLY SAFE:** Enterprise services with contractual privacy guarantees (e.g., Google Cloud AI, AWS Bedrock, Azure OpenAI).

## Quick Start

### Prerequisites
- Docker (recommended) or Java 17+ for JAR deployment
- Contrast API credentials ([how to get API credentials](https://docs.contrastsecurity.com/en/personal-keys.html))

### VS Code (GitHub Copilot) - One-Click Install

[![Install in VS Code](https://img.shields.io/badge/VS_Code-Install_contrast--mcp-0098FF?style=for-the-badge&logo=visualstudiocode&logoColor=ffffff)](vscode:mcp/install?%7B%22name%22%3A%22contrast-mcp%22%2C%22type%22%3A%22stdio%22%2C%22command%22%3A%22docker%22%2C%22args%22%3A%5B%22run%22%2C%22-e%22%2C%22CONTRAST_HOST_NAME%22%2C%22-e%22%2C%22CONTRAST_API_KEY%22%2C%22-e%22%2C%22CONTRAST_SERVICE_KEY%22%2C%22-e%22%2C%22CONTRAST_USERNAME%22%2C%22-e%22%2C%22CONTRAST_ORG_ID%22%2C%22-i%22%2C%22--rm%22%2C%22contrast%2Fmcp-contrast%3Alatest%22%2C%22-t%22%2C%22stdio%22%5D%2C%22env%22%3A%7B%22CONTRAST_HOST_NAME%22%3A%22%24%7Binput%3Acontrast_host_name%7D%22%2C%22CONTRAST_ORG_ID%22%3A%22%24%7Binput%3Acontrast_org_id%7D%22%2C%22CONTRAST_USERNAME%22%3A%22%24%7Binput%3Acontrast_username%7D%22%2C%22CONTRAST_API_KEY%22%3A%22%24%7Binput%3Acontrast_api_key%7D%22%2C%22CONTRAST_SERVICE_KEY%22%3A%22%24%7Binput%3Acontrast_service_key%7D%22%7D%7D)

Click the button above to automatically install in VS Code. For manual setup, see [VS Code (GitHub Copilot) Installation Guide](docs/installation-guides/install-vscode.md).

### IntelliJ IDEA (GitHub Copilot)

Add this to your `mcp.json` configuration file and replace the placeholder values with your Contrast credentials:

```json
{
  "servers": {
    "contrastmcp": {
      "command": "docker",
      "args": [
        "run",
        "-e",
        "CONTRAST_HOST_NAME",
        "-e",
        "CONTRAST_API_KEY",
        "-e",
        "CONTRAST_SERVICE_KEY",
        "-e",
        "CONTRAST_USERNAME",
        "-e",
        "CONTRAST_ORG_ID",
        "-i",
        "--rm",
        "contrast/mcp-contrast:latest",
        "-t",
        "stdio"
      ],
      "env": {
        "CONTRAST_HOST_NAME": "example.contrastsecurity.com",
        "CONTRAST_API_KEY": "example",
        "CONTRAST_SERVICE_KEY": "example",
        "CONTRAST_USERNAME": "example@example.com",
        "CONTRAST_ORG_ID": "example"
      }
    }
  }
}
```

📖 [Full IntelliJ (GitHub Copilot) Installation Guide](docs/installation-guides/install-intellij.md) - Includes step-by-step setup and JAR deployment option

### Other AI Assistants

- **[Claude Code](docs/installation-guides/install-claude-code.md)** - Anthropic's official CLI tool
- **[Claude Desktop](docs/installation-guides/install-claude-desktop.md)** - Standalone Claude application
- **[Cline Plugin](docs/installation-guides/install-cline.md)** - VS Code alternative AI assistant
- **[All Other MCP Hosts](docs/installation-guides/)** - Complete installation guides for oterm and more

## Sample Prompts
### For the Developer
#### Remediate Vulnerability in code
* Please list vulnerabilities for Application Y
* Give me details about vulnerability X on Application Y
* Review the vulnerability X and fix it.

#### 3rd Party Library Remediation
* Which libraries in Application X have vulnerabilities High or Critical and are also being actively used.
* Update library X with Critical vulnerability to the Safe version.
* Which libraries in Application X are not being used?

#### Retrieving application based on Tags
* Please give me the applications tagged with "backend"

#### Retrieving application based on Metadata
* Please give me the applications with metadata  "dev-team" "backend-team"

#### Retrieving vulnerabilities based on Session Metadata
* Give me the sesssion metadata for application x
* Give me the vulnerabilities in the latest session for application X
* Give me the vulnerabilities for session metadata "Branch Name" "feature/some-new-fix" for application X
* Give me the route coverage for the latest session for application X
* Give me the route coverage for session metadata "Branch Name" "feature/some-new-fix" for application X

### For the Security Professional
* Please give me a breakdown of applications and servers vulnerable to CVE-xxxx-xxxx
* Please list the libraries for application named xxx and tell me what version of commons-collections is being used
* Which Vulnerabilities in application X are being blocked by a Protect / ADR Rule?

## Getting the JAR File

If you're using JAR deployment (instead of Docker), you'll need the JAR file:

### Download (Recommended)

Download the latest pre-built JAR from [GitHub Releases](https://github.com/Contrast-Security-OSS/mcp-contrast/releases/latest).

The JAR file will be named `mcp-contrast-X.X.X.jar`.

### Build from Source

Alternatively, you can build from source if you need the latest development version. Requires Java 17+:

```bash
mvn clean install
```

The built JAR will be located at `target/mcp-contrast-X.X.X-SNAPSHOT.jar`




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
    "contrastmcp": {
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
    "contrastmcp": {
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

For VS Code with input variables, see the [VS Code Installation Guide](docs/installation-guides/install-vscode.md).

## Common Issues
If you are experiencing issues with the MCP server, here are some common troubleshooting steps:
### Review Log
A log will be created, by default under `/tmp/mcp-contrast.log` either locally or witin the docker container. You can view this log to see if there are any errors or issues with the MCP server.

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
`-Djavax.net.ssl.trustStore=/loctaion/to/mcp-truststore.jks, -Djavax.net.ssl.trustStorePassword=yourpassword`
More details on how to do this can be found in the [Java documentation](https://docs.oracle.com/cd/E19509-01/820-3503/6nf1il6er/index.html). Or ask your LLM to help you with this.

## Data Privacy

The Contrast MCP Server provides a bridge between your Contrast Data and the AI Agent/LLM of your choice.
By using Contrast's MCP server you will be providing your Contrast Data to your AI Agent/LLM, it is your responsibility to ensure that the AI Agent/LLM you use complies with your data privacy policy.
Depending on what questions you ask the following information will be provided to your AI Agent/LLM.
* Application Details
* Application Rule configuration
* Vulnerability Details
* Route Coverage data
* ADR/Protect Attack Event Details
