# Voice Support Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the existing "mic button in composer triggers full realtime voice" shape with two distinct features — push-to-talk transcribe in every composer (REST `/transcribe`), and full realtime voice mode only in workflow-test panel and the deployed SDK widget (assistant-ui `RealtimeVoiceAdapter` wrapping the existing `BrowserVoiceSession`). Delete the AI Hub realtime backend.

**Architecture:** Three layers. (1) Backend STT: new `SttProvider` SPI in `platform-ai-stt-api` with OpenAI/ElevenLabs/Deepgram REST impls, consumed by a shared `TranscribeService` and three thin transcribe controllers. (2) Push-to-talk frontend: a `usePushToTalk` hook using `MediaRecorder` that uploads to `/transcribe` then injects text via the assistant-ui composer runtime — wired into all three composers. (3) Realtime voice mode: a `ByteChefRealtimeVoiceAdapter` implementing assistant-ui's `RealtimeVoiceAdapter` interface and wrapping the existing `BrowserVoiceSession` transport. A new `<VoiceModeLayout>` (built from `@assistant-ui/react`'s `VoiceOrb` + connect/disconnect buttons) replaces `<Thread>` when the workflow trigger is `browser/v1/voiceSession`. AI Hub realtime stack (Path B) is deleted.

**Tech Stack:** Java 25, Spring Boot 4.0.5, Spring `RestClient`, JUnit 5, Mockito, Testcontainers. React 19, TypeScript 5.9, Vitest 4, `@assistant-ui/react`. Liquibase for the schema migration.

---

## Reference

- **Spec:** [docs/superpowers/specs/2026-05-19-voice-support-redesign-design.md](../specs/2026-05-19-voice-support-redesign-design.md)
- **Commit message convention:** Server changes: `194800 <description>`. Client changes: `194800 client - <description>`. Co-authored-by trailer required.
- **Ticket:** 194800 (epic-hypatia)
- **Server formatter:** Run `./gradlew spotlessApply` before every commit that touches Java.
- **Client formatter:** Run `cd client && npm run format` before every commit that touches client code.
- **Test naming:** unit `*Test`, integration `*IntTest`, camelCase method names, no underscores.
- **Default mic recorder mime type:** `audio/webm;codecs=opus` (Chromium/Firefox), `audio/mp4` fallback for Safari, detected via `MediaRecorder.isTypeSupported`.
- **STT provider key (default):** `OPENAI_GPT_4O_MINI_TRANSCRIBE`. Configured via Spring property `bytechef.ai.stt.provider`.
- **assistant-ui voice docs:** https://www.assistant-ui.com/docs/ui/voice — `RealtimeVoiceAdapter`, `VoiceOrb`, `useVoiceState`, `useVoiceControls`, `useChatRuntime({adapters: {voice: ...}})`.

---

## Phase 1 — STT Backend

Tasks 1–11. Ships working `/transcribe` endpoints behind the configured provider. Push-to-talk frontend (Phase 2) consumes these.

### Task 1: Create `platform-ai-stt-api` module skeleton

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-stt-api/build.gradle.kts`
- Modify: `settings.gradle.kts` (add module include)

- [ ] **Step 1: Create the build file**

`server/libs/platform/platform-ai/platform-ai-stt-api/build.gradle.kts`:
```kotlin
dependencies {
}
```

- [ ] **Step 2: Register the module in `settings.gradle.kts`**

Find the existing line `include("server:libs:platform:platform-ai:platform-ai-api")` and add immediately after:
```kotlin
include("server:libs:platform:platform-ai:platform-ai-stt-api")
```

- [ ] **Step 3: Verify Gradle can resolve the module**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-stt-api:tasks --no-daemon`
Expected: exits 0; lists Gradle tasks for the new module.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-stt-api settings.gradle.kts
git commit -m "$(cat <<'EOF'
194800 Add platform-ai-stt-api module skeleton

Empty module placeholder for the SttProvider SPI added in the next task.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Define `SttProvider` interface

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-stt-api/src/main/java/com/bytechef/platform/ai/stt/SttProvider.java`

- [ ] **Step 1: Write the interface**

```java
/*
 * Copyright 2025-present ByteChef Inc.
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

package com.bytechef.platform.ai.stt;

import java.io.InputStream;
import java.util.Map;

public interface SttProvider {

    String getKey();

    TranscriptResult transcribe(TranscribeRequest request);

    record TranscribeRequest(
        InputStream audio,
        String mimeType,
        String locale,
        Map<String, Object> connectionParameters) {
    }

    record TranscriptResult(
        String text,
        long durationMs,
        String detectedLocale) {
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-stt-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-stt-api
git commit -m "$(cat <<'EOF'
194800 Add SttProvider SPI

SttProvider exposes a single transcribe(TranscribeRequest) method
returning TranscriptResult. Impls live in separate modules and are
resolved by Spring bean map keyed by getKey().

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Create `platform-ai-stt-service` module skeleton

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-stt-service/build.gradle.kts`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create the build file**

`server/libs/platform/platform-ai/platform-ai-stt-service/build.gradle.kts`:
```kotlin
dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-stt-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
}
```

- [ ] **Step 2: Register module**

Add to `settings.gradle.kts` after the `platform-ai-stt-api` include:
```kotlin
include("server:libs:platform:platform-ai:platform-ai-stt-service")
```

- [ ] **Step 3: Verify**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-stt-service:tasks --no-daemon`
Expected: exits 0.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-stt-service settings.gradle.kts
git commit -m "$(cat <<'EOF'
194800 Add platform-ai-stt-service module skeleton

Will host TranscribeService — the shared validation + provider-routing
layer consumed by the three transcribe controllers.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: TranscribeService with failing test

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-stt-service/src/main/java/com/bytechef/platform/ai/stt/service/TranscribeService.java`
- Create: `server/libs/platform/platform-ai/platform-ai-stt-service/src/test/java/com/bytechef/platform/ai/stt/service/TranscribeServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
/*
 * Copyright 2025-present ByteChef Inc.
 * Licensed under the Apache License, Version 2.0
 */
package com.bytechef.platform.ai.stt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.stt.SttProvider;
import com.bytechef.platform.ai.stt.SttProvider.TranscribeRequest;
import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import java.io.ByteArrayInputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TranscribeServiceTest {

    @Test
    void testTranscribeDelegatesToConfiguredProvider() {
        SttProvider provider = mock(SttProvider.class);

        when(provider.getKey()).thenReturn("FAKE");
        when(provider.transcribe(any(TranscribeRequest.class)))
            .thenReturn(new TranscriptResult("hello world", 1200L, "en-US"));

        TranscribeService transcribeService = new TranscribeService(
            Map.of("FAKE", provider), "FAKE");

        TranscriptResult result = transcribeService.transcribe(
            new ByteArrayInputStream(new byte[] {1, 2, 3}), "audio/webm", "en", Map.of());

        assertThat(result.text()).isEqualTo("hello world");
        assertThat(result.durationMs()).isEqualTo(1200L);
    }

    @Test
    void testTranscribeRejectsUnsupportedMimeType() {
        TranscribeService transcribeService = new TranscribeService(Map.of(), "FAKE");

        assertThatThrownBy(() -> transcribeService.transcribe(
            new ByteArrayInputStream(new byte[] {1}), "application/json", null, Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported mime type");
    }

    @Test
    void testTranscribeFailsWhenProviderMissing() {
        TranscribeService transcribeService = new TranscribeService(Map.of(), "MISSING");

        assertThatThrownBy(() -> transcribeService.transcribe(
            new ByteArrayInputStream(new byte[] {1}), "audio/webm", null, Map.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MISSING");
    }
}
```

Add test-only dependencies to `build.gradle.kts`:
```kotlin
dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-stt-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-stt-service:test --tests "com.bytechef.platform.ai.stt.service.TranscribeServiceTest"`
Expected: FAIL — `TranscribeService` does not exist.

- [ ] **Step 3: Implement `TranscribeService`**

```java
/*
 * Copyright 2025-present ByteChef Inc.
 * Licensed under the Apache License, Version 2.0
 */
package com.bytechef.platform.ai.stt.service;

import com.bytechef.platform.ai.stt.SttProvider;
import com.bytechef.platform.ai.stt.SttProvider.TranscribeRequest;
import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TranscribeService {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
        "audio/webm", "audio/mp4", "audio/wav", "audio/mpeg", "audio/ogg");

    private final Map<String, SttProvider> providers;

    private final String providerKey;

    public TranscribeService(
        Map<String, SttProvider> providers,
        @Value("${bytechef.ai.stt.provider:OPENAI_GPT_4O_MINI_TRANSCRIBE}") String providerKey) {

        this.providers = providers;
        this.providerKey = providerKey;
    }

    public TranscriptResult transcribe(
        InputStream audio, String mimeType, String locale, Map<String, Object> connectionParameters) {

        if (!SUPPORTED_MIME_TYPES.contains(stripParameters(mimeType))) {
            throw new IllegalArgumentException("Unsupported mime type: " + mimeType);
        }

        SttProvider provider = providers.values()
            .stream()
            .filter(sttProvider -> providerKey.equals(sttProvider.getKey()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No SttProvider registered for key: " + providerKey));

        return provider.transcribe(new TranscribeRequest(audio, mimeType, locale, connectionParameters));
    }

    private static String stripParameters(String mimeType) {
        int semicolon = mimeType.indexOf(';');

        return semicolon < 0 ? mimeType : mimeType.substring(0, semicolon).trim();
    }
}
```

- [ ] **Step 4: Run test to confirm it passes**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-stt-service:test --tests "com.bytechef.platform.ai.stt.service.TranscribeServiceTest"`
Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-stt-service
git commit -m "$(cat <<'EOF'
194800 Add TranscribeService

Validates mime type, resolves SttProvider by configured key, delegates
to provider.transcribe(). Default provider key configured via
bytechef.ai.stt.provider.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: `platform-ai-stt-openai` module + OpenAI provider with failing test

**Files:**
- Create: `server/libs/platform/platform-ai/platform-ai-stt-openai/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `server/libs/platform/platform-ai/platform-ai-stt-openai/src/main/java/com/bytechef/platform/ai/stt/openai/OpenAiSttProvider.java`
- Create: `server/libs/platform/platform-ai/platform-ai-stt-openai/src/test/java/com/bytechef/platform/ai/stt/openai/OpenAiSttProviderTest.java`

- [ ] **Step 1: Create the build file**

```kotlin
dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-stt-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-web")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.squareup.okhttp3:mockwebserver")
}
```

- [ ] **Step 2: Register module**

`settings.gradle.kts`:
```kotlin
include("server:libs:platform:platform-ai:platform-ai-stt-openai")
```

- [ ] **Step 3: Write the failing test**

```java
/*
 * Copyright 2025-present ByteChef Inc.
 * Licensed under the Apache License, Version 2.0
 */
package com.bytechef.platform.ai.stt.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.stt.SttProvider.TranscribeRequest;
import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import java.io.ByteArrayInputStream;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiSttProviderTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();

        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testTranscribePostsMultipartAndParsesResponse() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"text\":\"hello world\",\"language\":\"english\",\"duration\":1.234}"));

        OpenAiSttProvider provider = new OpenAiSttProvider(
            RestClient.builder().baseUrl(mockWebServer.url("/").toString()).build());

        TranscriptResult result = provider.transcribe(new TranscribeRequest(
            new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
            "audio/webm", "en",
            Map.of("apiKey", "sk-test")));

        assertThat(result.text()).isEqualTo("hello world");
        assertThat(result.durationMs()).isEqualTo(1234L);
        assertThat(result.detectedLocale()).isEqualTo("english");

        RecordedRequest recorded = mockWebServer.takeRequest();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/v1/audio/transcriptions");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-test");
        assertThat(recorded.getHeader("Content-Type")).startsWith("multipart/form-data");
    }

    @Test
    void testGetKeyReturnsExpected() {
        OpenAiSttProvider provider = new OpenAiSttProvider(RestClient.builder().build());

        assertThat(provider.getKey()).isEqualTo("OPENAI_GPT_4O_MINI_TRANSCRIBE");
    }
}
```

- [ ] **Step 4: Run test to confirm it fails**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-stt-openai:test`
Expected: FAIL — `OpenAiSttProvider` does not exist.

