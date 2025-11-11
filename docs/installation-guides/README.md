# Installation Guides

Choose your MCP host to get started. Each guide provides step-by-step instructions for both Docker and JAR deployment.

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
- Requires Java 17+ and JAR file ([download](../../README.md#getting-the-jar-file))
