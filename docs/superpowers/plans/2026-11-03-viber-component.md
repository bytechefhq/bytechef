# Viber Component Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A `viber` component with New Message and New Event triggers and Send Text Message and Broadcast Text Message actions, plus a backward-compatible SDK addition that lets a webhook validate step see the connection.

**Architecture:** The SDK gains a sub-interface of `WebhookValidateFunction` whose extra `apply` takes connection parameters. The platform detects it at call time, so no getter is added and no existing snapshot changes. The component follows the Telegram layout. Shared trigger logic (signature, welcome body, webhook ownership, event-to-output mapping) lives in two util classes that both triggers call.

**Tech Stack:** Java 25, ByteChef component SDK (`ComponentDsl`), JUnit 5, Mockito, `MockContextSetupExtension`, `JsonFileAssert`.

**Spec:** `docs/superpowers/specs/2026-11-03-viber-component-design.md`

## Global Constraints

- Work in `/Volumes/Data/bytechef/bytechef-worktrees/viber` on branch `viber`. Never touch `/Volumes/Data/bytechef/bytechef-master`.
- Commit messages are one subject line. No body, no `Co-Authored-By`, no "Generated with" line. If the user assigns a ticket number, prefix every subject with it.
- Commit by path (`git add <paths>`), never `git add -A` or `git stash`.
- SDK change is a backward-compatible addition: no existing interface signature changes; existing components compile and behave as before; no existing definition snapshot changes.
- Viber base URL `https://chatapi.viber.com/pa`; auth header `X-Viber-Auth-Token`; signature header `X-Viber-Content-Signature` (hex HMAC-SHA256 of the raw body, keyed with the auth token).
- Viber reports errors as HTTP 200 with non-zero `status` and a `status_message`.
- `sender.name` at most 28 characters; `text` at most 7,000; `tracking_data` at most 4,096; `broadcast_list` 1 to 300 IDs.
- Outgoing messages are plain text (`type: text`) only.
- Test webhook URLs contain `/test/environments/`; deployed ones are `<public-url>/webhooks/{id}`.
- Java style from `CLAUDE.md`: blank line before control statements and after a variable modification that is then used; no trailing blank line in a class body; descriptive names; test methods camelCase with no underscores; no `TODO:` comments; no prose comments.
- Component tests: delete `src/test/resources/definition/viber_v1.json` and `build/resources/test/definition/` before rerunning the handler test whenever the definition changes.
- Before the final commit: `./gradlew spotlessApply` and `./gradlew :server:libs:modules:components:viber:check`.

## Review Focus

1. **Bot name longer than 28 characters as the sender name fallback.** Viber allows bot names up to 75 characters but `sender.name` only 28. Expect truncation to 28, not a Viber error. Test in Task 3.
2. **Missing or differently cased signature header.** Expect HTTP 401, not a NullPointerException. Test in Task 5.
3. **Non-ASCII message text (emoji, Cyrillic).** HMAC must run over the raw body's UTF-8 bytes. Test in Task 5.
4. **Expired or failing media link.** The event still reaches the workflow with `message.media` intact and no `file` key. Test in Task 6.
5. **`set_webhook` rejected (for example a non-HTTPS local URL).** Enable fails with a `ProviderException` naming Viber's `status_message`, not a silent success. Test in Task 5.

---

### Task 1: Backward-compatible SDK addition for connection-aware webhook validation