- [ ] **Step 5: Implement `OpenAiSttProvider`**

```java
/*
 * Copyright 2025-present ByteChef Inc.
 * Licensed under the Apache License, Version 2.0
 */
package com.bytechef.platform.ai.stt.openai;

import com.bytechef.platform.ai.stt.SttProvider;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiSttProvider implements SttProvider {

    public static final String KEY = "OPENAI_GPT_4O_MINI_TRANSCRIBE";

    private static final String MODEL = "gpt-4o-mini-transcribe";

    private final RestClient restClient;

    public OpenAiSttProvider(RestClient openAiSttRestClient) {
        this.restClient = openAiSttRestClient;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public TranscriptResult transcribe(TranscribeRequest request) {
        String apiKey = (String) request.connectionParameters().getOrDefault("apiKey", "");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new InputStreamResource(request.audio()) {
            @Override
            public String getFilename() {
                return "audio." + extension(request.mimeType());
            }

            @Override
            public long contentLength() throws IOException {
                return -1;
            }
        });
        body.add("model", MODEL);

        if (request.locale() != null && !request.locale().isBlank()) {
            body.add("language", request.locale());
        }

        OpenAiResponse response = restClient.post()
            .uri("/v1/audio/transcriptions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(OpenAiResponse.class);

        if (response == null) {
            throw new IllegalStateException("Empty response from OpenAI STT");
        }

        long durationMs = response.duration == null ? 0L : Math.round(response.duration * 1000);

        return new TranscriptResult(response.text, durationMs, response.language);
    }

    private static String extension(String mimeType) {
        return switch (mimeType) {
            case "audio/webm" -> "webm";
            case "audio/mp4" -> "mp4";
            case "audio/wav" -> "wav";
            case "audio/mpeg" -> "mp3";
            case "audio/ogg" -> "ogg";
            default -> "bin";
        };
    }

    @SuppressWarnings("PMD")
    private static final class OpenAiResponse {
        public String text;
        public String language;
        public Double duration;
    }
}
```

Add an autoconfiguration so the `RestClient` is wired with the OpenAI base URL:

`server/libs/platform/platform-ai/platform-ai-stt-openai/src/main/java/com/bytechef/platform/ai/stt/openai/OpenAiSttProviderConfiguration.java`:
```java
/*
 * Copyright 2025-present ByteChef Inc.
 * Licensed under the Apache License, Version 2.0
 */
package com.bytechef.platform.ai.stt.openai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class OpenAiSttProviderConfiguration {

    @Bean
    RestClient openAiSttRestClient(
        @Value("${bytechef.ai.stt.openai.base-url:https://api.openai.com}") String baseUrl) {

        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}
```

Add the autoconfig file:
`server/libs/platform/platform-ai/platform-ai-stt-openai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
```
com.bytechef.platform.ai.stt.openai.OpenAiSttProviderConfiguration
```

- [ ] **Step 6: Run test to confirm it passes**

Run: `./gradlew :server:libs:platform:platform-ai:platform-ai-stt-openai:test`
Expected: PASS, 2 tests.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-ai/platform-ai-stt-openai settings.gradle.kts
git commit -m "$(cat <<'EOF'
194800 Add OpenAiSttProvider (default STT)

Multipart upload to OpenAI's /v1/audio/transcriptions using
gpt-4o-mini-transcribe. Lives in community so CE deployments work
without EE modules. Base URL is overridable via
bytechef.ai.stt.openai.base-url for test isolation.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: `platform-ai-stt-elevenlabs` module (EE) + provider

**Files:**
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-elevenlabs/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-elevenlabs/src/main/java/com/bytechef/ee/platform/ai/stt/elevenlabs/ElevenLabsSttProvider.java`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-elevenlabs/src/main/java/com/bytechef/ee/platform/ai/stt/elevenlabs/ElevenLabsSttProviderConfiguration.java`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-elevenlabs/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-elevenlabs/src/test/java/com/bytechef/ee/platform/ai/stt/elevenlabs/ElevenLabsSttProviderTest.java`

- [ ] **Step 1: Build file**

```kotlin
dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-stt-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-web")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.squareup.okhttp3:mockwebserver")
}
```

- [ ] **Step 2: Register module**

`settings.gradle.kts`:
```kotlin
include("server:ee:libs:platform:platform-ai:platform-ai-stt-elevenlabs")
```

- [ ] **Step 3: Write failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.ai.stt.elevenlabs;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.stt.SttProvider.TranscribeRequest;
import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import java.io.ByteArrayInputStream;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 */
class ElevenLabsSttProviderTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();

        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testTranscribePostsMultipartAndParses() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"text\":\"voice text\",\"language_code\":\"en\"}"));

        ElevenLabsSttProvider provider = new ElevenLabsSttProvider(
            RestClient.builder().baseUrl(mockWebServer.url("/").toString()).build());

        TranscriptResult result = provider.transcribe(new TranscribeRequest(
            new ByteArrayInputStream(new byte[] {1, 2}),
            "audio/webm", "en",
            Map.of("apiKey", "el-test")));

        assertThat(result.text()).isEqualTo("voice text");
        assertThat(result.detectedLocale()).isEqualTo("en");

        RecordedRequest recorded = mockWebServer.takeRequest();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/v1/speech-to-text");
        assertThat(recorded.getHeader("xi-api-key")).isEqualTo("el-test");
    }

    @Test
    void testGetKey() {
        assertThat(new ElevenLabsSttProvider(RestClient.builder().build()).getKey())
            .isEqualTo("ELEVENLABS_SCRIBE");
    }
}
```

- [ ] **Step 4: Run test to confirm it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-stt-elevenlabs:test`
Expected: FAIL — `ElevenLabsSttProvider` does not exist.

- [ ] **Step 5: Implement provider**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.ai.stt.elevenlabs;

import com.bytechef.platform.ai.stt.SttProvider;
import java.io.IOException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 */
@Component
public class ElevenLabsSttProvider implements SttProvider {

    public static final String KEY = "ELEVENLABS_SCRIBE";

    private static final String MODEL = "scribe_v1";

    private final RestClient restClient;

    public ElevenLabsSttProvider(RestClient elevenLabsSttRestClient) {
        this.restClient = elevenLabsSttRestClient;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public TranscriptResult transcribe(TranscribeRequest request) {
        String apiKey = (String) request.connectionParameters().getOrDefault("apiKey", "");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new InputStreamResource(request.audio()) {
            @Override
            public String getFilename() {
                return "audio." + extension(request.mimeType());
            }

            @Override
            public long contentLength() throws IOException {
                return -1;
            }
        });
        body.add("model_id", MODEL);

        ElevenLabsResponse response = restClient.post()
            .uri("/v1/speech-to-text")
            .header("xi-api-key", apiKey)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(ElevenLabsResponse.class);

        if (response == null) {
            throw new IllegalStateException("Empty response from ElevenLabs STT");
        }

        return new TranscriptResult(response.text, 0L, response.languageCode);
    }

    private static String extension(String mimeType) {
        return switch (mimeType) {
            case "audio/webm" -> "webm";
            case "audio/mp4" -> "mp4";
            case "audio/wav" -> "wav";
            case "audio/mpeg" -> "mp3";
            case "audio/ogg" -> "ogg";
            default -> "bin";
        };
    }

    @SuppressWarnings("PMD")
    private static final class ElevenLabsResponse {
        public String text;
        public String languageCode;
    }
}
```

Configuration class:
```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.ai.stt.elevenlabs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 */
@AutoConfiguration
public class ElevenLabsSttProviderConfiguration {

