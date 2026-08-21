# Subagent Interactive Questions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a specialist subagent ask the user a real multiple-choice question, rendered as option buttons through the existing chat UI instead of paraphrased into prose by the parent agent.

**Architecture:** A specialist calls an `askUserQuestion` tool that writes an `ask-user-question` payload into a thread-bound `SubagentAskChannel` and tells its own LLM to stop. When the delegation returns, the delegate `ToolCallback` returns that payload as *its own* tool result — which places it on the parent's stream, because a delegate tool result is a main-agent tool result. The client renders it with the component it already has, once its dispatch stops keying on the tool name.

**Tech Stack:** Java 25, Spring Boot 4, Spring AI, `org.springaicommunity:spring-ai-agent-utils`, JUnit 5, Mockito, AssertJ; React 19, TypeScript, Vitest.

**Spec:** `docs/superpowers/specs/2026-08-07-subagent-interactive-questions-design.md`

**Hard prerequisite:** `docs/superpowers/plans/2026-08-07-subagent-conversation-memory.md` must be fully executed first. Without specialist memory, a specialist that asks a question and is re-delegated with the answer has forgotten why it asked — buttons on top of amnesia are worse than prose on top of amnesia. Do not start this plan until that one is merged.

## Global Constraints

- Files under `server/ee/` use the **ByteChef Enterprise license header** and a `@version ee` Javadoc tag; Spotless picks the header from that tag's presence in file content, not from the path. Files under `server/libs/` use the Apache 2.0 header and no such tag.
- Unit test classes end in `Test`, integration tests in `IntTest`. Test method names are camelCase with **no underscores** — Checkstyle enforces this on every method in test sources, including private helpers.
- No `TODO:` comments (Checkstyle `TodoComment`).
- One blank line before control statements, except immediately after an opening `{`; one blank line between a variable modification and the next statement using it; no trailing blank line before a class's closing `}`.
- Descriptive names everywhere, including lambda parameters. No `_` prefix on private methods.
- Client: object keys in natural ascending order (`sort-keys`, **not** auto-fixable); named imports sorted alphabetically within `{}`; interfaces end in `I` or `Props`; `twMerge` not `cn()`; Lucide icons imported with the `Icon` suffix.
- Run `./gradlew spotlessApply` before every server commit and `npm run format` before every client commit.
- Never judge a Gradle run piped into `tail`/`grep` — redirect to a file, check `$?` on its own line, then grep `^> Task .* FAILED`.
- Commit messages: `0 <description>` for server changes, `0 client - <description>` for client changes.

---

### Task 1: `SubagentAskChannel`

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent/SubagentAskChannel.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubagentAskChannelTest.java`

**Interfaces:**
- Produces: `SubagentAskChannel.runWithChannel(Supplier<T>) -> T`, `SubagentAskChannel.offer(String payloadJson) -> boolean`, `SubagentAskChannel.pending() -> String` (nullable). Task 3 calls `offer`; Task 4 calls `runWithChannel` and `pending`.

Copy the binding discipline of the existing `com.bytechef.ee.ai.hub.progress.SubagentProgressChannel` — a `ThreadLocal` holder, restored rather than cleared on exit so nesting is LIFO-safe. It differs in holding **at most one** payload: `offer` returns `false` if one is already pending, so Task 3 can turn a second question in one delegation into a tool error.

`runWithChannel` takes a `Supplier` rather than a `Runnable` because the delegate callback needs the wrapped call's return value.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubagentAskChannelTest {

    @Test
    void testOfferedPayloadIsVisibleWithinTheChannel() {
        String result = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskChannel.offer("{\"kind\":\"ask-user-question\"}");

            return SubagentAskChannel.pending();
        });

        assertThat(result).isEqualTo("{\"kind\":\"ask-user-question\"}");
    }

    @Test
    void testSecondOfferInOneDelegationIsRejected() {
        Boolean secondOfferAccepted = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskChannel.offer("{\"kind\":\"ask-user-question\"}");

            return SubagentAskChannel.offer("{\"kind\":\"ask-user-question\"}");
        });

        assertThat(secondOfferAccepted).isFalse();
    }

    @Test
    void testOfferOutsideAChannelIsIgnored() {
        assertThat(SubagentAskChannel.offer("{\"kind\":\"ask-user-question\"}")).isFalse();
        assertThat(SubagentAskChannel.pending()).isNull();
    }

    @Test
    void testNestedChannelsRestoreTheOuterBinding() {
        String outerPending = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskChannel.offer("outer");

            SubagentAskChannel.runWithChannel(() -> {
                SubagentAskChannel.offer("inner");

                return null;
            });

            return SubagentAskChannel.pending();
        });

        assertThat(outerPending).isEqualTo("outer");
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*SubagentAskChannelTest*' > /tmp/red1.log 2>&1; echo $?`

