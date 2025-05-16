package com.contrast.labs.ai.mcp.contrast.hints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enum representing different rule types and their associated hints.
 * These hints are used to provide guidance to AI agents when generating responses.
 */
public enum RuleHints {

    // General guidance that applies to all rule types
    GENERAL_GUIDANCE(Arrays.asList(
        "Where a vulnerable library exists in the stacktrace and it looks like that library may be the cause of the underlying vulnerability, prioritise upgrading that library but only fix the vulnerability explicitly asked by the developer",
        "Move the sanitisation/validation logic into a separate method to make it easier to add as a security control if needed.",
        "If the fix does not break the data flow between user input and sink and doesnt use well known libraries, you MUST give the user documentation on how to add a security control. Do not forget to inform the developer of this as you fix the vulnerability.",
        "If including regular expressions as a suggested fix, please advise that Contrast requires configuration via Security Controls in order to recognize this fix. Documentation for Security Controls is available at https://docs.contrastsecurity.com/en/security-controls.html",
        "Prefer to use industry best practices in remediating vulnerabilities from reputable sources like OWASP.",
        "When using libraries for validation ensure they're well known and maintained e.g. like dompurify, esapi, etc."
    )),
    
    HQL_INJECTION("hql-injection", Arrays.asList(
        "If all permutations of an input variable are known, use a allow list / enum to break the dataflow.",
        "If prepared statement already, it's likely that an injection exists in the string build query prior to prepare statement."
    )),
    
    SQL_INJECTION("sql-injection", Arrays.asList(
        "If all permutations of an input variable are known, use a allow list / enum to break the dataflow.",
        "If prepared statement already, it's likely that an injection exists in the string build query prior to prepare statement."
    )),
    
