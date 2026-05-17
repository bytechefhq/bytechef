# Keywords

`Keywords` flags any of a configured list of words when they appear in the input. It runs in the
**PREFLIGHT stage** of `CheckForViolations`, before sibling masking detectors (SecretKeys, PII,
URLs) rewrite tokens. Running post-mask would let upstream detectors silently swallow the very
substring a deny-list keyword was authored to block (e.g. an `AKIA` keyword would never fire
because `SecretKeys` preflight already replaced the surrounding AWS key with a placeholder).

## Properties

| Property | Required | Default | Description |
|---|:---:|:---:|---|
| `keywords` | yes | — | List of words to detect |
| `caseSensitive` | no | `false` | When off, matching is case-insensitive (default). When on, matches must match casing exactly |

## Word-boundary matching

`KeywordMatcherUtils` uses Unicode-aware word boundaries (`\p{L}|\p{N}|_`) so a keyword `"cat"`:

- **does** match: `"the cat sat"`, `"cat,"`, `"cat."`, `"cat!"` (punctuation boundary)
- **does not** match: `"caterpillar"`, `"category"`, `"cats"` (letter on either side)

Regex metacharacters in the keyword are quoted automatically (`Pattern.quote`) — you don't have
to escape them. Keywords that themselves start or end with a non-word character (e.g. `"#promo"`)
match wherever a non-word character precedes/follows the entry; the lookarounds are symmetric and
satisfied by any non-word boundary, including string start/end.

## Cluster elements

- **`keywordsCheck`** — emits a `Violation.PatternViolation` listing the matched keywords. Wire it
  as a child of `CheckForViolations` to block requests when any deny-list keyword fires.
- **`keywordsSanitize`** — replaces each matched keyword with the `<KEYWORD>` placeholder via the
  shared mask-entity pass. Wire it as a child of `SanitizeText` to redact keywords from inputs or
  model responses rather than block.

## Example

Block any of three competitor names:

```json
{
  "type": "guardrails/v1/keywords",
  "parameters": {
    "keywords": ["CompetitorA", "CompetitorB", "CompetitorC"],
    "caseSensitive": false
  }
}
```

## When NOT to use

- For **structured patterns** (account numbers, ticket IDs, URLs), use Custom Regex instead.
  Keywords are literal strings.
- For **fuzzy matching** ("anything that mentions our brand even with typos"), use `Custom`
  with an LLM classifier prompt. Literal keywords don't catch typos or alternate spellings.
- For **PII** (emails, phone numbers, etc.), use `PII` — the rule-based detector is designed
  for these shapes and word-boundary keyword matching would miss most of them.

## Edge cases

- **Empty keyword list** is treated as a configuration error and forces fail-closed.
- **Null or empty entries** within the list are skipped silently. Whitespace-only or
  punctuation-only entries are matched as-is (no stripping); supply already-trimmed values if
  you want them to behave as you expect.
