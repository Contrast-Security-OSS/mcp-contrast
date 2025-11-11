# Installing Contrast MCP Server in Cline

This guide covers how to install and configure the Contrast MCP Server with the Cline plugin for VS Code.

> [!NOTE] Official Cline Documentation Reference
> Cline's full documentation for configuring MCP servers can be found here: https://docs.cline.bot/mcp/configuring-mcp-servers

## Prerequisites

- VS Code with Cline plugin installed
- Contrast API credentials ([how to get API credentials](https://docs.contrastsecurity.com/en/personal-keys.html))
- **Choose one deployment method:**
  - Docker (recommended)
  - Java 17+ and the built JAR file

## Installation Steps

1. **Open Cline MCP Configuration**
   - With the Cline plugin installed, select the MCP button in the top right corner of the screen

   ![Cline MCP Button](../../images/cline1.png)

2. **Configure MCP Servers**
   - Select "Configure MCP Servers"
   - This will open the JSON configuration file for MCP

   ![Cline Configuration Screen](../../images/cline2.png)

3. **Add Contrast MCP Configuration**

   Choose either Docker or JAR deployment:

### Option 1: Docker Deployment (Recommended)

Add the following configuration to the JSON file and replace the placeholder values in the `env` section with your actual Contrast credentials:

```json
{
  "mcpServers": {
    "contrast": {
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
      },
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

Replace the placeholder values with your actual Contrast credentials.

### Option 2: JAR Deployment

If you prefer to run the JAR directly (requires Java 17+):

```json
{
  "mcpServers": {
    "contrast": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/mcp-contrast-X.X.X.jar",
        "--CONTRAST_HOST_NAME=example.contrastsecurity.com",
        "--CONTRAST_API_KEY=example",
        "--CONTRAST_SERVICE_KEY=example",
        "--CONTRAST_USERNAME=example@example.com",
        "--CONTRAST_ORG_ID=example"
      ],
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

Replace the placeholder values with your actual Contrast credentials.

Replace `/path/to/mcp-contrast-X.X.X.jar` with the path to your downloaded or built JAR file.

**Getting the JAR file:**
- **Download** from [GitHub Releases](https://github.com/Contrast-Security-OSS/mcp-contrast/releases/latest) (recommended)
- **Build** from source ([instructions](../../README.md#build-from-source))


**Save Configuration**
- Save the configuration file
- The Contrast MCP server should now appear in the list of MCP servers

## Verify Installation

Once configured, you should see the Contrast MCP server in the list of MCP servers. If you expand it, you should see a list of available tools.

![Cline Tools List](../../images/cline3.png)

Test with a query like: "List applications in Contrast"

## Configuration Notes

- **CONTRAST_HOST_NAME**: Your Contrast instance hostname (without `https://`)
- **CONTRAST_API_KEY**: Your API key from Contrast
- **CONTRAST_SERVICE_KEY**: Your service key from Contrast
- **CONTRAST_USERNAME**: Your Contrast username (usually your email)
- **CONTRAST_ORG_ID**: Your organization ID from Contrast
- **disabled**: Set to `false` to enable the server
- **autoApprove**: Array of tool names that should be auto-approved (leave empty for manual approval)

## Troubleshooting

If you encounter issues:

1. **Docker not found**: Ensure Docker is installed and running
2. **Server not appearing**: Check the JSON syntax is valid
3. **Connection errors**: Verify your Contrast credentials are correct
4. **Image not found**: Pull the latest image manually:
   ```bash
   docker pull contrast/mcp-contrast:latest
   ```

For more troubleshooting help, see the [Common Issues](../../README.md#common-issues) section in the main README.

## Proxy Configuration

If you're behind a corporate proxy, see the [Proxy Configuration](../../README.md#proxy-configuration) section in the main README.
