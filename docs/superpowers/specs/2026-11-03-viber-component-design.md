# Viber component: receive, respond and send text messages

Viber REST Bot API reference: https://developers.viber.com/docs/api/rest-bot-api/

## Intent

ByteChef has no Viber component on any branch. The user wants a Viber bot that a ByteChef workflow can listen to and
talk through. The model use case is a support-style bot. A Viber user writes to the bot, a workflow picks the message
up, decides an answer (possibly with an AI agent), and replies to that user. A user who opens the chat for the first
time gets a welcome message. Other workflows push text messages to users on their own, one user at a time or many at
once, for example order or status updates.

## Goals

1. **New Message trigger.** Fires when a user sends the bot any kind of message.
2. **New Event trigger.** Fires on the Viber events the user picks: new message, conversation started (the user opened
   the chat), subscribed, unsubscribed. It can answer a conversation-started event with a fixed welcome text.
3. **Full support for incoming message types.** Text, picture, video, file, sticker, location, contact and URL all reach
   the workflow with their data. Pictures, videos and files are also downloaded into a ByteChef file.
4. **Send Text Message action.** Sends plain text to one user. Responding to a user is this same action pointed at the
   sender's ID, since Viber has no separate reply call.
5. **Broadcast Text Message action.** Sends one plain text to up to 300 users in a single call.
6. **Webhook ownership guard.** A trigger refuses to take over a bot whose webhook already points at a different
   workflow, and only clears the webhook on disable if it still points at itself.
7. **Signature verification.** Every incoming call is checked against Viber's signature before a workflow runs on it.

## Non-goals

- **Rich outgoing messages.** No pictures, videos, files, stickers, locations, contacts, URLs, rich media, carousels
  or keyboards in this version. Plain text only, decided by the user.
- **Workflow-composed welcome message.** The welcome text is fixed in the trigger's settings. The user confirmed this
  is enough.
- **User lookup action** (`get_user_details`). Viber allows only two lookups per user every 12 hours. Dropped with the
  user's agreement.
- **Online status and account info actions** (`get_online`, `get_account_info`). Not asked for.
- **Viber as an approval channel** (the `APPROVAL_CHANNELS` cluster element that Telegram, WhatsApp and others have).
  Not asked for.
- **Sharing one bot between several listening workflows.** Viber allows one webhook per bot and ByteChef has no
  mechanism to fan one provider webhook out to several workflows. The user accepted one listening workflow per bot.
- **Delivered, seen and failed events.** Not offered on New Event. They report on outgoing messages, not on users.

## Anti-goals (failure even if it "works")

- Enabling a second workflow on the same bot silently steals the first workflow's messages.
- Disabling one workflow silently cuts off another workflow that owns the bot's webhook.
- A forged call to the webhook URL starts a workflow.
- A media message reaches the workflow with nothing usable in it.
- A trigger that never fires because enabling it failed without a clear error.

## Constraints from Viber

- **Base URL** `https://chatapi.viber.com/pa/`. Every request carries the bot token in the `X-Viber-Auth-Token` header.
  Errors come back as HTTP 200 with a non-zero `status` and a `status_message` in the JSON body.
- **One webhook per bot.** `set_webhook` takes `url`, optional `event_types`, `send_name`, `send_photo`. Posting an
  empty `url` removes the webhook. `message`, `subscribed` and `unsubscribed` are always delivered whatever
  `event_types` says. `conversation_started`, `delivered`, `seen` and `failed` can be filtered out.
- **Handshake.** During `set_webhook`, Viber POSTs a `webhook` event to the URL and expects HTTP 200, otherwise
  `set_webhook` fails with status 1 (`invalidUrl`). The URL must be HTTPS with a trusted certificate.
- **Signature.** Every callback carries `X-Viber-Content-Signature`, a hex-encoded HMAC-SHA256 of the raw body keyed
  with the bot token.
- **Retries.** A callback that doesn't get HTTP 200 is retried up to 10 times, at growing intervals up to 900 seconds.
- **Who can be messaged.** Only users subscribed to the bot. A user subscribes by sending the bot a message. The one
  exception is the welcome message: the reply to a `conversation_started` callback, returned as the HTTP response body
  of that callback, in the same shape as a `send_message` request without `receiver`.
- **send_message.** Required `receiver`, `type` (`text` here), `sender.name` (at most 28 characters), `text` (at most
  7,000 characters). Optional `sender.avatar`, `tracking_data` (at most 4,096 characters, echoed back on the user's
  reply), `min_api_version`. The response carries `message_token`.
- **broadcast_message.** Same fields plus `broadcast_list` of up to 300 subscribed user IDs, instead of `receiver`.
  Placeholders `replace_me_with_receiver_id`, `replace_me_with_url_encoded_receiver_id` and `replace_me_with_user_name`
  are substituted per receiver. The response carries `failed_list` with per-receiver status. 500 requests per 10
  seconds.
- **get_account_info** returns, among other fields, the bot `name` and the current `webhook` URL.
- **Commercial terms.** Since 2024-02-05 new Viber bots are only created on commercial terms. Testing needs an existing
  bot or that agreement.

