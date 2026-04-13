# Security Policy

## Reporting Vulnerabilities

To report a security vulnerability in this project, please follow Contrast Security's responsible disclosure policy:

**https://www.contrastsecurity.com/disclosure-policy**

Do not open a public GitHub issue for security vulnerabilities.

---

## Dependency Soak Window Policy (ENTSEC-1742)

All dependency version upgrades in this repository are subject to a **7-day soak window**. No dependency version published fewer than 7 days ago may be introduced into any build.

This is enforced automatically via Dependabot's `cooldown: default-days: 7` configuration in `.github/dependabot.yml`.

### Scope

- All Maven dependencies in `pom.xml`
- All transitive dependencies pulled in via BOMs (Spring Boot, Spring AI)

### Rationale

The soak window mitigates supply chain attacks — including dependency confusion and malware injected into new package releases — by allowing time for the security community to identify and report issues before we adopt a new version.

### Break Glass Procedure

Critical security patches may need to be applied faster than the 7-day window allows. To bypass the soak window:

1. **Post in `#contrast-labs`** on Slack with:
   - The dependency name and version
   - The CVE or security advisory requiring the update
   - The urgency and impact if not patched immediately
2. **Await Labs sign-off** — a member of `#contrast-labs` must approve the exception
3. **Document the exception** — record the approval in the PR description and link to the `#contrast-labs` thread
4. **Merge with approval** — proceed with the update once approved

> All exclusions must go through `#contrast-labs`. Do not bypass the soak window without explicit approval.
