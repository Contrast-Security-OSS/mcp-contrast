# Installing Contrast MCP Server in Claude Desktop

This guide covers how to install and configure the Contrast MCP Server with Claude Desktop.

> [!NOTE] Official Claude Desktop Documentation Reference
> Anthropic's full documentation for connecting local MCP servers can be found here: https://modelcontextprotocol.io/docs/develop/connect-local-servers

## Prerequisites

- Claude Desktop installed ([download](https://claude.ai/download))
- Contrast API credentials ([how to get API credentials](https://docs.contrastsecurity.com/en/personal-keys.html))
- **Choose one deployment method:**
  - Docker (recommended)
  - Java 17+ and the built JAR file

## Installation Steps

1. **Locate Configuration File**

   Claude Desktop stores its MCP configuration in `claude_desktop_config.json`:

   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
   - **Linux**: `~/.config/Claude/claude_desktop_config.json`

2. **Edit Configuration**

   Choose either Docker or JAR deployment:

### Option 1: Docker Deployment (Recommended)

Add the following configuration to your `claude_desktop_config.json` file. If the file doesn't exist, create it with this content:

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
        "CONTRAST_API_KEY": "xxx",
        "CONTRAST_SERVICE_KEY": "xxx",
        "CONTRAST_USERNAME": "xxx.xxx@example.com",
        "CONTRAST_ORG_ID": "xxx"
      }
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
        "--CONTRAST_API_KEY=xxx",
        "--CONTRAST_SERVICE_KEY=xxx",
        "--CONTRAST_USERNAME=xxx.xxx@example.com",
        "--CONTRAST_ORG_ID=xxx"
      ]
    }
  }
}
```

Replace the placeholder values with your actual Contrast credentials.

Replace `/path/to/mcp-contrast-X.X.X.jar` with the path to your downloaded or built JAR file.

**Getting the JAR file:**
- **Download** from [GitHub Releases](https://github.com/Contrast-Security-OSS/mcp-contrast/releases/latest) (recommended)
- **Build** from source ([instructions](../../README.md#build-from-source))


After saving the configuration, completely quit and restart Claude Desktop for the changes to take effect.

## Verify Installation

1. After restarting Claude Desktop, you should see the Contrast MCP tools available
2. Test with a query like: "List applications in Contrast"
3. Claude should be able to access your Contrast Security data

## Configuration Notes

- **CONTRAST_HOST_NAME**: Your Contrast instance hostname (without `https://`)
- **CONTRAST_API_KEY**: Your API key from Contrast
- **CONTRAST_SERVICE_KEY**: Your service key from Contrast
- **CONTRAST_USERNAME**: Your Contrast username (usually your email)
- **CONTRAST_ORG_ID**: Your organization ID from Contrast
  

## Troubleshooting

If you encounter issues:

1. **Docker not found**: Ensure Docker is installed and running
2. **Configuration not loading**: Check JSON syntax is valid (use a JSON validator)
3. **Connection errors**: Verify your Contrast credentials are correct
4. **Tools not appearing**: Ensure Claude Desktop was completely restarted
5. **Image not found**: Pull the latest image manually:
   ```bash
   docker pull contrast/mcp-contrast:latest
   ```

**Check Claude Desktop Logs:**
- **macOS**: `~/Library/Logs/Claude/mcp*.log`
- **Windows**: `%APPDATA%\Claude\logs\mcp*.log`
- **Linux**: `~/.config/Claude/logs/mcp*.log`

For more troubleshooting help, see the [Common Issues](../../README.md#common-issues) section in the main README.

## Proxy Configuration

If you're behind a corporate proxy, see the [Proxy Configuration](../../README.md#proxy-configuration) section in the main README.

## Additional Resources

- [Model Context Protocol Documentation](https://modelcontextprotocol.io/docs/develop/connect-local-servers)
- [Claude Desktop Documentation](https://claude.ai/docs)
