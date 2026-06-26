# Local MCP Server installation guides

These guides cover the [Local MCP Server](../local-mcp-server.md), the open-source server you run yourself over stdio with Contrast API and service keys. Choose your MCP host to get started. Each guide provides step-by-step instructions for both Docker and JAR deployment.

> **Looking for the Hosted MCP Server?** If you use Contrast SaaS, the [Hosted MCP Server](./install-hosted.md) is the easiest way to connect. It needs no local process, no keys, and no container. These local-server guides are for when you cannot use the hosted server, or when you need raw SARIF scan output.

## MCP Hosts

- **[VS Code (GitHub Copilot)](./install-vscode.md)**
- **[IntelliJ IDEA (GitHub Copilot)](./install-intellij.md)**
- **[Claude Code](./install-claude-code.md)**
- **[Claude Desktop](./install-claude-desktop.md)**
- **[Cline](./install-cline.md)**
- **[oterm](./install-oterm.md)**

> **Using a different MCP host?** The Contrast MCP Server works with any MCP-compatible client. If your host isn't listed above, you can adapt the Docker or JAR configuration from any of these guides to your client's MCP configuration format.

## Deployment Methods

All guides support both deployment methods - choose the one that fits your environment:

**Docker (Recommended)**
- No Java installation required
- Easy updates with `docker pull`
- Consistent across all platforms

**JAR**
- No Docker required
- Direct process control for debugging
- Works in Docker-restricted environments
- Requires Java 21+ and JAR file ([download](../local-mcp-server.md#getting-the-jar-file))
