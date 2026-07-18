# Contrast MCP Server

[![Java CI with Gradle](https://github.com/Contrast-Security-OSS/mcp-contrast/actions/workflows/build.yml/badge.svg)](https://github.com/Contrast-Security-OSS/mcp-contrast/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The Contrast MCP Server connects Contrast Security to your AI coding agent so you can remediate vulnerabilities, update insecure libraries, and analyze security coverage through natural language.

It comes in two forms.

- **[Hosted MCP Server](#hosted-mcp-server-recommended)** is a remote MCP server that Contrast runs for you. It is the simplest path for Contrast SaaS customers, with browser-based OAuth sign-in and nothing to install. **Recommended for most users.**
- **[Local MCP Server](#local-mcp-server)** is the open-source server in this repository that you run yourself with API keys. It is the right choice for on-premises and EOP (Enterprise On-Premises) instances.

> [!WARNING]
> **CRITICAL SECURITY WARNING:** Exposing Contrast vulnerability data to an AI service that trains on your prompts can leak sensitive information. Only use the Contrast MCP Server with environments that contractually guarantee data isolation and prohibit model training on your inputs.
>
> **Verify AI Data Privacy:** Confirm that your service agreement prevents model training on your prompts and consult your security team before sharing Contrast data.
>
> **UNSAFE:** Public consumer LLM sites (e.g., free ChatGPT, Gemini, Claude) that use prompts for training.
>
> **POTENTIALLY SAFE:** Enterprise services with contractual privacy guarantees (e.g., Google Cloud AI, AWS Bedrock, Azure OpenAI).

## Contents

- [Hosted MCP Server (recommended)](#hosted-mcp-server-recommended)
  - [Prerequisites](#prerequisites)
  - [Connect](#connect)
  - [Connection details](#connection-details)
  - [Supported clients](#supported-clients)
  - [Security and privacy](#security-and-privacy)
  - [Available tools](#available-tools-hosted)
- [Local MCP Server](#local-mcp-server)
  - [Available tools](#available-tools-local)
  - [Quick start](#quick-start)
  - [More setup and troubleshooting](#more-setup-and-troubleshooting)
- [Sample prompts](#sample-prompts)
- [Data privacy](#data-privacy)

## Hosted MCP Server (recommended)

The Hosted MCP Server, a remote MCP server that Contrast operates for you, is the easiest way to connect an AI agent to Contrast. You point your client at one URL, sign in through your browser, and your agent can start asking questions about your security data. There are no API keys to copy around, no container or JAR to keep updated, and no local process to run.

The hosted server is read-only and is available now for Contrast SaaS.

### Prerequisites

- A Contrast SaaS account with access to at least one organization
- An MCP client that supports Streamable HTTP transport and OAuth 2.0 with PKCE (see [Supported clients](#supported-clients))
- A modern web browser for the OAuth sign-in

### Connect

Add the server to Claude Code by pointing it at your Contrast host followed by `/mcp`.

```bash
claude mcp add --transport http contrast-hosted-mcp https://app.contrastsecurity.com/mcp
```

Replace `app.contrastsecurity.com` with your organization's Contrast URL if you use a dedicated instance. The first time your agent calls a tool, your browser opens for sign-in. If sign-in does not start automatically, run `/mcp` in Claude Code and choose Authenticate for `contrast-hosted-mcp`. You log in with your existing Contrast credentials, choose an organization, and approve read access. Your session refreshes on its own, so you typically sign in once and keep working.

For step-by-step setup for Claude Code, Claude Desktop, the Codex CLI, the GitHub Copilot CLI, and opencode, see the [Hosted MCP Server installation guide](docs/installation-guides/install-hosted.md).

### Connection details

Any MCP client that supports Streamable HTTP transport and OAuth 2.0 with PKCE can connect.

| Setting | Value |
|---------|-------|
| Endpoint URL | `https://<your-contrast-host>/mcp` (for example `https://app.contrastsecurity.com/mcp`) |
| Transport | Streamable HTTP (stateless) |
| HTTP method | `POST` |
| Authentication | OAuth 2.0 with PKCE (S256) |
| OAuth scopes | `openid`, `profile`, `offline_access` |

Your client discovers the OAuth configuration automatically through the `WWW-Authenticate` response header, which points to the standard `/.well-known/oauth-protected-resource` metadata document. Clients that support Dynamic Client Registration can register at `/oauth2/connect/register` on the Contrast origin.

### Supported clients

| Client | Status |
|--------|--------|
| Claude Code CLI | Working |
| Codex CLI | Working |
| GitHub Copilot CLI | Working |
| opencode | Working |
| Claude Desktop | Working |
| Gemini CLI | Not yet supported, OAuth compatibility issue |
| VS Code Copilot plugin | Not yet supported, OAuth compatibility issue |

Support for more clients is in progress as their OAuth handling matures. If your client fails during OAuth registration before the login page appears, that is usually a client compatibility issue rather than a problem with your account.

### Security and privacy

The hosted server changes how access works without changing what you are allowed to see.

- **OAuth, not API keys.** You sign in through your browser, so there are no long-lived keys to distribute or store on developer machines.
- **Read-only.** Every hosted tool is read-only. You cannot modify, update, or delete data through the hosted server.
- **Organization-scoped.** Each session is bound to the single organization you select at sign-in, so there is no organization ID to guess or get wrong.
- **Your existing permissions apply.** Every request carries your identity to Contrast, which enforces the same role-based access control as the web interface. If you cannot see something in Contrast, your agent cannot see it either.
- **No data storage.** The hosted server stores none of your data, and your token never appears in a tool response.

The shared warning above still applies. Tool results become part of your AI conversation, so follow your organization's policy on what security data can be sent to your chosen AI client and model.

### Available tools (hosted)

The hosted server provides read-only tools across the domains below. Your agent calls them automatically based on your questions.

<details>
<summary>Show hosted tools</summary>

#### Authentication
| Tool | Description |
|------|-------------|
| `get_user_info` | Show who you are signed in as and which organization is active |

#### Vulnerabilities (Assess)
| Tool | Description |
|------|-------------|
| `search_vulnerabilities` | Search vulnerabilities across all applications |
| `search_app_vulnerabilities` | Search vulnerabilities within a specific application with session filtering |
| `get_vulnerability` | Get detailed vulnerability info including remediation guidance |
| `list_vulnerability_types` | List all available vulnerability types for filtering |

#### Applications
| Tool | Description |
|------|-------------|
| `search_applications` | Search applications by name, tag, or metadata filters |
| `get_session_metadata` | Get session metadata fields available for an application |

#### Libraries (SCA)
| Tool | Description |
|------|-------------|
| `list_application_libraries` | List libraries used by an application with class usage statistics and vulnerability counts |
| `list_applications_by_cve` | Find applications affected by a specific CVE |

#### Protection (ADR/Protect)
| Tool | Description |
|------|-------------|
| `search_attacks` | Search attack events with filtering by status, type, and rules |
| `get_protect_rules` | Get protection rules configured for an application |

#### Coverage
| Tool | Description |
|------|-------------|
| `get_route_coverage` | Get route coverage data showing exercised vs discovered routes |

#### SAST (Scan)
| Tool | Description |
|------|-------------|
| `get_scan_project` | Get SAST project details and vulnerability counts |

#### Issues, Incidents, and Observations
These tools require the Contrast unified data platform (NorthStar) to be enabled for your organization.

| Tool | Description |
|------|-------------|
| `search_issues` | Search and filter security issues across your organization |
| `get_issue` | Get full details for a specific issue |
| `get_issue_summary` | Get a concise summary of a specific issue |
| `get_issue_count` | Count issues matching filters without fetching full details |
| `list_issue_incidents` | List incidents linked to an issue |
| `list_issues_by_library` | List open issues associated with an application library |
| `search_incidents` | Search and filter incidents |
| `get_incident` | Get full details for a specific incident |
| `get_incident_summary` | Get a concise summary of a specific incident |
| `list_incident_issues` | List issues linked to an incident |
| `get_observation` | Get full details for a specific observation |
| `list_issue_observations` | List observations linked to an issue (cursor-paginated) |
| `list_incident_observations` | List observations linked to an incident (cursor-paginated) |
| `get_incident_observation_count` | Count observations linked to an incident without paging |

</details>

## Local MCP Server

The Local MCP Server is the open-source server in this repository. Your MCP client launches it as a local process over stdio, it authenticates with Contrast API and service keys, and it connects to your own Contrast instance, including on-premises and EOP. Use it when you cannot use the hosted server, or when you need raw SARIF scan output.

### Available tools (local)

The Local MCP Server provides 14 tools for security analysis and vulnerability management.

#### Applications
| Tool | Description |
|------|-------------|
| `search_applications` | Search applications by name, tag, or metadata filters |
| `get_session_metadata` | Get session metadata fields available for an application |

#### Servers
| Tool | Description |
|------|-------------|
| `search_servers` | Search the server inventory for agent health and Protect coverage |

#### Vulnerabilities
| Tool | Description |
|------|-------------|
| `search_vulnerabilities` | Search vulnerabilities across all applications (org-level) |
| `search_app_vulnerabilities` | Search vulnerabilities within a specific application with session filtering |
| `get_vulnerability` | Get detailed vulnerability info including stack trace and remediation guidance |
| `list_vulnerability_types` | List all available vulnerability types for filtering |

#### Libraries (SCA)
| Tool | Description |
|------|-------------|
| `list_application_libraries` | List libraries used by an application with class usage statistics and vulnerability counts |
| `list_applications_by_cve` | Find applications affected by a specific CVE |

#### Protection (ADR/Protect)
| Tool | Description |
|------|-------------|
| `search_attacks` | Search attack events with filtering by status, type, and rules |
| `get_protect_rules` | Get protection rules configured for an application |

#### Coverage
| Tool | Description |
|------|-------------|
| `get_route_coverage` | Get route coverage data showing exercised vs discovered routes |

#### SAST (Scan)
| Tool | Description |
|------|-------------|
| `get_scan_project` | Get SAST project details and vulnerability counts |
| `get_scan_results` | Get SAST scan results in SARIF format |

### Quick start

#### Prerequisites
- Docker (recommended) or Java 21+ for JAR deployment
- Contrast API credentials ([how to get API credentials](https://docs.contrastsecurity.com/en/personal-keys.html))

#### VS Code (GitHub Copilot) - One-Click Install

[![Install in VS Code](https://img.shields.io/badge/VS_Code-Install_contrast--mcp-0098FF?style=for-the-badge&logo=visualstudiocode&logoColor=ffffff)](vscode:mcp/install?%7B%22name%22%3A%22contrast%22%2C%22type%22%3A%22stdio%22%2C%22command%22%3A%22docker%22%2C%22args%22%3A%5B%22run%22%2C%22-e%22%2C%22CONTRAST_HOST_NAME%22%2C%22-e%22%2C%22CONTRAST_API_KEY%22%2C%22-e%22%2C%22CONTRAST_SERVICE_KEY%22%2C%22-e%22%2C%22CONTRAST_USERNAME%22%2C%22-e%22%2C%22CONTRAST_ORG_ID%22%2C%22-i%22%2C%22--rm%22%2C%22contrast%2Fmcp-contrast%3Alatest%22%2C%22-t%22%2C%22stdio%22%5D%2C%22env%22%3A%7B%22CONTRAST_HOST_NAME%22%3A%22%24%7Binput%3Acontrast_host_name%7D%22%2C%22CONTRAST_ORG_ID%22%3A%22%24%7Binput%3Acontrast_org_id%7D%22%2C%22CONTRAST_USERNAME%22%3A%22%24%7Binput%3Acontrast_username%7D%22%2C%22CONTRAST_API_KEY%22%3A%22%24%7Binput%3Acontrast_api_key%7D%22%2C%22CONTRAST_SERVICE_KEY%22%3A%22%24%7Binput%3Acontrast_service_key%7D%22%7D%2C%22inputs%22%3A%5B%7B%22type%22%3A%22promptString%22%2C%22id%22%3A%22contrast_host_name%22%2C%22description%22%3A%22Your%20Contrast%20Server%27s%20host%20name%20%28with%20or%20without%20https%3A%2F%2F%29%22%7D%2C%7B%22type%22%3A%22promptString%22%2C%22id%22%3A%22contrast_org_id%22%2C%22description%22%3A%22Your%20Contrast%20Organization%20ID.%22%7D%2C%7B%22type%22%3A%22promptString%22%2C%22id%22%3A%22contrast_username%22%2C%22description%22%3A%22Your%20Contrast%20User%20Name%20%28Usually%20your%20email%29.%22%7D%2C%7B%22type%22%3A%22promptString%22%2C%22id%22%3A%22contrast_api_key%22%2C%22description%22%3A%22API%20key%20for%20your%20Contrast%20Installation.%22%2C%22password%22%3Atrue%7D%2C%7B%22type%22%3A%22promptString%22%2C%22id%22%3A%22contrast_service_key%22%2C%22description%22%3A%22Service%20key%20for%20your%20Contrast%20Installation.%22%2C%22password%22%3Atrue%7D%5D%7D)

Click the button above to automatically install in VS Code. For manual setup, see the [VS Code (GitHub Copilot) Installation Guide](docs/installation-guides/install-vscode.md).

#### IntelliJ IDEA (GitHub Copilot)

Add this to your `mcp.json` configuration file and replace the placeholder values with your Contrast credentials:

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

📖 [Full IntelliJ (GitHub Copilot) Installation Guide](docs/installation-guides/install-intellij.md) - Includes step-by-step setup and JAR deployment option

#### Other AI Assistants

- **[Claude Code](docs/installation-guides/install-claude-code.md)** - Anthropic's official CLI tool
- **[Claude Desktop](docs/installation-guides/install-claude-desktop.md)** - Standalone Claude application
- **[Cline Plugin](docs/installation-guides/install-cline.md)** - VS Code alternative AI assistant
- **[All Other MCP Hosts](docs/installation-guides/)** - Complete installation guides for oterm and more

### More setup and troubleshooting

Getting the JAR file (download, attestation verification, and build from source), proxy configuration, and troubleshooting have moved to the [Local MCP Server guide](docs/local-mcp-server.md).

## Sample prompts

These prompts work with either server.

### For the Developer
#### Remediate Vulnerabilities in Code
* Please list vulnerabilities for Application Y.
* Give me details about vulnerability X in Application Y.
* Review vulnerability X and fix it.

#### Third-Party Library Remediation
* Which libraries in Application X have high or critical vulnerabilities and are actively used?
* Update library X, which has a critical vulnerability, to the safe version.
* Which libraries in Application X are not being used?

#### Retrieve Applications by Tag
* Please give me the applications tagged with "backend."

#### Retrieve Applications by Metadata
* Please give me the applications with metadata "dev-team" and "backend-team."

#### Retrieve Vulnerabilities by Session Metadata
* Give me the session metadata for Application X.
* Give me the vulnerabilities in the latest session for Application X.
* Give me the vulnerabilities for session metadata "Branch Name" "feature/some-new-fix" for Application X.
* Give me the route coverage for the latest session for Application X.
* Give me the route coverage for session metadata "Branch Name" "feature/some-new-fix" for Application X.

### For the Security Professional
* Please give me a breakdown of applications and servers vulnerable to CVE-xxxx-xxxx.
* Please list the libraries for the application named xxx and tell me what version of commons-collections is being used.
* Which vulnerabilities in Application X are being blocked by a Protect or ADR rule?

## Data privacy

The Contrast MCP Server provides a bridge between your Contrast Data and the AI Agent/LLM of your choice.
By using Contrast's MCP server you will be providing your Contrast Data to your AI Agent/LLM, it is your responsibility to ensure that the AI Agent/LLM you use complies with your data privacy policy.
Depending on what questions you ask the following information will be provided to your AI Agent/LLM.
* Application Details
* Application Rule configuration
* Vulnerability Details
* Route Coverage data
* ADR/Protect Attack Event Details
* Server inventory and agent details (hostnames, paths, agent versions, environments, log levels, and tags)

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the complete release history, including breaking changes and new features.
