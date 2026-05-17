# PII

`PII` is a rule-based detector for personally-identifiable information. It runs in the **preflight
stage** of `CheckForViolations` (or `SanitizeText`) and emits matched spans for the parent
advisor's longest-first masking pass.

## What it detects

The built-in entity catalogue covers globally-recognized identifiers and several locale-specific
forms:

- **Global**: `EMAIL_ADDRESS`, `PHONE_NUMBER`, `CREDIT_CARD`, `IP_ADDRESS` (v4), `IBAN_CODE`,
  `DATE_TIME`, `LOCATION`, `MEDICAL_LICENSE`, `CRYPTO`
- **United States**: `US_SSN`, `US_DRIVER_LICENSE`, `US_PASSPORT`, `US_BANK_NUMBER`, `US_ITIN`
- **United Kingdom**: `UK_NHS`, `UK_NINO`
- **Italy**: `IT_FISCAL_CODE`, `IT_DRIVER_LICENSE`, `IT_VAT_CODE`, `IT_IDENTITY_CARD`,
  `IT_PASSPORT`
- **Spain, Poland, Singapore, Australia, India, Finland**: regional national-ID and passport
  formats

Run `PiiDetector.getPiiDetectionOptions()` in code or open the `Entities` dropdown in the editor
for the live list — new entities are added behind the same property key without a cluster-element
version bump.

## Properties

| Property | Required | Default | Description |
|---|:---:|:---:|---|
| `type` | yes | `ALL` | `ALL` scans every built-in entity. `SELECTED` enables only the entities listed in `entities` |
| `entities` | conditional | — | Subset of entity types to scan when `type = SELECTED`. Hidden when `type = ALL` |

There is no `customRegexes` property on this guardrail. For project-specific patterns, attach the
standalone **Custom Regex** action alongside `PII` in the same `CheckForViolations` parent. Both
run in the preflight stage; their masks are merged in the same longest-first pass so overlap (an
internal id contained inside an email, etc.) is handled correctly.

## Two cluster elements

`PII` exposes two cluster elements with the same configuration surface:

- **`piiCheck`** — `CHECK_FOR_VIOLATIONS` type. Emits a `Violation.PatternViolation` so the
  parent advisor blocks the request and lists the entity types in the violation's diagnostic
  info.
- **`piiSanitize`** — `SANITIZE_TEXT` type. Emits the same mask entities but the parent advisor
  rewrites the text instead of blocking.

Both implement `PreflightMasking` so the parent's mask pass merges PII matches with secret-key,
URL, and custom-regex matches.

## Mask placeholders

Each entity type renders as `<TYPE>` — `<EMAIL_ADDRESS>`, `<US_SSN>`, `<CREDIT_CARD>`, etc. The placeholder
is stable across runs so downstream tools (a chat-memory store, a logging pipeline) can rely on
matching on the placeholder string.

## Example

Scan everything, block on any hit:

```json
{
  "type": "guardrails/v1/piiCheck",
  "parameters": { "type": "ALL" }
}
```

Scan only EU-relevant identifiers, mask in place:

```json
{
  "type": "guardrails/v1/piiSanitize",
  "parameters": {
    "type": "SELECTED",
    "entities": ["EMAIL_ADDRESS", "PHONE_NUMBER", "IBAN_CODE", "IT_FISCAL_CODE", "UK_NINO"]
  }
}
```

## Edge cases

- **Overlap with URLs guardrail**: emails contain a domain-shaped substring; URLs are handled by
  the separate `URLs` guardrail, not by PII. The advisor's longest-first mask pass across
  PII + URLs + secret-key + custom-regex matches guarantees the email is masked as a whole
  `<EMAIL_ADDRESS>` rather than being split.
- **Overlap with secret keys**: a JWT looks like base64 with two dots. PII does not classify
  JWTs, but if you author a custom regex that matches both, the longest match wins.
- **Phone numbers**: the phone regex matches the common `NNN-NNN-NNNN` shape, with an optional
  country code (`+1`), optional parentheses around the area code, and dots, dashes, or
  whitespace as separators. Order numbers and tracking IDs that happen to fit that shape will
  match; free-form digit runs that do not fit the 3-3-{4-6} grouping will not. If your domain
  has structured non-PII numbers that match the shape, pair PII with a Custom Regex allowlist.

## Known false positives

The PII detector is intentionally configured for maximum recall, so the following inputs match
even though no actual PII is present:

- Bare numeric runs (`123456789`) match `US_BANK_NUMBER`, `AU_TFN`, and `US_SSN` (the
  no-dash 9-digit form). Page references, order IDs, and version-string components frequently
  trip these.
- IPv4-shaped dotted quads (`1.2.3.4`, `0.0.0.0`) match `IP_ADDRESS` regardless of whether
  they are actually IPs or, for example, version-string segments.
- `[A-Z]\d{7}` and `[A-Z]\d{8}` shapes are shared by US_DRIVER_LICENSE, IT_PASSPORT, ES_NIF,
  ES_NIE, SG_NRIC_FIN, US_PASSPORT, and others. An order ID like `D1234567` will fire as
  `US_DRIVER_LICENSE`.

If your workflow processes prose with structured numeric IDs that genuinely are not PII, use
`TYPE='SELECTED'` (with an explicit `entities` list) to disable the broad numeric detectors
that misfire, or combine PII with a Custom Regex allowlist that re-permits known-good shapes
before the PII pass runs.
