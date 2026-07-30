# Two-tier response envelope with notices, and point-of-use teaching

**Status:** accepted (AIML-942)

Tool descriptions had grown into multi-section documents that explained response quirks up front, costing context tokens in every session whether or not the quirk ever occurred. We decided to deliver interpretation facts at the point of use instead. Conditional quirks (an unscored item, a missing total, an empty session) are emitted as informational entries in the response envelope only when the condition occurs. Always-true field meaning is carried by self-describing field names, renaming a misleading field rather than documenting it.

To support this, the envelope's informational tier is renamed from `warnings` to `notices`, full-depth, wire field, `WarningCollector` becoming `NoticeCollector`, and `warn()` becoming `notice()`. "Warnings" primed agents to relay teaching notes to users as problems and to spend turns resolving notes that describe normal behavior. The envelope keeps exactly two tiers, `errors` (actionable, fix the call and retry) and `notices` (informational, read while interpreting the result), because that is the only distinction that changes agent behavior.

## Considered options

- A third `notes` tier beside `errors` and `warnings`. Rejected. Agents behave identically on a warning and a note, and an earlier consolidation had already merged the informational tiers once.
- Keeping the `warnings` name to avoid a breaking wire change. Rejected while the rename window is open. The field was nearly unused, and it is about to become the primary teaching channel, so the name will never be cheaper to fix than now.
- Alternative names `messages` (collides with chat message arrays in LLM contexts), `advisories` (reads as security advisories in an appsec product), `info` (log-level connotation). `notices` fits both teaching notes and degradation reports.

## Consequences

- The rename is a breaking change for consumers that read the `warnings` field. It ships in one coordinated envelope sweep together with response-field renames, so the shape breaks once.
- An always-emitted notice is a design smell. If a notice fires on every response, its fact is not conditional and belongs in another home (field name, parameter description, or description body per MCP_STANDARDS.md).
- Degradation reporting keeps the "not available (retrieval error)" suffix convention so agents can distinguish transient absence from legitimate absence.