**Files:**
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/TriggerDefinition.java` (next to `WebhookValidateFunction`, around line 691)
- Modify: `sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java` (next to `webhookValidate(...)`, around line 3796)
- Modify: `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/TriggerDefinitionServiceImpl.java` (`executeWebhookValidate` around 315, `doExecuteWebhookValidate`/`executeWebhookValidate` around 700-720, `validateWebhookRequest` around 735 and its caller around 625)
- Test: `server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/TriggerDefinitionServiceTest.java`

**Interfaces:**
- Produces, in `TriggerDefinition`:
  ```java
  @FunctionalInterface
  interface WebhookValidateWithConnectionFunction extends WebhookValidateFunction {
      WebhookValidateResponse apply(
          Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
          HttpParameters parameters, WebhookBody body, WebhookMethod method, TriggerContext context);

      @Override
      default WebhookValidateResponse apply(
          Parameters inputParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body,
          WebhookMethod method, TriggerContext context) {

          throw new UnsupportedOperationException(
              "This webhook validate function needs connection parameters");
      }
  }
  ```
- Produces, in `ComponentDsl.ModifiableTriggerDefinition`:
  `public ModifiableTriggerDefinition webhookValidateWithConnection(WebhookValidateWithConnectionFunction webhookValidate)`, which stores into the existing `webhookValidateFunction` field. A distinct name, not an overload of `webhookValidate`, so existing method references stay unambiguous.
- No new getter. `getWebhookValidate()` returns the sub-interface instance, so the platform's `workflowSyncValidation` flag and every snapshot stay as they are.

- [ ] **Step 1: Write the failing tests** in `TriggerDefinitionServiceTest`:
  - `testExecuteWebhookValidatePassesConnectionParameters`: register a trigger whose `getWebhookValidate()` returns a `WebhookValidateWithConnectionFunction` that captures its `connectionParameters` and returns `WebhookValidateResponse.unauthorized()`. Call `executeWebhookValidate(...)` with a `ComponentConnection` whose parameters are `Map.of("authToken", "secret")`. Assert the status is 401 and the captured `connectionParameters.getRequiredString("authToken")` equals `"secret"`.
  - `testExecuteWebhookValidateCallsLegacyFunctionUnchanged`: register a plain `WebhookValidateFunction` lambda that records it was called and returns `ok()`. Assert status 200 and that it was called once.
  - `testExecuteTriggerRevalidatesWithConnection`: unvalidated `WebhookRequest` through the trigger execution path with a `WebhookValidateWithConnectionFunction` returning 401. Assert the existing "Invalid trigger signature." failure is raised and the function received the connection parameters.

- [ ] **Step 2: Run them to verify they fail**

  Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test --tests '*TriggerDefinitionServiceTest'`
  Expected: compilation failure on `WebhookValidateWithConnectionFunction`.

- [ ] **Step 3: Add the interface and builder method** exactly as in Interfaces, with Javadoc matching the neighbouring style.

- [ ] **Step 4: Route in `TriggerDefinitionServiceImpl`.** Thread `@Nullable ComponentConnection componentConnection` into the private `executeWebhookValidate` and `validateWebhookRequest`. When the function is an `instanceof WebhookValidateWithConnectionFunction`, call the seven-argument `apply` with `ParametersFactory.create(componentConnection)`; otherwise call the six-argument `apply` exactly as today. `doExecuteTrigger` already has `componentConnection` for the revalidation call.

- [ ] **Step 5: Run the tests and verify they pass**, plus the existing snapshot tests that must not change:

  Run: `./gradlew :server:libs:platform:platform-component:platform-component-service:test :server:libs:modules:components:xero:test :server:libs:modules:components:telegram:test :server:libs:modules:components:webhook:test`
  Expected: PASS, and `git status` shows no changed `*_v1.json`.

- [ ] **Step 6: Commit**

  ```bash
  git add sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/TriggerDefinition.java sdks/backend/java/component-api/src/main/java/com/bytechef/component/definition/ComponentDsl.java server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/service/TriggerDefinitionServiceImpl.java server/libs/platform/platform-component/platform-component-service/src/test/java/com/bytechef/platform/component/service/TriggerDefinitionServiceTest.java
  git commit -m "Let a webhook validate function receive the connection parameters"
  ```

### Task 2: Viber component skeleton and connection

