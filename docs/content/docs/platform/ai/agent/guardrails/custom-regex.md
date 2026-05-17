---
title: Custom Regex
description: Operator-supplied regex patterns with named placeholders for project-specific identifiers
---

`Custom Regex` runs operator-supplied regex patterns against the input. It runs in the **preflight stage** so its matches are masked before LLM-stage checks run, and so its placeholders merge with PII / Secret Keys / URL placeholders in the parent's longest-first mask pass.

This is the right component when no built-in detector covers your domain — internal ticket IDs, proprietary order numbers, customer-account formats, study identifiers, etc.

---

## Properties

| Property | Description |
|---|---|
| **Patterns** | Array of named regex entries. Each entry has a **Name** (used as the placeholder `[name]` in sanitize mode and as the violation identifier in check mode) and a **Regex** (a Java regular expression). At least one entry is required |

### Regex Syntax

Each entry's **Regex** field accepts two forms:

- **Plain Java regex**: `ORD-\d{4}` — matched with default flags (case-sensitive).
- **JavaScript-style literal**: `/pattern/flags` — supports `i` (case-insensitive), `m` (multiline), `s` (dotall), and `u` (Unicode). Example: `/ssn-\d{4}/i`.

Both forms are validated up front:

- Maximum expression length so a workflow author can't paste a multi-megabyte pathological pattern.
- Per-match character budget so catastrophic-backtracking inputs abort with an execution-budget error rather than hanging the worker thread.

A bad pattern is treated as a **configuration error** and the request is blocked — a broken regex is an operator bug, not a transient outage, and silently allowing requests through a dead detector is worse than blocking until someone fixes it.

---

## Two Variants

- **Custom Regex (check)** — flags every match. The violation diagnostic info carries the pattern names so you can see which entries fired (useful when several entries are configured).
- **Custom Regex (sanitize)** — replaces each match with `[name]` (the entry's own name in square brackets). Stable, operator-controlled placeholder.

The sanitize variant uses `[name]` rather than the shared `<TYPE>` convention used by PII / Secret Keys / URLs because the operator owns the naming. Name your entries in upper-case (`CUSTOMER_ID`) if you want LLM-stage downstream checks to see entries as the same shape as built-in masks — the classifier doesn't care about the brackets vs. angle brackets.

---

## Examples

One internal-ID pattern, with a JS-style literal for case insensitivity:

```json
{
  "type": "guardrails/v1/customRegexCheck",
  "parameters": {
    "patterns": [
      { "name": "ORDER_ID", "regex": "/ORD-\\d{4,}/i" }
    ]
  }
}
```

Multiple named patterns running together, mask in place:

```json
{
  "type": "guardrails/v1/customRegexSanitize",
  "parameters": {
    "patterns": [
      { "name": "internal-id", "regex": "MY-INTERNAL-\\d{4}" },
      { "name": "ticket",      "regex": "TCK-\\d{6}" },
      { "name": "study-id",    "regex": "STUDY-[A-Z]{2}-\\d{3}" }
    ]
  }
}
```

A workflow run with `"ref MY-INTERNAL-1234 for TCK-987654"` produces `"ref [internal-id] for [ticket]"`.

---

## Edge Cases

- **Replacement-string metacharacters**: a pattern name containing `$` or `\` would normally be interpreted as a regex back-reference. The sanitize variant quotes them safely so a name like `$1\0` renders literally as `[$1\0]`.
- **Multiple-entry budget failures**: each entry runs in its own try/catch. If two entries are pathological, both surface in the aggregated exception (as suppressed causes) so you can fix all bad patterns at once instead of finding them one at a time across runs.
