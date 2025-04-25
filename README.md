# Contrast MCP Server

[![Java CI with Maven](https://github.com/Contrast-Labs/mcp-contrast/actions/workflows/build.yml/badge.svg)](https://github.com/Contrast-Labs/mcp-contrast/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.contrast.labs/mcp-contrast.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.contrast.labs%22%20AND%20a:%22mcp-contrast%22)
[![Install in VS Code Docker](https://img.shields.io/badge/VS_Code-docker-0098FF?style=flat-square&logo=githubcopilot&logoColor=white)](https://insiders.vscode.dev/redirect/mcp/install?name=contrastmcp&config=%7B%22command%22:%22docker%22,%22args%22:%5B%22run%22,%20%22-e%22,%22CONTRAST_HOST_NAME%22,%20%22-e%22,%22CONTRAST_API_KEY%22,%20%22-e%22,%22CONTRAST_SERVICE_KEY%22,%20%22-e%22,%22CONTRAST_USERNAME%22,%20%22-e%22,%22CONTRAST_ORG_ID%22,%20%20%22-i%22,%20%22--rm%22,%20%22contrast-mcp%22,%20%22-t%22,%20%22stdio%22%5D,%22env%22:%7B%22CONTRAST_HOST_NAME%22:%22example.contrastsecurity.com%22,%22CONTRAST_API_KEY%22:%22example%22,%22CONTRAST_SERVICE_KEY%22:%22example%22,%22CONTRAST_USERNAME%22:%22example@example.com%22,%22CONTRAST_ORG_ID%22:%22example%22%7D%7D)
![output.gif](images/output.gif)

## Table of Contents 
- [Sample Prompts](#sample-prompts)
  - [For the Developer](#for-the-developer)
    - [Remediate Vulnerability in code](#remediate-vulnerability-in-code)
    - [3rd Party Library Remediation](#3rd-party-library-remediation)
  - [For the Security Professional](#for-the-security-professional)
- [Build](#build)
- [Run](#run)
- [Docker](#docker)
  - [Build Docker Image](#build-docker-image)
  - [Run with Docker](#run-with-docker)
  - [Using Copilot + Petclinic](#using-copilot--petclinic)
  - [Install via Link](#install-via-link)
  - [Manual Install of MCP Server](#manual-install-of-mcp-server)
  - [Using Cline Plugin](#using-cline-plugin)
  - [Using oterm](#using-oterm)
- [Proxy Configuration](#proxy-configuration)
  - [Java Process](#java-process)
  - [Docker](#docker-1)

## Sample Prompts
### For the Developer
#### Remediate Vulnerability in code
1. Please list vulnerabilities for Application Y
2. Give me details about vulnerability X on Application Y
3. Review the vulnerability X and fix it.

#### 3rd Party Library Remediation
1. Which libraries in Application X have vulnerabilities High or Critical and are also being actively used.
2. Update library X with Critical vulnerability to the Safe version.

* Which libraries in Application X are not being used?

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

## Build
Requires Java 17+

`mvn clean install`

## Run
To add the MCP Server to your local AI system, modify the config.json file and add the following

```json
"mcpServers": {
    "contrast-mcp": {
      "command": "/usr/bin/java", "args": ["-jar","/Users/name/workspace/mcp-contrast/mcp-contrast/target/mcp-contrast-0.0.1-SNAPSHOT.jar",
        "--CONTRAST_HOST_NAME=example.contrastsecurity.com",
        "--CONTRAST_API_KEY=xxx",
        "--CONTRAST_SERVICE_KEY=xxx",
        "--CONTRAST_USERNAME=xxx.xxx@contrastsecurity.com",
        "--CONTRAST_ORG_ID=xxx"]
    }
}
```

You obviously need to configure the above to match your contrast API Creds.

## Docker

### Build Docker Image
```bash
docker build -t contrast-mcp .
```

### Run with Docker
```bash
docker run \
  -e CONTRAST_HOST_NAME=example.contrastsecurity.com \
  -e CONTRAST_API_KEY=example \
  -e CONTRAST_SERVICE_KEY=example \
  -e CONTRAST_USERNAME=example@exampe.com \
  -e CONTRAST_ORG_ID=example \
  -i contrast-mcp \
  -t stdio
  ```



### Using Copilot + Petclinic
Download and Build Contrast-MCP as a Docker Image
```bash
git clone git@github.com:Contrast-Labs/mcp-contrast.git
cd mcp-contrast
docker build -t contrast-mcp .
```

Then download the Vulnerable Pet Clinic.
`git clone https://github.com/Contrast-Security-OSS/vulnerable-spring-petclinic.git`
Open the project in VSCode.
Edit the contrast_security.yaml file and configure it with your AGENT credentials
```yaml
api:
  url: https://xxx/Contrast
  api_key: xxx
  service_key: xxx
  user_name: xxx
# All other contrast config is done in the docker-compose file. Do not check this file in to git!
```
Then you can build and run using docker-compose
`docker compose up --build`
It will build and run the services that make up petclinic.
To build out the vulnerabilites and attack events run
`./testscript.sh`
Select option 25. ( this will exercise the app and perform attacks to populate the vulnerabilities and attack events)
#### Install via Link
Click following link  >>> [![Install in VS Code Docker](https://img.shields.io/badge/VS_Code-docker-0098FF?style=flat-square&logo=githubcopilot&logoColor=white)](https://insiders.vscode.dev/redirect/mcp/install?name=contrastmcp&config=%7B%22command%22:%22docker%22,%22args%22:%5B%22run%22,%20%22-e%22,%22CONTRAST_HOST_NAME%22,%20%22-e%22,%22CONTRAST_API_KEY%22,%20%22-e%22,%22CONTRAST_SERVICE_KEY%22,%20%22-e%22,%22CONTRAST_USERNAME%22,%20%22-e%22,%22CONTRAST_ORG_ID%22,%20%20%22-i%22,%20%22--rm%22,%20%22contrast-mcp%22,%20%22-t%22,%20%22stdio%22%5D,%22env%22:%7B%22CONTRAST_HOST_NAME%22:%22example.contrastsecurity.com%22,%22CONTRAST_API_KEY%22:%22example%22,%22CONTRAST_SERVICE_KEY%22:%22example%22,%22CONTRAST_USERNAME%22:%22example@example.com%22,%22CONTRAST_ORG_ID%22:%22example%22%7D%7D) <<<
Allow the extension to be installed in your VSCode instance.
Select Install Server

![install-server.png](images/install-server.png)

This will install the MCP Server. You will need to configure the server with your Contrast API credentials.
![install2.png](images/install2.png)



#### Manual Install of MCP Server
In VSCode go to settings and search for "mcp"
![vscode-config.png](images%2Fvscode-config.png)
Edit the Settings.json or select modify in workspace. If you want to enable this MCP sever just for this workspace.
Then add the following to the settings.json file.
```json
"mcp": {
    "inputs": [],
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
            "contrast-mcp",
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
```

Please note the credentials here are the API Credentials, not Agent credentials.
You should also see a small start button appear in the json file as you can see above. Click it to start the MCP server.

Once complete you should see the Contrast MCP Tools in the Tools drop down and you should be ready to perform queries!
![vscode-config3.png](images%2Fvscode-config3.png)


### Using Cline Plugin
With the Cline plugin installed, select the MCP button in the top right corner of the screen.
![cline1.png](images/cline1.png)
Then select configure MCP Servers. This will open up a the JSON configuration for MCP.
![cline2.png](images/cline2.png)
Add the following the json configuration
```json
{
  "mcpServers": {
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
        "contrast-mcp",
        "-t",
        "stdio"
      ],
      "env": {
        "CONTRAST_HOST_NAME": "example.contrastsecurity.com",
        "CONTRAST_API_KEY": "example",
        "CONTRAST_SERVICE_KEY": "example",
        "CONTRAST_USERNAME": "example@example.com",
        "CONTRAST_ORG_ID": "example"
      },
      "disabled": false,
      "autoApprove": []
    }
  }
}
```
Once done you should see the contrast mcp server appear in the list of MCP servers, if you expand it you should see a list of available tools.
![cline3.png](images/cline3.png)

### Using oterm
oterm is  terminal wrapper for ollama. One of its features is the ability to add MCP servers to specific LLM Models.
https://ggozad.github.io/oterm/

![tools.png](images/tools.png)

![chat.png](images/chat.png)




## Proxy Configuration

### Java Process
If you need to configure a proxy for your Java process when using the standalone JAR, you can set the Java system properties for HTTP and HTTPS proxies:

```bash
java -Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=8080 -jar /path/to/mcp-contrast-0.0.1-SNAPSHOT.jar --CONTRAST_HOST_NAME=example.contrastsecurity.com --CONTRAST_API_KEY=example --CONTRAST_SERVICE_KEY=example --CONTRAST_USERNAME=example@example.com --CONTRAST_ORG_ID=example
```

If your proxy requires authentication, you can also set:

```bash
java -Dhttp.proxyHost=proxy.example.com -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=8080 -Dhttp.proxyUser=username -Dhttp.proxyPassword=password -Dhttps.proxyUser=username -Dhttps.proxyPassword=password -jar /path/to/mcp-contrast-0.0.1-SNAPSHOT.jar --CONTRAST_HOST_NAME=example.contrastsecurity.com --CONTRAST_API_KEY=example --CONTRAST_SERVICE_KEY=example --CONTRAST_USERNAME=example@example.com --CONTRAST_ORG_ID=example
```

When configuring in your config.json file, include the proxy settings in the args array:

```json
"mcpServers": {
  "contrast-assess": {
    "command": "/usr/bin/java", 
    "args": [
      "-Dhttp.proxyHost=proxy.example.com", 
      "-Dhttp.proxyPort=8080", 
      "-Dhttps.proxyHost=proxy.example.com", 
      "-Dhttps.proxyPort=8080",
      "-jar",
      "/Users/name/workspace/mcp-contrast/mcp-contrast/target/mcp-contrast-0.0.1-SNAPSHOT.jar",
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
  -e HTTP_PROXY="http://proxy.example.com:8080" \
  -e HTTPS_PROXY="http://proxy.example.com:8080" \
  -e CONTRAST_HOST_NAME=example.contrastsecurity.com \
  -e CONTRAST_API_KEY=example \
  -e CONTRAST_SERVICE_KEY=example \
  -e CONTRAST_USERNAME=example \
  -e CONTRAST_ORG_ID=example \
  -i \
  contrast-mcp \
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
        "-e", "HTTP_PROXY=http://proxy.example.com:8080",
        "-e", "HTTPS_PROXY=http://proxy.example.com:8080",
        "-i",
        "--rm",
        "contrast-mcp",
        "-t",
        "stdio"
        ],
        "env": {
            "CONTRAST_HOST_NAME": "example.contrastsecurity.com",
            "CONTRAST_API_KEY": "example",
            "CONTRAST_SERVICE_KEY": "example",
            "CONTRAST_USERNAME": "example@example.com",
            "CONTRAST_ORG_ID": "example",
            "HTTP_PROXY": "http://proxy.example.com:8080",
            "HTTP_PROXY": "http://proxy.example.com:8080"
        }
    }
  }
}
```
