---
title: Secret Keys
description: Rule-based detection of API tokens and credential shapes — AWS, GitHub, Stripe, OpenAI, Slack, Google, JWT, and a tunable random-string detector
---

`Secret Keys` detects API tokens and credential shapes in the input. It runs in the **preflight stage** so the matched substrings are masked before the LLM sees them.

---

## What It Detects

The detector combines three signals, gated by the **Permissiveness** setting:

1. **Named-provider tokens** (always on): exact regex matches for known token shapes.
   - `AWS_ACCESS_KEY` (`AKIA…`), `AWS_SECRET_KEY`
   - `GITHUB_PAT` (`ghp_…`), `GITHUB_FINE_GRAINED_PAT` (`github_pat_…`)
   - `SLACK_TOKEN` (`xox[abp]-…`)
   - `STRIPE_KEY` (`sk_live_…`, `pk_test_…`, etc.)
   - `GOOGLE_API_KEY` (`AIza…`)
   - `OPENAI_KEY` (`sk-…`)
   - `JWT` (the three-segment base64 form)
2. **Random-looking high-information strings** (Strict and Balanced): tokens that are long enough, varied enough, and have enough character-class diversity (lowercase, uppercase, digit, symbol) to look credential-shaped rather than English text.
3. **Generic key-value assignments** (Strict only): patterns like `apikey="..."`, `secret = '...'`, `token: ...` — useful when an LLM dumps a config snippet but noisy if your inputs legitimately contain config samples.

---

## Properties

| Property | Description |
|---|---|
| **Permissiveness** | `Strict` / `Balanced` (default) / `Permissive`. See the table below |
| **Allowed File Extensions** | Skip detection inside markdown fenced code blocks tagged with these languages (e.g. `py`, `js`, `ts`). Useful when input legitimately contains code samples |

### Permissiveness Levels

| Level | Named providers | Prefixed tokens | Random strings | Generic `apikey=...` | Notes |
|---|:---:|:---:|:---:|:---:|---|
| **Strict** | yes | yes | yes (≥10 chars, ≥3.0 bits entropy, ≥2 char classes) | yes | Most coverage, more false positives. Picks up anything that *looks* credential-shaped |
| **Balanced** (default) | yes | yes | yes (≥10 chars, ≥3.8 bits entropy, ≥3 char classes) | no | Good middle ground. Random-string detector runs with stricter shape thresholds |
| **Permissive** | yes | yes | yes (≥30 chars, ≥4.0 bits entropy, ≥2 char classes) | no | Highest bar for random strings. Use this when your input legitimately contains long random IDs. Prefixed tokens (`AKIA…`, `ghp_…`, `sk-…`, `xox…`, `SG.…`, `hf_…`) still match regardless of level |

For project-specific secret shapes, attach the standalone **Custom Regex** action alongside Secret Keys in the same `Check For Violations` parent.

---

## Two Variants

- **Secret Keys (check)** — emits a violation listing the detected provider types so you can see, e.g., that `["AWS_ACCESS_KEY", "JWT"]` triggered the block.
- **Secret Keys (sanitize)** — masks each match with `<TYPE>` (`<AWS_ACCESS_KEY>`, `<JWT>`, etc.). Stable placeholders so downstream tools can post-process consistently.

---

## Examples

Block any AWS / GitHub / Stripe leak in the input:

```json
{
  "type": "guardrails/v1/secretKeysCheck",
  "parameters": { "permissiveness": "PERMISSIVE" }
}
```

Catch broad random-string secrets too, and skip detection inside code blocks the user pastes for debugging:

```json
{
  "type": "guardrails/v1/secretKeysCheck",
  "parameters": {
    "permissiveness": "STRICT",
    "allowedFileExtensions": ["py", "js", "ts", "json"]
  }
}
```

Mask outbound LLM responses so any credential the model surfaces is redacted:

```json
{
  "type": "guardrails/v1/secretKeysSanitize",
  "parameters": { "permissiveness": "BALANCED" }
}
```

---

## Edge Cases

- **`SHA:` and `Bearer ` prefixes**: not in the detector. The matcher's token alphabet excludes spaces and colons, so these never assemble into a single match. To flag bearer tokens in HTTP headers, route the request body through a Custom Regex pattern like `Bearer\\s+[A-Za-z0-9._\\-]{20,}`.
- **Code-fence allowlist**: only suppresses inside markdown code fences (\`\`\`py … \`\`\`). It does not suppress inline backticks or unfenced code samples.
- **JWT detection**: base-64-with-dots heuristic, no signature verification. A string that *looks* like a JWT but isn't (three random segments separated by dots) is still flagged. Pair with Custom Regex allowlists if your domain has JWT-shaped non-secret IDs.