    @Bean
    RestClient elevenLabsSttRestClient(
        @Value("${bytechef.ai.stt.elevenlabs.base-url:https://api.elevenlabs.io}") String baseUrl) {

        return RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}
```

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
```
com.bytechef.ee.platform.ai.stt.elevenlabs.ElevenLabsSttProviderConfiguration
```

- [ ] **Step 6: Run test to confirm it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-stt-elevenlabs:test`
Expected: PASS, 2 tests.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-stt-elevenlabs settings.gradle.kts
git commit -m "$(cat <<'EOF'
194800 Add ElevenLabsSttProvider (EE)

ElevenLabs Scribe REST one-shot transcription. Key ELEVENLABS_SCRIBE.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: `platform-ai-stt-deepgram` module (EE) + provider

**Files:**
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-deepgram/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-deepgram/src/main/java/com/bytechef/ee/platform/ai/stt/deepgram/DeepgramSttProvider.java`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-deepgram/src/main/java/com/bytechef/ee/platform/ai/stt/deepgram/DeepgramSttProviderConfiguration.java`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-deepgram/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-stt-deepgram/src/test/java/com/bytechef/ee/platform/ai/stt/deepgram/DeepgramSttProviderTest.java`

- [ ] **Step 1: Build file**

```kotlin
dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-stt-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-web")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.squareup.okhttp3:mockwebserver")
}
```

- [ ] **Step 2: Register**

```kotlin
include("server:ee:libs:platform:platform-ai:platform-ai-stt-deepgram")
```

- [ ] **Step 3: Write failing test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.ai.stt.deepgram;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.stt.SttProvider.TranscribeRequest;
import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import java.io.ByteArrayInputStream;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 */
class DeepgramSttProviderTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();

        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testTranscribePostsBinaryAndParses() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"results":{"channels":[{"alternatives":[{"transcript":"deep voice"}]}]},
                 "metadata":{"duration":2.5}}"""));

        DeepgramSttProvider provider = new DeepgramSttProvider(
            RestClient.builder().baseUrl(mockWebServer.url("/").toString()).build());

        TranscriptResult result = provider.transcribe(new TranscribeRequest(
            new ByteArrayInputStream(new byte[] {1, 2}),
            "audio/webm", "en",
            Map.of("apiKey", "dg-test")));

        assertThat(result.text()).isEqualTo("deep voice");
        assertThat(result.durationMs()).isEqualTo(2500L);

        RecordedRequest recorded = mockWebServer.takeRequest();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).contains("/v1/listen");
        assertThat(recorded.getPath()).contains("model=nova-3");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Token dg-test");
    }

    @Test
    void testGetKey() {
        assertThat(new DeepgramSttProvider(RestClient.builder().build()).getKey())
            .isEqualTo("DEEPGRAM_NOVA_3");
    }
}
```

- [ ] **Step 4: Run test to confirm it fails**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-stt-deepgram:test`
Expected: FAIL.

- [ ] **Step 5: Implement provider**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */
package com.bytechef.ee.platform.ai.stt.deepgram;

import com.bytechef.platform.ai.stt.SttProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @version ee
 */
@Component
public class DeepgramSttProvider implements SttProvider {

    public static final String KEY = "DEEPGRAM_NOVA_3";

    private final RestClient restClient;

    public DeepgramSttProvider(RestClient deepgramSttRestClient) {
        this.restClient = deepgramSttRestClient;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public TranscriptResult transcribe(TranscribeRequest request) {
        String apiKey = (String) request.connectionParameters().getOrDefault("apiKey", "");

        String uri = UriComponentsBuilder.fromPath("/v1/listen")
            .queryParam("model", "nova-3")
            .queryParam("language", request.locale() == null ? "en" : request.locale())
            .queryParam("smart_format", "true")
            .build()
            .toUriString();

        DeepgramResponse response = restClient.post()
            .uri(uri)
            .header("Authorization", "Token " + apiKey)
            .contentType(MediaType.parseMediaType(request.mimeType()))
            .body(new InputStreamResource(request.audio()))
            .retrieve()
            .body(DeepgramResponse.class);

        if (response == null || response.results == null || response.results.channels == null
            || response.results.channels.isEmpty()
            || response.results.channels.get(0).alternatives == null
            || response.results.channels.get(0).alternatives.isEmpty()) {

            throw new IllegalStateException("Empty response from Deepgram STT");
        }

        String text = response.results.channels.get(0).alternatives.get(0).transcript;
        long durationMs = response.metadata == null || response.metadata.duration == null
            ? 0L : Math.round(response.metadata.duration * 1000);

        return new TranscriptResult(text == null ? "" : text, durationMs, request.locale());
    }

    @SuppressWarnings("PMD")
    private static final class DeepgramResponse {
        public Results results;
        public Metadata metadata;

        static final class Results {
            public java.util.List<Channel> channels;
        }

        static final class Channel {
            public java.util.List<Alternative> alternatives;
        }

        static final class Alternative {
            public String transcript;
        }

        static final class Metadata {
            public Double duration;
        }
    }
}
```

Configuration + autoconfig file analogous to the previous two providers (base URL property `bytechef.ai.stt.deepgram.base-url` defaults to `https://api.deepgram.com`).

- [ ] **Step 6: Run test to confirm it passes**

Run: `./gradlew :server:ee:libs:platform:platform-ai:platform-ai-stt-deepgram:test`
Expected: PASS, 2 tests.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-ai/platform-ai-stt-deepgram settings.gradle.kts
git commit -m "$(cat <<'EOF'
194800 Add DeepgramSttProvider (EE)

Deepgram Nova-3 REST one-shot transcription via /v1/listen. Posts raw
audio body (no multipart) per Deepgram's API. Key DEEPGRAM_NOVA_3.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: Configure default STT provider in server-app

**Files:**
- Modify: `server/apps/server-app/src/main/resources/application.yml` (or the equivalent main config file)
- Modify: server-app `build.gradle.kts` to add the STT modules as runtime deps so beans are discovered.

- [ ] **Step 1: Add Spring property**

Add under `bytechef:` (create the nested key if absent):
```yaml
bytechef:
  ai:
    stt:
      provider: OPENAI_GPT_4O_MINI_TRANSCRIBE
      openai:
        # set via env in deployments
        # base-url default is fine
```

- [ ] **Step 2: Add STT modules to server-app dependencies**

In `server/apps/server-app/build.gradle.kts`, add:
```kotlin
runtimeOnly(project(":server:libs:platform:platform-ai:platform-ai-stt-service"))
runtimeOnly(project(":server:libs:platform:platform-ai:platform-ai-stt-openai"))
```

- [ ] **Step 3: Verify server-app compiles**

Run: `./gradlew :server:apps:server-app:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add server/apps/server-app
git commit -m "$(cat <<'EOF'
194800 Wire STT provider config into server-app

OpenAI provider available in community server-app at runtime; default
provider key set via bytechef.ai.stt.provider.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 9: Add `/transcribe` route to `WebhookTriggerController`

**Files:**
- Modify: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/main/java/com/bytechef/platform/webhook/web/rest/WebhookTriggerController.java`
- Modify: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/build.gradle.kts` (add `platform-ai-stt-service` dep)
- Create: `server/libs/platform/platform-webhook/platform-webhook-rest/platform-webhook-rest-impl/src/test/java/com/bytechef/platform/webhook/web/rest/WebhookTriggerControllerTranscribeTest.java`

- [ ] **Step 1: Add dependency**

In the controller module's `build.gradle.kts`:
```kotlin
implementation(project(":server:libs:platform:platform-ai:platform-ai-stt-service"))
```

- [ ] **Step 2: Write the failing test**

Read the existing controller to see the test pattern used in the module (`Read` tool on the controller and look for existing test files alongside). Then create:

```java
/*
 * Copyright 2025-present ByteChef Inc.
 * Licensed under the Apache License, Version 2.0
 */
package com.bytechef.platform.webhook.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import com.bytechef.platform.ai.stt.service.TranscribeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
class WebhookTriggerControllerTranscribeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranscribeService transcribeService;

    @Test
    void testTranscribeReturnsText() throws Exception {
        when(transcribeService.transcribe(any(), eq("audio/webm"), eq("en"), any()))
            .thenReturn(new TranscriptResult("hello", 1000L, "en"));

        MockMultipartFile audio = new MockMultipartFile(
            "audio", "clip.webm", "audio/webm", new byte[] {1, 2, 3, 4});

        mockMvc.perform(multipart("/webhooks/test-webhook-id/transcribe")
            .file(audio)
            .param("locale", "en"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("hello"))
            .andExpect(jsonPath("$.durationMs").value(1000));
    }
}
```

(If the existing test infrastructure in this module uses a different harness, mirror it rather than `@SpringBootTest`.)

- [ ] **Step 3: Run test to confirm it fails**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:test --tests "*WebhookTriggerControllerTranscribeTest*"`
Expected: FAIL — 404 (no route).

- [ ] **Step 4: Add the route to `WebhookTriggerController`**

Add this method (and the `TranscribeService` constructor dependency) to the existing controller class. Keep the existing methods intact.

```java
@PostMapping("/webhooks/{webhookId}/transcribe")
public ResponseEntity<TranscribeResponse> transcribe(
    @PathVariable String webhookId,
    @RequestPart("audio") MultipartFile audio,
    @RequestParam(name = "locale", required = false) String locale) throws IOException {

    validateWebhookExists(webhookId);

    if (audio.getSize() > 25 * 1024 * 1024) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }

    TranscriptResult result = transcribeService.transcribe(
        audio.getInputStream(),
        audio.getContentType() == null ? "audio/webm" : audio.getContentType(),
        locale,
        Map.of());

    return ResponseEntity.ok(new TranscribeResponse(result.text(), result.durationMs(), result.detectedLocale()));
}

public record TranscribeResponse(String text, long durationMs, String locale) {
}
```

- `validateWebhookExists(webhookId)` calls into the same webhook resolution path that the existing `voice-session-token` route uses (find that path while reading the controller; reuse the existing helper rather than duplicating logic). If no helper exists, extract one.
- Add `private final TranscribeService transcribeService;` field and constructor parameter; do not modify existing constructor parameters except to append.

- [ ] **Step 5: Run test to confirm it passes**

