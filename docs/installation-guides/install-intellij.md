# Installing Contrast MCP Server in IntelliJ

This guide covers how to install and configure the Contrast MCP Server in IntelliJ IDEA using GitHub Copilot.

## Prerequisites

- IntelliJ IDEA with GitHub Copilot installed
- Contrast API credentials ([how to get API credentials](https://docs.contrastsecurity.com/en/personal-keys.html))
- **Choose one deployment method:**
  - Docker (recommended)
  - Java 17+ and the built JAR file

## Installation Steps

1. **Enable Agent Mode in Copilot**
   - Open IntelliJ IDEA
   - Select the Agent Mode in Copilot

2. **Access MCP Configuration**
   - Click on the Tools drop down
   - Select "Add more tools"

3. **Add Configuration**

   Choose either Docker or JAR deployment:

### Option 1: Docker Deployment (Recommended)

Add the below configuration to the `mcp.json` file and replace the placeholder values in the `env` section with your actual Contrast credentials:

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

### Option 2: JAR Deployment

If you prefer to run the JAR directly (requires Java 17+):

```json
{
  "servers": {
    "contrastmcp": {
      "command": "java",
      "args": [
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

**Getting the JAR file:**
- **Download** from [GitHub Releases](https://github.com/Contrast-Security-OSS/mcp-contrast/releases/latest) (recommended)
- **Build** from source ([instructions](../../README.md#build-from-source))

Replace `/path/to/mcp-contrast-X.X.X.jar` with the path to your downloaded or built JAR file.

> ⚠️ **Security Note:** Replace all `example` values with your actual Contrast credentials. The credentials here are the API Credentials, not Agent credentials.

## Verify Installation

1. Save the configuration file
2. Restart IntelliJ if needed
3. Check that the Contrast MCP Server appears in your tools list
4. Test with a query like: "List applications in Contrast"

## Configuration Notes

- **CONTRAST_HOST_NAME**: Your Contrast instance hostname (without `https://`)
- **CONTRAST_API_KEY**: Your API key from Contrast
- **CONTRAST_SERVICE_KEY**: Your service key from Contrast
- **CONTRAST_USERNAME**: Your Contrast username (usually your email)
- **CONTRAST_ORG_ID**: Your organization ID from Contrast

## Troubleshooting

If you encounter issues:

1. **Docker not found**: Ensure Docker is installed and running
2. **Connection errors**: Verify your Contrast credentials are correct
3. **Image not found**: Pull the latest image manually:
   ```bash
   docker pull contrast/mcp-contrast:latest
   ```

For more troubleshooting help, see the [Common Issues](../../README.md#common-issues) section in the main README.

## Proxy Configuration

If you're behind a corporate proxy, see the [Proxy Configuration](../../README.md#proxy-configuration) section in the main README.

## Next Steps

- See [Sample Prompts](../../README.md#sample-prompts) for example queries
- Review [Data Privacy](../../README.md#data-privacy) considerations

## Related Documentation

- [Main README](../../README.md)
- [VS Code Installation](./install-vscode.md)
- [Cline Installation](./install-cline.md)
- [Claude Desktop Installation](./install-claude-desktop.md)
- [oterm Installation](./install-oterm.md)