## Decided with the user

- One Viber bot feeds one listening workflow.
- Two triggers, New Message and New Event, rather than one trigger per event type or a single combined trigger.
- Plain-text replies only in this version.
- Incoming pictures, stickers and every other message type are fully supported.
- The workflow reacts to conversation started, subscribed and unsubscribed through New Event.
- The welcome message is a fixed text set on the trigger.
- Actions are Send Text Message and Broadcast Text Message. No user lookup.
- Testing in the editor is blocked on a bot a deployed workflow uses. People test with a separate test bot.
- The component SDK may change to close the signature gap, but only as a backward-compatible addition. Existing
  components, their webhook validate functions, and anything compiled against the current SDK keep working unchanged.

## Design

### Component

`viber`, version 1, in `server/libs/modules/components/viber`, following the Telegram component's layout
(`ViberComponentHandler`, `connection/`, `action/`, `trigger/`, `constant/`, `util/`). Categories: communication.
Registered in `settings.gradle.kts`, with an icon, `README.md` and generated documentation.

### Connection

Custom authorization with one required secret property, **Auth Token**. `baseUri` is
`https://chatapi.viber.com/pa`. The token goes into the `X-Viber-Auth-Token` header on every request. A connection
test calls `get_account_info` and fails on a non-zero `status`.

### Error handling

Every Viber call checks the JSON `status`. A non-zero status throws a `ProviderException` that names the
`status_message` and the numeric status, for example "Viber rejected the request: receiverNotSubscribed (6)". This
applies to actions and to `set_webhook` during enable.

### Send Text Message action

Inputs:
- **Receiver ID**, required. Typically `sender.id` from the trigger output.
- **Text**, required, at most 7,000 characters.
- **Sender Name**, optional, at most 28 characters. When empty, the bot's own name from `get_account_info` is used.
  *Builder's call, see below.*
- **Sender Avatar URL**, optional.
- **Tracking Data**, optional. Comes back on the user's next message so a workflow can keep conversation state.

Output: `status`, `status_message`, `message_token`.

### Broadcast Text Message action

Same inputs as Send Text Message, with **Receiver IDs** (a list, 1 to 300 entries) instead of Receiver ID. The
property description mentions the three placeholders. Output: `status`, `status_message`, `message_token`,
`failed_list`.

### New Message trigger

Dynamic webhook. No inputs.
- **Enable** registers the webhook with `event_types` empty, which still delivers `message`, `subscribed` and
  `unsubscribed`.
- **Request** runs the workflow only for `message` events. `subscribed` and `unsubscribed` are acknowledged with HTTP
  200 and produce no run.

### New Event trigger

Dynamic webhook. Inputs:
- **Events**, a required multi-select of New Message, Conversation Started, Subscribed, Unsubscribed. Default New
  Message.
- **Welcome Message**, optional text, shown only when Conversation Started is selected. Plus optional Welcome Sender
  Name, following the same fallback as the send action.

Behaviour:
- **Enable** sends `conversation_started` in `event_types` only when that event is selected.
- **Request** runs the workflow only for the selected events. Unselected `message`, `subscribed` and `unsubscribed`
  events are acknowledged with HTTP 200 and produce no run.
- **Conversation started with a welcome message.** The HTTP response to Viber carries the welcome message as a
  `send_message`-shaped body without `receiver`. The workflow still runs for the event. The platform already does this:
  `WebhookWorkflowExecutorImpl.validateAndExecuteAsync` returns the validate step's body to the caller and starts the
  workflow when its status is 200.

### Trigger output

Both triggers emit the same shape, one object per event:
- `event`: `message`, `conversation_started`, `subscribed` or `unsubscribed`.
- `timestamp`, `message_token`.
- `user`: `id`, `name`, `avatar`, `country`, `language`, `api_version`. For `message` events this is Viber's `sender`.
  For `unsubscribed` only `id` is present. Normalising both callbacks to `user` is the builder's call; keeping Viber's
  original field names is acceptable if documented.
- `message` (message events only): `type`, `text`, `media`, `thumbnail`, `file_name`, `file_size`, `duration`,
  `sticker_id`, `location` (`lat`, `lon`), `contact` (`name`, `phone_number`, `avatar`), `tracking_data`.
- `file` (picture, video and file messages only): the media downloaded and stored as a ByteChef file, so later steps
  can save it or hand it to an AI agent. Viber media links can expire, which is why the download happens in the
  trigger.
- `conversation_started` only: `type`, `context`, `subscribed`.

The output schema is declared statically so the editor shows the fields without a test run.

### Webhook handshake and ownership guard