**Files:**
- Create: `server/libs/modules/components/viber/build.gradle.kts` (same content as Telegram's: `version="1.0"`, `implementation("org.slf4j:slf4j-api")`)
- Modify: `settings.gradle.kts` (add `include("server:libs:modules:components:viber")` in alphabetical order, after `vbout`)
- Create: `.../viber/src/main/java/com/bytechef/component/viber/ViberComponentHandler.java`
- Create: `.../viber/connection/ViberConnection.java`
- Create: `.../viber/constant/ViberConstants.java`
- Create: `.../viber/util/ViberUtils.java`
- Create: `.../viber/src/main/resources/assets/viber.svg` (Viber logo as SVG)
- Test: `.../viber/src/test/java/com/bytechef/component/viber/ViberComponentHandlerTest.java`, `.../viber/util/ViberUtilsTest.java`, `.../viber/connection/ViberConnectionTest.java`

All Java files carry the Apache 2.0 header used by Telegram.

**Interfaces:**
- `ViberConstants` string constants: `AUTH_TOKEN = "authToken"`, `RECEIVER = "receiver"`, `RECEIVERS = "receivers"`, `TEXT = "text"`, `SENDER_NAME = "senderName"`, `SENDER_AVATAR = "senderAvatar"`, `TRACKING_DATA = "trackingData"`, `EVENTS = "events"`, `WELCOME_MESSAGE = "welcomeMessage"`, `WELCOME_SENDER_NAME = "welcomeSenderName"`; event values `MESSAGE = "message"`, `CONVERSATION_STARTED = "conversation_started"`, `SUBSCRIBED = "subscribed"`, `UNSUBSCRIBED = "unsubscribed"`, `WEBHOOK = "webhook"`; `SENDER_NAME_MAX_LENGTH = 28`.
- `ViberConnection.CONNECTION_DEFINITION`: `baseUri` `https://chatapi.viber.com/pa`; `authorization(AuthorizationType.CUSTOM)` with required string `AUTH_TOKEN` labelled "Auth Token", and `.apply(...)` returning `ApplyResponse.ofHeaders(Map.of("X-Viber-Auth-Token", List.of(token)))` (pattern: `BaserowConnection`). `.test((connectionParameters, context) -> ViberUtils.getAccountInfo(context))` so a bad token fails the connection test with Viber's message. Help link `https://docs.bytechef.io/reference/components/viber_v1#connection-setup`, version 1.
- `ViberUtils`:
  - `static Map<String, Object> checkStatus(Map<String, Object> response)` returns the response when `status` is 0, otherwise throws `ProviderException` with message `"Viber rejected the request: %s (%d)".formatted(statusMessage, status)`.
  - `static Map<String, Object> getAccountInfo(Context context)` POSTs `/get_account_info` with an empty JSON body, `ResponseType.JSON`, and returns `checkStatus(...)`.
- `ViberComponentHandler`: `component("viber")`, title "Viber", a one-sentence description, icon `path:assets/viber.svg`, category `ComponentCategory.COMMUNICATION`, the connection, `customAction(true)` with help `https://developers.viber.com/docs/api/rest-bot-api/`, version 1. Actions and triggers are added by later tasks.

- [ ] **Step 1: Write the failing tests.** `ViberComponentHandlerTest.testGetComponentDefinition` uses `JsonFileAssert.assertEquals("definition/viber_v1.json", new ViberComponentHandler().getDefinition())`. `ViberUtilsTest`: `testCheckStatusReturnsResponseOnZeroStatus`, `testCheckStatusThrowsWithViberMessage` (input `Map.of("status", 6, "status_message", "receiverNotSubscribed")`, assert message `"Viber rejected the request: receiverNotSubscribed (6)"`), `testGetAccountInfoPostsToAccountInfoEndpoint` (captures `/get_account_info`, JSON response type). `ViberConnectionTest.testConnectionTestFailsOnRejectedToken`: the test consumer throws `ProviderException` when account info returns `{"status": 2, "status_message": "invalidAuthToken"}`.
- [ ] **Step 2: Run to verify failure.** Run: `./gradlew :server:libs:modules:components:viber:test`. Expected: compilation failure.
- [ ] **Step 3: Implement** the files above.
- [ ] **Step 4: Run to verify pass.** Same command. Expected: PASS and `viber_v1.json` generated.
- [ ] **Step 5: Commit** the new component directory and `settings.gradle.kts`: `git commit -m "Add the Viber component with its connection"`.

### Task 3: Send Text Message action

**Files:**
- Create: `.../viber/action/ViberSendTextMessageAction.java`
- Modify: `.../viber/util/ViberUtils.java`, `ViberComponentHandler.java`
- Test: `.../viber/action/ViberSendTextMessageActionTest.java`, `ViberUtilsTest.java`

**Interfaces:**
- Consumes: `ViberUtils.checkStatus`, `ViberUtils.getAccountInfo`, constants from Task 2.
- Produces:
  - `ViberUtils.resolveSenderName(@Nullable String senderName, Context context): String` returns `senderName` when not blank, otherwise the account info `name`; the result is cut to `SENDER_NAME_MAX_LENGTH`.
  - `ViberUtils.buildSender(Parameters inputParameters, String senderNameKey, Context context): Map<String, Object>` returns `{name}` plus `avatar` when `SENDER_AVATAR` is set.
  - `ViberSendTextMessageAction.ACTION_DEFINITION` named `sendTextMessage`, title "Send Text Message". Properties: `RECEIVER` required string "Receiver ID" (description says to use `user.id` from a Viber trigger); `TEXT` required, `maxLength(7000)`, `ControlType.TEXT_AREA`; `SENDER_NAME` optional, `maxLength(28)`; `SENDER_AVATAR` optional; `TRACKING_DATA` optional, `maxLength(4096)`. Output schema `status` (integer), `status_message`, `message_token` (string). `perform(Parameters, Parameters, Context): Map<String, Object>` POSTs `/send_message` with `{receiver, type: "text", sender, text, tracking_data?}` and returns `checkStatus(...)`.
  - Registered in the handler both as an action and as `tool(...)` in `clusterElements`, as Telegram does.

- [ ] **Step 1: Write the failing tests.**
  - `testPerformSendsTextMessage`: assert POST to `/send_message` with body `Map.of("receiver", "user-1", "type", "text", "sender", Map.of("name", "Support"), "text", "Hello")` and that the response map is returned.
  - `testPerformIncludesTrackingData`: body contains `"tracking_data", "order-42"`.
  - `testResolveSenderNameFallsBackToBotName`: blank sender name, account info `name` "Acme Support", result "Acme Support".
  - `testResolveSenderNameTruncatesTo28Characters`: account info name of 40 characters; result length 28 and equals its first 28 characters.
- [ ] **Step 2: Run to verify failure.** `./gradlew :server:libs:modules:components:viber:test`. Expected: compilation failure.
- [ ] **Step 3: Implement** the action and the two util methods.
- [ ] **Step 4: Regenerate the snapshot and run.** Delete `viber_v1.json` and `build/resources/test/definition/`, then the same command. Expected: PASS.
- [ ] **Step 5: Commit**: `git commit -m "Add the Viber Send Text Message action"`.

### Task 4: Broadcast Text Message action

**Files:**
- Create: `.../viber/action/ViberBroadcastTextMessageAction.java`
- Modify: `ViberComponentHandler.java`
- Test: `.../viber/action/ViberBroadcastTextMessageActionTest.java`

**Interfaces:**
- Consumes: `ViberUtils.buildSender`, `checkStatus`.
- Produces: `ViberBroadcastTextMessageAction.ACTION_DEFINITION` named `broadcastTextMessage`, title "Broadcast Text Message". Properties as Task 3 with `RECEIVERS` instead of `RECEIVER`: required `array(RECEIVERS).items(string())`, `minItems(1)`, `maxItems(300)`, label "Receiver IDs". The `TEXT` description lists the placeholders `replace_me_with_receiver_id`, `replace_me_with_url_encoded_receiver_id`, `replace_me_with_user_name`. Output adds `failed_list` (array of objects with `receiver`, `status`, `status_message`). `perform` POSTs `/broadcast_message` with `broadcast_list` in place of `receiver` and throws `IllegalArgumentException` when the list is empty or longer than 300, before any HTTP call.

- [ ] **Step 1: Write the failing tests**: `testPerformBroadcastsToReceivers` (body has `"broadcast_list", List.of("u1", "u2")` and no `receiver`), `testPerformRejectsMoreThan300Receivers` (301 IDs, `IllegalArgumentException`, no HTTP call verified with `verifyNoInteractions`).
- [ ] **Step 2: Run to verify failure.** Expected: compilation failure.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Regenerate snapshot and run.** Expected: PASS.
- [ ] **Step 5: Commit**: `git commit -m "Add the Viber Broadcast Text Message action"`.

### Task 5: Webhook support: signature, welcome body, ownership guard

**Files:**
- Create: `.../viber/util/ViberWebhookUtils.java`
- Test: `.../viber/util/ViberWebhookUtilsTest.java`

**Interfaces:**
- Consumes: `WebhookValidateWithConnectionFunction` (Task 1), `ViberUtils.getAccountInfo`, `checkStatus`, `resolveSenderName`.
- Produces:
  - `static WebhookValidateResponse validate(Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters, WebhookBody body, WebhookMethod method, TriggerContext context)`, usable as a `WebhookValidateWithConnectionFunction` method reference:
    1. Find `X-Viber-Content-Signature` case-insensitively in `headers.toMap()`. Missing returns `WebhookValidateResponse.unauthorized()`.
    2. Compute `HexFormat.of().formatHex(hmacSha256(authToken, rawBody UTF-8 bytes))`; compare with `MessageDigest.isEqual` on the UTF-8 bytes. Mismatch returns `unauthorized()`.
    3. If the body's `event` is `conversation_started` and `WELCOME_MESSAGE` is non-blank, return `WebhookValidateResponse.ok(Map.of("sender", Map.of("name", resolvedName), "type", "text", "text", welcome))`, with `resolvedName` from `resolveSenderName(WELCOME_SENDER_NAME, context)`.
    4. Otherwise `ok()`. A `webhook` handshake event that passes the signature gets plain `ok()`.
    A signature failure answers 401 through the SDK's existing `WebhookValidateResponse.unauthorized()`, not the 403 the spec mentions. Viber treats any non-200 the same way, and 401 matches how Xero rejects bad signatures.
  - `static WebhookValidateResponse validateOnEnable(Parameters, HttpHeaders, HttpParameters, WebhookBody, WebhookMethod, TriggerContext)` always returns `ok()`.
  - `static boolean isTestWebhookUrl(String webhookUrl)` is true when the URL contains `/test/environments/`.
  - `static WebhookEnableOutput enableWebhook(String webhookUrl, List<String> eventTypes, TriggerContext context)`:
    1. `getAccountInfo`; read `webhook` (absent or blank means none).
    2. Allowed when none, equal to `webhookUrl`, or when `webhookUrl` is not a test URL and the current one is a test URL. Otherwise throw `ProviderException("This Viber bot already sends its messages to another workflow (%s). Disable that workflow first, or clear the bot's webhook.".formatted(current))`.
    3. POST `/set_webhook` with `{url: webhookUrl, event_types: eventTypes, send_name: true, send_photo: true}`, `checkStatus`, return `new WebhookEnableOutput(null, null)`.
  - `static void disableWebhook(String webhookUrl, TriggerContext context)`: `getAccountInfo`; only when its `webhook` equals `webhookUrl`, POST `/set_webhook` with `{url: ""}` and `checkStatus`.

- [ ] **Step 1: Write the failing tests.** Compute expected signatures in the test with `javax.crypto.Mac` over a literal body.
  - `testValidateAcceptsValidSignature`, `testValidateRejectsWrongSignature` (401), `testValidateRejectsMissingSignatureHeader` (401), `testValidateMatchesSignatureHeaderCaseInsensitively` (header key `x-viber-content-signature`).
  - `testValidateSignsNonAsciiBody`: body `{"event":"message","message":{"type":"text","text":"Здраво 👋"}}`, correct signature accepted.
  - `testValidateReturnsWelcomeBodyOnConversationStarted`: welcome "Hi there", sender name "Acme"; assert status 200 and body equals `Map.of("sender", Map.of("name", "Acme"), "type", "text", "text", "Hi there")`.
  - `testValidateReturnsPlainOkWithoutWelcomeMessage`.
  - `testEnableWebhookSetsWebhookWhenNoneExists` (asserts `/set_webhook` body), `testEnableWebhookAllowsSameUrl`, `testEnableWebhookRejectsOtherWorkflowUrl` (`ProviderException` message contains the other URL, no `/set_webhook` call), `testEnableWebhookReplacesLeftoverTestUrlOnDeploy`, `testEnableWebhookRejectsDeployedUrlFromTestSession`, `testEnableWebhookFailsWhenViberRejectsUrl` (`set_webhook` returns `{"status": 1, "status_message": "invalidUrl"}`; `ProviderException` with "invalidUrl (1)").
  - `testDisableWebhookClearsOwnUrl`, `testDisableWebhookLeavesOtherUrlAlone` (no `/set_webhook` call).
- [ ] **Step 2: Run to verify failure.** Expected: compilation failure.
- [ ] **Step 3: Implement** `ViberWebhookUtils`. Catch `NoSuchAlgorithmException | InvalidKeyException` around `Mac` and rethrow as `ProviderException`, as `XeroUtils` does.
- [ ] **Step 4: Run to verify pass.**
- [ ] **Step 5: Commit**: `git commit -m "Add the Viber webhook signature, welcome and ownership support"`.

### Task 6: Event-to-output mapping with media download

**Files:**
- Create: `.../viber/util/ViberEventUtils.java`
- Modify: `.../viber/constant/ViberConstants.java` (add `EVENT_OUTPUT_PROPERTIES`)
- Test: `.../viber/util/ViberEventUtilsTest.java`

**Interfaces:**
- Produces:
  - `ViberConstants.EVENT_OUTPUT_PROPERTIES`: the static output schema from the spec's "Trigger output" section: `event`, `timestamp` (integer), `message_token` (integer), `user` object (`id`, `name`, `avatar`, `country`, `language`, `api_version` integer), `message` object (`type`, `text`, `media`, `thumbnail`, `file_name`, `file_size` integer, `duration` integer, `sticker_id` integer, `location` object `lat`/`lon` numbers, `contact` object `name`/`phone_number`/`avatar`, `tracking_data`), `fileEntry("file")`, `type`, `context`, `subscribed` boolean.
  - `static List<Map<String, Object>> toOutput(Map<String, Object> event, Set<String> selectedEvents, TriggerContext context)`:
    - Returns an empty list when `event` is `webhook` or not in `selectedEvents`. The platform creates no job for an empty list from a non-batch trigger.
    - Otherwise a single-element list. `user` is Viber's `sender` for `message`, `user` for `subscribed` and `conversation_started`, and `Map.of("id", user_id)` for `unsubscribed`. Copies `event`, `timestamp`, `message_token`, `message`, and for `conversation_started` also `type`, `context`, `subscribed`.
    - For `message.type` in `picture`, `video`, `file`: GET `message.media` with `Http.responseType(Http.ResponseType.BINARY)` and put the resulting `FileEntry` under `file`. When the download throws, log a warning with SLF4J and leave `file` out.

- [ ] **Step 1: Write the failing tests**: `testToOutputDropsWebhookHandshake`, `testToOutputDropsUnselectedEvent`, `testToOutputMapsTextMessage` (asserts `user.id` from `sender.id` and `message.text`), `testToOutputMapsUnsubscribedUserId`, `testToOutputKeepsConversationStartedFields`, `testToOutputDownloadsPicture` (mocked BINARY GET returns a `FileEntry`, asserted under `file`), `testToOutputKeepsEventWhenMediaDownloadFails` (GET throws; result has `message.media` and no `file`), `testToOutputMapsStickerLocationAndContact`.
- [ ] **Step 2: Run to verify failure.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run to verify pass.**
- [ ] **Step 5: Commit**: `git commit -m "Map Viber callbacks to trigger output and download media"`.

### Task 7: New Message trigger

**Files:**
- Create: `.../viber/trigger/ViberNewMessageTrigger.java`
- Modify: `ViberComponentHandler.java`
- Test: `.../viber/trigger/ViberNewMessageTriggerTest.java`

**Interfaces:**
- Consumes: `ViberWebhookUtils.validate`, `validateOnEnable`, `enableWebhook`, `disableWebhook`; `ViberEventUtils.toOutput`; `EVENT_OUTPUT_PROPERTIES`.
- Produces: `TRIGGER_DEFINITION` named `newMessage`, title "New Message", `TriggerType.DYNAMIC_WEBHOOK`, `webhookRawBody(true)`, no properties, output `outputSchema(object().properties(EVENT_OUTPUT_PROPERTIES))`. Wires `.webhookValidateWithConnection(ViberWebhookUtils::validate)`, `.webhookValidateOnEnable(ViberWebhookUtils::validateOnEnable)`, enable with `List.of()` event types, disable, and request returning `toOutput(body content, Set.of(MESSAGE), context)`. Help link `https://docs.bytechef.io/reference/components/viber_v1#new-message`.

- [ ] **Step 1: Write the failing tests**: `testWebhookEnableRegistersWithNoOptionalEvents` (`event_types` is an empty list), `testWebhookRequestEmitsMessage`, `testWebhookRequestDropsSubscribed`.
- [ ] **Step 2: Run to verify failure.**
- [ ] **Step 3: Implement** and register in the handler.
- [ ] **Step 4: Regenerate snapshot and run.** Expected: PASS.
- [ ] **Step 5: Commit**: `git commit -m "Add the Viber New Message trigger"`.

### Task 8: New Event trigger

**Files:**
- Create: `.../viber/trigger/ViberNewEventTrigger.java`
- Modify: `ViberComponentHandler.java`
- Test: `.../viber/trigger/ViberNewEventTriggerTest.java`

**Interfaces:**
- Produces: `TRIGGER_DEFINITION` named `newEvent`, title "New Event", same wiring as Task 7. Properties:
  - `EVENTS`: required `array(EVENTS).items(string().options(option("New Message", MESSAGE), option("Conversation Started", CONVERSATION_STARTED), option("Subscribed", SUBSCRIBED), option("Unsubscribed", UNSUBSCRIBED)))`, default `MESSAGE` (pattern: `GithubEventsTrigger`).
  - `WELCOME_MESSAGE`: optional string, `ControlType.TEXT_AREA`, `maxLength(7000)`, `displayCondition("contains(events, 'conversation_started')")`. Description states it is sent automatically when a user opens the chat, and not when testing in the editor.
  - `WELCOME_SENDER_NAME`: optional, `maxLength(28)`, same display condition.
- Enable passes `List.of(CONVERSATION_STARTED)` as event types when that event is selected, otherwise `List.of()`. Request passes the selected events to `toOutput`.

- [ ] **Step 1: Write the failing tests**: `testWebhookEnableRequestsConversationStartedWhenSelected`, `testWebhookEnableOmitsConversationStartedWhenNotSelected`, `testWebhookRequestEmitsSelectedEventOnly` (selected `SUBSCRIBED`; a `message` callback yields an empty list, a `subscribed` callback yields one element).
- [ ] **Step 2: Run to verify failure.**
- [ ] **Step 3: Implement** and register.
- [ ] **Step 4: Regenerate snapshot and run.** Expected: PASS.
- [ ] **Step 5: Commit**: `git commit -m "Add the Viber New Event trigger with a welcome message"`.

### Task 9: Documentation and full verification

**Files:**
- Create: `.../viber/src/main/resources/README.mdx`, `connection.mdx`, `newMessage.mdx`, `newEvent.mdx`, `sendTextMessage.mdx`, `broadcastTextMessage.mdx` (pattern: Telegram's resources)
- Generated: `docs/content/docs/reference/components/viber_v1.mdx`

Content the docs must carry:
- Connection setup: where to find the bot's auth token; new bots require Viber commercial terms; the webhook URL must be public HTTPS, so local development needs a tunnel.
- One bot feeds one enabled workflow. Testing in the editor needs a separate bot from any deployed workflow.
- A bot can only message users who have written to it. Use `user.id` from a trigger as the Receiver ID.
- The welcome message is not sent while testing in the editor.

- [ ] **Step 1: Write the docs files.**
- [ ] **Step 2: Generate documentation.** Run: `./gradlew :server:libs:modules:components:viber:generateDocumentation` (fall back to `./gradlew generateDocumentation` if the per-module task does not exist). Expected: `viber_v1.mdx` created.
- [ ] **Step 3: Format and check.** Run: `./gradlew spotlessApply` then `./gradlew :server:libs:modules:components:viber:check :server:libs:platform:platform-component:platform-component-service:check`. Expected: BUILD SUCCESSFUL, with Checkstyle, PMD and SpotBugs clean.
- [ ] **Step 4: Prove the server boots with the component.** Run: `./gradlew :server:apps:server-app:testIntegration`. Expected: PASS.
- [ ] **Step 5: Commit** the component docs and the generated `.mdx` as separate commits (generated files go in their own commit): `git commit -m "Add the Viber component documentation"` and `git commit -m "Generate the Viber component reference page"`.