Run: `./gradlew :server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl:test --tests "*WebhookTriggerControllerTranscribeTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-webhook
git commit -m "$(cat <<'EOF'
194800 Add POST /webhooks/{webhookId}/transcribe route

Push-to-talk transcribe endpoint for deployed SDK widget. Accepts
multipart audio (max 25 MB), delegates to TranscribeService, returns
{text, durationMs, locale}. Same auth boundary as existing
voice-session-token route on this controller.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 10: Add `/transcribe` route to `WebhookTriggerTestController`

**Files:**
- Modify: `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/src/main/java/com/bytechef/platform/configuration/web/rest/WebhookTriggerTestController.java`
- Modify: that module's `build.gradle.kts`
- Create: `WebhookTriggerTestControllerTranscribeTest.java` alongside.

Route is `/workflows/{workflowId}/test/transcribe`, auth is session cookie (already enforced on this controller — no `@PreAuthorize` annotation needed on the new method, the controller-level security applies).

- [ ] **Step 1: Add dependency**

In `server/libs/platform/platform-configuration/platform-configuration-rest/platform-configuration-rest-impl/build.gradle.kts`:
```kotlin
implementation(project(":server:libs:platform:platform-ai:platform-ai-stt-service"))
```

- [ ] **Step 2: Write the failing test**

`WebhookTriggerTestControllerTranscribeTest.java`:
```java
/*
 * Copyright 2025-present ByteChef Inc.
 * Licensed under the Apache License, Version 2.0
 */
package com.bytechef.platform.configuration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import com.bytechef.platform.ai.stt.service.TranscribeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser
class WebhookTriggerTestControllerTranscribeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranscribeService transcribeService;

    @Test
    void testTranscribeReturnsText() throws Exception {
        when(transcribeService.transcribe(any(), any(), any(), any()))
            .thenReturn(new TranscriptResult("test-panel hello", 750L, "en"));

        MockMultipartFile audio = new MockMultipartFile(
            "audio", "clip.webm", "audio/webm", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/workflows/wf-123/test/transcribe").file(audio))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("test-panel hello"))
            .andExpect(jsonPath("$.durationMs").value(750));
    }
}
```

- [ ] **Step 3: Run, confirm fail**

Run: `./gradlew :server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl:test --tests "*WebhookTriggerTestControllerTranscribeTest*"`
Expected: FAIL — 404.

- [ ] **Step 4: Add the route to `WebhookTriggerTestController`**

Append a `transcribeService` constructor parameter (do not change other parameters). Add method:

```java
@PostMapping("/workflows/{workflowId}/test/transcribe")
public ResponseEntity<TranscribeResponse> transcribe(
    @PathVariable String workflowId,
    @RequestPart("audio") MultipartFile audio,
    @RequestParam(name = "locale", required = false) String locale) throws IOException {

    if (audio.getSize() > 25 * 1024 * 1024) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    }

    TranscriptResult result = transcribeService.transcribe(
        audio.getInputStream(),
        audio.getContentType() == null ? "audio/webm" : audio.getContentType(),
        locale,
        Map.of());

    return ResponseEntity.ok(new TranscribeResponse(result.text(), result.durationMs(), result.detectedLocale()));
}

public record TranscribeResponse(String text, long durationMs, String locale) {
}
```

- [ ] **Step 5: Run, confirm pass**

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-configuration
git commit -m "$(cat <<'EOF'
194800 Add POST /workflows/{workflowId}/test/transcribe route

Push-to-talk transcribe endpoint for WorkflowTestChatPanel. Session
cookie auth (controller-level), same-origin only. Delegates to
TranscribeService.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 11: New `AiHubTranscribeController` in EE

**Files:**
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/transcribe/AiHubTranscribeController.java`
- Create: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/test/java/com/bytechef/ee/automation/aihub/web/transcribe/AiHubTranscribeControllerTest.java`
- Modify: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/build.gradle.kts`

- [ ] **Step 1: Add dependency**

```kotlin
implementation(project(":server:libs:platform:platform-ai:platform-ai-stt-service"))
```

- [ ] **Step 2: Write the failing test**

```java
/*
 * Copyright 2025 ByteChef
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * @version ee
 */
package com.bytechef.ee.automation.aihub.web.transcribe;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import com.bytechef.platform.ai.stt.service.TranscribeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiHubTranscribeController.class)
@WithMockUser
class AiHubTranscribeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TranscribeService transcribeService;

    @Test
    void testTranscribeReturnsText() throws Exception {
        when(transcribeService.transcribe(any(), any(), any(), any()))
            .thenReturn(new TranscriptResult("ai hub hello", 500L, "en"));

        MockMultipartFile audio = new MockMultipartFile(
            "audio", "clip.webm", "audio/webm", new byte[] {1, 2});

        mockMvc.perform(multipart("/internal/ai-hub/transcribe").file(audio))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("ai hub hello"))
            .andExpect(jsonPath("$.durationMs").value(500));
    }
}
```

- [ ] **Step 3: Run, confirm fail (no controller)**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-rest:test --tests "*AiHubTranscribeControllerTest*"`

- [ ] **Step 4: Implement controller**

```java
/*
 * Copyright 2025 ByteChef
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * @version ee
 */
package com.bytechef.ee.automation.aihub.web.transcribe;

import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import com.bytechef.platform.ai.stt.service.TranscribeService;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @version ee
 */
@RestController
@RequestMapping("/internal/ai-hub")
@PreAuthorize("isAuthenticated()")
public class AiHubTranscribeController {

    private final TranscribeService transcribeService;

    public AiHubTranscribeController(TranscribeService transcribeService) {
        this.transcribeService = transcribeService;
    }

    @PostMapping("/transcribe")
    public ResponseEntity<TranscribeResponse> transcribe(
        @RequestPart("audio") MultipartFile audio,
        @RequestParam(name = "locale", required = false) String locale) throws IOException {

        if (audio.getSize() > 25 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        TranscriptResult result = transcribeService.transcribe(
            audio.getInputStream(),
            audio.getContentType() == null ? "audio/webm" : audio.getContentType(),
            locale,
            Map.of());

        return ResponseEntity.ok(new TranscribeResponse(result.text(), result.durationMs(), result.detectedLocale()));
    }

    public record TranscribeResponse(String text, long durationMs, String locale) {
    }
}
```

- [ ] **Step 5: Run, confirm pass**

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest
git commit -m "$(cat <<'EOF'
194800 Add POST /internal/ai-hub/transcribe (EE)

Push-to-talk transcribe endpoint for AI Hub composer. Session cookie +
@PreAuthorize. Delegates to TranscribeService.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 2 — Push-to-Talk Frontend

Tasks 12–18. Adds `usePushToTalk` hook, `MicButton`, wires both into the three composers. After this phase, push-to-talk works end-to-end in all composers; AI Hub realtime is still present (deletion is Phase 4).

### Task 12: `usePushToTalk` hook (platform copy) with failing test

**Files:**
- Create: `client/src/shared/lib/voice/usePushToTalk.ts`
- Create: `client/src/shared/lib/voice/usePushToTalk.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {usePushToTalk} from './usePushToTalk';

class MockMediaRecorder {
    static isTypeSupported = vi.fn().mockReturnValue(true);
    ondataavailable: ((event: {data: Blob}) => void) | null = null;
    onstop: (() => void) | null = null;
    state: 'inactive' | 'recording' = 'inactive';

    constructor(public stream: MediaStream, public options: MediaRecorderOptions) {}

    start() {
        this.state = 'recording';
    }

    stop() {
        this.state = 'inactive';
        this.ondataavailable?.({data: new Blob(['mock-audio'], {type: 'audio/webm'})});
        this.onstop?.();
    }
}

describe('usePushToTalk', () => {
    let fetchMock: ReturnType<typeof vi.fn>;
    let onTranscript: ReturnType<typeof vi.fn>;
    let getUserMediaMock: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({text: 'hello world', durationMs: 1000, locale: 'en'}),
        });
        onTranscript = vi.fn();

        getUserMediaMock = vi.fn().mockResolvedValue({
            getTracks: () => [{stop: vi.fn()}],
        } as unknown as MediaStream);

        vi.stubGlobal('fetch', fetchMock);
        vi.stubGlobal('MediaRecorder', MockMediaRecorder);

        Object.defineProperty(globalThis.navigator, 'mediaDevices', {
            configurable: true,
            value: {getUserMedia: getUserMediaMock},
        });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('uploads the recorded blob and calls onTranscript with the returned text', async () => {
        const {result} = renderHook(() =>
            usePushToTalk({onTranscript, transcribeUrl: 'http://test/transcribe'})
        );

        await act(async () => {
            await result.current.start();
        });

        expect(result.current.status).toBe('recording');

        await act(async () => {
            await result.current.stop();
        });

        await waitFor(() => expect(onTranscript).toHaveBeenCalledWith('hello world'));
        expect(fetchMock).toHaveBeenCalledWith(
            'http://test/transcribe',
            expect.objectContaining({body: expect.any(FormData), method: 'POST'})
        );
        expect(result.current.status).toBe('idle');
    });

    it('reports error when fetch fails', async () => {
        fetchMock.mockResolvedValueOnce({ok: false, statusText: 'Server Error'});
        const onError = vi.fn();

        const {result} = renderHook(() =>
            usePushToTalk({onError, onTranscript, transcribeUrl: 'http://test/transcribe'})
        );

        await act(async () => {
            await result.current.start();
            await result.current.stop();
        });

        await waitFor(() => expect(result.current.status).toBe('error'));
        expect(onError).toHaveBeenCalledWith(expect.stringContaining('Server Error'));
    });
});
```

- [ ] **Step 2: Run, confirm fail**

Run: `cd client && npx vitest run src/shared/lib/voice/usePushToTalk.test.ts`
Expected: FAIL — module not found.

- [ ] **Step 3: Implement the hook**

```ts
import {useCallback, useRef, useState} from 'react';

export type PushToTalkStatusType = 'idle' | 'recording' | 'transcribing' | 'error';

