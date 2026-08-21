# T15 SSRF Defenses Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add SSRF protection to ByteChef's user-supplied-URL outbound surfaces (job/task webhooks, EE documentation fetch, open-redirect) via a shared CE URL validator with a global enable/disable + allowlist escape-hatch.

**Architecture:** Port the proven EE `AiObservabilityUrlValidator` logic into a CE `commons-util` `UrlValidator` primitive (scheme allowlist + resolve-all-records + private/loopback/link-local/CGNAT/IPv6-ULA blocking). Apply it at the selected Spring call sites (config read via `@Value`, matching the cycle-avoiding pattern from the TOTP work) and inside the static `RedirectValidator`. Refactor the EE validator to delegate to the new primitive.

**Tech Stack:** Java 25, JDK `java.net.InetAddress`/`URI` (no new deps), JUnit 5 + Mockito + AssertJ, Spring `@Value`.

## Global Constraints

- Files under `server/ee/` use the ByteChef Enterprise license header and a `@version ee` Javadoc tag; files under `server/libs/` use the Apache 2.0 header. (Spotless picks the header by file content.)
- Run `./gradlew spotlessApply` before every commit; per-module `:check` is the gate.
- Blank line before control statements and after a variable modification a later statement uses (Java style rules in CLAUDE.md).
- Test method names are camelCase without underscores; unit test classes end in `Test`.
- Commit messages: server-side `gecko <description>`; end every commit body with the `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.
- Branch is `0_732`, the user commits in parallel — never `git commit --amend`; make fresh commits and stage only files this plan touches.
- SSRF protection defaults: `bytechef.security.ssrf.enabled` = `true`, `bytechef.security.ssrf.allowed-hosts` = empty.
- The `allowed-hosts` allowlist matches **hostnames** (exact, case-insensitive, including literal-IP hosts). CIDR-range allowlisting is **not** implemented this cycle (documented trim of the spec's "hostnames/CIDRs"); note it in the tracker.

---

## File Structure

**Task 1 — CE primitive:**
- Create: `server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/UrlValidator.java`
- Create: `server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/UrlValidationException.java`
- Test: `server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/UrlValidatorTest.java`

**Task 2 — EE delegation:**
- Modify: `server/ee/libs/platform/platform-ai/platform-ai-observability/platform-ai-observability-api/src/main/java/com/bytechef/ee/platform/ai/observability/security/AiObservabilityUrlValidator.java`

**Task 3 — Job webhook save-time validation:**
- Modify: `server/libs/atlas/atlas-execution/atlas-execution-service/src/main/java/com/bytechef/atlas/execution/service/JobServiceImpl.java`
- Test: `server/libs/atlas/atlas-execution/atlas-execution-service/src/test/java/com/bytechef/atlas/execution/service/JobServiceWebhookUrlValidationTest.java`

**Task 4 — Webhook delivery-time re-validation:**
- Modify: `server/libs/platform/platform-coordinator/src/main/java/com/bytechef/platform/coordinator/event/listener/WebhookTaskStartedApplicationEventListener.java`
- Test: `server/libs/platform/platform-coordinator/src/test/java/com/bytechef/platform/coordinator/event/listener/WebhookTaskStartedApplicationEventListenerTest.java`

**Task 5 — Open-redirect private-target rejection:**
- Modify: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-api/src/main/java/com/bytechef/platform/webhook/rest/validator/RedirectValidator.java`
- Test: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-api/src/test/java/com/bytechef/platform/webhook/rest/validator/RedirectValidatorTest.java`

**Task 6 — EE documentation fetch validation:**
- Modify: `server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-service/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiServiceImpl.java`

**Task 7 — Close out tracker + checks:**
- Modify: `gecko-remediation-tasks.md`

---

## Task 1: CE `UrlValidator` primitive

**Files:** see File Structure, Task 1.

**Interfaces:**
- Produces:
  - `UrlValidator.validate(String url, java.util.Set<String> allowedHosts)` — throws `UrlValidationException` on malformed URL, non-http(s) scheme, unresolvable host, or any resolved address being private/loopback/link-local/any-local/multicast/CGNAT/IPv6-ULA, unless the literal host is in `allowedHosts` (case-insensitive).
  - `UrlValidator.isValid(String url, java.util.Set<String> allowedHosts)` — boolean wrapper (true iff `validate` would not throw).
  - `UrlValidationException extends RuntimeException`.

- [ ] **Step 1: Write the failing test**

Create `server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/UrlValidatorTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class UrlValidatorTest {

    @Test
    void testPublicLiteralIpPasses() {
        // 1.1.1.1 is a public literal IP; getAllByName parses it without DNS.
        assertThat(UrlValidator.isValid("https://1.1.1.1/path", Set.of())).isTrue();
    }

    @Test
    void testLoopbackBlocked() {
        assertThat(UrlValidator.isValid("http://127.0.0.1", Set.of())).isFalse();
        assertThat(UrlValidator.isValid("http://[::1]", Set.of())).isFalse();
    }

    @Test
    void testPrivateRangesBlocked() {
        assertThat(UrlValidator.isValid("http://10.0.0.1", Set.of())).isFalse();
        assertThat(UrlValidator.isValid("http://192.168.1.1", Set.of())).isFalse();
    }

    @Test
    void testLinkLocalAndCgnatBlocked() {
        assertThat(UrlValidator.isValid("http://169.254.169.254", Set.of())).isFalse();
        assertThat(UrlValidator.isValid("http://100.64.0.1", Set.of())).isFalse();
    }

    @Test
    void testNonHttpSchemeBlocked() {
        assertThat(UrlValidator.isValid("ftp://1.1.1.1", Set.of())).isFalse();
    }

    @Test
    void testMissingSchemeOrHostBlocked() {
        assertThat(UrlValidator.isValid("1.1.1.1", Set.of())).isFalse();
        assertThat(UrlValidator.isValid("http://", Set.of())).isFalse();
    }

    @Test
    void testAllowlistedPrivateHostPasses() {
        assertThat(UrlValidator.isValid("http://10.0.0.1", Set.of("10.0.0.1"))).isTrue();
    }

    @Test
    void testValidateThrowsWithMessage() {
        assertThatThrownBy(() -> UrlValidator.validate("http://127.0.0.1", Set.of()))
            .isInstanceOf(UrlValidationException.class);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails to compile**

Run: `./gradlew :server:libs:core:commons:commons-util:compileTestJava`
Expected: FAIL — `UrlValidator` / `UrlValidationException` not found.

- [ ] **Step 3: Create `UrlValidationException`**

Create `.../commons/util/UrlValidationException.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.commons.util;

/**
 * Thrown when a URL fails outbound-request (SSRF) validation.
 *
 * @author Ivica Cardic
 */
public class UrlValidationException extends RuntimeException {

    public UrlValidationException(String message) {
        super(message);
    }

    public UrlValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Create `UrlValidator`**

Create `.../commons/util/UrlValidator.java` (logic ported from the EE `AiObservabilityUrlValidator`, with the allowlist added):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.commons.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * Validates that a URL points to a public, non-internal host, to guard against Server-Side Request Forgery (SSRF).
 *
 * <p>
 * Rejects non-http(s) schemes and any host that resolves to a loopback, private (RFC 1918), link-local, any-local,
 * multicast, CGNAT (100.64.0.0/10) or IPv6 unique-local (fc00::/7) address. Every A/AAAA record is checked so a
 * multi-record hostname with a mixed public/private result cannot slip a private address through. A host whose literal
 * value is in {@code allowedHosts} (case-insensitive) bypasses the check, providing an escape-hatch for legitimate
 * internal targets.
 *
 * <p>
 * <b>DNS rebinding (accepted residual risk):</b> the JDK {@link java.net.http.HttpClient} does not expose a DNS
 * resolver hook, so connect-time resolution happens after this validator returns. Callers that need stronger guarantees
 * should re-validate per delivery attempt.
 *
 * @author Ivica Cardic
 */
public final class UrlValidator {

    private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");

    private UrlValidator() {
    }

    public static boolean isValid(String url, Set<String> allowedHosts) {
        try {
            validate(url, allowedHosts);

            return true;
        } catch (UrlValidationException urlValidationException) {
            return false;
        }
    }

    public static void validate(String url, Set<String> allowedHosts) {
        if (url == null || url.isBlank()) {
            throw new UrlValidationException("URL must not be null or blank");
        }

        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new UrlValidationException("Malformed URL: " + url, illegalArgumentException);
        }

        String scheme = uri.getScheme();

        if (scheme == null || !ALLOWED_URL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new UrlValidationException(
                "URL scheme '" + scheme + "' is not allowed. Only HTTP and HTTPS are permitted.");
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new UrlValidationException("URL must contain a valid host");
        }

        if (isAllowlisted(host, allowedHosts)) {
            return;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);

            if (addresses.length == 0) {
                throw new UrlValidationException("Cannot resolve URL host: " + url);
            }

            for (InetAddress address : addresses) {
                rejectPrivateAddress(host, address);
            }
        } catch (UnknownHostException unknownHostException) {
            throw new UrlValidationException("Cannot resolve URL host: " + url, unknownHostException);
        }
    }

    private static boolean isAllowlisted(String host, Set<String> allowedHosts) {
        for (String allowedHost : allowedHosts) {
            if (allowedHost.equalsIgnoreCase(host)) {
                return true;
            }
        }

        return false;
    }

    private static void rejectPrivateAddress(String host, InetAddress address) {
        if (address.isLinkLocalAddress()) {
            throw new UrlValidationException(
                "URL host '" + host + "' resolves to a link-local address, which is not allowed");
        }

        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isAnyLocalAddress()
            || address.isMulticastAddress()) {

            throw new UrlValidationException(
                "URL host '" + host + "' resolves to a private or loopback address, which is not allowed");
        }

        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            if (first == 100 && second >= 64 && second <= 127) {
                throw new UrlValidationException(
                    "URL host '" + host + "' resolves to a CGNAT address, which is not allowed");
            }
        } else if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();

            if ((bytes[0] & 0xFE) == 0xFC) {
                throw new UrlValidationException(
                    "URL host '" + host + "' resolves to an IPv6 unique local address, which is not allowed");
            }
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:core:commons:commons-util:test --tests "*UrlValidatorTest"`
Expected: PASS (8 tests).

- [ ] **Step 6: Format and commit**

Run: `./gradlew :server:libs:core:commons:commons-util:spotlessApply`

```bash
git add server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/UrlValidator.java \
        server/libs/core/commons/commons-util/src/main/java/com/bytechef/commons/util/UrlValidationException.java \
        server/libs/core/commons/commons-util/src/test/java/com/bytechef/commons/util/UrlValidatorTest.java
git commit -m "gecko Add shared UrlValidator SSRF primitive (T15)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2: Refactor EE `AiObservabilityUrlValidator` to delegate

**Files:** Modify the EE validator only.

**Interfaces:**
- Consumes: `UrlValidator.validate(String, Set)` from Task 1.
- Produces: `AiObservabilityUrlValidator.validateExternalUrl(String)` unchanged signature; now delegates.

- [ ] **Step 1: Confirm the EE module can see commons-util**

Run: `grep -n "commons-util" server/ee/libs/platform/platform-ai/platform-ai-observability/platform-ai-observability-api/build.gradle.kts`
Expected: a `commons-util` dependency line. If absent, add `implementation(project(":server:libs:core:commons:commons-util"))` to that `build.gradle.kts` in this step.

- [ ] **Step 2: Replace the body with delegation**

In `AiObservabilityUrlValidator.java`, keep the class/Javadoc/`@version ee`, replace the implementation so `validateExternalUrl` delegates and the private helpers are removed:

```java
package com.bytechef.ee.platform.ai.observability.security;

import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidator;
import java.util.Set;

/**
 * Validates that a URL points to a public, non-internal host. Delegates to the shared
 * {@link com.bytechef.commons.util.UrlValidator}; retained as the AI-observability-facing entry point and to preserve
 * its callers' exception type.
 *
 * @version ee
 */
public final class AiObservabilityUrlValidator {

    private AiObservabilityUrlValidator() {
    }

    public static void validateExternalUrl(String url) {
        try {
            UrlValidator.validate(url, Set.of());
        } catch (UrlValidationException urlValidationException) {
            throw new IllegalArgumentException(urlValidationException.getMessage(), urlValidationException);
        }
    }
}
```

(The existing callers catch/expect `IllegalArgumentException`, so the wrapper preserves that contract. Keep the full ByteChef Enterprise license header that is already at the top of the file — do not replace it with the Apache header.)

- [ ] **Step 3: Compile the EE module and any existing validator test**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api:compileJava`
Expected: BUILD SUCCESSFUL.
Then, if a test exists: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api:test 2>&1 | tail -15` — Expected: PASS (the behavior is preserved). If the test asserted private-address messages exactly, update the assertions to the new messages from `UrlValidator`.

- [ ] **Step 4: Format and commit**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api:spotlessApply`

```bash
git add server/ee/libs/platform/platform-ai/platform-ai-observability/platform-ai-observability-api/src/main/java/com/bytechef/ee/platform/ai/observability/security/AiObservabilityUrlValidator.java
# also add the build.gradle.kts if you changed it in Step 1
git commit -m "gecko Delegate AiObservabilityUrlValidator to shared UrlValidator (T15)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3: Job webhook URL validation at save time

**Files:** `JobServiceImpl.java` (+ new test).

**Interfaces:**
- Consumes: `UrlValidator.validate(String, Set)`.
- Produces: package-private instance method `void validateWebhookUrl(String url)` on `JobServiceImpl` that throws `UrlValidationException` when SSRF protection is enabled and the URL fails validation; no-op when `ssrfEnabled` is false. New constructor params bind `bytechef.security.ssrf.enabled` and `bytechef.security.ssrf.allowed-hosts`.

- [ ] **Step 1: Write the failing test**

Create `server/libs/atlas/atlas-execution/atlas-execution-service/src/test/java/com/bytechef/atlas/execution/service/JobServiceWebhookUrlValidationTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.atlas.execution.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.commons.util.UrlValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class JobServiceWebhookUrlValidationTest {

    private final JobRepository jobRepository = Mockito.mock(JobRepository.class);

    @Test
    void testRejectsPrivateWebhookUrlWhenEnabled() {
        JobServiceImpl jobService = new JobServiceImpl(jobRepository, true, Set.of());

        assertThatThrownBy(() -> jobService.validateWebhookUrl("http://169.254.169.254/"))
            .isInstanceOf(UrlValidationException.class);
    }

    @Test
    void testAllowsPublicWebhookUrlWhenEnabled() {
        JobServiceImpl jobService = new JobServiceImpl(jobRepository, true, Set.of());

        assertThatCode(() -> jobService.validateWebhookUrl("https://1.1.1.1/hook")).doesNotThrowAnyException();
    }

    @Test
    void testSkipsValidationWhenDisabled() {
        JobServiceImpl jobService = new JobServiceImpl(jobRepository, false, Set.of());

        assertThatCode(() -> jobService.validateWebhookUrl("http://169.254.169.254/")).doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails to compile**

Run: `./gradlew :server:libs:atlas:atlas-execution:atlas-execution-service:compileTestJava`
Expected: FAIL — the 3-arg constructor and `validateWebhookUrl` don't exist.

- [ ] **Step 3: Add config fields, constructor params, and the validation method**

In `JobServiceImpl.java`:

Add imports near the existing ones:
```java
import com.bytechef.commons.util.UrlValidator;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
```
(`java.util.Set` — add only if not already imported.)

Add fields next to `jobRepository`:
```java
    private final boolean ssrfEnabled;
    private final Set<String> ssrfAllowedHosts;
```

Replace the constructor:
```java
    @SuppressFBWarnings("EI2")
    public JobServiceImpl(JobRepository jobRepository) {
        this(jobRepository, true, Set.of());
    }
```
with a primary `@Value` constructor plus keep a convenience constructor for the existing wiring:
```java
    @SuppressFBWarnings("EI2")
    public JobServiceImpl(
        JobRepository jobRepository,
        @Value("${bytechef.security.ssrf.enabled:true}") boolean ssrfEnabled,
        @Value("${bytechef.security.ssrf.allowed-hosts:}") Set<String> ssrfAllowedHosts) {

        this.jobRepository = jobRepository;
        this.ssrfEnabled = ssrfEnabled;
        this.ssrfAllowedHosts = ssrfAllowedHosts;
    }
```
(Keep the rest of the original constructor body — there is none beyond `this.jobRepository = jobRepository;` per the current file. If the current constructor has more statements, preserve them.)

Change the webhook validation loop. Find:
```java
        for (Job.Webhook webhook : jobParametersDTO.getWebhooks()) {
            Assert.notNull(webhook.type(), "must define 'type' on webhook");
            Assert.notNull(webhook.url(), "must define 'url' on webhook");
        }
```
The enclosing method is currently `private static void validate(JobParametersDTO jobParametersDTO, Workflow workflow)`. Change its signature to a non-static instance method `private void validate(JobParametersDTO jobParametersDTO, Workflow workflow)` (so it can read the instance config) and update its single call site (search for `validate(` within `create`) — it is already an unqualified call, so no caller change is needed once it is non-static. Then extend the loop:
```java
        for (Job.Webhook webhook : jobParametersDTO.getWebhooks()) {
            Assert.notNull(webhook.type(), "must define 'type' on webhook");
            Assert.notNull(webhook.url(), "must define 'url' on webhook");

            validateWebhookUrl(webhook.url());
        }
```

Add the package-private method (so the test can call it directly) at the end of the class body, before the closing brace:
```java
    void validateWebhookUrl(String url) {
        if (ssrfEnabled) {
            UrlValidator.validate(url, ssrfAllowedHosts);
        }
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:atlas:atlas-execution:atlas-execution-service:test --tests "*JobServiceWebhookUrlValidationTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Format and commit**

Run: `./gradlew :server:libs:atlas:atlas-execution:atlas-execution-service:spotlessApply`

```bash
git add server/libs/atlas/atlas-execution/atlas-execution-service/src/main/java/com/bytechef/atlas/execution/service/JobServiceImpl.java \
        server/libs/atlas/atlas-execution/atlas-execution-service/src/test/java/com/bytechef/atlas/execution/service/JobServiceWebhookUrlValidationTest.java
git commit -m "gecko Validate outbound job webhook URLs against SSRF at save (T15)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4: Webhook delivery-time re-validation

**Files:** `WebhookTaskStartedApplicationEventListener.java` (+ new test).

**Interfaces:**
- Consumes: `UrlValidator.isValid(String, Set)`.
- Produces: package-private instance method `boolean isAllowedWebhookUrl(String url)` (true when SSRF disabled, else `UrlValidator.isValid`); the listener skips delivery + logs a warning when it returns false. New constructor params bind the same two SSRF properties.

- [ ] **Step 1: Write the failing test**

Create `server/libs/platform/platform-coordinator/src/test/java/com/bytechef/platform/coordinator/event/listener/WebhookTaskStartedApplicationEventListenerTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.execution.service.JobService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class WebhookTaskStartedApplicationEventListenerTest {

    private final JobService jobService = Mockito.mock(JobService.class);

    @Test
    void testPrivateUrlNotAllowedWhenEnabled() {
        WebhookTaskStartedApplicationEventListener listener =
            new WebhookTaskStartedApplicationEventListener(jobService, true, Set.of());

        assertThat(listener.isAllowedWebhookUrl("http://169.254.169.254/")).isFalse();
    }

    @Test
    void testPublicUrlAllowedWhenEnabled() {
        WebhookTaskStartedApplicationEventListener listener =
            new WebhookTaskStartedApplicationEventListener(jobService, true, Set.of());

        assertThat(listener.isAllowedWebhookUrl("https://1.1.1.1/hook")).isTrue();
    }

    @Test
    void testAllUrlsAllowedWhenDisabled() {
        WebhookTaskStartedApplicationEventListener listener =
            new WebhookTaskStartedApplicationEventListener(jobService, false, Set.of());

        assertThat(listener.isAllowedWebhookUrl("http://169.254.169.254/")).isTrue();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails to compile**

Run: `./gradlew :server:libs:platform:platform-coordinator:compileTestJava`
Expected: FAIL — the 3-arg constructor and `isAllowedWebhookUrl` don't exist.

- [ ] **Step 3: Add config + the guard and apply it before delivery**

In `WebhookTaskStartedApplicationEventListener.java`:

Add imports:
```java
import com.bytechef.commons.util.UrlValidator;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
```

Add fields next to `jobService`:
```java
    private final boolean ssrfEnabled;
    private final Set<String> ssrfAllowedHosts;
```

Replace the constructor:
```java
    @SuppressFBWarnings("EI2")
    public WebhookTaskStartedApplicationEventListener(
        JobService jobService,
        @Value("${bytechef.security.ssrf.enabled:true}") boolean ssrfEnabled,
        @Value("${bytechef.security.ssrf.allowed-hosts:}") Set<String> ssrfAllowedHosts) {

        this.jobService = jobService;
        this.ssrfEnabled = ssrfEnabled;
        this.ssrfAllowedHosts = ssrfAllowedHosts;
    }
```

Guard the delivery. Replace:
```java
                    rest.postForObject(webhook.url(), webhookEvent, String.class);

                    if (log.isDebugEnabled()) {
                        log.debug("Webhook url={}, type='{}' called", webhook.url(), webhook.type());
                    }
```
with:
```java
                    if (!isAllowedWebhookUrl(webhook.url())) {
                        log.warn("Skipping webhook delivery to disallowed url={}", webhook.url());

                        continue;
                    }

                    rest.postForObject(webhook.url(), webhookEvent, String.class);

                    if (log.isDebugEnabled()) {
                        log.debug("Webhook url={}, type='{}' called", webhook.url(), webhook.type());
                    }
```

Add the guard method before the class closing brace:
```java
    boolean isAllowedWebhookUrl(String url) {
        return !ssrfEnabled || UrlValidator.isValid(url, ssrfAllowedHosts);
    }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-coordinator:test --tests "*WebhookTaskStartedApplicationEventListenerTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Check the listener's Spring wiring still resolves**

The listener is constructed somewhere in a `@Configuration`/`BeanRegistrar`. Run:
`grep -rn "new WebhookTaskStartedApplicationEventListener" server --include="*.java" | grep -v build`
If it is constructed manually (not autowired), update that call site to pass the two SSRF args (read them there via `@Value` or `ApplicationProperties`, whichever that config class already uses). If it is a `@Component`/autowired bean, Spring injects the `@Value` params automatically — no change needed. Compile the module to confirm: `./gradlew :server:libs:platform:platform-coordinator:compileJava`.

- [ ] **Step 6: Format and commit**

Run: `./gradlew :server:libs:platform:platform-coordinator:spotlessApply`

```bash
git add server/libs/platform/platform-coordinator/src/main/java/com/bytechef/platform/coordinator/event/listener/WebhookTaskStartedApplicationEventListener.java \
        server/libs/platform/platform-coordinator/src/test/java/com/bytechef/platform/coordinator/event/listener/WebhookTaskStartedApplicationEventListenerTest.java
# also add the configuration class if Step 5 required a change
git commit -m "gecko Re-validate outbound webhook URLs against SSRF at delivery (T15)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5: Open-redirect private-target rejection

**Files:** `RedirectValidator.java` (+ new test).

**Interfaces:**
- Consumes: `UrlValidator.isValid(String, Set)`.
- Produces: `RedirectValidator.isValidRedirect(...)` unchanged signature; now also returns false when an absolute, non-same-host target resolves to a private/loopback address.

- [ ] **Step 1: Write the failing test**

Create `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-api/src/test/java/com/bytechef/platform/webhook/rest/validator/RedirectValidatorTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.webhook.rest.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RedirectValidatorTest {

    @Test
    void testRelativePathAllowed() {
        assertThat(RedirectValidator.isValidRedirect("/dashboard", "app.bytechef.io")).isTrue();
    }

    @Test
    void testAllowlistedPrivateLiteralTargetRejected() {
        assertThat(
            RedirectValidator.isValidRedirect("http://10.0.0.1/x", "app.bytechef.io", Set.of("10.0.0.1")))
                .isFalse();
    }

    @Test
    void testAllowlistedPublicTargetAllowed() {
        assertThat(RedirectValidator.isValidRedirect("https://1.1.1.1/x", "app.bytechef.io", Set.of("1.1.1.1")))
            .isTrue();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-api:test --tests "*RedirectValidatorTest"`
Expected: FAIL — `testAllowlistedPrivateLiteralTargetRejected` returns true today (allowlist match short-circuits without an address check).

- [ ] **Step 3: Add the private-address check to the allowlist branch**

In `RedirectValidator.java`, add the import:
```java
import com.bytechef.commons.util.UrlValidator;
```

Replace the whitelist branch:
```java
            // Check against whitelist if provided
            if (allowedDomains != null && !allowedDomains.isEmpty()) {
                for (String allowedDomain : allowedDomains) {
                    if (hostMatchesDomain(host, allowedDomain)) {
                        return true;
                    }
                }
            }
```
with:
```java
            // Check against whitelist if provided
            if (allowedDomains != null && !allowedDomains.isEmpty()) {
                for (String allowedDomain : allowedDomains) {
                    if (hostMatchesDomain(host, allowedDomain)) {
                        // Defense-in-depth: even an allowlisted domain must not resolve to a private/loopback address.
                        return UrlValidator.isValid(redirectUrl, Set.of());
                    }
                }
            }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-api:test --tests "*RedirectValidatorTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Format and commit**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-api:spotlessApply`

```bash
git add server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-api/src/main/java/com/bytechef/platform/webhook/rest/validator/RedirectValidator.java \
        server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-api/src/test/java/com/bytechef/platform/webhook/rest/validator/RedirectValidatorTest.java
git commit -m "gecko Reject private/loopback redirect targets in RedirectValidator (T15)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6: EE documentation-fetch validation

**Files:** `ApiConnectorAiServiceImpl.java` (EE).

**Interfaces:**
- Consumes: `UrlValidator.validate(String, Set)`.
- Produces: `fetchDocumentation` validates the URL before scraping. SSRF config bound via `@Value` constructor params.

- [ ] **Step 1: Read the current constructor and fields**

Run: `sed -n '55,75p' server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-service/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiServiceImpl.java`
Note the exact constructor parameter list and field assignments to extend them in Step 2.

- [ ] **Step 2: Add SSRF config + validate before scrape**

In `ApiConnectorAiServiceImpl.java`:

Add imports:
```java
import com.bytechef.commons.util.UrlValidator;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
```

Add fields:
```java
    private final boolean ssrfEnabled;
    private final Set<String> ssrfAllowedHosts;
```

Extend the constructor parameter list with the two `@Value` params (append them to the existing parameters) and assign the fields:
```java
        @Value("${bytechef.security.ssrf.enabled:true}") boolean ssrfEnabled,
        @Value("${bytechef.security.ssrf.allowed-hosts:}") Set<String> ssrfAllowedHosts
```
```java
        this.ssrfEnabled = ssrfEnabled;
        this.ssrfAllowedHosts = ssrfAllowedHosts;
```

Guard `fetchDocumentation(String documentationUrl)`. Replace:
```java
    private String fetchDocumentation(String documentationUrl) {
        WebScrapeService.ScrapeResult result = webScrapeService.scrape(documentationUrl);
```
with:
```java
    private String fetchDocumentation(String documentationUrl) {
        if (ssrfEnabled) {
            UrlValidator.validate(documentationUrl, ssrfAllowedHosts);
        }

        WebScrapeService.ScrapeResult result = webScrapeService.scrape(documentationUrl);
```
Also guard the crawl overload `fetchDocumentation(String documentationUrl, int maxPages)` — add the same `if (ssrfEnabled) { UrlValidator.validate(documentationUrl, ssrfAllowedHosts); }` at its top, before `webScrapeService.crawl(...)` (and before it delegates to the single-arg overload, to validate even when `maxPages <= 1`). Place the guard as the first statement of the method.

- [ ] **Step 3: Compile the EE module**

Run: `./gradlew :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-service:compileJava`
Expected: BUILD SUCCESSFUL. (No new unit test: this method is private and reached only through AI-generation flows; the validator itself is covered by `UrlValidatorTest`. Note this gap explicitly in the commit body.)

- [ ] **Step 4: Format and commit**

Run: `./gradlew :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-service:spotlessApply`

```bash
git add server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-service/src/main/java/com/bytechef/ee/platform/apiconnector/configuration/service/ApiConnectorAiServiceImpl.java
git commit -m "gecko Validate documentation-fetch URL against SSRF (T15)

No dedicated unit test: fetchDocumentation is private and only reachable via
AI generation flows; the SSRF logic is covered by UrlValidatorTest.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 7: Close out the tracker

- [ ] **Step 1: Run check on all touched modules**

Run:
```bash
./gradlew \
  :server:libs:core:commons:commons-util:check \
  :server:libs:atlas:atlas-execution:atlas-execution-service:check \
  :server:libs:platform:platform-coordinator:check \
  :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-api:check \
  :server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api:check \
  :server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-service:check
```
Expected: BUILD SUCCESSFUL. Fix any checkstyle/PMD/SpotBugs findings the new code introduces. (If an unrelated pre-existing test in one of these modules is already broken on the branch, confirm via `git stash` or by checking it fails without your changes, and note it rather than fixing out-of-scope code.)

- [ ] **Step 2: Mark T15 done in the tracker**

In `gecko-remediation-tasks.md`, change `- [ ] **T15.` to `- [x] **T15.` and append a short note:
> **Done** (spec/plan `docs/superpowers/{specs,plans}/2026-06-21-ssrf-defenses*`): shared `commons-util` `UrlValidator` (ported from EE `AiObservabilityUrlValidator`, which now delegates to it) applied to outbound job/task webhooks (save + delivery), EE documentation fetch, and the open-redirect validator. Global `bytechef.security.ssrf.{enabled,allowed-hosts}` escape-hatch (default on, empty allowlist; hostname-match only, CIDR deferred). Deferred by design: central `HttpClientExecutor` and infra connections (JDBC/RabbitMQ/Cassandra) — private hosts are legitimate there. DNS-rebinding residual documented (per-attempt validation).

- [ ] **Step 3: Commit**

```bash
git add gecko-remediation-tasks.md
git commit -m "gecko Mark T15 SSRF defenses done

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- Shared CE primitive + EE delegation → Tasks 1, 2 ✓
- Config (`enabled` + `allowed-hosts`) → bound at each Spring call site (Tasks 3, 4, 6); RedirectValidator unconditional (Task 5) ✓
- Outbound job/task webhooks (save + delivery) → Tasks 3, 4 ✓
- EE documentation fetch → Task 6 ✓
- Open-redirect private-target rejection → Task 5 ✓
- Out-of-scope items (central executor, infra connections, DNS-rebind) → recorded in tracker note (Task 7) ✓

**Placeholder scan:** No TBD/TODO; every code step shows full code. The Step 5 wiring check in Task 4 and the Step 1 constructor read in Task 6 are explicit "inspect then adjust" instructions (the exact constructor arglist must be read from the file at implementation time), not silent placeholders.

**Type consistency:** `UrlValidator.validate(String, Set<String>)` / `isValid(String, Set<String>)` and `UrlValidationException` are used identically across Tasks 1–6. Constructor SSRF params are `(boolean ssrfEnabled, Set<String> ssrfAllowedHosts)` everywhere. Property keys `bytechef.security.ssrf.enabled` / `bytechef.security.ssrf.allowed-hosts` are identical at all `@Value` sites.

**Scope trim noted:** `allowed-hosts` is hostname-match only (CIDR deferred) — called out in Global Constraints and the tracker note, consistent with the spec's "hostnames/CIDRs" wording flagged as a documented reduction.
