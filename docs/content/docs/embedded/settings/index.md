---
title: Settings
description: Manage Signing Keys and API Keys for your embedded integration.
---

![Settings overview](settings-overview.png)

---

The **Embedded → Settings** area holds the two credential types your application uses to talk to ByteChef:

- **Signing Keys** — RSA keypairs used to sign JWTs that authenticate your **end users** into the embedded session.
- **API Keys** — secret tokens used for **server-to-server** access from your backend to ByteChef's admin/internal APIs.

Both pages are admin-only.

---

## Signing Keys

![Signing Keys table](settings-signing-keys.png)

Signing Keys are how ByteChef trusts that a JWT presented by the React SDK really came from your backend. When you create a Signing Key, ByteChef generates an RSA 2048-bit keypair — the public key is stored, the private key is shown to you once.

Your backend then signs short-lived JWTs (algorithm `RS256`, set the JWT header `kid` to the Key Id) and passes them to `useConnectDialog` or `fetch` calls against the embedded API.

### Key Features

| Feature | Description |
|---|---|
| One-time private key display | The private key appears once at creation time. Save it securely (e.g. as a backend secret). |
| Key Id (`kid`) | A stable identifier for the public key. Goes into the JWT header so ByteChef can verify the signature. |
| Last Used Date | When a JWT signed with this key was last verified — handy for spotting unused or rotated keys. |
| Rotation | Create a new Signing Key, deploy the new private key in your backend, then delete the old key. |

### Table Columns

| Column | Description |
|---|---|
| Name | The friendly name you gave the key. |
| Key Id | The `kid` value. Hover to copy. |
| Created Date | When the key was created. |
| Last Used Date | When a JWT signed with this key was last accepted. Empty if never used. |
| Created By | Username of the admin who created it. |

### How to Use

#### Creating a Signing Key

1. Click **New Signing Key** in the top-right corner.
2. Enter a **Name** (at least 2 characters).
3. Click **Save**.
4. **Copy the private key now** — the dialog shows it once and ByteChef does not store it. After you click **Done**, it's gone.

#### Using the key in your backend

Sign each end-user session JWT with the private key. See **[Quick Start step 5](/embedded/quickstart)** for a Node.js example.

#### Rotating a key

1. Create a new Signing Key.
2. Roll out the new private key + `kid` to your backend.
3. Once you've confirmed traffic is using the new key (check **Last Used Date**), delete the old one.

---

## API Keys

![API Keys table](settings-api-keys.png)

API Keys are bearer tokens for **server-to-server** calls from your backend to ByteChef's embedded admin/internal APIs — for example, listing integrations, looking up connected users, or programmatically managing configurations.

They do **not** replace Signing Keys: end-user-scoped calls (those made on behalf of a specific connected user) must still use a JWT signed with a Signing Key.

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
4. **Copy the secret key now** — like Signing Keys, the secret appears once and is gone after you close the dialog.

#### Using the key

Pass it as a bearer token:

```http
GET /api/embedded/internal/integrations HTTP/1.1
Authorization: Bearer <your-api-key-secret>
X-Environment: DEVELOPMENT
```

#### Losing an API Key

If you lose the secret, you cannot recover it. Delete the lost key and create a replacement.
