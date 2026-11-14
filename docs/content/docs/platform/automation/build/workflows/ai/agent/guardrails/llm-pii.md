---
title: LLM PII
description: Model-based detection of personal data that does not follow a fixed format.
---

**LLM PII** asks a model to find personal data in the text, using the same entity types as the rule-based [PII](/platform/automation/build/workflows/ai/agent/guardrails/pii) detector. Because it does not depend on a fixed format, it finds values that regular expressions miss, such as an address written in a sentence. It runs in the LLM stage and uses the parent's **Model** child. Attach it under [Check for Violations](/platform/automation/build/workflows/ai/agent/guardrails/check-for-violations) to block, or under [Sanitize Text](/platform/automation/build/workflows/ai/agent/guardrails/sanitize-text) to replace each value with `<ENTITY_TYPE>`.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| **Validate Input** | Boolean | `true` | Check the user input before it is sent to the model. |
| **Validate Output** | Boolean | `true` | Check the model response before it is returned. |
| **Entities** | Array of strings | None | The entity types the model looks for. The options are the 36 types listed on the [PII](/platform/automation/build/workflows/ai/agent/guardrails/pii#entity-types) page. Select at least one; an empty list is an execution failure. |

## When to use it

- Use rule-based PII first. It is faster, has no model cost, and is deterministic.
- Add LLM PII to the same parent for values without a fixed format, and for regions where the rule-based patterns are too strict.
- Because it runs after preflight masking, values that PII already masked reach LLM PII as placeholders, such as `<EMAIL_ADDRESS>`. Only the remaining text is sent to the guardrail model.

## Results

- **Under Check for Violations** (`llmPiiCheck`): one violation listing the found entity types in `info.entityTypes`, with `matchCount` set to the number of values found.
- **Under Sanitize Text** (`llmPiiSanitize`): every occurrence of each found value is replaced with `<ENTITY_TYPE>`, longest value first. LLM PII runs after all preflight sanitizers.

## Invented values

A model sometimes reports a value that is not in the text. LLM PII keeps only values that appear in the text exactly as reported:

| Model answer | Result |
|---|---|
| All reported values are in the text | All are used. |
| Up to half of the reported values are not in the text | Those values are dropped and an error is written to the step's logs. The rest are used. |
| More than half, or all, of the reported values are not in the text | Execution failure: the request is blocked (Check for Violations) or the text is withheld (Sanitize Text). |

Values that are empty or contain only spaces or punctuation are always dropped.

## Examples

Rule-based and model-based PII in one parent:

```json
{
  "name": "checkForViolations_1",
  "type": "checkForViolations/v1/checkForViolations",
  "parameters": {},
  "clusterElements": {
    "model": { "name": "openAi_2", "type": "openAi/v1/model", "parameters": { "model": "gpt-4o-mini" } },
    "checkForViolations": [
      { "name": "pii_1", "type": "pii/v1/piiCheck", "parameters": { "type": "ALL" } },
      { "name": "llmPii_1", "type": "llmPii/v1/llmPiiCheck", "parameters": { "entities": ["LOCATION", "EMAIL_ADDRESS", "PHONE_NUMBER"] } }
    ]
  }
}
```

Mask addresses and dates in the model response:

```json
{
  "name": "llmPii_1",
  "type": "llmPii/v1/llmPiiSanitize",
  "parameters": { "entities": ["LOCATION", "DATE_TIME"], "validateInput": false }
}
```

## Limitations

- There is no entity type for names of people.
- LLM PII makes one model call per checked side and uses its own fixed system message; **Customize System Message** on Check for Violations does not affect it.
- It does not receive earlier conversation turns.
- If the parent has no **Model** child, the agent run fails.

## Related

- [PII](/platform/automation/build/workflows/ai/agent/guardrails/pii)
- [llmPii/v1 reference](/reference/components/llmPii_v1)
