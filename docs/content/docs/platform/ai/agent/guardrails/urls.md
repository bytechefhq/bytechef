---
title: URLs
description: Allowlist-based URL policy enforcement with scheme, port, userinfo, and subdomain controls
---

`URLs` enforces an allowlist on the URLs that may appear in the input. It runs in the **preflight stage**, recognises two URL shapes (full-scheme URLs and single-colon schemes), and either flags or masks anything outside the policy.

---

## Properties

| Property | Description |
|---|---|
| **Allowed URLs** | Allowlist of URLs, host names, full URLs, or IP / CIDR ranges. Empty means "everything is blocked" |
| **Allowed Schemes** | Schemes that are permitted (default: `https`, `http`). Other schemes — including `data:`, `javascript:`, `vbscript:` injection vectors — are rejected |
| **Block Userinfo** | When on, `https://user:pass@host/` is blocked even if the host is allowlisted (phishing defence). Default on |
| **Allow Subdomain** | When on, allowing `example.com` also allows `api.example.com`, `staging.api.example.com`. Default on |

---

## Allowlist Entry Forms

The **Allowed URLs** array accepts these forms; the detector picks the right matcher per entry:

| Form | Example | Matches |
|---|---|---|
| Bare host | `example.com` | Any URL whose host equals `example.com` (plus subdomains if **Allow Subdomain** is on). All paths and ports allowed |
| Host with path | `api.example.com/v2/` | Same host match, plus the URL's path must start with the entry's path. Trailing slash matters: `example.com/admin` matches `/admin` and `/admin/users` but not `/administrator` |
| Full URL | `https://localhost:5173` or `https://localhost:5173/admin` | Same host (with optional subdomain), exact port match if the entry specifies a port, and path-prefix match if the entry has a path. If the entry omits the port, any port on the host is allowed |
| CIDR range | `10.0.0.0/24` | Any IPv4 inside the range (prefix `0..32`) |

---

## Two Variants

- **URLs (check)** — flags violations (`HOST_NOT_ALLOWED`, `SCHEME_NOT_ALLOWED`, `USERINFO_BLOCKED`, `MALFORMED_URL`). Each blocked URL becomes one violation.
- **URLs (sanitize)** — replaces each blocked URL with `<URL>`. Stable placeholder so any downstream regex can post-process the masked text. The **Block Userinfo** label becomes **Sanitize Userinfo** in the sanitize variant since masking and blocking are different operator-facing actions.

---

## Examples

Block everything except your own dev server:

```json
{
  "type": "guardrails/v1/urlsCheck",
  "parameters": {
    "allowedUrls": ["http://localhost:5173"],
    "allowedSchemes": ["http", "https"],
    "allowSubdomain": false,
    "blockUserinfo": true
  }
}
```

Allow your prod API and any subdomain, plus an internal RFC-1918 range:

```json
{
  "type": "guardrails/v1/urlsCheck",
  "parameters": {
    "allowedUrls": [
      "https://api.example.com",
      "10.0.0.0/8"
    ],
    "allowedSchemes": ["https"],
    "allowSubdomain": true
  }
}
```

Mask all unrecognised URLs in the LLM's response so they don't leak to the user:

```json
{
  "type": "guardrails/v1/urlsSanitize",
  "parameters": {
    "allowedUrls": ["https://docs.example.com"],
    "allowedSchemes": ["https"]
  }
}
```

---

## Edge Cases

- **IPv6**: parsed as `HOST_NOT_ALLOWED` since the allowlist machinery only carries IPv4 entries. IPv6 URLs are flagged by default — to allow them, post-process with a regex or extend the detector.
- **Punycode IDN**: `xn--exmple-cua.com` is matched as a literal host. An allowlist entry of `example.com` does **not** match the punycode form. Add both spellings if you need to permit IDN traffic.
- **Bare hosts / IPs in prose**: bare domains and bare IPv4 are **not** detected. Only scheme-prefixed forms (`https://10.0.0.1`, `http://example.com`) are scanned. If you need to block bare-host references, prefix them or run a separate `Keywords` guardrail.
- **`javascript:` / `vbscript:` / `data:` schemes**: matched separately as injection vectors. Allowlist them via **Allowed Schemes** if you genuinely need to permit them; otherwise they're flagged regardless of the **Allowed URLs** list.