interface UsePushToTalkArgsI {
    locale?: string;
    onError?: (message: string) => void;
    onTranscript: (text: string) => void;
    transcribeUrl: string;
}

interface UsePushToTalkResultI {
    error: string | null;
    start: () => Promise<void>;
    status: PushToTalkStatusType;
    stop: () => Promise<void>;
}

function pickMimeType(): string {
    if (typeof MediaRecorder === 'undefined') return 'audio/webm';

    if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) return 'audio/webm;codecs=opus';

    if (MediaRecorder.isTypeSupported('audio/mp4')) return 'audio/mp4';

    return 'audio/webm';
}

export function usePushToTalk({
    locale,
    onError,
    onTranscript,
    transcribeUrl,
}: UsePushToTalkArgsI): UsePushToTalkResultI {
    const [status, setStatus] = useState<PushToTalkStatusType>('idle');
    const [error, setError] = useState<string | null>(null);
    const recorderRef = useRef<MediaRecorder | null>(null);
    const streamRef = useRef<MediaStream | null>(null);
    const chunksRef = useRef<Blob[]>([]);

    const reportError = useCallback(
        (message: string) => {
            setStatus('error');
            setError(message);
            onError?.(message);
        },
        [onError]
    );

    const start = useCallback(async () => {
        try {
            setError(null);
            const stream = await navigator.mediaDevices.getUserMedia({
                audio: {echoCancellation: true, noiseSuppression: true},
            });

            const mimeType = pickMimeType();
            const recorder = new MediaRecorder(stream, {mimeType});

            chunksRef.current = [];
            recorder.ondataavailable = (event) => {
                if (event.data && event.data.size > 0) chunksRef.current.push(event.data);
            };

            recorderRef.current = recorder;
            streamRef.current = stream;
            recorder.start();
            setStatus('recording');
        } catch (caught) {
            reportError(caught instanceof Error ? caught.message : 'Microphone unavailable');
        }
    }, [reportError]);

    const stop = useCallback(async () => {
        const recorder = recorderRef.current;
        const stream = streamRef.current;

        if (!recorder || !stream) return;

        const finished = new Promise<void>((resolve) => {
            recorder.onstop = () => resolve();
        });

        recorder.stop();
        await finished;

        for (const track of stream.getTracks()) track.stop();

        recorderRef.current = null;
        streamRef.current = null;

        const blob = new Blob(chunksRef.current, {type: recorder.mimeType || 'audio/webm'});

        chunksRef.current = [];
        setStatus('transcribing');

        try {
            const form = new FormData();

            form.append('audio', blob, 'utterance.webm');

            if (locale) form.append('locale', locale);

            const response = await fetch(transcribeUrl, {body: form, method: 'POST'});

            if (!response.ok) {
                reportError(`Transcribe failed: ${response.statusText}`);
                return;
            }

            const data = (await response.json()) as {text: string};

            onTranscript(data.text);
            setStatus('idle');
        } catch (caught) {
            reportError(caught instanceof Error ? caught.message : 'Transcribe failed');
        }
    }, [locale, onTranscript, reportError, transcribeUrl]);

    return {error, start, status, stop};
}
```

- [ ] **Step 4: Run, confirm pass**

Run: `cd client && npx vitest run src/shared/lib/voice/usePushToTalk.test.ts`
Expected: PASS, 2 tests.

- [ ] **Step 5: Commit**

```bash
cd client && npm run format
cd ..
git add client/src/shared/lib/voice/
git commit -m "$(cat <<'EOF'
194800 client - Add usePushToTalk hook

MediaRecorder-based push-to-talk capture. Records WebM/Opus (mp4
fallback for Safari), uploads as multipart to a transcribeUrl, returns
text via onTranscript callback. Used by all three composers.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 13: `MicButton` component (platform copy)

**Files:**
- Create: `client/src/shared/lib/voice/MicButton.tsx`
- Create: `client/src/shared/lib/voice/MicButton.test.tsx`

- [ ] **Step 1: Write failing test**