Expected: non-zero — the class does not exist.

- [ ] **Step 3: Implement**

```java
public final class SubagentAskChannel {

    private static final ThreadLocal<String[]> HOLDER = new ThreadLocal<>();

    private SubagentAskChannel() {
    }

    public static <T> T runWithChannel(Supplier<T> supplier) {
        String[] previous = HOLDER.get();

        HOLDER.set(new String[1]);

        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                HOLDER.remove();
            } else {
                HOLDER.set(previous);
            }
        }
    }

    public static boolean offer(String payloadJson) {
        String[] slot = HOLDER.get();

        if (slot == null || slot[0] != null) {
            return false;
        }

        slot[0] = payloadJson;

        return true;
    }

    public static @Nullable String pending() {
        String[] slot = HOLDER.get();

        return slot == null ? null : slot[0];
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*SubagentAskChannelTest*' > /tmp/green1.log 2>&1; echo $?`

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent/SubagentAskChannel.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubagentAskChannelTest.java
git commit -m "0 Add a thread-bound channel for subagent questions"
```

---

### Task 2: Publish the ask-user-question payload contract

**Files:**
- Modify: `server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/AskUserQuestionToolCallback.java`
- Test: `server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/AskUserQuestionToolCallbackTest.java`

**Interfaces:**
- Produces: `AskUserQuestionToolCallback.KIND` and `AskUserQuestionToolCallback.serialiseQuestions(List<AskUserQuestionTool.Question>) -> List<Map<String, Object>>`, both widened from package-private/private to `public static`. Task 3 uses them.

The subagent tool must emit a byte-identical payload to the main agent's, because both feed the same client renderer. Reusing this method is what guarantees that; a second private copy in the EE module would drift the first time either side changed a field name.

This is CE code — Apache header, no `@version ee` tag.

- [ ] **Step 1: Write the failing test**

```java
    @Test
    void testSerialiseQuestionsIsReusableAndCarriesEveryRenderedField() {
        List<Map<String, Object>> serialised = AskUserQuestionToolCallback.serialiseQuestions(
            List.of(new AskUserQuestionTool.Question(
                "Which agent?", "Agent", false,
                List.of(new AskUserQuestionTool.Option("Support", "the support agent")))));

        assertThat(serialised).hasSize(1);
        assertThat(serialised.get(0)).containsKeys("question", "header", "multiSelect", "options");
    }
```

Construct `AskUserQuestionTool.Question` / `Option` with whatever constructor or builder the version on the classpath actually exposes — read `AskUserQuestionTool` before writing this, and match it rather than assuming the argument order above.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test --tests '*AskUserQuestionToolCallbackTest*' > /tmp/red2.log 2>&1; echo $?`

Expected: non-zero — `serialiseQuestions` is not accessible.

- [ ] **Step 3: Widen the two members**

Change `static final String KIND` to `public static final String KIND` and `private static List<Map<String, Object>> serialiseQuestions(...)` to `public static List<Map<String, Object>> serialiseQuestions(...)`. Add a Javadoc line on each recording that they are the shared wire contract consumed by the EE subagent ask tool, so a future reader does not narrow them again.

Change nothing else in this class.

- [ ] **Step 4: Run to verify it passes, and that nothing else broke**

Run:
```bash
./gradlew :server:libs:ai:ai-copilot:ai-copilot-tool:test > /tmp/green2.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/green2.log
```

Expected: exit 0, no FAILED tasks.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/ai/ai-copilot/ai-copilot-tool/src/main/java/com/bytechef/ai/copilot/tool/AskUserQuestionToolCallback.java \
        server/libs/ai/ai-copilot/ai-copilot-tool/src/test/java/com/bytechef/ai/copilot/tool/AskUserQuestionToolCallbackTest.java
