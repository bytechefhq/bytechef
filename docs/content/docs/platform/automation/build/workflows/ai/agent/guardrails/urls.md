---
title: URLs
description: Allowlist-based URL policy with host, path, port, scheme, credential, subdomain, and IPv4 range controls.
---

**URLs** finds URLs in the text and flags or masks every URL that is not on your allowlist. It runs in the preflight stage and needs no model. Attach it under [Check for Violations](/platform/automation/build/workflows/ai/agent/guardrails/check-for-violations) to block text with a disallowed URL, or under [Sanitize Text](/platform/automation/build/workflows/ai/agent/guardrails/sanitize-text) to replace each disallowed URL with `<URL>`.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| **Validate Input** | Boolean | `true` | Check the user input before it is sent to the model. |
| **Validate Output** | Boolean | `true` | Check the model response before it is returned. |
| **Allowed URLs** | Array of strings | Empty | Hosts, host and path prefixes, full URLs, or IPv4 CIDR ranges that are permitted. When empty, every URL with a `://` scheme is disallowed. |
| **Allowed Schemes** | Array of strings | `https`, `http` | Permitted schemes. Options: `https`, `http`, `ftp`, `data`, `javascript`, `vbscript`, `mailto`. A URL with any other scheme is disallowed, even if its host is on the allowlist. |
| **Block Userinfo** | Boolean | `true` | Disallow URLs that contain credentials, such as `https://user:pass@example.com`, even if the host is allowed. Labeled **Sanitize Userinfo** under Sanitize Text. |
| **Allow Subdomain** | Boolean | `true` | Extend host and full-URL entries to their subdomains. When on, `example.com` also allows `api.example.com` and `staging.api.example.com`. When off, only the exact host matches. |

## Which URLs are checked

| Form | Example | How it is checked |
|---|---|---|
| Any `scheme://` URL | `https://example.com/a`, `ftp://files.example.com` | Credentials first (**Block Userinfo**), then the scheme (**Allowed Schemes**), then the host, port, and path (**Allowed URLs**). |
| `data:`, `javascript:`, `vbscript:`, `mailto:` | `javascript:alert(1)` | Scheme only. Disallowed unless the scheme is in **Allowed Schemes**. |

Hosts without a scheme (`example.com`) and bare IP addresses are not detected. To block them, use [Keywords](/platform/automation/build/workflows/ai/agent/guardrails/keywords) or [Custom Regex](/platform/automation/build/workflows/ai/agent/guardrails/custom-regex).

## Allowlist entries

| Form | Example | Matches |
|---|---|---|
| Host | `example.com` | That host on any port and path, plus subdomains when **Allow Subdomain** is on. |
| Host and path | `example.com/docs` | That exact host (subdomains are not included) and a path that is `/docs` or starts with `/docs/`. `/documents` does not match. |
| Full URL | `https://localhost:5173/admin` | The host (plus subdomains when **Allow Subdomain** is on), the port if the entry has one, and the path prefix if the entry has a path. The entry's scheme is not compared; schemes are controlled by **Allowed Schemes**. |
| IPv4 CIDR range | `10.0.0.0/8` | Any URL whose host is an IPv4 address in the range. The prefix must be `0` to `32`. |

Matching ignores case and a leading `www.` on both the URL and the entry. Internationalized host names are converted to their ASCII (punycode) form on both sides, so an entry written in Unicode matches the punycode form of the same host. An entry that cannot be parsed matches nothing and is logged as a warning.

## Results

- **Under Check for Violations** (`urlsCheck`): one violation, with `matchCount` set to the number of disallowed URLs. The reason for each URL (host, scheme, credentials, or malformed URL) is not included in the output.
- **Under Sanitize Text** (`urlsSanitize`): each disallowed URL is replaced with `<URL>`. Allowed URLs are left as they are.

## Examples

Allow only a local development server:

```json
{
  "name": "urls_1",
  "type": "urls/v1/urlsCheck",
  "parameters": {
    "allowedUrls": ["http://localhost:5173"],
    "allowedSchemes": ["http", "https"],
    "allowSubdomain": false,
    "blockUserinfo": true
  }
}
```

Allow a production API with its subdomains, plus a private network range:

```json
{
  "name": "urls_1",
  "type": "urls/v1/urlsCheck",
  "parameters": {
    "allowedUrls": ["https://api.example.com", "10.0.0.0/8"],
    "allowedSchemes": ["https"],
    "allowSubdomain": true
  }
}
```

Mask every URL in the model response except links to your documentation:

```json
{
  "name": "urls_1",
  "type": "urls/v1/urlsSanitize",
  "parameters": { "allowedUrls": ["docs.example.com"], "allowedSchemes": ["https"], "validateInput": false }
}
```

## Limitations

- CIDR entries cover IPv4 only. A URL with an IPv6 host is disallowed unless an entry matches it as a host.
- A URL that cannot be parsed is disallowed.

## Related

- [urls/v1 reference](/reference/components/urls_v1)
