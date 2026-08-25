---
title: Signing Keys
description: Manage Signing Keys for your embedded integration.
---

<!-- TODO screenshot: Embedded → Settings - the settings landing page showing the available sections -->

---

## Signing Keys

<!-- TODO screenshot: Embedded → Settings → Signing Keys - the signing-keys table with the New Signing Key button -->

Signing Keys are how ByteChef trusts that a JWT presented by the React SDK really came from your backend. When you create a Signing Key, ByteChef generates an RSA 2048-bit keypair - the public key is stored, the private key is shown to you once.

Your backend then signs short-lived JWTs (algorithm `RS256`, set the JWT header `kid` to the Key Id) and passes them to `useConnectDialog` or `fetch` calls against the embedded API.

### Key Features

| Feature | Description |
|---|---|
| One-time private key display | The private key appears once at creation time. Save it securely (e.g. as a backend secret). |
| Key Id (`kid`) | A stable identifier for the public key. Goes into the JWT header so ByteChef can verify the signature. |
| Last Used Date | When a JWT signed with this key was last verified - handy for spotting unused or rotated keys. |
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
4. **Copy the private key now** - the dialog shows it once and ByteChef does not store it. After you click **Done**, it's gone.

#### Using the key in your backend

Sign each end-user session JWT with the private key. See **[Quick Start step 5](/platform/embedded/get-started/quick-start)** for a Node.js example.

#### Rotating a key

1. Create a new Signing Key.
2. Roll out the new private key + `kid` to your backend.
3. Once you've confirmed traffic is using the new key (check **Last Used Date**), delete the old one.
