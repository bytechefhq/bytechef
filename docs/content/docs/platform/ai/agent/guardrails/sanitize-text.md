---
title: Sanitize Text
description: The non-blocking parent action that runs configured sanitizers and rewrites matched spans with placeholders
---

`Sanitize Text` is the **rewrite-in-place** counterpart of `Check For Violations`. It runs the configured child sanitizers over the input (or the LLM's response) and replaces matched spans with placeholders rather than blocking the request.

The two parents share the same child architecture; the only difference is the verdict path: `Check For Violations` blocks on any violation; `Sanitize Text` rewrites and forwards.

---

## When to Use It

The most common pattern is wiring `Sanitize Text` to the **outbound** side of an agent — run it on the LLM's response before the user sees it, so anything the model accidentally surfaces (PII from a tool result, a leaked URL from a documentation passage, a secret embedded in a code sample) is masked.

For inbound text you usually want `Check For Violations` instead — blocking is the right default for adversarial signals like jailbreak attempts, NSFW content, off-topic redirects.

```
                          inbound                               outbound
user input  →  Check For Violations  →  AI Agent  →  Sanitize Text  →  user-visible reply
```

---

## Compatible Children

`Sanitize Text` accepts the **Sanitize** variant of every preflight rule-based check:

- **PII** (sanitize variant)
- **Secret Keys** (sanitize variant)
- **URLs** (sanitize variant)
- **Custom Regex** (sanitize variant)
- **Keywords** (sanitize variant)
- **LLM PII** (sanitize variant)

LLM-stage classifiers (Jailbreak, NSFW, Topical Alignment, Custom) do **not** have sanitize variants — they emit verdicts on whole inputs, not span-level matches, so masking doesn't apply. For those checks use `Check For Violations`.

**Keywords** has both flavours: use the **Check** variant under `Check For Violations` to block on a deny-list match, or the **Sanitize** variant under `Sanitize Text` to mask matched keywords with `<KEYWORD>` placeholders instead.

---

## Properties

`Sanitize Text` itself is a thin parent; the per-sanitizer properties (allowlists, thresholds, `validateInput`, `validateOutput`) live on the child sanitizers. A **Model** child is required only if you attach the **LLM PII** sanitizer; pure rule-based sanitizers work without a model.

---

## Mask Merging

When multiple sanitizers fire on overlapping spans (an email contains a domain that's also matched by URLs; a JWT contains base64 fragments), the system merges every sanitizer's mask entities and applies them **longest-first**. The longest match wins, so the output is `<EMAIL_ADDRESS>` rather than being split across `<EMAIL_ADDRESS>` and `<URL>`.

This is the same merge logic `Check For Violations` uses for its preflight stage, just applied to sanitize-only output.

---

## Failure Behavior

A failing sanitizer is fail-closed: even though `Sanitize Text` itself is non-blocking by design, a sanitizer that cannot run means the masking pass was incomplete, so the advisor replaces the response with a withheld-placeholder rather than risk leaking unredacted text. See the [Guardrails overview](./) for the platform-wide failure-behavior contract.

---

## Example

Outbound mask: scrub PII, secrets, and unrecognised URLs from the LLM's response:

```json
{
  "type": "guardrails/v1/sanitizeText",
  "extensions": {
    "clusterElements": {
      "sanitizeText": [
        { "type": "guardrails/v1/piiSanitize",        "parameters": { "type": "ALL" } },
        { "type": "guardrails/v1/secretKeysSanitize", "parameters": { "permissiveness": "BALANCED" } },
        {
          "type": "guardrails/v1/urlsSanitize",
          "parameters": {
            "allowedUrls": ["https://docs.example.com"],
            "allowedSchemes": ["https"],
            "allowSubdomain": true
          }
        }
      ]
    }
  }
}
```

Inbound + outbound combined:

```json
[
  { "type": "guardrails/v1/checkForViolations",
    "extensions": { "clusterElements": {
      "model": { "type": "openAi/v1/model", "parameters": { "model": "gpt-4o-mini" } },
      "checkForViolations": [
        { "type": "guardrails/v1/jailbreak", "parameters": {} },
        { "type": "guardrails/v1/nsfw",      "parameters": {} },
        { "type": "guardrails/v1/piiCheck",  "parameters": { "type": "ALL" } }
      ]
    } } },
  { "type": "agenticAi/v1/run", "parameters": { "...": "..." } },
  { "type": "guardrails/v1/sanitizeText",
    "extensions": { "clusterElements": { "sanitizeText": [
      { "type": "guardrails/v1/piiSanitize" },
      { "type": "guardrails/v1/secretKeysSanitize" }
    ] } } }
]
```
