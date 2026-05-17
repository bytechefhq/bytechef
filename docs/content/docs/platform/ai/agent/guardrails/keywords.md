---
title: Keywords
description: Word-boundary-aware match against a configured keyword list, run at PREFLIGHT against the raw input. Available as both a Check (block) and a Sanitize (mask) variant.
---

`Keywords` flags or masks any of a configured list of words when they appear in the input. It runs in the **PREFLIGHT stage** of `Check For Violations` or `Sanitize Text`, against the raw input text before sibling masking detectors (PII, Secret Keys, URLs) rewrite tokens.

This is a deliberate design choice: a keyword list authored against masked text would break the moment the secret-key detector ate the prefix or the PII detector replaced the local-part of an email — the deny-list token would have been replaced with `<AWS_ACCESS_KEY>` or `<EMAIL_ADDRESS>` before the keyword check ever saw it. Running at PREFLIGHT against raw input means deny-list entries always match the characters the operator actually wrote.

The **Check** variant emits a violation listing the matched keywords — wire it under `Check For Violations` to block on a deny-list match. The **Sanitize** variant replaces every matched keyword with the `<KEYWORD>` placeholder via the shared mask-entity pass — wire it under `Sanitize Text` when the right action is to redact the keyword rather than block the request.

---

## Properties

| Property | Description |
|---|---|
| **Keywords** | List of words to detect. At least one entry is required |
| **Case Sensitive** | When off, matching is case-insensitive (default). When on, matches must match casing exactly |

---

## Word-Boundary Matching

Keywords uses Unicode-aware word boundaries (`\p{L}|\p{N}|_`) so a keyword `"cat"`:

- **does** match: `"the cat sat"`, `"cat,"`, `"cat."`, `"cat!"` (punctuation boundary)
- **does not** match: `"caterpillar"`, `"category"`, `"cats"` (letter on either side)

This is symmetric — if your keyword itself starts or ends with a non-word character (e.g., `":hashtag"`, `"#promo"`), the matcher relaxes the boundary on that side automatically. You don't need to escape regex metacharacters; the matcher quotes them for you.

---

## Cluster Elements

- **Keywords (check)** — emits a violation listing the matched keywords. Wire it as a child of `Check For Violations` to block requests when any deny-list keyword fires.
- **Keywords (sanitize)** — replaces every matched keyword with a `<KEYWORD>` placeholder through the shared mask-entity pass. Wire it as a child of `Sanitize Text` to redact keywords from inputs or model responses without blocking.

---

## Examples

Block any of three competitor names:

```json
{
  "type": "guardrails/v1/keywordsCheck",
  "parameters": {
    "keywords": ["CompetitorA", "CompetitorB", "CompetitorC"],
    "caseSensitive": false
  }
}
```

Mask competitor names in the model's response instead of blocking:

```json
{
  "type": "guardrails/v1/keywordsSanitize",
  "parameters": {
    "keywords": ["CompetitorA", "CompetitorB", "CompetitorC"],
    "caseSensitive": false
  }
}
```

---

## When NOT to Use

- For **structured patterns** (account numbers, ticket IDs, URLs), use `Custom Regex` instead. Keywords are literal strings.
- For **fuzzy matching** ("anything that mentions our brand even with typos"), use `Custom` with an LLM classifier prompt. Literal keywords don't catch typos or alternate spellings.
- For **PII** (emails, phone numbers, etc.), use `PII` — the rule-based detector is designed for these shapes and word-boundary keyword matching would miss most of them.

---

## Edge Cases

- **Empty keyword list** is treated as a configuration error and forces fail-closed.
- **`null` and empty-string entries** are silently skipped by the matcher. Other entries (including whitespace-only strings and entries containing punctuation) are matched as literal text — they go straight to `Pattern.quote(...)`, so a list entry like `"foo,"` matches the literal substring `"foo,"` (comma included), not `"foo"`. Sanitize your list at authoring time if you need leading/trailing punctuation trimmed.
- **Regex metacharacters** in a keyword are matched literally — `.` only matches a period, `.*` only matches the two-character sequence, `(?:` does not throw at request-time. The matcher escapes user input before compiling the pattern.
