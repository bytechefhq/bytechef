# platform-webhook-rest-impl

REST controllers for the public webhook surface, including the file-entry content endpoint that serves webhook-produced files to unauthenticated callers.

## File entry URL signing

The `/file-entries/{id}/content` endpoint serves files produced by webhook executions. It accepts two token formats:

- **Signed (preferred):** `v1.<exp>.<payload>.<sig>` — HMAC-SHA256 token with a TTL. Minted by `FileEntryTokens.toSignedToken(fileEntry)`.
- **Legacy:** base64-encoded `<extension>_;_<mime>_;_<name>_;_<url>` — unsigned, no expiry. Accepted while `bytechef.file-storage.signed-url.required=false`. Each legacy access emits a rate-limited (≤ 1/min) WARN tagged with the `FILE_ENTRY_LEGACY` marker.

### Operator runbook

**Initial enablement:**

Signed URLs are enabled automatically. The signing key is derived from the existing `EncryptionKey` (which every ByteChef deployment has) via `HMAC-SHA256(decode(encryptionKey), "bytechef-file-storage-signed-url-v1")`, so there is no separate secret to generate or manage.

**Cutover to strict mode (after legacy traffic drains):**
1. Monitor logs for the `FILE_ENTRY_LEGACY` marker. When the rate drops to zero for ≥ 2 × default TTL, you can flip strict mode.
2. Set `BYTECHEF_FILE_STORAGE_SIGNED_URL_REQUIRED=true` and restart.
3. Legacy URLs (if any remain) will return 404. Roll back the flag if you see legitimate traffic affected.

**Key rotation:**

The signing key changes only when the encryption key changes. Rotate the encryption key to rotate the signing key; all in-flight signed URLs minted before the rotation become invalid immediately.
