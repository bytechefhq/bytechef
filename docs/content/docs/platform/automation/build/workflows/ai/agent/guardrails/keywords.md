---
title: Keywords
description: Whole-word matching against a keyword list, to block or mask listed terms.
---

**Keywords** finds the words and phrases in a list you configure. It runs in the preflight stage and needs no model. Attach it under [Check for Violations](/platform/automation/build/workflows/ai/agent/guardrails/check-for-violations) to block text that contains a listed keyword, or under [Sanitize Text](/platform/automation/build/workflows/ai/agent/guardrails/sanitize-text) to replace each occurrence with `<KEYWORD>`. Typical uses are competitor names, internal code names, and banned terms.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| **Validate Input** | Boolean | `true` | Check the user input before it is sent to the model. |
| **Validate Output** | Boolean | `true` | Check the model response before it is returned. |
| **Keywords** | Array of strings | None | The words or phrases to find. Required, with at least one entry; an empty list is an execution failure. |
| **Case Sensitive** | Boolean | `false` | When `false`, matching ignores case. |

## Matching rules

- **Whole words.** A keyword matches only when it is not directly preceded or followed by a letter, digit, or underscore (in any language). The keyword `cat` matches `the cat sat`, `cat,`, and `cat!`, but not `cats`, `category`, or `caterpillar`.
- **Literal text.** Each keyword is matched exactly as written. Regular-expression characters such as `.` or `*` have no special meaning, and punctuation or spaces in an entry are part of the keyword: `foo,` matches only `foo` followed by a comma.
- **Raw text.** Under Check for Violations, Keywords never masks and always sees the text as the user wrote it, so a keyword that overlaps an email address or a secret still matches.
- Empty entries are skipped.

## Results

- **Under Check for Violations** (`keywordsCheck`): one violation, with `matchCount` set to the number of distinct keywords found.
- **Under Sanitize Text** (`keywordsSanitize`): every occurrence is replaced with `<KEYWORD>`. When a keyword overlaps a longer match from PII, Secret Keys, or URLs, the longer match wins.

## Examples

Block three competitor names:

```json
{
  "name": "keywords_1",
  "type": "keywords/v1/keywordsCheck",
  "parameters": { "keywords": ["CompetitorA", "CompetitorB", "CompetitorC"], "caseSensitive": false }
}
```

Mask the same names in the model response instead of blocking:

```json
{
  "name": "keywords_1",
  "type": "keywords/v1/keywordsSanitize",
  "parameters": { "keywords": ["CompetitorA", "CompetitorB", "CompetitorC"], "validateInput": false }
}
```

## When to use another detector

| Need | Use |
|---|---|
| Structured values such as account numbers or ticket IDs | [Custom Regex](/platform/automation/build/workflows/ai/agent/guardrails/custom-regex) |
| Misspellings, paraphrases, or indirect mentions | [Custom](/platform/automation/build/workflows/ai/agent/guardrails/custom) with a classifier prompt |
| Emails, phone numbers, and other personal data | [PII](/platform/automation/build/workflows/ai/agent/guardrails/pii) |

## Related

- [keywords/v1 reference](/reference/components/keywords_v1)
