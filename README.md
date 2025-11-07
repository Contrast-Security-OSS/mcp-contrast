# Contrast MCP Server

[![Java CI with Maven](https://github.com/Contrast-Labs/mcp-contrast/actions/workflows/build.yml/badge.svg)](https://github.com/Contrast-Labs/mcp-contrast/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


The Contrast MCP Server allow you to connect Contrast Security to your AI coding agent to automatically remediate vulnerabilities, update insecure libraries, and analyze security coverage—all through natural language prompts.

![output.gif](images/output.gif)

## Quick Install

**VS Code (One-Click Install):**

[![Install in VS Code](https://img.shields.io/badge/VS_Code-Install_contrast--mcp-0098FF?style=for-the-badge&logo=visualstudiocode&logoColor=ffffff)](vscode:mcp/install?%7B%22name%22%3A%22contrast-mcp%22%2C%22type%22%3A%22stdio%22%2C%22command%22%3A%22docker%22%2C%22args%22%3A%5B%22run%22%2C%22-e%22%2C%22CONTRAST_HOST_NAME%22%2C%22-e%22%2C%22CONTRAST_API_KEY%22%2C%22-e%22%2C%22CONTRAST_SERVICE_KEY%22%2C%22-e%22%2C%22CONTRAST_USERNAME%22%2C%22-e%22%2C%22CONTRAST_ORG_ID%22%2C%22-i%22%2C%22--rm%22%2C%22contrast%2Fmcp-contrast%3Alatest%22%2C%22-t%22%2C%22stdio%22%5D%2C%22env%22%3A%7B%22CONTRAST_HOST_NAME%22%3A%22%24%7Binput%3Acontrast_host_name%7D%22%2C%22CONTRAST_ORG_ID%22%3A%22%24%7Binput%3Acontrast_org_id%7D%22%2C%22CONTRAST_USERNAME%22%3A%22%24%7Binput%3Acontrast_username%7D%22%2C%22CONTRAST_API_KEY%22%3A%22%24%7Binput%3Acontrast_api_key%7D%22%2C%22CONTRAST_SERVICE_KEY%22%3A%22%24%7Binput%3Acontrast_service_key%7D%22%7D%7D)

**Other Clients:** See [Installation Guide](#docker) below

---

> [!WARNING]
> **CRITICAL SECURITY WARNING:** 
> EXPOSING YOUR CONTRAST VULNERABILITY DATA TO A LLM THAT TRAINS ON YOUR DATA CAN POTENTIALLY EXPOSE YOUR VULNERABILITY DATA TO THE OUTSIDE WORLD. 
> Thus, do not use mcp-contrast functions which pull sensitive data with a LLM that trains on your data.
>
> **Verify AI Data Privacy:** Before sending vulnerability data to an AI, you must confirm that your service agreement guarantees your data will not be used for model training.
>
> **UNSAFE:** Public consumer websites (e.g., the free versions of ChatGPT, Gemini, Claude). These services often use your input for training.
>
> **POTENTIALLY-SAFE:** Enterprise-grade services (e.g. Google Cloud AI, AWS Bedrock, Azure OpenAI) or paid plans that contractually ensure data privacy and prevent model training on your prompts, verify with your information security teams.

---

## Table of Contents
- [Contrast MCP Server](#contrast-mcp-server)
  - [Quick Install](#quick-install)
  - [Table of Contents](#table-of-contents)
  - [Sample Prompts](#sample-prompts)
    - [For the Developer](#for-the-developer)
    - [For the Security Professional](#for-the-security-professional)
  - [Data Privacy](#data-privacy)
  - [Installation](#installation)
    - [Prerequisites](#prerequisites)
    - [Install in VS Code / GitHub Copilot](#install-in-vs-code--github-copilot-recommended)
    - [Install in Other MCP Hosts](#install-in-other-mcp-hosts)
  - [Download](#download)
  - [Build from Source](#build-from-source)
  - [Proxy Configuration](#proxy-configuration)
  - [Common Issues](#common-issues)

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

## Data Privacy
The Contrast MCP Server provides a bridge between your Contrast Data and the AI Agent/LLM of your choice.
By using Contrast's MCP server you will be providing your Contrast Data to your AI Agent/LLM, it is your responsibility to ensure that the AI Agent/LLM you use complies with your data privacy policy.
Depending on what questions you ask the following information will be provided to your AI Agent/LLM.
* Application Details
* Application Rule configuration
* Vulnerability Details
* Route Coverage data
* ADR/Protect Attack Event Details

## Installation

### Prerequisites

- Java 17+ (for JAR deployment)
- Docker (for Docker deployment - recommended)
- Contrast API credentials ([how to get API credentials](https://docs.contrastsecurity.com/en/personal-keys.html))

### Install in VS Code / GitHub Copilot (Recommended)

#### One-Click Install

Click the button below for automatic installation in VS Code:

[![Install in VS Code](https://img.shields.io/badge/VS_Code-Install_contrast--mcp-0098FF?style=for-the-badge&logo=visualstudiocode&logoColor=ffffff)](vscode:mcp/install?%7B%22name%22%3A%22contrast-mcp%22%2C%22type%22%3A%22stdio%22%2C%22command%22%3A%22docker%22%2C%22args%22%3A%5B%22run%22%2C%22-e%22%2C%22CONTRAST_HOST_NAME%22%2C%22-e%22%2C%22CONTRAST_API_KEY%22%2C%22-e%22%2C%22CONTRAST_SERVICE_KEY%22%2C%22-e%22%2C%22CONTRAST_USERNAME%22%2C%22-e%22%2C%22CONTRAST_ORG_ID%22%2C%22-i%22%2C%22--rm%22%2C%22contrast%2Fmcp-contrast%3Alatest%22%2C%22-t%22%2C%22stdio%22%5D%2C%22env%22%3A%7B%22CONTRAST_HOST_NAME%22%3A%22%24%7Binput%3Acontrast_host_name%7D%22%2C%22CONTRAST_ORG_ID%22%3A%22%24%7Binput%3Acontrast_org_id%7D%22%2C%22CONTRAST_USERNAME%22%3A%22%24%7Binput%3Acontrast_username%7D%22%2C%22CONTRAST_API_KEY%22%3A%22%24%7Binput%3Acontrast_api_key%7D%22%2C%22CONTRAST_SERVICE_KEY%22%3A%22%24%7Binput%3Acontrast_service_key%7D%22%7D%7D)

This will automatically configure VS Code with secure input variables for your credentials.

#### Manual VS Code Installation

See the complete [VS Code Installation Guide](docs/installation-guides/install-vscode.md) for manual setup instructions.

### Install in Other MCP Hosts

The Contrast MCP Server can be installed in any MCP-compatible host. Each guide includes both Docker and JAR installation options:

- **[IntelliJ IDEA](docs/installation-guides/install-intellij.md)** - GitHub Copilot integration
- **[Cline Plugin](docs/installation-guides/install-cline.md)** - VS Code extension
- **[Claude Desktop](docs/installation-guides/install-claude-desktop.md)** - Standalone application
- **[oterm](docs/installation-guides/install-oterm.md)** - Terminal wrapper for Ollama

## Download

Download the latest pre-built JAR from [GitHub Releases](https://github.com/Contrast-Security-OSS/mcp-contrast/releases/latest).

The JAR file will be named `mcp-contrast-X.X.X.jar`.

## Build from Source

For developers who want to build locally, requires Java 17+:

```bash
mvn clean install
```

The built JAR will be located at `target/mcp-contrast-0.0.X-SNAPSHOT.jar`




## Proxy Configuration

### Java Process
If you need to configure a proxy for your Java process when using the standalone JAR, you can set the Java system properties for HTTP and HTTPS proxies:

```bash
java -Dhttp_proxy_host=proxy.example.com -Dhttp_proxy_port=8080 -jar /path/to/mcp-contrast-X.X.X.jar --CONTRAST_HOST_NAME=example.contrastsecurity.com --CONTRAST_API_KEY=example --CONTRAST_SERVICE_KEY=example --CONTRAST_USERNAME=example@example.com --CONTRAST_ORG_ID=example
```

When configuring in your config.json file, include the proxy settings in the args array:

```json
"mcpServers": {
  "contrast-assess": {
    "command": "/usr/bin/java", 
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
```

### Docker
When running the MCP server in Docker, you can configure the proxy by passing the relevant environment variables:


```bash
docker run \
  -e http_proxy_host="proxy.example.com" \
  -e http_proxy_port="8080" \
  -e CONTRAST_HOST_NAME=example.contrastsecurity.com \
  -e CONTRAST_API_KEY=example \
  -e CONTRAST_SERVICE_KEY=example \
  -e CONTRAST_USERNAME=example \
  -e CONTRAST_ORG_ID=example \
  -i \
  contrast/mcp-contrast:latest \
  -t stdio

```

For VS Code configuration with Docker and proxy, modify the settings.json like this:

```json
"mcp": {
  "inputs": [],
  "servers": {
    "contrast-mcp": {
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
        "-e", "http_proxy_host",
        "-e", "http_proxy_port",
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
            "CONTRAST_ORG_ID": "example",
            "http_proxy_host": "proxy.example.com",
            "http_proxy_port": "8080"
        }
    }
  }
}
```

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


