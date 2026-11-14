---
title: Custom Regex
description: Your own named regular expressions, for identifiers that no built-in detector covers.
---

**Custom Regex** runs your own named regular expressions against the text. It runs in the preflight stage and needs no model. Use it for values specific to your business, such as internal ticket IDs, order numbers, account formats, or study identifiers. Attach it under [Check for Violations](/platform/automation/build/workflows/ai/agent/guardrails/check-for-violations) to block text with a match, or under [Sanitize Text](/platform/automation/build/workflows/ai/agent/guardrails/sanitize-text) to replace each match with `[name]`.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| **Validate Input** | Boolean | `true` | Check the user input before it is sent to the model. |
| **Validate Output** | Boolean | `true` | Check the model response before it is returned. |
| **Patterns** | Array of objects | None | The named patterns. Required, with at least one entry. |

Each entry in **Patterns** has:

| Field | Type | Required | Description |
|---|---|---|---|
| **Name** | String | Yes | The placeholder `[name]` under Sanitize Text, and the violation name under Check for Violations. |
| **Regex** | String | Yes | A Java regular expression, or a `/pattern/flags` literal. |

## Regex syntax

- **Plain Java regex**, for example `ORD-\d{4}`. Matching is case-sensitive.
- **Literal with flags**, for example `/ssn-\d{4}/i`. Supported flags:

| Flag | Effect |
|---|---|
| `i` | Case-insensitive (Unicode-aware) |
| `m` | Multiline: `^` and `$` match at line breaks |
| `s` | Dot-all: `.` matches line breaks |
| `u` | Unicode character classes |
| `x` | Comments: whitespace and `#` comments in the pattern are ignored |
| `g` | Accepted and ignored. All matches are always found. |

Any other flag is an error.

## Results

- **Under Check for Violations** (`customRegexCheck`): one violation. If one pattern matched, the violation is named after that pattern; if several matched, it is named `customRegex`. `info.patternNames` lists the patterns that matched, and `matchCount` is the number of matches.
- **Under Sanitize Text** (`customRegexSanitize`): each match is replaced with `[name]`. The patterns are applied one after another in the order listed, and Custom Regex rewrites the text immediately, so sanitizers attached after it see the placeholders. Special characters in a name, such as `$` or `\`, are inserted literally.

## Examples

Block order IDs, case-insensitively:

```json
{
  "name": "customRegex_1",
  "type": "customRegex/v1/customRegexCheck",
  "parameters": {
    "patterns": [
      { "name": "ORDER_ID", "regex": "/ORD-\\d{4,}/i" }
    ]
  }
}
```

Mask several identifiers:

```json
{
  "name": "customRegex_1",
  "type": "customRegex/v1/customRegexSanitize",
  "parameters": {
    "patterns": [
      { "name": "internal-id", "regex": "MY-INTERNAL-\\d{4}" },
      { "name": "ticket", "regex": "TCK-\\d{6}" },
      { "name": "study-id", "regex": "STUDY-[A-Z]{2}-\\d{3}" }
    ]
  }
}
```

With this configuration, `ref MY-INTERNAL-1234 for TCK-987654` becomes `ref [internal-id] for [ticket]`.

## Limits and errors

Each of the following is an execution failure, so the request is blocked (Check for Violations) or the text is withheld (Sanitize Text):

| Condition | Limit |
|---|---|
| **Patterns** is empty, or an entry has an empty **Name** or **Regex** | None |
| The regex does not compile, or uses an unsupported flag | None |
| The regex is too long | 4,096 characters |
| The text to scan is too long | 1,048,576 characters |
| A match takes too long (for example, catastrophic backtracking) | 10,000,000 character reads per scan |

When several patterns exceed the match limit, all of them are reported in one error.

## Related

- [PII](/platform/automation/build/workflows/ai/agent/guardrails/pii)
- [Keywords](/platform/automation/build/workflows/ai/agent/guardrails/keywords)
- [customRegex/v1 reference](/reference/components/customRegex_v1)
