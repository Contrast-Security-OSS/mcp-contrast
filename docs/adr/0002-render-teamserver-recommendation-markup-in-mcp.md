# Render TeamServer recommendation markup to Markdown inside MCP

**Status:** accepted (AIML-1357)

TeamServer serves per-vulnerability-type remediation guidance (the Recommendation, exposed as `howToFix` in `get_vulnerability`) in two variants and offers no fully rendered one. The `text` variant is a lossy flattening: it leaks `$$LINK_DELIM$$` link delimiters, drops code-block boundaries, and prefixes lines with literal tabs. The `formattedText` variant carries Contrast's in-house mustache-style markup (`{{#paragraph}}`, `{{#javaBlock}}`, `{{#link}}url$$LINK_DELIM$$label{{/link}}`) plus a variables map, and is what the NorthStar UI renders client-side. We decided that this server renders `formattedText` to Markdown itself, using JMustache (`com.samskivert:jmustache`) with a lambda-per-token context, the same architecture TeamServer's own `JMustacheHelper` uses to render this markup to HTML for attestation PDFs. Ours is new Apache-2.0 code emitting Markdown; nothing is copied from the commercially licensed TeamServer file.

## Considered options

- Keep consuming `text` and scrub only `$$LINK_DELIM$$`. Rejected: code-fence and link-label boundaries do not exist in `text`, so unfenced code mixed into prose (a confirmed QA finding) could never be fixed from that field.
- Ask TeamServer or platform-public-api for a rendered variant. Rejected: none exists anywhere in the company; the only renderers are contrast-ui (mustache.js to HTML) and TeamServer's `JMustacheHelper` (JMustache to HTML), both wrong output targets for an LLM consumer.
- Hand-rolled regex parser with no new dependency. Rejected: the markup nests sections, and JMustache is BSD-licensed, tiny, dependency-free, and long-stable.

## Consequences

- The renderer must never leak raw markup and never drop content. Unknown tokens are unwrapped (inner content kept, tag dropped) with a warning logged that a new TeamServer tag was found and the tag inventory may need re-scanning. Whole-template render failure falls back to the scrubbed `text` variant.
- The token inventory is owned by TeamServer and can grow. `JMustacheHelper` in the teamserver repo and `coreMustacheRenderers.ts` in contrast-ui are the reference inventories to re-scan.
- Reading single-vulnerability recommendations requires a Licensed application; unlicensed apps 403 (see CONTEXT.md "Licensed application").
