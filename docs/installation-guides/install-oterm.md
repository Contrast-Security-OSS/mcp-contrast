# Installing Contrast MCP Server in oterm

This guide covers how to install and configure the Contrast MCP Server with oterm, a terminal wrapper for Ollama.

## Prerequisites

- oterm installed ([installation guide](https://ggozad.github.io/oterm/))
- Ollama installed and running
- Contrast API credentials ([how to get API credentials](https://docs.contrastsecurity.com/en/personal-keys.html))
- **Choose one deployment method:**
  - Docker (recommended)
  - Java 17+ and the built JAR file

## About oterm

oterm is a terminal-based interface for Ollama that allows you to add MCP servers to specific LLM models. This enables you to use local LLMs with Contrast Security data.

🔗 [oterm Documentation](https://ggozad.github.io/oterm/)

## Installation Steps

1. **Install oterm** (if not already installed)
   ```bash
   pip install oterm
   ```

2. **Configure MCP Server**

   Add the Contrast MCP server configuration to oterm's MCP configuration. Refer to oterm's documentation for the specific configuration file location and format.

   Choose either Docker or JAR deployment:

### Option 1: Docker Deployment (Recommended)

Basic configuration structure:
```json
{
  "servers": {
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

3. **Start oterm with MCP Support**

   Start oterm and ensure MCP servers are enabled for your desired model.

## Configuration Notes

- **CONTRAST_HOST_NAME**: Your Contrast instance hostname (with or without `https://`)
- **CONTRAST_API_KEY**: Your API key from Contrast
- **CONTRAST_SERVICE_KEY**: Your service key from Contrast
- **CONTRAST_USERNAME**: Your Contrast username (usually your email)
- **CONTRAST_ORG_ID**: Your organization ID from Contrast

## Verify Installation

1. Start oterm with your preferred Ollama model
2. Check that Contrast MCP tools are listed in the available tools
3. Test with a query like: "List applications in Contrast"

![oterm Tools](../../images/tools.png)

![oterm Chat](../../images/chat.png)

## Troubleshooting

If you encounter issues:

1. **Docker not found**: Ensure Docker is installed and running
2. **Ollama connection errors**: Verify Ollama is running (`ollama list`)
3. **MCP server not loading**: Check oterm configuration syntax
4. **Contrast connection errors**: Verify your Contrast credentials are correct
5. **Image not found**: Pull the latest image manually:
   ```bash
   docker pull contrast/mcp-contrast:latest
   ```

For more troubleshooting help, see the [Common Issues](../../README.md#common-issues) section in the main README.

## Proxy Configuration

If you're behind a corporate proxy, see the [Proxy Configuration](../../README.md#proxy-configuration) section in the main README.

## Benefits of Using oterm

- **Local LLMs**: Use Ollama models locally with Contrast data
- **Privacy**: Keep your interactions local
- **Terminal Interface**: Fast, keyboard-driven interface
- **Model Flexibility**: Switch between different Ollama models easily

## Additional Resources

- [oterm GitHub Repository](https://github.com/ggozad/oterm)
- [Ollama Documentation](https://ollama.ai/docs)

