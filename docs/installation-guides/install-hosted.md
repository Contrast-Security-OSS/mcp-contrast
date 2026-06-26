# Hosted MCP Server installation guide

The [Hosted MCP Server](../../README.md#hosted-mcp-server-recommended) is a remote MCP server that Contrast runs for you. You point your client at one URL, sign in through your browser, and your agent can start asking questions about your security data. There are no API keys to manage and nothing to install.

This guide covers per-client setup. The steps are nearly identical across clients, because every client does the same two things. It points at a URL and completes a browser OAuth sign-in.

## Before you start

- A Contrast SaaS account with access to at least one organization
- One of the [supported clients](#supported-clients) below
- A modern web browser for the OAuth sign-in

Your endpoint is your Contrast host followed by `/mcp`. The examples below use `https://app.contrastsecurity.com/mcp`. Replace `app.contrastsecurity.com` with your organization's Contrast URL if you use a dedicated instance.

## Claude Code (CLI)

Add the server.

```bash
claude mcp add --transport http contrast-hosted-mcp https://app.contrastsecurity.com/mcp
```

Verify it was added.

```bash
claude mcp list
```

You should see `contrast-hosted-mcp` in the list. The first time a tool is called, your browser opens for sign-in.

## Claude Desktop

Claude Desktop requires an HTTPS endpoint, so it works with your Contrast URL but not with a plain `http://localhost` address.

1. Open **Claude Desktop** settings.
2. Go to the **MCP Servers** section.
3. Add a new server with these values.
   - **Name** is `contrast-hosted-mcp`
   - **Transport** is HTTP
   - **URL** is `https://app.contrastsecurity.com/mcp`
4. Save and restart Claude Desktop.

## Codex CLI

Add the server as an HTTP MCP server pointed at your Contrast endpoint, then trigger any Contrast tool to start the browser sign-in. Use the same endpoint URL shown above.

## GitHub Copilot CLI

Add the server as an HTTP MCP server pointed at your Contrast endpoint, then trigger any Contrast tool to start the browser sign-in. Use the same endpoint URL shown above.

## opencode

Add the server as an HTTP MCP server pointed at your Contrast endpoint, then trigger any Contrast tool to start the browser sign-in. Use the same endpoint URL shown above.

## Other MCP clients

Any MCP client that supports Streamable HTTP transport and OAuth 2.0 with PKCE can connect.

| Setting | Value |
|---------|-------|
| Endpoint URL | `https://<your-contrast-host>/mcp` |
| Transport | Streamable HTTP (stateless) |
| HTTP method | `POST` |
| Authentication | OAuth 2.0 with PKCE (S256) |
| OAuth scopes | `openid`, `profile`, `offline_access` |

Your client discovers the OAuth configuration automatically through the `WWW-Authenticate` response header, which points to the standard `/.well-known/oauth-protected-resource` metadata document. Clients that support Dynamic Client Registration can register at `/oauth2/connect/register` on the Contrast origin.

## Signing in

When you first use a Contrast tool, your browser opens for sign-in.

1. Log in with your Contrast credentials, the same ones you use for the Contrast web interface.
2. Select an organization if you belong to more than one.
3. Approve read access.

After signing in, the connection is active. Your access token refreshes on its own, so you typically do not sign in again during a session. Each session is scoped to one organization. To use a different organization, remove and re-add the connection, then sign in again and pick the other organization.

> Never paste bearer tokens, refresh tokens, or other credentials into chat, tickets, or issue comments. If you expose a token, contact your Contrast administrator.

## Verifying the connection

Ask your agent to run `get_user_info`. A successful response shows your user identity and the organization you are connected to. In Claude Code you can simply say "Run get_user_info to check my connection."

## Supported clients

| Client | Status |
|--------|--------|
| Claude Code CLI | Working |
| Codex CLI | Working |
| GitHub Copilot CLI | Working |
| opencode | Working |
| Claude Desktop | Working (requires an HTTPS endpoint) |
| Gemini CLI | Not yet supported, OAuth compatibility issue |
| VS Code Copilot plugin | Not yet supported, OAuth compatibility issue |

Support for more clients is in progress as their OAuth handling matures. If your client fails during OAuth registration before the login page appears, that is usually a client compatibility issue rather than a problem with your account.

## Troubleshooting

- **Sign-in completes but nothing happens.** Your client may not have finished the OAuth token exchange. Remove the connection, re-add it, and try again. Check that your browser is not blocking pop-ups or redirects from the Contrast domain.
- **Token expired.** Access tokens are short-lived and refresh on their own. Retry the request. If it keeps failing, remove and re-add the connection for a fresh sign-in.
- **503, MCP authentication is not available.** OAuth-based MCP access is not enabled for your Contrast environment. Contact Contrast to have it enabled.
- **Authorization error from a tool.** The tools respect your Contrast role-based permissions. Confirm you selected the right organization with `get_user_info`, and check that your role allows the data you are requesting.
- **Cannot connect at all.** Verify the endpoint URL ends with `/mcp`, confirm your client is in the supported list, and ensure you have network access to your Contrast instance.