```tsx
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import {MicButton} from './MicButton';

describe('MicButton', () => {
    it('calls onClick when clicked', () => {
        const onClick = vi.fn();

        render(<MicButton onClick={onClick} status="idle" />);

        fireEvent.click(screen.getByRole('button', {name: /record/i}));
        expect(onClick).toHaveBeenCalled();
    });

    it('renders a stop indicator when recording', () => {
        render(<MicButton onClick={() => undefined} status="recording" />);
        expect(screen.getByRole('button', {name: /stop/i})).toBeInTheDocument();
    });

    it('renders a spinner while transcribing', () => {
        render(<MicButton onClick={() => undefined} status="transcribing" />);
        expect(screen.getByTestId('mic-spinner')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run, confirm fail**

- [ ] **Step 3: Implement**

```tsx
import {Button} from '@/components/ui/button';
import {Loader2Icon, MicIcon, SquareIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import type {PushToTalkStatusType} from './usePushToTalk';

interface MicButtonPropsType {
    className?: string;
    disabled?: boolean;
    onClick: () => void;
    status: PushToTalkStatusType;
}

export function MicButton({className, disabled, onClick, status}: MicButtonPropsType) {
    const label = status === 'recording' ? 'Stop recording' : 'Record voice message';
    const isBusy = status === 'transcribing';

    return (
        <Button
            aria-label={label}
            className={twMerge('size-8', className)}
            disabled={disabled || isBusy}
            onClick={onClick}
            size="icon"
            variant="ghost"
        >
            {status === 'recording' && <SquareIcon className="size-4 fill-red-500 text-red-500" />}
            {status === 'transcribing' && (
                <Loader2Icon className="size-4 animate-spin" data-testid="mic-spinner" />
            )}
            {(status === 'idle' || status === 'error') && <MicIcon className="size-4" />}
        </Button>
    );
}
```

- [ ] **Step 4: Run, confirm pass**

- [ ] **Step 5: Commit**

```bash
cd client && npm run format
cd ..
git add client/src/shared/lib/voice/MicButton.tsx client/src/shared/lib/voice/MicButton.test.tsx
git commit -m "$(cat <<'EOF'
194800 client - Add MicButton for push-to-talk

Toggles between mic (idle), red square (recording), spinner
(transcribing). Used by all three composers.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 14: Mirror `usePushToTalk` + `MicButton` into SDK widget

**Files:**
- Create: `sdks/frontend/automation/chat/library/src/lib/usePushToTalk.ts`
- Create: `sdks/frontend/automation/chat/library/src/lib/MicButton.tsx`
- (No tests in the SDK copy — duplication of platform tests would rot; SDK build verifies it compiles.)

- [ ] **Step 1: Copy verbatim**

Copy the two platform files into the SDK location. Adjust imports to match SDK paths (the SDK may not have `@/components/ui/button` — use the SDK's existing Button component or inline a button if none exists; check `sdks/frontend/automation/chat/library/src/components/`). Adjust `tailwind-merge` import only if the SDK doesn't bundle it (it does — see `thread.tsx`).

- [ ] **Step 2: Verify SDK builds**

Run: `cd sdks/frontend/automation/chat/library && npm run build` (or whichever script the SDK uses; check `package.json`).
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add sdks/frontend/automation/chat/library/src/lib/
git commit -m "$(cat <<'EOF'
194800 client - Mirror usePushToTalk + MicButton into SDK widget

Identical copy. Platform and SDK copies stay duplicated for now;
consolidating is a separate refactor.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 15: Wire `usePushToTalk` into `AiHubChatComposer`

**Files:**
- Modify: `client/src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx`

- [ ] **Step 1: Read the file end-to-end**

Use `Read` on `AiHubChatComposer.tsx`. Locate:
- The mic button JSX (around the existing `MicIcon`/`MicOffIcon`).
- The current voice handlers (`voice.start()` / `voice.stop()` calls).
- The `useAiHubVoiceSession` import.

Leave the existing voice block in place — deletion is Phase 4. We add push-to-talk alongside it temporarily so we can A/B verify the new path before removing the old.

- [ ] **Step 2: Add the new wiring**

Add imports:
```ts
import {MicButton} from '@/shared/lib/voice/MicButton';
import {usePushToTalk} from '@/shared/lib/voice/usePushToTalk';
import {useComposerRuntime} from '@assistant-ui/react';
```

Inside the component body, after existing hooks:
```ts
const composerRuntime = useComposerRuntime();
const pushToTalk = usePushToTalk({
    onTranscript: (text) => {
        composerRuntime.setText(text);
        composerRuntime.send();
    },
    transcribeUrl: '/api/automation/internal/ai-hub/transcribe',
});
```

In the JSX where the existing mic button lives, render the new `MicButton` immediately next to it (we will remove the old one in Phase 4):
```tsx
<MicButton
    onClick={() => (pushToTalk.status === 'recording' ? pushToTalk.stop() : pushToTalk.start())}
    status={pushToTalk.status}
/>
```

- [ ] **Step 3: Manual verification**

Run the dev stack (`cd server && docker compose -f docker-compose.dev.infra.yml up -d`, then `./gradlew -p server/apps/server-app bootRun` and `cd client && npm run dev`).

In the browser: open an AI Hub task, click the new mic button, speak, click again — verify the text appears in the composer and the user message is sent. Confirm no console errors.

- [ ] **Step 4: Run client checks**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass.

- [ ] **Step 5: Commit**

```bash
cd client && npm run format
cd ..
git add client/src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx
git commit -m "$(cat <<'EOF'
194800 client - Wire push-to-talk into AiHubChatComposer

New MicButton + usePushToTalk hook posts to /internal/ai-hub/transcribe
and sends the returned text via the composer runtime. Existing
realtime mic button stays in place temporarily; removed in Phase 4.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 16: Wire `usePushToTalk` into `WorkflowTestChatPanel`'s composer

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel.tsx`

- [ ] **Step 1: Read the file**

Locate the composer area. The `workflowId` is already in scope (used by the existing voice session hook).

- [ ] **Step 2: Add the wiring**

```ts
import {MicButton} from '@/shared/lib/voice/MicButton';
import {usePushToTalk} from '@/shared/lib/voice/usePushToTalk';
import {useComposerRuntime} from '@assistant-ui/react';

// ...inside component:
const composerRuntime = useComposerRuntime();
const pushToTalk = usePushToTalk({
    onTranscript: (text) => {
        composerRuntime.setText(text);
        composerRuntime.send();
    },
    transcribeUrl: `/api/automation/workflows/${workflowId}/test/transcribe`,
});
```

Render `<MicButton ... />` next to the existing mic button (Phase 4 removes the old one).

- [ ] **Step 3: Manual verify** — same as Task 15 but in the workflow test panel.

- [ ] **Step 4: Client checks** — `cd client && npm run check`

- [ ] **Step 5: Commit**

```
194800 client - Wire push-to-talk into WorkflowTestChatPanel
```

---

### Task 17: Wire `usePushToTalk` into SDK widget composer

**Files:**
- Modify: `sdks/frontend/automation/chat/library/src/components/assistant-ui/thread.tsx`
- Modify: `sdks/frontend/automation/chat/library/src/AutomationChatProvider.tsx` (pass `webhookUrl` down if not already in scope at the composer)

- [ ] **Step 1: Read the files**

The widget's webhook URL is configured at provider level. Surface it to the composer via context if not already.

- [ ] **Step 2: Add wiring**

```ts
import {MicButton} from '../../lib/MicButton';
import {usePushToTalk} from '../../lib/usePushToTalk';
import {useComposerRuntime} from '@assistant-ui/react';

// In ComposerAction or wherever the mic button is:
const composerRuntime = useComposerRuntime();
const pushToTalk = usePushToTalk({
    onTranscript: (text) => {
        composerRuntime.setText(text);
        composerRuntime.send();
    },
    transcribeUrl: `${webhookUrl}/transcribe`,
});
```

Render `<MicButton ... />` alongside the existing mic button.

- [ ] **Step 3: Verify SDK builds**

- [ ] **Step 4: Manual verify** via the widget demo page (if one exists).

- [ ] **Step 5: Commit**

```
194800 client - Wire push-to-talk into SDK widget composer
```

---

### Task 18: Phase 2 checkpoint — verify all three composers

- [ ] **Manual verification across all three surfaces:**
  - AI Hub composer push-to-talk works
  - WorkflowTestChatPanel push-to-talk works
  - SDK widget push-to-talk works (use the demo page or wire the widget into a sample app)

- [ ] **Run full client checks:** `cd client && npm run check`
- [ ] **Run server checks:** `./gradlew check`

(No code commit — this is a verification gate before Phase 3.)

---

## Phase 3 — Realtime Voice Mode Refactor

Tasks 19–27. Replace the bespoke voice UI in test panel + widget with assistant-ui's voice primitives wrapped around the existing `BrowserVoiceSession`. AI Hub realtime is still intact at the end of this phase; deletion is Phase 4.

### Task 19: Extend `BrowserVoiceSession` with new event hooks (platform copy)

**Files:**
- Modify: `client/src/shared/lib/browser-voice/BrowserVoiceSession.ts`
- Modify: `client/src/shared/lib/browser-voice/BrowserVoiceSession.test.ts` (create if absent)

- [ ] **Step 1: Read the existing class**

Catalog the existing event surface (`onEvent`, `onStatusChange`, etc.) and how the WebSocket message loop translates incoming frames into events.

- [ ] **Step 2: Write a failing test for the new events**

Test that:
1. `onSpeakingChange(true)` fires when the next incoming audio frame arrives after a silent period; `onSpeakingChange(false)` after no frames for >300 ms.
2. `onVolume(level)` fires at ~20 Hz while mic input is active.
3. `setMuted(true)` stops outgoing frames; `setMuted(false)` resumes.

(Use a mock WebSocket and `vi.useFakeTimers()`. Mock `AudioContext` and `AudioWorkletNode` minimally.)

- [ ] **Step 3: Run, confirm fail**

- [ ] **Step 4: Implement**

Add to the constructor options:
```ts
interface BrowserVoiceSessionOptionsI {
    // ...existing
    onSpeakingChange?: (isAssistantSpeaking: boolean) => void;
    onStatusChange?: (status: 'connecting' | 'active' | 'closed' | 'error') => void;
    onVolume?: (level: number) => void;
}
```

Add `setMuted(muted: boolean): void` method that toggles a flag the audio worklet checks before posting frames to the WebSocket.

For `onSpeakingChange`: maintain a `lastAssistantFrameAt` timestamp updated whenever an audio frame arrives from the server. A 100 ms tick checks the timestamp; if updated within 300 ms and current state is `listening`, transition to `speaking` and fire callback; if no updates for 300 ms and state is `speaking`, transition back.

For `onVolume`: in the existing mic worklet message handler, compute RMS over the buffer (`sqrt(sum(s^2) / n)`) and post upstream at most every 50 ms.

- [ ] **Step 5: Run, confirm pass**

- [ ] **Step 6: Commit**

```
194800 client - Extend BrowserVoiceSession with status/speaking/volume events and setMuted
```

---

### Task 20: Mirror `BrowserVoiceSession` changes into SDK copy

**Files:**
- Modify: `sdks/frontend/automation/chat/library/src/lib/BrowserVoiceSession.ts`

- [ ] **Step 1: Apply the same diff verbatim**

- [ ] **Step 2: Verify SDK builds**

- [ ] **Step 3: Commit**

```
194800 client - Mirror BrowserVoiceSession extensions into SDK
```

---

### Task 21: `ByteChefRealtimeVoiceAdapter` (platform copy) with failing test

**Files:**
- Create: `client/src/shared/lib/voice/ByteChefRealtimeVoiceAdapter.ts`
- Create: `client/src/shared/lib/voice/ByteChefRealtimeVoiceAdapter.test.ts`

- [ ] **Step 1: Write failing test**

```ts
import type {RealtimeVoiceAdapter} from '@assistant-ui/react';
import {describe, expect, it, vi} from 'vitest';

import {ByteChefRealtimeVoiceAdapter} from './ByteChefRealtimeVoiceAdapter';

vi.mock('@/shared/lib/browser-voice/BrowserVoiceSession', () => ({
    BrowserVoiceSession: vi.fn().mockImplementation((options) => {
        const instance = {
            start: vi.fn().mockResolvedValue(undefined),
            stop: vi.fn(),
            setMuted: vi.fn(),
            _options: options,
        };

        setTimeout(() => options.onStatusChange?.('active'), 0);

        return instance;
    }),
}));

describe('ByteChefRealtimeVoiceAdapter', () => {
    it('mints a token, opens a session, and emits running status', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            json: () => Promise.resolve({token: 'tkn', wsPath: '/webhooks/x/wss'}),
        });

        vi.stubGlobal('fetch', fetchMock);

        const adapter = new ByteChefRealtimeVoiceAdapter({
            tokenUrl: 'http://server/webhooks/x/voice-session-token',
        });

        const session = adapter.connect({});
        const statuses: RealtimeVoiceAdapter.Status[] = [];

        session.onStatusChange((status) => statuses.push(status));

        await new Promise((resolve) => setTimeout(resolve, 10));

        expect(fetchMock).toHaveBeenCalledWith(
            'http://server/webhooks/x/voice-session-token',
            expect.objectContaining({method: 'POST'})
        );
        expect(statuses).toContainEqual({type: 'running'});
    });
});
```

- [ ] **Step 2: Run, confirm fail**

- [ ] **Step 3: Implement**

```ts
import type {RealtimeVoiceAdapter} from '@assistant-ui/react';
import {createVoiceSession} from '@assistant-ui/react';

import {BrowserVoiceSession} from '@/shared/lib/browser-voice/BrowserVoiceSession';

interface AdapterConfigI {
    sampleRate?: 16000 | 24000;
    tokenUrl: string;
}

export class ByteChefRealtimeVoiceAdapter implements RealtimeVoiceAdapter {
    constructor(private readonly config: AdapterConfigI) {}

    connect(options: {abortSignal?: AbortSignal}): RealtimeVoiceAdapter.Session {
        return createVoiceSession(options, async (helpers) => {
            const response = await fetch(this.config.tokenUrl, {method: 'POST'});

            if (!response.ok) throw new Error(`Voice token request failed: ${response.statusText}`);

            const {token, wsPath} = (await response.json()) as {token: string; wsPath: string};

            const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = `${wsProtocol}//${window.location.host}${wsPath}?sessionToken=${token}`;

            const session = new BrowserVoiceSession({
                onEvent: (event) => {
                    if (event.type === 'transcript_partial') {
                        helpers.emitTranscript({isFinal: false, role: 'user', text: event.text ?? ''});
                    }

                    if (event.type === 'transcript_final') {
                        helpers.emitTranscript({isFinal: true, role: 'user', text: event.text ?? ''});
                    }

                    if (event.type === 'assistant_text') {
                        helpers.emitTranscript({
                            isFinal: !!event.done,
                            role: 'assistant',
                            text: event.text ?? '',
                        });
                    }
                },
                onSpeakingChange: (isAssistantSpeaking) =>
                    helpers.emitMode(isAssistantSpeaking ? 'speaking' : 'listening'),
                onStatusChange: (status) => {
                    if (status === 'active') helpers.setStatus({type: 'running'});

                    if (status === 'closed') helpers.end('finished');

                    if (status === 'error') helpers.end('error');
                },
                onVolume: (level) => helpers.emitVolume(level),
                sampleRate: this.config.sampleRate ?? 16000,
                url: wsUrl,
            });

            await session.start();

            return {
                disconnect: () => session.stop(),
                mute: () => session.setMuted(true),
                unmute: () => session.setMuted(false),
            };
        });
    }
}

