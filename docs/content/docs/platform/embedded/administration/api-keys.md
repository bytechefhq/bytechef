---
title: API Keys
description: Manage API Keys for your embedded integration.
---

<!-- TODO screenshot: Embedded → Settings - the settings landing page showing the available sections -->

---

## API Keys

<!-- TODO screenshot: Embedded → Settings → API Keys - the keys table and the New API Key button -->

API Keys are bearer tokens for **server-to-server** calls from your backend to the embedded public API - for example listing a user's integrations, executing a component action, or reading their workflow executions.

They act on behalf of a connected user, just like a Signing Key JWT does; the difference is only in how that user is named. An API Key is accepted **only** on the `/api/embedded/v1/{externalUserId}/…` routes, where the path segment names the user. A request without that segment has no user to resolve and is rejected, so browser-originated calls (which have no user id in the path) still need a JWT signed with a Signing Key.

### Key Features

| Feature | Description |
|---|---|
| One-time secret display | The secret key appears once at creation. Save it securely. |
| Environment scoping | API keys are created against a specific environment (Development / Stage / Production). |
| Last Used Date | When the key was last used to authenticate a request. |

### Table Columns

| Column | Description |
|---|---|
| Name | The friendly name you gave the key. |
| Secret Key | Obfuscated. Visible only at creation. |
| Created Date | When the key was created. |
| Last Used Date | When the key was last used. Empty if never used. |
| Created By | Username of the admin who created it. |

### How to Use

#### Creating an API Key

1. Click **New API Key** in the top-right corner.
2. Enter a **Name**.
3. Click **Save**.
4. **Copy the secret key now** - like Signing Keys, the secret appears once and is gone after you close the dialog.

#### Using the key

Pass it as a bearer token against the public API's `/{externalUserId}`-prefixed routes - the connected user is identified by the path segment:

```http
GET /api/embedded/v1/user-42/integrations HTTP/1.1
Authorization: Bearer <your-api-key-secret>
X-Environment: DEVELOPMENT
```

API Keys are not accepted on the `/api/embedded/internal` endpoints: those carry no user id in the path, so they are reached either from a ByteChef admin session or with a Signing Key JWT (which is how the embedded workflow builder authenticates from your end user's browser).

#### Losing an API Key

If you lose the secret, you cannot recover it. Delete the lost key and create a replacement.
