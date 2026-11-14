---
title: PII
description: Rule-based detection of personal data such as emails, phone numbers, credit cards, IBANs, and regional ID numbers.
---

**PII** finds personally identifiable information with regular expressions. It runs in the preflight stage, needs no model, and returns the same result for the same text every time. Attach it under [Check for Violations](/platform/automation/build/workflows/ai/agent/guardrails/check-for-violations) to block text that contains personal data, or under [Sanitize Text](/platform/automation/build/workflows/ai/agent/guardrails/sanitize-text) to replace each value with a `<ENTITY_TYPE>` placeholder.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| **Validate Input** | Boolean | `true` | Check the user input before it is sent to the model. |
| **Validate Output** | Boolean | `true` | Check the model response before it is returned. |
| **Type** | String | `ALL` | **All** scans for every entity type. **Selected** scans only the types in **Entities**. Required. |
| **Entities** | Array of strings | None | The entity types to scan for. Shown only when **Type** is **Selected**. At least one is required when **Type** is **Selected**; an empty list is an execution failure. |

## Entity types

| Region | Entity types |
|---|---|
| Global | `EMAIL_ADDRESS`, `PHONE_NUMBER`, `CREDIT_CARD`, `IP_ADDRESS` (IPv4), `IBAN_CODE`, `CRYPTO` (Bitcoin address), `DATE_TIME` (ISO-8601 date or date-time), `LOCATION` (street address ending in Street, Avenue, Road, and similar), `MEDICAL_LICENSE` |
| United States | `US_SSN`, `US_BANK_NUMBER`, `US_DRIVER_LICENSE`, `US_ITIN`, `US_PASSPORT` |
| United Kingdom | `UK_NHS`, `UK_NINO` |
| Spain | `ES_NIF`, `ES_NIE` |
| Italy | `IT_FISCAL_CODE`, `IT_VAT_CODE`, `IT_DRIVER_LICENSE`, `IT_PASSPORT`, `IT_IDENTITY_CARD` |
| Poland | `PL_PESEL` |
| Singapore | `SG_NRIC_FIN`, `SG_UEN` |
| Australia | `AU_ABN`, `AU_ACN`, `AU_TFN`, `AU_MEDICARE` |
| India | `IN_AADHAAR`, `IN_PAN`, `IN_PASSPORT`, `IN_VEHICLE_REGISTRATION`, `IN_VOTER` |
| Finland | `FI_PERSONAL_IDENTITY_CODE` |

Detection checks the format only. It does not validate checksums, so any value with the right shape matches.

## Results

- **Under Check for Violations** (`piiCheck`): one violation listing the matched entity types in `info.entityTypes`, with `matchCount` set to the number of matched values.
- **Under Sanitize Text** (`piiSanitize`): each match is replaced with `<ENTITY_TYPE>`, for example `<EMAIL_ADDRESS>` or `<US_SSN>`. When PII overlaps a match from Secret Keys, URLs, or Keywords, the longer match wins.

## Examples

Block any personal data:

```json
{ "name": "pii_1", "type": "pii/v1/piiCheck", "parameters": { "type": "ALL" } }
```

Mask only a few types:

```json
{
  "name": "pii_1",
  "type": "pii/v1/piiSanitize",
  "parameters": {
    "type": "SELECTED",
    "entities": ["EMAIL_ADDRESS", "PHONE_NUMBER", "IBAN_CODE", "IT_FISCAL_CODE", "UK_NINO"]
  }
}
```

## False positives

With **Type** set to **All**, several broad patterns match ordinary numbers and codes:

| Entity type | Matches |
|---|---|
| `US_BANK_NUMBER` | Any run of 8 to 17 digits, such as order numbers. |
| `US_SSN`, `AU_TFN` | Any run of 9 digits. |
| `PL_PESEL` | Any run of 11 digits. |
| `PHONE_NUMBER` | Three digits, three digits, then four to six digits, with optional `+`, parentheses, and `-`, `.`, or space separators. |
| `DATE_TIME` | Any ISO-8601 date, such as `2026-01-31`. |
| `MEDICAL_LICENSE` | Two capital letters followed by six digits. |

If your text contains such values, set **Type** to **Selected** and choose only the types you need.

## Limitations

- There is no entity type for names of people.
- Values written in a free form, such as a street address that does not end in a street suffix, are not detected. [LLM PII](/platform/automation/build/workflows/ai/agent/guardrails/llm-pii) asks a model to find the same entity types and does not depend on a fixed format.
- Only IPv4 addresses are detected.
- For identifiers specific to your business, add [Custom Regex](/platform/automation/build/workflows/ai/agent/guardrails/custom-regex) to the same parent.

## Related

- [LLM PII](/platform/automation/build/workflows/ai/agent/guardrails/llm-pii)
- [pii/v1 reference](/reference/components/pii_v1)