export function createWebhookVoiceAdapter(webhookUrl: string): RealtimeVoiceAdapter {
    return new ByteChefRealtimeVoiceAdapter({tokenUrl: `${webhookUrl}/voice-session-token`});
}
```

- [ ] **Step 4: Run, confirm pass**

- [ ] **Step 5: Commit**

```
194800 client - Add ByteChefRealtimeVoiceAdapter (assistant-ui RealtimeVoiceAdapter impl)
```

---

### Task 22: Mirror adapter into SDK widget

**Files:**
- Create: `sdks/frontend/automation/chat/library/src/lib/ByteChefRealtimeVoiceAdapter.ts`

- [ ] Copy the platform file. Adjust the `BrowserVoiceSession` import path. Commit.

```
194800 client - Mirror ByteChefRealtimeVoiceAdapter into SDK widget
```

---

### Task 23: `VoiceModeLayout` component (platform copy)

**Files:**
- Create: `client/src/shared/lib/voice/VoiceModeLayout.tsx`
- Create: `client/src/shared/lib/voice/VoiceModeLayout.test.tsx`

- [ ] **Step 1: Write failing test**

```tsx
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@assistant-ui/react', () => ({
    AuiIf: ({children}: {children: React.ReactNode}) => <>{children}</>,
    useVoiceState: () => undefined,
    VoiceConnectButton: () => <button>Connect</button>,
    VoiceDisconnectButton: () => <button>Disconnect</button>,
    VoiceOrb: () => <div data-testid="voice-orb" />,
}));

import {VoiceModeLayout} from './VoiceModeLayout';

describe('VoiceModeLayout', () => {
    it('renders idle state with Connect button when no voice session', () => {
        render(<VoiceModeLayout sessionLimitSeconds={150} />);
        expect(screen.getByTestId('voice-orb')).toBeInTheDocument();
        expect(screen.getByText('Connect')).toBeInTheDocument();
    });
});
```

- [ ] **Step 2: Run, confirm fail**

- [ ] **Step 3: Implement**

```tsx
import {
    useVoiceState,
    VoiceConnectButton,
    VoiceDisconnectButton,
    VoiceOrb,
} from '@assistant-ui/react';

interface VoiceModeLayoutPropsType {
    activeLabel?: string;
    idleLabel?: string;
    sessionLimitSeconds: number;
    speakingLabel?: string;
}

