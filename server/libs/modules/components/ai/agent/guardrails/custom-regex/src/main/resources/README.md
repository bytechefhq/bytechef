# Custom Regex

`Custom Regex` runs operator-supplied regex patterns against the input. It runs in the
**preflight stage** so its matches are masked before LLM-stage checks run, and so its placeholders
merge with PII / Secret Keys / URL placeholders in the parent's longest-first mask pass.

This is the right component when no built-in detector covers your domain — internal ticket IDs,
proprietary order numbers, customer-account formats, study identifiers, etc.

## Properties

| Property | Required | Default | Description |
|---|:---:|:---:|---|
| `patterns` | yes | — | Array of named regex entries. Each entry has `name` (used as the placeholder `[name]` in sanitize mode and as the violation identifier in check mode) and `regex` (a Java regular expression) |

Only the `patterns` array is supported — there is no single-form shortcut. Add one entry per
pattern; entries run in declaration order and all matches are aggregated.

### Regex syntax

The detector accepts two forms in the `regex` field:

- **Plain Java regex**: `ORD-\\d{4}` — matched with default flags (case-sensitive).
- **JavaScript-style literal**: `/pattern/flags` — supports `i` (case-insensitive), `m`
  (multiline), `s` (dotall), and `u` (Unicode). Example: `/ssn-\\d{4}/i`.

Both forms run through `RegexParserUtils.compile`, which enforces:

- Maximum expression length (`MAX_EXPRESSION_LENGTH`) so a workflow author can't paste a
  5MB pathological pattern.
- Per-match character budget (`bounded(...)`) so catastrophic-backtracking inputs abort with a
  `RegexExecutionLimitException` rather than hanging the worker thread.

A bad pattern is treated as a **configuration error** and the request is blocked — a broken
regex is an operator bug, not a transient outage, and silently allowing requests through a dead
detector is worse than blocking until someone fixes it.

## Two cluster elements

- **`customRegexCheck`** — flags every match. Violation diagnostic info carries
  `patternNames` so the operator can see which entries fired (useful when several entries are
  configured).
- **`customRegexSanitize`** — replaces each match with `[name]` (the entry's own name in square
  brackets). Stable, operator-controlled placeholder.

The sanitize variant uses `[name]` rather than the shared `<TYPE>` convention used by PII /
Secret Keys / URLs because the operator owns the naming. If you'd like LLM-stage checks
downstream to see entries as the same shape, name them in upper-case (`CUSTOMER_ID`) — the
classifier doesn't care about the brackets vs. angle brackets.

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

A workflow run with `"ref MY-INTERNAL-1234 for TCK-987654"` produces
`"ref [internal-id] for [ticket]"`.

## Edge cases

- **Replacement-string metacharacters**: a pattern name containing `$` or `\` would normally be
  interpreted as a regex back-reference in `Matcher.replaceAll`. The sanitize variant calls
  `Matcher.quoteReplacement(...)` so a name like `$1\\0` renders literally as `[$1\\0]`.
- **Multiple-entry budget failures**: each entry runs in its own try/catch. If two entries are
  pathological, both surface in the aggregated exception (as suppressed causes) so the operator
  can fix all bad patterns at once instead of finding them one at a time across runs.
- **`validateRegex(...)` SPI**: the cluster element exposes
  `CustomRegex.validateRegex(String regex)` as a workflow-save-time hook. A future workflow
  validator can call it from the editor so a bad regex fails fast instead of blocking every
  request at runtime.
