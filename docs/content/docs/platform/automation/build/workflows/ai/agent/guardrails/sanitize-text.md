---
title: Sanitize Text
description: The guardrail parent that masks matched values in AI Agent input and output instead of blocking.
---

**Sanitize Text** is the masking guardrail parent. It runs every attached sanitizer on the user input before the model is called and on the model response before it is returned, and replaces matched values with placeholders such as `<EMAIL_ADDRESS>`, `<AWS_ACCESS_KEY>`, or `<URL>`. The request and the response always go through; only the matched values change.

Use it when masking is more useful than refusing: personal data a user pastes into a prompt, or a secret, address, or internal URL the model repeats from a tool result or a retrieved document.

## Add Sanitize Text

1. In the AI Agent's advanced editor, add **Sanitize Text** to the agent's **Guardrails** slot. An agent accepts only one.
2. Add sanitizers to the parent's **Sanitize Text** slot.
3. If you added **LLM PII**, add a chat model to the parent's **Model** slot.
4. On each sanitizer, use **Validate Input** and **Validate Output** to choose whether it masks the user input, the model response, or both (both default to `true`).

## Properties

**Sanitize Text** has no properties of its own. All settings are on the sanitizers.

## Cluster element slots

| Slot | Required | Accepts |
|---|---|---|
| **Model** | Only when LLM PII is attached | One chat model. The editor marks the slot as required. |
| **Sanitize Text** | No | Any number of sanitizers. |

## Sanitizers

| Sanitizer | Stage | Placeholder |
|---|---|---|
| [PII](/platform/automation/build/workflows/ai/agent/guardrails/pii) | Preflight | `<ENTITY_TYPE>`, for example `<EMAIL_ADDRESS>` |
| [Secret Keys](/platform/automation/build/workflows/ai/agent/guardrails/secret-keys) | Preflight | `<SECRET_TYPE>`, for example `<GITHUB_PAT>` |
| [URLs](/platform/automation/build/workflows/ai/agent/guardrails/urls) | Preflight | `<URL>` |
| [Keywords](/platform/automation/build/workflows/ai/agent/guardrails/keywords) | Preflight | `<KEYWORD>` |
| [Custom Regex](/platform/automation/build/workflows/ai/agent/guardrails/custom-regex) | Preflight | `[name]`, the pattern's own name in square brackets |
| [LLM PII](/platform/automation/build/workflows/ai/agent/guardrails/llm-pii) | LLM | `<ENTITY_TYPE>` |

Jailbreak, NSFW, Topical Alignment, and Custom produce a verdict on the whole text, not on individual values, so they are only available under [Check for Violations](/platform/automation/build/workflows/ai/agent/guardrails/check-for-violations).

## How masking works

For each side, the sanitizers run in this order:

1. **Preflight sanitizers** run in the order they are attached. PII, Secret Keys, URLs, and Keywords collect their matches without changing the text yet. Custom Regex replaces its matches immediately, so preflight sanitizers attached after it see the `[name]` placeholders.
2. **Mask pass.** The collected matches are merged and replaced in one longest-match-first pass. When two matches overlap, the longer one wins, so an email address is replaced by one `<EMAIL_ADDRESS>` rather than a mix of partial placeholders.
3. **LLM PII** runs last, on the already masked text.

On the input side, Sanitize Text masks user messages and a human's answer returned to a tool. Other messages, such as the system prompt and tool results, are not changed.

When both parents are attached, **Check for Violations** checks the original input before Sanitize Text masks it, and checks the response after Sanitize Text has masked it.

## When a sanitizer fails

If any sanitizer cannot run (for example, LLM PII cannot reach its model, or a Custom Regex pattern is invalid), the masking is incomplete. Sanitize Text then replaces the whole text with a fixed placeholder reporting that the sanitizer failed and the response was withheld:

- A failure on the input side returns the placeholder without calling the model.
- A failure on the output side replaces the model's response.

The step output then includes `"guardrail": { "guardrail.sanitizeWithheld": true }`. See [Guardrails, monitoring guardrail results](/platform/automation/build/workflows/ai/agent/guardrails#monitoring-guardrail-results).

If LLM PII is attached and the **Model** slot is empty, the agent run fails before the model is called.

## Example

An AI Agent task that blocks jailbreak attempts on the input and masks PII, secrets, and unknown URLs on both sides:

```json
{
  "name": "aiAgent_1",
  "type": "aiAgent/v1/chat",
  "parameters": { "userPrompt": "${trigger_1.message}" },
  "clusterElements": {
    "model": { "name": "openAi_1", "type": "openAi/v1/model", "parameters": { "model": "gpt-4o" } },
    "guardrails": [
      {
        "name": "checkForViolations_1",
        "type": "checkForViolations/v1/checkForViolations",
        "parameters": {},
        "clusterElements": {
          "model": { "name": "openAi_2", "type": "openAi/v1/model", "parameters": { "model": "gpt-4o-mini" } },
          "checkForViolations": [
            { "name": "jailbreak_1", "type": "jailbreak/v1/jailbreak", "parameters": { "validateOutput": false } }
          ]
        }
      },
      {
        "name": "sanitizeText_1",
        "type": "sanitizeText/v1/sanitizeText",
        "parameters": {},
        "clusterElements": {
          "sanitizeText": [
            { "name": "pii_1", "type": "pii/v1/piiSanitize", "parameters": { "type": "ALL" } },
            { "name": "secretKeys_1", "type": "secretKeys/v1/secretKeysSanitize", "parameters": { "permissiveness": "BALANCED" } },
            {
              "name": "urls_1",
              "type": "urls/v1/urlsSanitize",
              "parameters": { "allowedUrls": ["docs.example.com"], "allowedSchemes": ["https"], "allowSubdomain": true }
            }
          ]
        }
      }
    ]
  }
}
```

## Limitations

- With the **Stream Chat** action, the response is masked chunk by chunk. A value split across two chunks is not masked, and a chunk that was already sent cannot be recalled. Use the **Chat** action when every value must be masked.
- Placeholders replace the value only. The model and the caller see that a value was there, and of which type.

## Related

- [Check for Violations](/platform/automation/build/workflows/ai/agent/guardrails/check-for-violations)
- [Guardrails overview](/platform/automation/build/workflows/ai/agent/guardrails)
- [sanitizeText/v1 reference](/reference/components/sanitizeText_v1)
