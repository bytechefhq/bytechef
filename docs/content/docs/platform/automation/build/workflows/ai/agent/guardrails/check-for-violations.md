---
title: Check for Violations
description: The guardrail parent that blocks an AI Agent request or response when any attached check fires.
---

**Check for Violations** is the blocking guardrail parent. It runs every attached detector on the user input before the model is called and on the model response before it is returned. If any detector fires on the input, the agent returns the **Blocked Message** and the model is not called. If any detector fires on the response, the response is replaced with the **Blocked Message**.

Use it for signals where refusing is the right outcome: jailbreak attempts, unsafe content, off-topic requests, and secrets or personal data that must never be sent or returned.

## Add Check for Violations

1. In the AI Agent's advanced editor, add **Check for Violations** to the agent's **Guardrails** slot. An agent accepts only one.
2. Add detectors to the parent's **Check for Violations** slot. See [Detectors](#detectors).
3. If any detector is LLM-based, add a chat model to the parent's **Model** slot. This model is separate from the agent's own model.
4. Optionally change the **Blocked Message**.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| **Customize System Message** | Boolean | `false` | Replace the system message sent to the LLM-based detectors. |
| **System Message** | String | Built-in classifier rubric | The system message for Jailbreak, NSFW, Topical Alignment, and Custom. Shown only when **Customize System Message** is `true`. |
| **Blocked Message** | String | `I cannot process this request due to content policy.` | The text the agent returns instead of the response when a check fires or fails. |

The built-in system message tells the model to return exactly two fields, `confidenceScore` and `flagged`, and defines an 11-step confidence scale from `0.0` (certain not violative) to `1.0` (certain violative). If you replace it, keep both field names: a response without them is an execution failure. LLM PII does not use this system message.

## Cluster element slots

| Slot | Required | Accepts |
|---|---|---|
| **Model** | Only when an LLM-based detector is attached | One chat model. The editor marks the slot as required. |
| **Check for Violations** | No | Any number of detectors. |

## Detectors

| Detector | Stage | Needs Model |
|---|---|:---:|
| [PII](/platform/automation/build/workflows/ai/agent/guardrails/pii) | Preflight | No |
| [Secret Keys](/platform/automation/build/workflows/ai/agent/guardrails/secret-keys) | Preflight | No |
| [URLs](/platform/automation/build/workflows/ai/agent/guardrails/urls) | Preflight | No |
| [Custom Regex](/platform/automation/build/workflows/ai/agent/guardrails/custom-regex) | Preflight | No |
| [Keywords](/platform/automation/build/workflows/ai/agent/guardrails/keywords) | Preflight | No |
| [Jailbreak](/platform/automation/build/workflows/ai/agent/guardrails/jailbreak) | LLM | Yes |
| [NSFW](/platform/automation/build/workflows/ai/agent/guardrails/nsfw) | LLM | Yes |
| [Topical Alignment](/platform/automation/build/workflows/ai/agent/guardrails/topical-alignment) | LLM | Yes |
| [Custom](/platform/automation/build/workflows/ai/agent/guardrails/custom) | LLM | Yes |
| [LLM PII](/platform/automation/build/workflows/ai/agent/guardrails/llm-pii) | LLM | Yes |
| Jev Input Guardrail, Jev Output Guardrail ([TypeSafe AI](/reference/components/typesafe_v1)) | LLM | No, uses its own connection |

If an LLM-based detector is attached and the **Model** slot is empty, the agent run fails when it builds the guardrail.

## How checks run

On each side (input and output), the parent runs the detectors whose **Validate Input** or **Validate Output** setting is on:

1. **Preflight stage.** Rule-based detectors run against the raw text. PII, Secret Keys, URLs, and Custom Regex also report their matches as mask entities. Keywords does not mask, so its keyword list always matches the text the user typed.
2. **Mask pass.** The mask entities from all preflight detectors are merged and replaced with `<TYPE>` placeholders in one longest-match-first pass. This masked text is used only for the LLM stage; it is not forwarded to the agent's model.
3. **LLM stage.** LLM-based detectors classify the masked text, so personal data and secrets matched in the preflight stage are not sent to the guardrail model.

Every detector runs even after another one has fired, so the result lists all violations at once. Input checks also cover a human's answer returned to a tool (for example, from an approval or a question to the user).

When the agent has a chat memory, input checks in the LLM stage (except LLM PII) also receive the earlier turns of the conversation.

## When a check fires or fails

The agent returns the **Blocked Message** as its response, and the step output includes a `guardrail.violations` list. Each entry has:

| Field | Description |
|---|---|
| `guardrail` | The detector that fired, for example `piiCheck`, `jailbreak`, or the name of a Custom classifier. |
| `matchCount` | Number of matched values for rule-based detectors and LLM PII. `0` for classifier verdicts. |
| `executionFailed` | `true` when the detector could not run. |
| `info` | Detector details, such as `entityTypes`, `providerTypes`, or `patternNames`. Matched text is never included. |
| `confidenceScore` | The classifier score, for classifier violations. |
| `failureKind` | The failure type, for execution failures. |

A detector that cannot run (guardrail model error, invalid configuration) produces a violation with `executionFailed: true`, so the request is blocked. See [Guardrails, monitoring guardrail results](/platform/automation/build/workflows/ai/agent/guardrails#monitoring-guardrail-results) for the full output shape.

## Example

An AI Agent task with a **Check for Violations** parent that blocks PII, secrets, and jailbreak attempts:

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
        "parameters": { "blockedMessage": "I can't help with that request." },
        "clusterElements": {
          "model": { "name": "openAi_2", "type": "openAi/v1/model", "parameters": { "model": "gpt-4o-mini" } },
          "checkForViolations": [
            { "name": "pii_1", "type": "pii/v1/piiCheck", "parameters": { "type": "ALL" } },
            { "name": "secretKeys_1", "type": "secretKeys/v1/secretKeysCheck", "parameters": { "permissiveness": "BALANCED" } },
            { "name": "jailbreak_1", "type": "jailbreak/v1/jailbreak", "parameters": { "validateOutput": false, "threshold": 0.7 } }
          ]
        }
      }
    ]
  }
}
```

## Related

- [Sanitize Text](/platform/automation/build/workflows/ai/agent/guardrails/sanitize-text)
- [Guardrails overview](/platform/automation/build/workflows/ai/agent/guardrails)
- [checkForViolations/v1 reference](/reference/components/checkForViolations_v1)
