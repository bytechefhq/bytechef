---
title: Secret Keys
description: Rule-based detection of API keys, tokens, and other credential-shaped strings.
---

**Secret Keys** finds API keys, access tokens, and other strings that look like credentials. It runs in the preflight stage and needs no model. Attach it under [Check for Violations](/platform/automation/build/workflows/ai/agent/guardrails/check-for-violations) to block text that contains a secret, or under [Sanitize Text](/platform/automation/build/workflows/ai/agent/guardrails/sanitize-text) to replace each secret with a `<SECRET_TYPE>` placeholder.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| **Validate Input** | Boolean | `true` | Check the user input before it is sent to the model. |
| **Validate Output** | Boolean | `true` | Check the model response before it is returned. |
| **Permissiveness** | String | `BALANCED` | How aggressively to scan: **Strict** (`STRICT`), **Balanced** (`BALANCED`), or **Permissive** (`PERMISSIVE`). See [Permissiveness levels](#permissiveness-levels). |
| **Allowed File Extensions** | Array of strings | None | Language tags of Markdown fenced code blocks to skip, for example `py`, `js`, `ts`. Detection still runs outside those blocks. |

## What it detects

Secret Keys combines four detectors. Each match is reported with a type:

| Type | Detects | Levels |
|---|---|---|
| `AWS_ACCESS_KEY` | `AKIA` followed by 16 characters | All |
| `AWS_SECRET_KEY` | A quoted 40-character value shortly after the word `aws` | All |
| `GITHUB_PAT` | `ghp_` followed by 36 characters | All |
| `GITHUB_FINE_GRAINED_PAT` | `github_pat_` followed by 82 characters | All |
| `SLACK_TOKEN` | `xoxa-`, `xoxb-`, or `xoxp-` tokens | All |
| `STRIPE_KEY` | `sk_live_`, `sk_test_`, `pk_live_`, or `pk_test_` keys | All |
| `GOOGLE_API_KEY` | `AIza` followed by 35 characters | All |
| `OPENAI_KEY` | `sk-` followed by at least 20 characters | All |
| `JWT` | Three dot-separated segments starting with `ey` | All |
| `PREFIXED_SECRET` | Any token starting with `key-`, `sk-`, `sk_`, `pk_`, `pk-`, `ghp_`, `AKIA`, `xox`, `SG.`, `hf_`, `api-`, `apikey-`, `token-`, or `secret-` | All |
| `HIGH_ENTROPY_TOKEN` | Random-looking strings. The thresholds depend on the level. | All |
| `GENERIC_SECRET` | Assignments such as `api_key=...`, `secret: ...`, `token=...`, `password=...` with a value of 16 or more characters | Strict only |

### Permissiveness levels

The levels differ in how the high-entropy detector judges a random-looking string, and in whether generic assignments are flagged.

| Level | Minimum length | Minimum entropy (bits per character) | Minimum character classes | Generic assignments |
|---|---|---|---|---|
| **Strict** | 10 | 3.0 | 2 | Yes |
| **Balanced** (default) | 10 | 3.8 | 3 | No |
| **Permissive** | 30 | 4.0 | 2 | No |

Character classes are lowercase letters, uppercase letters, digits, and symbols. In **Balanced** and **Permissive**, the high-entropy detector also skips the parts of `http://` and `https://` URLs and tokens that end in a common file extension, such as `report.pdf`. Use **Permissive** when your text legitimately contains long random IDs.

## Results

- **Under Check for Violations** (`secretKeysCheck`): one violation listing the matched types in `info.providerTypes`.
- **Under Sanitize Text** (`secretKeysSanitize`): each match is replaced with `<TYPE>`, for example `<GITHUB_PAT>` or `<HIGH_ENTROPY_TOKEN>`.

## Examples

Block known provider keys while tolerating long random IDs:

```json
{ "name": "secretKeys_1", "type": "secretKeys/v1/secretKeysCheck", "parameters": { "permissiveness": "PERMISSIVE" } }
```

Scan broadly, but skip code blocks that users paste for debugging:

```json
{
  "name": "secretKeys_1",
  "type": "secretKeys/v1/secretKeysCheck",
  "parameters": { "permissiveness": "STRICT", "allowedFileExtensions": ["py", "js", "ts", "json"] }
}
```

Mask any credential the model repeats in its response:

```json
{
  "name": "secretKeys_1",
  "type": "secretKeys/v1/secretKeysSanitize",
  "parameters": { "permissiveness": "BALANCED", "validateInput": false }
}
```

## Limitations

- **Allowed File Extensions** only applies to fenced code blocks whose opening fence carries one of the listed tags, such as ` ```py `. Inline code and code without a fence are scanned.
- A token is a run of letters, digits, `-`, `_`, and `.`. A secret that contains other characters, such as `/` or `+`, is split and may not be detected by the high-entropy detector.
- `Bearer` tokens in HTTP headers are not detected as a type of their own. Add a [Custom Regex](/platform/automation/build/workflows/ai/agent/guardrails/custom-regex) pattern such as `Bearer\s+[A-Za-z0-9._\-]{20,}`.
- JWT detection checks the shape only, not the signature. Any three dot-separated segments starting with `ey` match.

## Related

- [Custom Regex](/platform/automation/build/workflows/ai/agent/guardrails/custom-regex)
- [secretKeys/v1 reference](/reference/components/secretKeys_v1)