git commit -m "0 Publish the ask-user-question payload contract for reuse"
```

---

### Task 3: The specialist-facing ask tool

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent/SubagentAskUserQuestionToolCallback.java`
- Test: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubagentAskUserQuestionToolCallbackTest.java`

**Interfaces:**
- Consumes: `SubagentAskChannel.offer(String)` (Task 1); `AskUserQuestionToolCallback.KIND` and `serialiseQuestions(...)` (Task 2).
- Produces: `new SubagentAskUserQuestionToolCallback()` — a `ToolCallback` named `askUserQuestion`. Task 4 registers it on specialist `ChatClient`s.

Mirror the structure of `AskUserQuestionToolCallback`: wrap `org.springaicommunity.agent.tools.AskUserQuestionTool` via `ToolCallbacks`, capture the questions the LLM supplied, and build the `{kind, questions}` envelope with the now-public `serialiseQuestions`.

The one behavioural difference is the return value. The main agent returns the envelope, because the client renders the main agent's tool result. This one writes the envelope to the channel and returns a **stop instruction** to its own LLM:

```java
    private static final String STOP_INSTRUCTION =
        "Question posed to the user. Stop now and return a one-line summary of what you asked. " +
            "You will be re-invoked with the user's answer and your prior context intact.";
```

Returning the envelope here instead would be a real bug, not a style choice: the specialist's LLM would read its own question back as though it were the answer and invent the user's decision.

- [ ] **Step 1: Write the failing tests**

```java
    @Test
    void testCallWritesTheEnvelopeToTheChannelAndReturnsTheStopInstruction() {
        String result = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();

            String returned = toolCallback.call(VALID_TOOL_INPUT, null);

            assertThat(SubagentAskChannel.pending()).contains("\"kind\":\"ask-user-question\"");

            return returned;
        });

        assertThat(result).contains("Stop now");
        assertThat(result).doesNotContain("ask-user-question");
    }

    @Test
    void testSecondQuestionInOneDelegationReturnsAToolError() {
        String result = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskUserQuestionToolCallback toolCallback = new SubagentAskUserQuestionToolCallback();

            toolCallback.call(VALID_TOOL_INPUT, null);

            return toolCallback.call(VALID_TOOL_INPUT, null);
        });

        assertThat(result).contains("error");
    }
```

`VALID_TOOL_INPUT` is a JSON string in the shape `AskUserQuestionTool` expects — copy one from the existing `AskUserQuestionToolCallback` tests rather than inventing it.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*SubagentAskUserQuestionToolCallbackTest*' > /tmp/red3.log 2>&1; echo $?`

Expected: non-zero — the class does not exist.

- [ ] **Step 3: Implement**

Build the envelope exactly as the CE callback does:

```java
            Map<String, Object> envelope = new LinkedHashMap<>();

            envelope.put("kind", AskUserQuestionToolCallback.KIND);
            envelope.put("questions", AskUserQuestionToolCallback.serialiseQuestions(questions));

            boolean accepted = SubagentAskChannel.offer(JsonUtils.write(envelope));

            if (!accepted) {
                return ToolErrors.toolError(
                    jsonMapper, "A question is already pending for this delegation — ask one question, stop, and " +
                        "continue after the answer.");
            }

            return STOP_INSTRUCTION;
```

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*SubagentAskUserQuestionToolCallbackTest*' > /tmp/green3.log 2>&1; echo $?`

Expected: exit 0.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/subagent/SubagentAskUserQuestionToolCallback.java \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubagentAskUserQuestionToolCallbackTest.java
git commit -m "0 Add the specialist-facing askUserQuestion tool"
```

---

### Task 4: Relay the question out of the delegation

**Files:**
- Modify: `server/libs/automation/automation-ai/automation-ai-tool/src/main/java/com/bytechef/automation/ai/tool/ManagerSubAgentToolCallback.java`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/PersonalAgentManagerConfiguration.java`
- Test: `server/libs/automation/automation-ai/automation-ai-tool/src/test/java/com/bytechef/automation/ai/tool/ManagerSubAgentToolCallbackTest.java`

**Interfaces:**
- Consumes: `SubagentAskChannel` (Task 1), `SubagentAskUserQuestionToolCallback` (Task 3).

`ManagerSubAgentToolCallback` lives in CE `automation-ai-tool` while `SubagentAskChannel` is EE. Rather than move either, introduce a tiny CE seam so the callback stays edition-clean:

