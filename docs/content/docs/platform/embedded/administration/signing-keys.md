---
title: Signing Keys
description: Create and rotate the RSA keys your backend uses to sign connected-user JWTs for the embedded SDK and API.
---

<!-- TODO screenshot: Embedded → Settings - the settings landing page showing the available sections -->

A Signing Key lets ByteChef verify that a JWT presented by the frontend SDK was issued by your backend. When you create a Signing Key, ByteChef generates an RSA 2048-bit key pair. It stores the public key and shows you the private key once.

Your backend signs a short-lived JWT for each end user with the private key and passes it to the SDK (for example `useConnectDialog`) or sends it as a bearer token to the embedded API. The JWT subject (`sub`) is the user's external id, and ByteChef resolves or creates the matching connected user from it.

Signing Keys are managed under **Embedded → Settings → Signing Keys**. Only tenant admins can open the page, and any tenant admin can view, rename or delete every key in the environment.

<!-- TODO screenshot: Embedded → Settings → Signing Keys - the signing-keys table with the New Signing Key button -->

## Creating a Signing Key

1. Select the environment (Development, Staging or Production) in which the key is used. A key verifies only tokens sent to the same environment.
2. Go to **Embedded → Settings → Signing Keys** and click **New Signing Key**.
3. Enter a **Name** (at least 2 characters).
4. Click **Create Signing Key**.
5. Click **Copy** and store the private key as a secret in your backend. The dialog shows it once and ByteChef does not store it. After you click **Done**, it cannot be displayed again.
6. Copy the **Key Id** from the table. Hover over the value and click the copy icon.

To rename a key, click the edit icon in its row, change the **Name** and click **Save**.

## Signing a JWT

Sign each end-user JWT with algorithm `RS256` and set the JWT header `kid` to the Key Id. See [Quick Start](/platform/embedded/get-started/quick-start) for a Node.js example.

| Claim | Type | Required | Description |
|---|---|---|---|
| `sub` | string | Yes | The external id of your end user. When the request path contains an external user id (`/api/embedded/v1/{externalUserId}/…`), it must equal `sub`, or the request is rejected. |
| `exp` | number | Recommended | Expiry time. Keep tokens short-lived (for example 10 minutes) and mint a new one per session. |
| `connectionDialogAllowed` | boolean | No | Default `true`. Set `false` to stop the user from creating new connections in the embedded workflow builder and the embedded connections API. A non-boolean value makes the token invalid. |
| `environmentId` | number | No | When present, must match the request environment (`0` Development, `1` Staging, `2` Production), or the request is rejected. |

A decoded token for the connected user `user-123`, valid in Production and barred from creating new connections, looks like this:

```json
{
  "header": {"alg": "RS256", "kid": "<key-id-from-the-table>", "typ": "JWT"},
  "payload": {
    "sub": "user-123",
    "iat": 1779098400,
    "exp": 1779099000,
    "environmentId": 2,
    "connectionDialogAllowed": false
  }
}
```

The request environment comes from the `X-Environment` header (`DEVELOPMENT`, `STAGING` or `PRODUCTION`, case-insensitive). Without the header, ByteChef uses `PRODUCTION`.

## Table columns

| Column | Description |
|---|---|
| **Name** | The name you gave the key. |
| **Key Id** | The `kid` value that goes into the JWT header. |
| **Created Date** | When the key was created. |
| **Last Used Date** | When a JWT signed with this key was last accepted. Updated at most once a minute. |
| **Created By** | The admin who created the key. |

## Rotating a key

1. Create a new Signing Key.
2. Deploy the new private key and Key Id to your backend.
3. When all tokens are signed with the new key, delete the old key with the delete icon in its row. Tokens signed with a deleted key are rejected.

## Related

- [API Keys](/platform/embedded/administration/api-keys)
- [Quick Start](/platform/embedded/get-started/quick-start)
- [Syncing Connected Users](/platform/embedded/get-started/initial-setup/syncing-connected-users-programmatically)
