# Installation Guides

Detailed installation instructions for the Contrast MCP Server across different MCP hosts (AI coding assistants and IDEs).

Each guide provides **both Docker and JAR deployment options**, allowing you to choose the method that best fits your environment.

## Available Guides

### [VS Code / GitHub Copilot](./install-vscode.md)
Install Contrast MCP Server in Visual Studio Code with GitHub Copilot.

**Best For:** VS Code users, developers who want the easiest setup

**Deployment Options:**
- ✅ One-click Docker installation via deep link (recommended)
- ✅ Manual Docker configuration with secure input variables
- ✅ JAR deployment for environments without Docker

**Key Features:**
- Secure credential handling with input variables
- Password-masked API keys
- Works with both user and workspace settings

---

### [IntelliJ IDEA](./install-intellij.md)
Install Contrast MCP Server in IntelliJ IDEA using GitHub Copilot's Agent Mode.

**Best For:** IntelliJ users, Java/Kotlin developers

**Deployment Options:**
- ✅ Docker (recommended)
- ✅ JAR deployment

**Key Features:**
- GitHub Copilot Agent Mode integration
- Simple MCP configuration via `mcp.json`

---

### [Cline Plugin](./install-cline.md)
Install Contrast MCP Server with the Cline plugin for VS Code.

**Best For:** Cline users, developers who prefer Cline's AI interface

**Deployment Options:**
- ✅ Docker (recommended)
- ✅ JAR deployment

**Key Features:**
- Visual MCP server management
- Tool auto-approval options
- Easy enable/disable toggle

---

### [Claude Desktop](./install-claude-desktop.md)
Install Contrast MCP Server in Claude Desktop, Anthropic's standalone application.

**Best For:** Claude Desktop users, non-IDE workflows

**Deployment Options:**
- ✅ Docker (recommended)
- ✅ JAR deployment
- ✅ Environment variable support for both methods

**Key Features:**
- Standalone desktop application
- Cross-platform (macOS, Windows, Linux)
- Simple JSON configuration

---

### [oterm](./install-oterm.md)
Install Contrast MCP Server with oterm, a terminal wrapper for Ollama.

**Best For:** Terminal users, local LLM enthusiasts, privacy-focused developers

**Deployment Options:**
- ✅ Docker (recommended)
- ✅ JAR deployment

**Key Features:**
- Terminal-based interface
- Local Ollama model support
- Privacy-preserving (no cloud APIs)
- Works with any Ollama model

---

## Deployment Method Comparison

### Docker Deployment (Recommended)
**Advantages:**
- ✅ No Java installation required
- ✅ Consistent environment across all platforms
- ✅ Easy updates (`docker pull`)
- ✅ Isolation from host system

**Requirements:**
- Docker installed and running

### JAR Deployment
**Advantages:**
- ✅ No Docker required
- ✅ Direct Java process control
- ✅ Easier debugging and logging
- ✅ Works in Docker-restricted environments

**Requirements:**
- Java 17+ installed
- JAR file: [Download](../../README.md#download) or [Build from Source](../../README.md#build-from-source)

## General Requirements

All installation methods require:
- **Contrast API credentials** ([how to get](https://docs.contrastsecurity.com/en/personal-keys.html))
- The appropriate MCP host (VS Code, IntelliJ, Cline, Claude Desktop, or oterm)
- **Either** Docker **or** Java 17+ (depending on deployment method chosen)

## Security Note

All guides follow security best practices:
- Credentials should never be committed to version control
- VS Code uses input variables with password masking
- Other clients should use environment variables where possible
- Always use API credentials, not Agent credentials

## Need Help?

- [Common Issues](../../README.md#common-issues) - Troubleshooting guide
- [Proxy Configuration](../../README.md#proxy-configuration) - Enterprise proxy setup
- [Main README](../../README.md) - General documentation

## Related Documentation

- [Sample Prompts](../../README.md#sample-prompts) - Example queries to try
- [Data Privacy](../../README.md#data-privacy) - Important privacy information
- [CLAUDE.md](../../CLAUDE.md) - AI agent contribution guidelines
