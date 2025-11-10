# Installing Contrast MCP Server in Claude Code

This guide covers how to install and configure the Contrast MCP Server with Claude Code.

## Prerequisites

- Claude Code installed ([download](https://claude.ai/code))
- Contrast API credentials ([how to get API credentials](https://docs.contrastsecurity.com/en/personal-keys.html))
- **Choose one deployment method:**
  - Docker (recommended)
  - Java 17+ and the built JAR file

## Installation Steps

Claude Code uses a simple CLI command to add MCP servers. Choose either Docker or JAR deployment:

### Option 1: Docker Deployment (Recommended)

Add the Contrast MCP server using Docker:

```bash
claude mcp add --transport stdio contrast \
  --env CONTRAST_HOST_NAME=example.contrastsecurity.com \
  --env CONTRAST_API_KEY=your_api_key \
  --env CONTRAST_SERVICE_KEY=your_service_key \
  --env CONTRAST_USERNAME=your_username \
  --env CONTRAST_ORG_ID=your_org_id \
  -- docker run -e CONTRAST_HOST_NAME -e CONTRAST_API_KEY -e CONTRAST_SERVICE_KEY \
  -e CONTRAST_USERNAME -e CONTRAST_ORG_ID -i --rm contrast/mcp-contrast:latest -t stdio
```

Replace the placeholder values with your actual Contrast credentials.

**Understanding the command:**
- `--transport stdio` - Uses stdio communication protocol
- `--env` flags - Pass your Contrast credentials as environment variables
- `--` - Separates Claude Code flags from the Docker command
- Everything after `--` is the Docker run command

### Option 2: JAR Deployment

If you prefer to run the JAR directly (requires Java 17+):

```bash
claude mcp add --transport stdio contrast \
  -- java -jar /path/to/mcp-contrast-X.X.X.jar \
  --CONTRAST_HOST_NAME=example.contrastsecurity.com \
  --CONTRAST_API_KEY=your_api_key \
  --CONTRAST_SERVICE_KEY=your_service_key \
  --CONTRAST_USERNAME=your_username \
  --CONTRAST_ORG_ID=your_org_id
```

**Getting the JAR file:**
- **Download** from [GitHub Releases](https://github.com/Contrast-Security-OSS/mcp-contrast/releases/latest) (recommended)
- **Build** from source ([instructions](../../README.md#build-from-source))

Replace `/path/to/mcp-contrast-X.X.X.jar` with the actual path to your downloaded or built JAR file, and replace the credential values with your actual Contrast credentials.

> ⚠️ **Security Note:** Replace all `example` and placeholder values with your actual Contrast credentials. The credentials here are the API Credentials, not Agent credentials.

## Configuration Scopes

Claude Code supports three configuration scopes. By default, servers are added to your **local** scope (project-specific, private to you). You can specify a different scope with the `--scope` flag:

- **`--scope local`** (default): Available only to you in the current project
- **`--scope project`**: Shared with everyone via `.mcp.json` file (can be committed to version control)
- **`--scope user`**: Available to you across all projects on your machine

**Example with project scope:**
```bash
claude mcp add --transport stdio contrast --scope project \
  --env CONTRAST_HOST_NAME=... \
  -- docker run -e CONTRAST_HOST_NAME ... contrast/mcp-contrast:latest -t stdio
```

## Verify Installation

Once configured, verify the server was added successfully:

```bash
# List all configured MCP servers
claude mcp list

# Get details for the Contrast server
claude mcp get contrast
```

Within Claude Code, you can also check server status with:
```
> /mcp
```

Test with a query like: "List applications in Contrast"

## Managing Your Server

Use these commands to manage the Contrast MCP server:

```bash
# View server configuration
claude mcp get contrast

# Remove the server
claude mcp remove contrast

# List all servers
claude mcp list
```

## Configuration Notes

- **CONTRAST_HOST_NAME**: Your Contrast instance hostname (without `https://`)
- **CONTRAST_API_KEY**: Your API key from Contrast
- **CONTRAST_SERVICE_KEY**: Your service key from Contrast
- **CONTRAST_USERNAME**: Your Contrast username (usually your email)
- **CONTRAST_ORG_ID**: Your organization ID from Contrast

## Troubleshooting

If you encounter issues:

1. **Docker not found**: Ensure Docker is installed and running
   ```bash
   docker --version
   docker pull contrast/mcp-contrast:latest
   ```

2. **Connection errors**: Verify your Contrast credentials are correct using the `claude mcp get contrast` command

3. **Server not starting**: Check the logs and server status:
   ```bash
   # Within Claude Code
   > /mcp
   ```

4. **Environment variable issues**: Ensure all required credentials are provided as `--env` flags

For more troubleshooting help, see the [Common Issues](../../README.md#common-issues) section in the main README.

## Proxy Configuration

If you're behind a corporate proxy, you'll need to configure Docker or Java to use your proxy settings. See the [Proxy Configuration](../../README.md#proxy-configuration) section in the main README for details.

## Next Steps

- See [Sample Prompts](../../README.md#sample-prompts) for example queries
- Review [Data Privacy](../../README.md#data-privacy) considerations

## Windows Users

On native Windows (not WSL), you need to use the `cmd /c` wrapper for Docker commands:

```bash
claude mcp add --transport stdio contrast \
  --env CONTRAST_HOST_NAME=... \
  -- cmd /c docker run -e CONTRAST_HOST_NAME ... contrast/mcp-contrast:latest -t stdio
```

## Related Documentation

- [Main README](../../README.md)
- [All Installation Guides](./README.md)