function formatDuration(totalSeconds: number): string {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

export function VoiceModeLayout({
    activeLabel = 'Listening',
    idleLabel = 'Tap to start',
    sessionLimitSeconds,
    speakingLabel = 'Speaking',
}: VoiceModeLayoutPropsType) {
    const voiceState = useVoiceState();
    const isActive = voiceState?.status.type === 'running';
    const isSpeaking = voiceState?.mode === 'speaking';

    const statusLabel = !isActive ? idleLabel : isSpeaking ? speakingLabel : activeLabel;

    return (
        <div className="flex h-full flex-col bg-black text-white">
            <header className="flex justify-end p-4">
                <span className="rounded-full border border-emerald-500/40 px-3 py-1 text-xs tracking-widest text-emerald-400">
                    {formatDuration(sessionLimitSeconds)} LIMIT
                </span>
            </header>

            <main className="flex flex-1 flex-col items-center justify-center gap-8">
                <VoiceOrb className="size-48" variant="emerald" />
                <div className="text-center">
                    <p className="text-xs tracking-[0.3em] text-muted-foreground">{statusLabel.toUpperCase()}</p>
                </div>
            </main>

            <footer className="flex justify-center pb-12">
                {isActive ? <VoiceDisconnectButton /> : <VoiceConnectButton />}
            </footer>
        </div>
    );
}
```

- [ ] **Step 4: Run, confirm pass**

- [ ] **Step 5: Commit**

```
194800 client - Add VoiceModeLayout (phone-call UI for voice-only workflows)
```

---

### Task 24: Mirror `VoiceModeLayout` into SDK widget

- [ ] Copy the file. Adjust imports. Commit.

---

### Task 25: Trigger-based rendering in `WorkflowTestChatPanel`

**Files:**
- Modify: `client/src/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel.tsx`

- [ ] **Step 1: Read the file**

Locate where `useChatRuntime` (or equivalent) is called and where `<Thread />` is rendered.

- [ ] **Step 2: Add the branch**

```tsx
import {createWebhookVoiceAdapter} from '@/shared/lib/voice/ByteChefRealtimeVoiceAdapter';
import {VoiceModeLayout} from '@/shared/lib/voice/VoiceModeLayout';

// inside component:
const isVoiceOnlyWorkflow = useMemo(
    () =>
        workflow?.triggers?.some(
            (trigger) => trigger.componentName === 'browser' && trigger.triggerName === 'voiceSession'
        ) ?? false,
    [workflow?.triggers]
);

const voiceAdapter = useMemo(
    () =>
        isVoiceOnlyWorkflow && webhookUrl
            ? createWebhookVoiceAdapter(webhookUrl)
            : undefined,
    [isVoiceOnlyWorkflow, webhookUrl]
);

const runtime = useChatRuntime({
    /* ...existing args... */
    adapters: voiceAdapter ? {voice: voiceAdapter} : undefined,
});

return (
    <AssistantRuntimeProvider runtime={runtime}>
        {isVoiceOnlyWorkflow ? (
            <VoiceModeLayout sessionLimitSeconds={sessionLimitFromTrigger ?? 150} />
        ) : (
            <Thread />
        )}
    </AssistantRuntimeProvider>
);
```

The exact `webhookUrl` / runtime wiring should mirror what the file already does for non-voice flows.

- [ ] **Step 3: Manual verify**

Build a workflow with the `browser/v1/voiceSession` trigger. Open it in the test panel. Verify `<VoiceModeLayout>` renders (not `<Thread>`). Click Connect, speak, verify VoiceOrb animates and `Disconnect` appears. Click Disconnect.

- [ ] **Step 4: Client checks** — `cd client && npm run check`

- [ ] **Step 5: Commit**

```
194800 client - Render VoiceModeLayout in test panel when trigger is browser/v1/voiceSession
```

---

### Task 26: Trigger-based rendering in SDK widget

**Files:**
- Modify: `sdks/frontend/automation/chat/library/src/AutomationChatProvider.tsx` (or wherever the runtime + render decision is made — check by reading the file)

- [ ] **Step 1: Read the file**

Use `Read` on `AutomationChatProvider.tsx` and `components/assistant-ui/thread.tsx`. Locate:
- Where `useChatRuntime` (or its SDK equivalent) is called and configured.
- Where the workflow trigger metadata is fetched (or where it's already available — the SDK fetches the webhook info via the same `/voice-session-token` endpoint preflight).
- Where the SDK currently renders `<Thread />`.

- [ ] **Step 2: Fetch trigger metadata if not already available**

If the SDK does not currently fetch workflow trigger info, add a small fetch in the provider:
```ts
useEffect(() => {
    if (!webhookUrl) return;

    fetch(`${webhookUrl}/metadata`, {method: 'GET'})
        .then((response) => response.json())
        .then((metadata: {triggers: {componentName: string; triggerName: string}[]}) => {
            setTriggers(metadata.triggers);
        })
        .catch(() => setTriggers([]));
}, [webhookUrl]);
```

(If a `/metadata` route does not exist for public webhooks, expose one as part of this task — the test panel already has it. Otherwise piggyback on the existing token preflight response.)

- [ ] **Step 3: Branch on trigger type**

```tsx
import {createWebhookVoiceAdapter} from './lib/ByteChefRealtimeVoiceAdapter';
import {VoiceModeLayout} from './lib/VoiceModeLayout';

// inside the component:
const isVoiceOnlyWorkflow = useMemo(
    () =>
        triggers.some(
            (trigger) => trigger.componentName === 'browser' && trigger.triggerName === 'voiceSession'
        ),
    [triggers]
);

const voiceAdapter = useMemo(
    () => (isVoiceOnlyWorkflow && webhookUrl ? createWebhookVoiceAdapter(webhookUrl) : undefined),
    [isVoiceOnlyWorkflow, webhookUrl]
);

const runtime = useChatRuntime({
    /* ...existing args... */
    adapters: voiceAdapter ? {voice: voiceAdapter} : undefined,
});

return (
    <AssistantRuntimeProvider runtime={runtime}>
        {isVoiceOnlyWorkflow ? (
            <VoiceModeLayout sessionLimitSeconds={sessionLimitSeconds ?? 150} />
        ) : (
            children
        )}
    </AssistantRuntimeProvider>
);
```

`sessionLimitSeconds` reads from the trigger metadata once Task 27 surfaces it; until then the default 150 stands.

- [ ] **Step 4: Build the SDK**

Run: `cd sdks/frontend/automation/chat/library && npm run build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual verify in the widget demo**

If the SDK has a demo/sample app (check `sdks/frontend/automation/chat/`), run it against a deployed workflow with the voice trigger. Verify `<VoiceModeLayout>` renders.

- [ ] **Step 6: Commit**

```bash
git add sdks/frontend/automation/chat/library
git commit -m "$(cat <<'EOF'
194800 client - Render VoiceModeLayout in SDK widget for voice trigger

Provider inspects workflow trigger metadata; if any trigger is
browser/v1/voiceSession, install the realtime voice adapter and render
VoiceModeLayout instead of the chat thread.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 27: Add `sessionLimitSeconds` property to `BrowserVoiceSessionTrigger`

**Files:**
- Modify: `server/libs/modules/components/browser/src/main/java/com/bytechef/component/browser/trigger/BrowserVoiceSessionTrigger.java`
- Modify: corresponding test or generated definition; the component test infrastructure regenerates `.json` definitions.

- [ ] **Step 1: Delete stale test definition snapshots**

```bash
rm -rf server/libs/modules/components/browser/src/test/resources/definition
rm -rf server/libs/modules/components/browser/build/resources/test/definition
```

- [ ] **Step 2: Add the property**

Add to the existing `properties()` chain inside `TRIGGER_DEFINITION`:
```java
integer(SESSION_LIMIT_SECONDS)
    .label("Session limit (seconds)")
    .description("Maximum duration of a voice session. 0 means no limit.")
    .defaultValue(150)
```

Add the constant:
```java
public static final String SESSION_LIMIT_SECONDS = "sessionLimitSeconds";
```

- [ ] **Step 3: Run tests to regenerate definitions**

Run: `./gradlew :server:libs:modules:components:browser:test`
Expected: PASS; new `.json` snapshot includes the property.

- [ ] **Step 4: Surface the property server-side**

In `WebhookWebSocketHandler` (the existing handler for `/webhooks/{id}/wss`), read `sessionLimitSeconds` from trigger parameters and enforce as the session cap (it already enforces 30 min default — replace that default with this property where present).

- [ ] **Step 5: Surface the property to the client**

The trigger model is already exposed via the existing trigger metadata API. Verify the new property appears in the response for a published workflow's trigger.

- [ ] **Step 6: Wire client `<VoiceModeLayout>` countdown**

In `VoiceModeLayout`, accept `sessionLimitSeconds` (already done) and add a `useEffect` countdown that decrements while `isActive`. Display remaining time when active; display total limit when idle.

- [ ] **Step 7: Update tests + commit**

```bash
./gradlew spotlessApply
cd client && npm run format
cd ..
git add server/libs/modules/components/browser client/src/shared/lib/voice/VoiceModeLayout.tsx sdks/frontend/automation/chat/library/src/lib/VoiceModeLayout.tsx
git commit -m "$(cat <<'EOF'
194800 Add sessionLimitSeconds property to browser/voiceSession trigger

Workflow author configures the per-session cap. WebhookWebSocketHandler
enforces it server-side; VoiceModeLayout shows it as a countdown chip
in the header.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Phase 4 — AI Hub Realtime Deletion

Tasks 28–32. Removes the old AI Hub realtime backend and the realtime block in the AI Hub composer. Push-to-talk from Phase 2 is the replacement; do not start this phase until Phase 2 is verified end-to-end.

### Task 28: Remove realtime block from `AiHubChatComposer`

**Files:**
- Modify: `client/src/pages/automation/ai-hub/composer/AiHubChatComposer.tsx`
- Delete: `client/src/pages/automation/ai-hub/hooks/useAiHubVoiceSession.ts`

- [ ] **Step 1: Read the composer**

Identify and remove:
- Import of `useAiHubVoiceSession`
- `const voice = useAiHubVoiceSession(...)` block and all references
- `pendingVoiceUserMessage` drain effect (around lines 116–139 per the exploration map)
- Voice error toast effect (around 143–153)
- Voice-status banner
- Task-switch voice stop effect (around 159–167)
- The old `MicIcon`/`MicOffIcon` button branch — keep only the `<MicButton ... />` from Task 15

- [ ] **Step 2: Delete the hook file**

```bash
rm client/src/pages/automation/ai-hub/hooks/useAiHubVoiceSession.ts
```

- [ ] **Step 3: Client checks**

Run: `cd client && npm run check`
Expected: PASS. Resolve any remaining references to the deleted hook (TypeScript will surface them).

- [ ] **Step 4: Manual verify**

Open AI Hub composer. Verify the (now sole) mic button is the push-to-talk one. Verify there is no voice-status banner or voice error toast slot anymore.

- [ ] **Step 5: Commit**

```bash
cd client && npm run format
cd ..
git add client/src/pages/automation/ai-hub
git commit -m "$(cat <<'EOF'
194800 client - Remove realtime voice from AI Hub composer

Delete useAiHubVoiceSession hook and its wiring in AiHubChatComposer.
Push-to-talk (added in 194800 earlier) is the sole mic path now. AI Hub
no longer has a realtime voice surface.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 29: Delete server-side AI Hub realtime controllers and handlers

**Files:**
- Delete: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/voice/AiHubVoiceSessionTokenController.java`
- Delete: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-rest/src/main/java/com/bytechef/ee/automation/aihub/web/voice/AiHubVoiceWebSocketHandler.java`
- Delete the supporting `BrowserVoiceSessionTokenService`-equivalent for AI Hub if it exists in this package, plus any `VoiceMetricsRecorder` registrations specific to AI Hub paths.
- Modify: any `WebSocketConfigurer` that registers the AI Hub voice handler — remove the registration.

- [ ] **Step 1: Find all references**

Run: `grep -rln "AiHubVoiceWebSocketHandler\|AiHubVoiceSessionTokenController" server/`
Expected: only the files being deleted and the WS configurer.

- [ ] **Step 2: Delete files and update the configurer**

- [ ] **Step 3: Verify EE module compiles**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:automation-ai-hub-rest:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```
194800 Delete AI Hub voice session controller, WS handler, and registration
```

---

### Task 30: Delete `AiHubVoiceProvider` SPI + Deepgram realtime provider + module

**Files:**
- Delete: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-api/src/main/java/com/bytechef/ee/automation/aihub/voice/AiHubVoiceProvider.java`
- Delete: `server/ee/libs/automation/automation-ai-hub/automation-ai-hub-api/src/main/java/com/bytechef/ee/automation/aihub/voice/AiHubVoiceProviderSession.java`
- Delete: the realtime Deepgram module entirely (whichever module hosts the realtime `DeepgramVoiceProvider`, e.g. `automation-ai-hub-voice-deepgram`)
- Modify: `settings.gradle.kts` to remove the deleted module's `include(...)`

- [ ] **Step 1: Verify no other consumers**

Run: `grep -rln "AiHubVoiceProvider\|DeepgramVoiceProvider" server/`
Expected: only the files being deleted (the new REST `DeepgramSttProvider` is a separate class — don't confuse them).

- [ ] **Step 2: Delete files and module**

- [ ] **Step 3: Verify EE compiles**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:compileJava`

- [ ] **Step 4: Commit**

```
194800 Delete AiHubVoiceProvider SPI and realtime Deepgram provider module
```

---

### Task 31: Liquibase migration — drop `voice_provider` column

**Files:**
- Create: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/changelog/2026/2026-05-19-drop-ai-hub-voice-provider.xml` (or whichever path the existing changelogs use — check `master.xml`)
- Modify: the master changelog include list
- Modify: the `AiHubWorkspaceSettings` JDBC entity (or equivalent) — remove the `voiceProvider` field, getter/setter, and any GraphQL/REST surface that exposed it.

- [ ] **Step 1: Read the existing master changelog to confirm path**

Use `Read` on `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml`.

- [ ] **Step 2: Write the migration**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.6.xsd">

    <changeSet author="bytechef" id="2026-05-19-drop-ai-hub-voice-provider">
        <dropColumn tableName="ai_hub_workspace_settings" columnName="voice_provider"/>
    </changeSet>
</databaseChangeLog>
```

(Replace `ai_hub_workspace_settings` with the actual table name. Find it by `grep -rln "voice_provider" server/`.)

- [ ] **Step 3: Include in master changelog**

Add an `<include file="config/liquibase/changelog/2026/2026-05-19-drop-ai-hub-voice-provider.xml"/>` line at the end of the existing 2026 includes (or wherever the existing pattern places monthly groupings).

- [ ] **Step 4: Remove the field from the entity**

In `AiHubWorkspaceSettings.java`: delete the `voiceProvider` field, its accessor methods, any annotations, any constructor argument.

- [ ] **Step 5: Verify compile + DB applies**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-service:testIntegration` (or any integration test that boots Liquibase against PostgreSQL).
Expected: PASS — migration runs cleanly.

- [ ] **Step 6: Commit**

```
194800 Drop ai_hub_workspace_settings.voice_provider column
```

---

### Task 32: Remove voice routing from `AiHubRoutingAgent` and Phase 4 close

**Files:**
- Modify: `server/ee/libs/automation/automation-ai-hub/.../AiHubRoutingAgent.java` (or wherever the routing agent lives — find via `grep -rln "AiHubRoutingAgent" server/ee/`)

- [ ] **Step 1: Read the agent**

Identify any branches that depended on `voiceProvider` workspace state or live audio frames. Push-to-talk transcripts flow through the same text path as typed messages, so the routing agent does not need a voice branch.

- [ ] **Step 2: Remove voice-specific branches**

Delete the code paths. Leave the text-only path intact.

- [ ] **Step 3: Run agent's existing tests**

Run: `./gradlew :server:ee:libs:automation:automation-ai-hub:...:test`
Expected: PASS.

- [ ] **Step 4: Commit**

```
194800 Remove voice routing from AiHubRoutingAgent
```

- [ ] **Step 5: Phase 4 final checks**

```bash
./gradlew check
cd client && npm run check
cd ..
```

Expected: all green.

---

## Final verification

- [ ] **Run the full server check:** `./gradlew check`
- [ ] **Run the full client check:** `cd client && npm run check`
- [ ] **Manual end-to-end:**
  - AI Hub composer: push-to-talk works; no realtime UI present.
  - WorkflowTestChatPanel with a text-trigger workflow: push-to-talk works in composer; no `<VoiceModeLayout>`.
  - WorkflowTestChatPanel with `browser/v1/voiceSession` trigger: `<VoiceModeLayout>` renders, realtime voice connects, VoiceOrb animates listening/speaking, countdown chip updates, disconnect works.
  - SDK widget: same two cases as the test panel.
- [ ] **Spot-check Liquibase ran on a fresh DB:** drop the test DB volume, restart server, verify `voice_provider` column is absent from `ai_hub_workspace_settings`.

---

## Notes for the implementer

- Several earlier tasks tell you to **read** an existing file before editing. Do that — the test panel and SDK widget have their own runtime-wiring patterns (`useChatRuntime` shape, provider hierarchy) that this plan can't predict in full.
- The spec is the source of truth for **intent**; this plan is the source of truth for **sequence**. If a step here contradicts the spec, fix the plan and ask. Don't silently ship something the spec didn't intend.
- **Don't skip commits.** Frequent small commits make it easy to bisect when the integration test inevitably surfaces a wiring mistake.
- After each phase, re-read the **Surface matrix** in the spec to confirm what should and shouldn't work at that point.
- Memory: the project has [feedback_design_then_plan_then_impl.md](feedback_design_then_plan_then_impl.md). This is the impl step. If a design question surfaces mid-implementation, stop, update the spec, then continue.