    NOSQL_INJECTION("nosql-injection", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow."
    )),
    
    NOSQL_INJECTION_DYNAMODB("nosql-injection-dynamodb", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow."
    )),
    
    TRACE_ENABLED("trace-enabled", Collections.emptyList()),
    
    CSRF("csrf", Arrays.asList(
        "Requests that have header based authentication (jwt, API keys, etc) are not vulnerable to CSRF. Java made improvements to agents to include this in analysis as of 6.12.0.",
        "If possible enable CSRF protections that already exist in the application, e.g Spring-Security if using Spring. If that is not possible consider CSRFGuard."
    )),
    
    PATH_TRAVERSAL("path-traversal", Arrays.asList(
        "If possible do not put user input into the filename. Use a randomly generated string as the file name.",
        "If user data is required in the file path, consider using the latest version of OWASP ESAPI."
    )),
    
    UNVALIDATED_FORWARD("unvalidated-forward", Collections.emptyList()),
    
    CMD_INJECTION("cmd-injection", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow.",
        "If possible use ProcessBuilder and pass each arguments in as individual elements in the array e.g ProcessBuilder processBuilder = new ProcessBuilder(new String[]{\"ls\",\"-l\",\"/tmp/\"}); Rather than concatenating arguments into a single String."
    )),
    
    ESCAPE_TEMPLATES_OFF("escape-templates-off", Collections.emptyList()),
    
    EXPRESSION_LANGUAGE_INJECTION("expression-language-injection", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow.",
        "escapeEcmaScript() in Apache string utils is not a valid sanitization for this vulnerability."
    )),
    
    HEADER_INJECTION("header-injection", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow."
    )),
    
    LDAP_INJECTION("ldap-injection", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow.",
        "Consider using org.owasp.esapi.Encoder.encodeForLDAP(java.lang.String) for sanitization."
    )),
    
    REFLECTED_XSS("reflected-xss", Arrays.asList(
        "Consider using org.owasp.validator.html.scan.AbstractAntiSamyScanner.scan(java.lang.String) for sanitization."
    )),
    
    STORED_XSS("stored-xss", Arrays.asList(
        "Consider using org.owasp.validator.html.scan.AbstractAntiSamyScanner.scan(java.lang.String) for sanitization."
    )),
    
    UNSAFE_CODE_EXECUTION("unsafe-code-execution", Collections.emptyList()),
    
    XPATH_INJECTION("xpath-injection", Arrays.asList(
        "Consider using org.owasp.esapi.Encoder.encodeForXPath(java.lang.String) for sanitization."
    )),
    
    OVERLY_PERMISSIVE_CROSS_DOMAIN_POLICY("overly-permissive-cross-domain-policy", Collections.emptyList()),
    
    VERB_TAMPERING("verb-tampering", Collections.emptyList()),
    
    XXE("xxe", Arrays.asList(
        "Where user input can enter into the XML being read ensure that External Entities are disabled and ensure the document cannot supply its own doctype. e.g",
        "DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();",
        "docBuilderFactory.setFeature(\"http://xml.org/sax/features/external-general-entities\", false);",
        "docBuilderFactory.setFeature(\"http://apache.org/xml/features/disallow-doctype-decl\", true);"
    )),
    
    UNTRUSTED_DESERIALIZATION("untrusted-deserialization", Arrays.asList(
        "For Java Object Deserialization use a java.io.ObjectInputFilter to create a whitelist of known good classes that are expected."
    )),
    
    AUTHORIZATION_RULES_MISORDERED("authorization-rules-misordered", Collections.emptyList()),
    
    FORMS_AUTH_PROTECTION("forms-auth-protection", Collections.emptyList()),
    
    FORMS_AUTH_REDIRECT("forms-auth-redirect", Collections.emptyList()),
    
    INSECURE_JSP_ACCESS("insecure-jsp-access", Collections.emptyList()),
    
    UNVALIDATED_REDIRECT("unvalidated-redirect", Collections.emptyList()),
    
    CRYPTO_BAD_MAC("crypto-bad-mac", Arrays.asList(
        "If the crypto is not used for security sensitive functionality, this may not be an issue."
    )),
    
    HARDCODED_KEY("hardcoded-key", Arrays.asList(
        "Agents just look at the variable name to see if it seems like a \"key\" and if it's statically defined, which can be wrong. With context, check if it makes sense."
    )),
    
    INSECURE_SOCKET_FACTORY("insecure-socket-factory", Collections.emptyList()),
    
    VIEWSTATE_ENCRYPTION_DISABLED("viewstate-encryption-disabled", Collections.emptyList()),
    
    FORMS_AUTH_SSL("forms-auth-ssl", Collections.emptyList()),
    
    EVENT_VALIDATION_DISABLED("event-validation-disabled", Collections.emptyList()),
    
    REDOS("redos", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow."
    )),
    
    REFLECTION_INJECTION("reflection-injection", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow."
    )),
    
    SMTP_INJECTION("smtp-injection", Arrays.asList(
        "If all permutations of an input variable are known, use an allow list / enum to break the dataflow."
    )),
    
    UNSAFE_XML_DECODE("unsafe-xml-decode", Collections.emptyList()),
    
    CACHE_CONTROL_DISABLED("cache-control-disabled", Collections.emptyList()),
    
    TRUST_BOUNDARY_VIOLATION("trust-boundary-violation", Collections.emptyList()),
    
    COOKIE_FLAGS_MISSING("cookie-flags-missing", Collections.emptyList()),
    
    COOKIE_HEADER_MISSING_FLAGS("cookie-header-missing-flags", Collections.emptyList()),
    
    CUSTOM_ERRORS_OFF("custom-errors-off", Collections.emptyList()),
    
    HEADER_CHECKING_DISABLED("header-checking-disabled", Collections.emptyList()),
    
    HTTPONLY("httponly", Collections.emptyList()),
    
    MAX_REQUEST_LENGTH("max-request-length", Collections.emptyList()),
    
    REQUEST_VALIDATION_CONTROL_DISABLED("request-validation-control-disabled", Collections.emptyList()),
    
    REQUEST_VALIDATION_DISABLED("request-validation-disabled", Collections.emptyList()),
    
    ROLE_MANAGER_PROTECTION("role-manager-protection", Collections.emptyList()),
    
    ROLE_MANAGER_SSL("role-manager-ssl", Collections.emptyList()),
    
    SECURE_FLAG_MISSING("secure-flag-missing", Collections.emptyList()),
    
    VIEWSTATE_MAC_DISABLED("viewstate-mac-disabled", Collections.emptyList()),
    
    WCF_EXCEPTION_DETAILS("wcf-exception-details", Collections.emptyList()),
    
    AUTHORIZATION_MISSING_DENY("authorization-missing-deny", Collections.emptyList()),
    
    HARDCODED_PASSWORD("hardcoded-password", Arrays.asList(
        "Agents just look at the variable name to see if it seems like a \"password\" and if it's statically defined, which can be wrong. With context, check if it makes sense."
    )),
    
    INSECURE_AUTH_PROTOCOL("insecure-auth-protocol", Collections.emptyList()),
    
    PLAINTEXT_CONN_STRINGS("plaintext-conn-strings", Collections.emptyList()),
    
    SESSION_REWRITING("session-rewriting", Collections.emptyList()),
    
    SPRING_UNCHECKED_AUTOBINDING("spring-unchecked-autobinding", Collections.emptyList()),
    
    SSRF("ssrf", Arrays.asList(
        "We should only report this if user input can control the protocol or host of a string but worth mentioning."
    )),
    
    UNSAFE_READLINE("unsafe-readline", Collections.emptyList()),
    
    WCF_DETECT_REPLAYS("wcf-detect-replays", Collections.emptyList()),
    
    X_POWERED_BY_HEADER("x-powered-by-header", Collections.emptyList()),
    
    SESSION_REGENERATE("session-regenerate", Collections.emptyList()),
    
    SESSION_TIMEOUT("session-timeout", Collections.emptyList()),
    
    WEAK_MEMBERSHIP_CONFIG("weak-membership-config", Collections.emptyList()),
    
    CRYPTO_BAD_CIPHERS("crypto-bad-ciphers", Arrays.asList(
        "If the crypto is not used for security sensitive functionality, this may not be an issue.",
        "If the vulnerability exists in a 3rd party library, rather than accessible code, consider upgrading that library."
    )),
    
    CRYPTO_WEAK_RANDOMNESS("crypto-weak-randomness", Arrays.asList(
        "If the crypto is not used for security sensitive functionality, this may not be an issue.",
        "If the vulnerability exists in a 3rd party library, rather than accessible code, consider upgrading that library."
    )),
    
    AUTOCOMPLETE_MISSING("autocomplete-missing", Collections.emptyList()),
    
    CACHE_CONTROLS_MISSING("cache-controls-missing", Collections.emptyList()),
    
    CLICKJACKING_CONTROL_MISSING("clickjacking-control-missing", Collections.emptyList()),
    
    CSP_HEADER_INSECURE("csp-header-insecure", Arrays.asList(
        "We dislike how this rule works. IMO agents should just send up the CSP and it be evaluated using https://csp-evaluator.withgoogle.com/, which is open source."
    )),
    
    CSP_HEADER_MISSING("csp-header-missing", Arrays.asList(
        "Add guidance on what we look for in a secure CSP, so they don't add a CSP and then we report the above rule.",
        "It's worth noting sometimes these headers could be set at the perimeter (app gateway, load balancer, etc). It's probably not that common but could happen."
    )),
    
    PARAMETER_POLLUTION("parameter-pollution", Collections.emptyList()),
    
    COMPILATION_DEBUG("compilation-debug", Collections.emptyList()),
    
    HSTS_HEADER_MISSING("hsts-header-missing", Collections.emptyList()),
    
    HTTP_ONLY_DISABLED("http-only-disabled", Collections.emptyList()),
    
    RAILS_HTTP_ONLY_DISABLED("rails-http-only-disabled", Collections.emptyList()),
    
    TRACE_ENABLED_ASPX("trace-enabled-aspx", Collections.emptyList()),
    
    VERSION_HEADER_ENABLED("version-header-enabled", Collections.emptyList()),
    
    WCF_METADATA_ENABLED("wcf-metadata-enabled", Collections.emptyList()),
    
    XCONTENTTYPE_HEADER_MISSING("xcontenttype-header-missing", Collections.emptyList()),
    
    XXSSPROTECTION_HEADER_DISABLED("xxssprotection-header-disabled", Arrays.asList(
        "This header is deprecated https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/X-XSS-Protection",
        "We should recommend using a CSP instead."
    )),
    
    LOG_INJECTION("log-injection", Collections.emptyList());

    private final String ruleName;
    private final List<String> hints;

    /**
     * Constructor for enum values with rule name and hints.
     * @param ruleName The rule name as it appears in Contrast
     * @param hints The list of hints to provide for this rule
     */
    RuleHints(String ruleName, List<String> hints) {
        this.ruleName = ruleName;
        this.hints = Collections.unmodifiableList(hints);
    }
    
    /**
     * Constructor for general guidance that doesn't have a specific rule name.
     * @param hints The list of general guidance hints
     */
    RuleHints(List<String> hints) {
        this.ruleName = this.name().toLowerCase().replace("_", "-");
        this.hints = Collections.unmodifiableList(hints);
    }
    
    /**
     * Get the rule name as it appears in Contrast.
     * @return The rule name
     */
    public String getRuleName() {
        return ruleName;
    }
    
    /**
     * Get the list of hints associated with this rule.
     * @return An unmodifiable list of hints
     */
    public List<String> getHints() {
        return hints;
    }
    
    /**
     * Find a RuleHints enum by its rule name.
     * @param ruleName The rule name to search for
     * @return The matching RuleHints enum, or null if not found
     */
    public static RuleHints findByRuleName(String ruleName) {
        for (RuleHints hint : values()) {
            if (hint.getRuleName().equals(ruleName)) {
                return hint;
            }
        }
        return null;
    }
}
