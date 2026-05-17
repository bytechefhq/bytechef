---
title: LLM PII
description: Classifier-based PII detection for free-form spans that rule-based regex misses
---

`LLM PII` is a classifier-based PII detector that complements the rule-based `PII` component. It runs in the **LLM stage** of `Check For Violations` (or `Sanitize Text`) and asks an LLM to identify spans of personally-identifiable information that don't fit clean regex shapes.

---

## When to Use It

- The input contains PII in free-form prose ("My name is Jane Doe and I live at 123 Main St…").
- You're working in a locale where the rule-based PII detector has thin coverage.
- You want a defence-in-depth layer behind the rule-based detector.

For known structured PII (emails, phone numbers, IBANs, SSNs), the rule-based `PII` component is both cheaper and more deterministic. Use both together: the preflight rule-based detector handles the common cases, and `LLM PII` catches the long tail.

---

## Properties

| Property | Description |
|---|---|
| **Entities** | Subset of PII categories to ask the classifier to find. Defaults to a broad set covering names, addresses, dates of birth, ID numbers, and free-form locations |

---

## Required: Model Child

Reads the `Model` child attached to the parent `Check For Violations` (or `Sanitize Text`). Without one, the cluster element throws a configuration error and the request is blocked.

---

## Two Variants

- **LLM PII (check)** — emits a violation listing the entity types the classifier reported.
- **LLM PII (sanitize)** — masks each detected span with `<TYPE>` placeholders, the same as rule-based PII.

Both run in the LLM stage, so they consume preflight-masked text. The classifier sees, e.g., `"<EMAIL_ADDRESS> lives at 123 Main St"` rather than the raw email — but the address (which the rule-based PII detector doesn't know how to find) is still in the visible text for the classifier to flag.

---

## Hallucination Defence

LLMs occasionally invent spans that aren't in the input — saying "I detected the SSN 123-45-6789" when no such substring exists. The detector defends against this in two layers:

1. **Substring verification**: every span the classifier returns is checked to be a literal substring of the input. Spans that don't match the input verbatim are dropped with a per-call WARN log line.
2. **All-hallucinated fail-closed**: if every returned span fails substring verification, the check escalates to a "100% hallucination" exception and the advisor treats it as a check failure → request blocked.

The two-layer design means a partial hallucination (the classifier returns 5 spans, 1 is hallucinated) costs you that single span but doesn't degrade the rest of the result. Only the all-hallucinated case escalates to a block.

---

## Example

Combined rule-based + LLM PII for defence in depth:

```json
{
  "type": "guardrails/v1/checkForViolations",
  "extensions": {
    "clusterElements": {
      "model": {
        "type": "openAi/v1/model",
        "parameters": { "model": "gpt-4o-mini" }
      },
      "checkForViolations": [
        { "type": "guardrails/v1/piiCheck",     "parameters": { "type": "ALL" } },
        { "type": "guardrails/v1/llmPiiCheck",  "parameters": {} }
      ]
    }
  }
}
```

Sanitize prose-form PII inline (mask in place rather than block):

```json
{
  "type": "guardrails/v1/llmPiiSanitize",
  "parameters": {
    "entities": ["PERSON", "STREET_ADDRESS", "DATE_OF_BIRTH"]
  }
}
```

---

## Cost Considerations

`LLM PII` makes one LLM call per request. Pair with a fast / cheap model (gpt-4o-mini, claude-3-5-haiku, gemini-1.5-flash) — the prompt is short and the output is a structured list, so even small models do well here.

If your traffic is high, consider running rule-based `PII` alone and reserving `LLM PII` for a sampled subset of requests via a router upstream. The rule-based detector covers ~80% of common PII at a fraction of the cost.