```java
package com.bytechef.automation.ai.tool;

/** Carries a question raised inside a subagent delegation back to the delegate callback. */
public interface SubAgentAskRelay {

    <T> T runWithChannel(java.util.function.Supplier<T> supplier);

    @Nullable
    String pending();
}
```

`ManagerSubAgentToolCallback` takes an optional `SubAgentAskRelay` (null = today's behavior). The EE side supplies an implementation delegating to `SubagentAskChannel`.

The casing difference between `SubAgentAskRelay` and `SubagentAskChannel` is deliberate, not a slip: each matches its own module's existing convention — `ManagerSubAgentToolCallback` and `SubAgentGuardrailedChatClient` spell it `SubAgent`, while `SubagentProgressChannel` and `SubagentProgressEmitter` spell it `Subagent`. The codebase is already split on this. Match the neighbours rather than unifying, and do not "fix" one to match the other in this change.

Start with `personal_agent_manager` only. It is the specialist whose prompt already tells it to hand questions back, so it is where the change is observable, and a single wiring proves the path before it is repeated.

- [ ] **Step 1: Write the failing test**

Stub the relay rather than reaching for the EE channel — this is CE code and must test without it. A hand-written stub is clearer than a mock here because `runWithChannel` has to actually invoke its supplier.

```java
    private static final String ASK_PAYLOAD = "{\"kind\":\"ask-user-question\",\"questions\":[]}";

    private static final class StubAskRelay implements SubAgentAskRelay {

        private final @Nullable String pendingPayload;

        private StubAskRelay(@Nullable String pendingPayload) {
            this.pendingPayload = pendingPayload;
        }

        @Override
        public <T> T runWithChannel(Supplier<T> supplier) {
            return supplier.get();
        }

        @Override
        public @Nullable String pending() {
            return pendingPayload;
        }
    }

    @Test
    void testPendingQuestionIsReturnedInsteadOfTheSpecialistSummary() {
        ManagerSubAgentToolCallback toolCallback = newToolCallback(new StubAskRelay(ASK_PAYLOAD));

        String result = toolCallback.call("{\"request\":\"create an agent\"}", null);

        assertThat(result).isEqualTo(ASK_PAYLOAD);
    }

    @Test
    void testSpecialistSummaryIsReturnedWhenNoQuestionWasRaised() {
        ManagerSubAgentToolCallback toolCallback = newToolCallback(new StubAskRelay(null));

        String result = toolCallback.call("{\"request\":\"list agents\"}", null);

        assertThat(result).isEqualTo(STUB_SPECIALIST_SUMMARY);
    }

    @Test
    void testNullRelayKeepsTodaysBehaviour() {
        ManagerSubAgentToolCallback toolCallback = newToolCallback(null);

        String result = toolCallback.call("{\"request\":\"list agents\"}", null);

        assertThat(result).isEqualTo(STUB_SPECIALIST_SUMMARY);
    }
```

Build `newToolCallback(...)` on the existing `ChatClient` stubbing already used in this test class — read how the current tests fake `chatClient.prompt(...).call().content()` and reuse it, returning `STUB_SPECIALIST_SUMMARY`. The third test is the regression guard for every delegate that is not wired in this task.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test --tests '*ManagerSubAgentToolCallbackTest*' > /tmp/red4.log 2>&1; echo $?`

Expected: non-zero.

- [ ] **Step 3: Wrap the delegated call and prefer a pending question**

In `call(...)`, wrap the existing `CurrentAgentContext.callWith(...)` invocation so the channel is bound around it, then check for a pending question before returning:

```java
            String response = askRelay == null
                ? invokeSpecialist(request, forwardedContext, parentAgent)
                : askRelay.runWithChannel(() -> invokeSpecialist(request, forwardedContext, parentAgent));

            if (askRelay != null) {
                String pendingQuestion = askRelay.pending();

                if (pendingQuestion != null) {
                    // The specialist's own summary is discarded here by design: it is a one-line "I asked the user
                    // something", and rendering it next to the question card would restate the question in prose.
                    return pendingQuestion;
                }
            }
```

`askRelay.pending()` must be read **inside** nothing — call it after `runWithChannel` returns, using the value the relay captured before unbinding. Implement the EE relay so `runWithChannel` stashes the pending payload before the `finally` restores the previous binding, and `pending()` returns that stash.

- [ ] **Step 4: Register the ask tool on the specialist ChatClient**

In `PersonalAgentManagerConfiguration.personalAgentManagerChatClient`, add `new SubagentAskUserQuestionToolCallback()` to the existing `.defaultTools(...)` list, and pass the EE relay into `createPersonalAgentManagerToolCallback`.

- [ ] **Step 5: Run the affected suites**

Run:
```bash
./gradlew :server:libs:automation:automation-ai:automation-ai-tool:test \
          :server:ee:libs:ai:ai-hub:ai-hub-service:test > /tmp/green4.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/green4.log
```

Expected: exit 0, no FAILED tasks. `PersonalAgentManagerConfigurationTest` must still pass unmodified.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/automation/automation-ai/automation-ai-tool/src \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/java/com/bytechef/ee/ai/hub/config/PersonalAgentManagerConfiguration.java
git commit -m "0 Relay a specialist's question to the parent as its tool result"
```

---

### Task 5: Client — dispatch on payload kind

**Files:**
- Modify: `client/src/shared/components/ai-chat/messages/toToolResultDataPart.ts`
- Test: `client/src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts`

**Interfaces:**
- Consumes: the payload produced by Task 4.

`toToolResultDataPart(toolCallName, eventContent)` dispatches through five `if (toolCallName === …)` branches and returns `undefined` when none match. A delegate tool is named `personal_agent_manager`, so the ask payload currently falls straight through. This is the single client-side blocker.

Add a fallback **after** the existing branches: parse once, and if the payload carries a `kind` this file already knows, route to the same result the tool-name branch would have produced. Scope it to known kinds — a blanket parse-and-sniff would attempt JSON on every tool result in the system and let an unrelated tool emitting a `kind` field hijack a renderer.

- [ ] **Step 1: Write the failing tests**

```ts
describe('toToolResultDataPart payload-kind fallback', () => {
    it('renders an ask-user-question payload returned by a delegate tool', () => {
        const result = toToolResultDataPart(
            'personal_agent_manager',
            JSON.stringify({
                kind: 'ask-user-question',
                questions: [{multiSelect: false, options: [{label: 'Support'}], question: 'Which agent?'}],
            })
        );

        expect(result).toMatchObject({ok: true, type: 'data-ask-user-question'});
    });

    it('ignores an unknown kind', () => {
        const result = toToolResultDataPart('personal_agent_manager', JSON.stringify({kind: 'something-else'}));

        expect(result).toBeUndefined();
    });

    it('ignores a non-JSON result without throwing', () => {
        expect(() => toToolResultDataPart('personal_agent_manager', 'Created agent 7.')).not.toThrow();
        expect(toToolResultDataPart('personal_agent_manager', 'Created agent 7.')).toBeUndefined();
    });
});
```

- [ ] **Step 2: Run to verify they fail**

Run: `cd client && npx vitest run src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts`

Expected: the first test fails with `undefined`.

- [ ] **Step 3: Implement the fallback**

Extract the body of the existing `askUserQuestion` branch into a local helper so both entry points build the identical result object, then add before the final `return undefined;`:

```ts
    const KNOWN_PAYLOAD_KINDS = new Set(['ask-user-question']);

    const fallbackParsed = parseJson<{kind?: string}>(eventContent, `${toolCallName} result`);

    if (fallbackParsed && fallbackParsed.kind && KNOWN_PAYLOAD_KINDS.has(fallbackParsed.kind)) {
        return toAskUserQuestionDataPart(eventContent);
    }

    return undefined;
```

`parseJson` already returns `null` rather than throwing on malformed input, so the non-JSON case needs no extra guard.

Note the `sort-keys` rule is **not** auto-fixable — order object keys alphabetically by hand in both the test fixtures and any object literal added here.

- [ ] **Step 4: Run to verify they pass**

Run: `cd client && npx vitest run src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts`

Expected: all pass.

- [ ] **Step 5: Full client check**

```bash
cd client
npm run format
npm run check
```

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
git add client/src/shared/components/ai-chat/messages/toToolResultDataPart.ts \
        client/src/shared/components/ai-chat/messages/tests/toToolResultDataPart.test.ts
git commit -m "0 client - Render tool results by payload kind, not tool name"
```

---

### Task 6: Prompts

**Files:**
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_personal_agent_manager.txt`
- Modify: `server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt`

The feature does not work without this. The specialist prompt currently instructs the opposite of the new behavior, and the parent will paraphrase a question the user can already see.

- [ ] **Step 1: Update the specialist prompt**

In the Policy section of `prompt_personal_agent_manager.txt`, replace:

> - If the request is ambiguous (which agent, what the instructions should say),
>   return the concrete question instead of guessing.

with:

> - If the request is ambiguous (which agent, what the instructions should say),
>   call askUserQuestion with 2-4 concrete options, then stop and return a
>   one-line summary of what you asked. Do not guess, and do not ask more than
>   one question per turn — you will be re-invoked with the answer and your
>   earlier context intact.

Add `askUserQuestion` to the tool list at the top of the same file, matching the existing entry style.

- [ ] **Step 2: Update the parent prompt**

In the `personal_agent_manager` section of `prompt_ai_hub_build.txt`, add:

> When a specialist returns an ask-user-question payload, the user is already
> looking at the question — do not repeat, paraphrase, or answer it yourself.
> End your turn. On the next turn, delegate again with the user's answer.

- [ ] **Step 3: Verify the prompts still load**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test > /tmp/green6.log 2>&1; echo $?`

Expected: exit 0. `PersonalAgentManagerConfigurationTest` reads the prompt resource at bean construction and fails loudly if it is unreadable.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_personal_agent_manager.txt \
        server/ee/libs/ai/ai-hub/ai-hub-service/src/main/resources/prompt_ai_hub_build.txt
git commit -m "0 Teach the personal agent manager to ask instead of hand back"
```

---

### Task 7: End-to-end pinning

**Files:**
- Create: `server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubagentAskIntTest.java`

Drives a stub specialist `ChatClient` that calls the ask tool, through the real `ManagerSubAgentToolCallback` and relay.

- [ ] **Step 1: Write the test**

Two assertions, one per direction:

```java
    @Test
    void testDelegateReturnsTheQuestionPayloadWhenTheSpecialistAsks() {
        String result = managerSubAgentToolCallback.call("{\"request\":\"create an agent\"}", null);

        assertThat(result).contains("\"kind\":\"ask-user-question\"");
    }

    @Test
    void testChannelIsUnboundAfterTheDelegationSoTheNextTurnStartsClean() {
        managerSubAgentToolCallback.call("{\"request\":\"create an agent\"}", null);

        assertThat(SubagentAskChannel.pending()).isNull();
    }
```

The second matters more than it looks: a leaked `ThreadLocal` on a pooled tool-execution thread would make an unrelated later delegation return a stale question card.

- [ ] **Step 2: Run to verify they pass**

Run: `./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:test --tests '*SubagentAskIntTest*' > /tmp/green7.log 2>&1; echo $?`

Expected: exit 0.

- [ ] **Step 3: Full verification**

```bash
./gradlew spotlessApply
./gradlew :server:ee:libs:ai:ai-hub:ai-hub-service:check \
          :server:libs:automation:automation-ai:automation-ai-tool:check \
          :server:libs:ai:ai-copilot:ai-copilot-tool:check --continue > /tmp/final.log 2>&1
echo $?
grep -E "^> Task .* FAILED" /tmp/final.log
cd client && npm run check
```

Expected: exit 0 for both, no FAILED tasks.

- [ ] **Step 4: Commit**

```bash
git add server/ee/libs/ai/ai-hub/ai-hub-service/src/test/java/com/bytechef/ee/ai/hub/subagent/SubagentAskIntTest.java
git commit -m "0 Pin the subagent question relay end to end"
```

---

## Rollout to the other specialists

Task 4 wires `personal_agent_manager` only. Once the path is proven in use, repeat steps 4.4 and 6.1 for each remaining specialist: register `SubagentAskUserQuestionToolCallback` on its `ChatClient`, pass the relay into its delegate callback, and update its prompt. No further changes to the channel, the tool, the relay, or the client are needed — that is the point of doing one first.

## Out of scope

- Two questions in one delegation. The channel rejects the second by design.
- Enforcing that a specialist stops after asking; the prompt instructs it, nothing gates it. Worst case is wasted work before the question surfaces.
- The MCP manager surface, which has no chat client to render a question.