- **Handshake.** Viber calls the URL with a `webhook` event while `set_webhook` is still running. Which trigger step
  receives it depends on the workflow's state. `WebhookTriggerController` routes a call to `webhookValidateOnEnable`
  only on HEAD or while the workflow is disabled. A first deploy enables triggers before the workflow, so the
  handshake lands there. Redeploying an already-enabled workflow swaps triggers while the workflow stays enabled
  (`ProjectDeploymentFacadeImpl`, the old-triggers-off, new-triggers-on block), so the handshake takes the normal path
  through `webhookValidate` and `webhookRequest`. Therefore:
  - `webhookValidateOnEnable` answers HTTP 200, as `MondayNewItemInBoardTrigger` does.
  - `webhookValidate` also answers a `webhook` event with HTTP 200, after the signature check if the check lives there.
  - `webhookRequest` drops `webhook` events, so a redeploy never produces a run for the handshake.
- **Enable guard.** Before `set_webhook`, call `get_account_info`. If its `webhook` is non-empty and not equal to this
  trigger's webhook URL, fail the enable with a clear message: the bot is already used by another workflow, disable that
  one first. Re-enabling the same workflow (same URL) passes.
- **Disable guard.** On disable, call `get_account_info`. Clear the webhook (empty `url`) only if it still equals this
  trigger's URL. Otherwise do nothing.
- **Editor test listener, decided with the user.** The editor's Test button registers the test endpoint's URL, not the
  deployment URL. A test enable is blocked by the guard like any other, so people test on a bot no deployed workflow
  uses. One exception, approved by the user: a deployment enable may replace a webhook that points at ByteChef's own test endpoint, since a test
  session that never cleaned up (a closed browser tab) would otherwise block every later deploy. The builder tells the
  two apart by the URL path the platform generates for test versus deployed webhooks.
- **Known gap, my call.** A webhook set outside ByteChef, or by an old workflow whose URL changed, also blocks
  enable. The error message tells the user to clear it, for example with a `set_webhook` call with an empty URL. A
  "take over anyway" switch is out of scope.

### Signature verification

Every callback is checked: hex HMAC-SHA256 of the raw body with the auth token must equal
`X-Viber-Content-Signature`, compared in constant time. A mismatch must not start a workflow.

**Platform gap.** `TriggerDefinition.WebhookValidateFunction` receives input parameters, headers, body and context, but
not the connection, so it cannot read the auth token. Xero gets away with it because its key is a trigger input.
**Decided with the user: option 1, the SDK change, ships with this component**, so forged calls are rejected outright.
It must be a backward-compatible addition: no existing interface signature changes, and existing components compile
and behave as before. If that can't be done, use option 2 instead.

1. **Additive SDK change.** Give `WebhookValidateFunction` (and `WebhookValidateOnEnableFunction`) access to the
   connection parameters, for example through a new overload or a new functional interface, without breaking existing
   components. `TriggerDefinitionFacadeImpl.executeWebhookValidate` already resolves the `ComponentConnection`, so the
   value is available one layer down. The check then lives in the validate step and answers HTTP 403 on a mismatch.
2. **Check in the request step.** `WebhookRequestFunction` does receive connection parameters. Verify there and return
   an empty list on a mismatch. For a non-batch trigger, `TriggerCompletionHandler` creates one job per list element,
   so an empty list creates no job. Returning `null` does not work: it creates a job with an empty output.
   `WebhookBody.getRawContent()` is available in the request step and survives the broker hop. The cost: a forged call
   still gets HTTP 200, still receives the welcome body, and still leaves a trigger-execution row.

The same empty-list return is how both triggers drop unselected events and `webhook` handshakes.

Copying the token into a trigger input is rejected: users would paste a secret twice.

### Welcome message and the validate step

If the signature check lives in the validate step (option 1), the same step returns the welcome body for a
`conversation_started` event when the trigger has a welcome text. The welcome body needs the sender name. When
Welcome Sender Name is empty, the bot name comes from `get_account_info`. That works under either option: the validate
step's `TriggerContext` is built with the trigger's connection (`TriggerDefinitionServiceImpl`), so `context.http(...)`
already carries the auth header. Only the raw token value, needed for the HMAC, is missing there.

**Editor test mode.** The editor's test webhook endpoint (`WebhookTriggerTestController`) answers a plain 200 and drops
the validate body, so the welcome message is not sent while testing in the editor. The README says so.

## Left to the builder

- The exact shape of the additive SDK change (overload or new functional interface).
- Whether Sender Name falls back to the bot name from `get_account_info` (one extra call per send, possibly cached per
  connection) or is simply required. The fallback is preferred.
- How the downloaded media is named and typed when Viber gives no file name (pictures and videos).
- Whether to normalise `sender` and `user` into one `user` field.
- Property descriptions, help links, README wording and the icon.

## Testing

- Unit tests for each action (request body shape, error mapping on non-zero status) and for each trigger (enable with
  and without conversation started, the ownership guard on enable and disable, event filtering, signature accept and
  reject, welcome body, media download).
- Component definition JSON snapshot generated by the component test, per the repo's component testing rules.
- Platform-side tests that the validate step receives the connection through the new addition, and that a component
  using the existing validate function still gets called the same way.
- A live check against a real Viber bot is desirable but depends on the user having a bot. Viber needs a public HTTPS
  URL, so a local run needs a tunnel.
