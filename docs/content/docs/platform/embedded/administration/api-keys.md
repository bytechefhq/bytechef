---
title: API Keys
description: Create API keys for server-to-server calls from your backend to the embedded public API.
---

<!-- TODO screenshot: Embedded → Settings - the settings landing page showing the available sections -->

An embedded API Key is a bearer token for server-to-server calls from your backend to the embedded public API, for example listing a user's integrations, executing a component action, or reading workflow executions. Do not expose an API Key in a browser or other client-side code. Browser-side calls use a JWT signed with a [Signing Key](/platform/embedded/administration/signing-keys).

An API Key acts on behalf of the connected user named in the request path. It is accepted only on the `/api/embedded/v1/{externalUserId}/…` routes. A request without an external user id in the path is rejected. If no connected user exists for that id, ByteChef creates one on the first request.

API Keys are managed under **Embedded → Settings → API Keys**. Only tenant admins can open the page.

<!-- TODO screenshot: Embedded → Settings → API Keys - the keys table and the New API Key button -->

## Creating an API Key

1. Select the environment (Development, Staging or Production) in which the key is used. A key authenticates only requests sent to the same environment.
2. Go to **Embedded → Settings → API Keys** and click **New API Key**.
3. Enter a **Name** (at least 2 characters).
4. Click **Create API Key**.
5. Click **Copy** and store the secret key in your backend. The dialog shows it once. After you click **Done**, it cannot be displayed again.

To rename a key, click the edit icon in its row, change the **Name** and click **Save**. To revoke a key, click the delete icon in its row.

## Using an API Key

Send the secret as a bearer token and name the connected user in the path:

```http
GET /api/embedded/v1/user-42/integrations HTTP/1.1
Authorization: Bearer <your-api-key-secret>
X-Environment: DEVELOPMENT
```

| Header | Description |
|---|---|
| `Authorization` | `Bearer <secret>`. Only API Keys created on this page (type Embedded) are accepted. |
| `X-Environment` | `DEVELOPMENT`, `STAGING` or `PRODUCTION` (case-insensitive). Defaults to `PRODUCTION`. Must match the environment the key was created in. |

API Keys are not accepted on the `/api/embedded/internal` endpoints. Those carry no user id in the path and are reached from a ByteChef admin session or with a Signing Key JWT (which is how the embedded workflow builder authenticates from your end user's browser). Requests for a connected user that an admin has disabled are rejected.

## Table columns

| Column | Description |
|---|---|
| **Name** | The name you gave the key. |
| **Secret Key** | Obfuscated. The full value is shown only at creation. |
| **Created Date** | When the key was created. |
| **Last Used Date** | When the key last authenticated a request. Updated at most once a minute. |
| **Created By** | The admin who created the key. |

## Replacing a lost key

A lost secret cannot be recovered. Create a new key, deploy it to your backend, and delete the lost key.

## Related

- [Signing Keys](/platform/embedded/administration/signing-keys)
- [Quick Start](/platform/embedded/get-started/quick-start)
