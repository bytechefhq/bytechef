# MCP OAuth Dynamic Client Registration — Design

Date: 2026-10-13
Status: Draft for review

## Goal

A user picks a hosted MCP server, clicks **Sign in**, and gets a working connection without first creating an
OAuth app on the provider side. 87 of the 97 servers in the seed catalog
(`server/libs/modules/components/mcp-client/src/main/resources/mcp-registry/servers.json`) require this.

The mechanism follows the MCP authorization spec:

1. **Discover** — RFC 9728 protected resource metadata, then RFC 8414 / OpenID Connect authorization server metadata.
2. **Register** — RFC 7591 dynamic client registration.
3. **Authorize** — authorization code flow with PKCE (S256), sending the RFC 8707 `resource` parameter on the
   authorization request, the token exchange and every refresh.

## Decisions

| Decision | Choice |
|---|---|
| Scope | Generic platform OAuth2 capability; `mcp-client` is the first user. |
| Client lifetime | One registered client per connection, stored in the connection's encrypted parameters. A reconnect registers a fresh client and replaces the stored one. |
| Redirect handoff | Sealed authorization context: an AES-GCM blob produced by the server, held by the browser, posted back with the code. Stateless, no new table, multi-instance safe. |
| Dialog | The shared `ConnectionDialog` stays component-agnostic. No `mcpClient` special cases. |
| Authorization type | No new `AuthorizationType`. Discovery mode is `OAUTH2_AUTHORIZATION_CODE_PKCE` plus a new `protectedResourceUrl` function. |
| Embedded | Discovery mode is not available through the embedded (connected-user) flow in this iteration, because that flow computes OAuth2 parameters eagerly when serving integration DTOs. The embedded SDK still receives the `authorizationContext` pass-through. |

## Current State (relevant findings)

- `ConnectionDefinitionServiceImpl.getOAuth2AuthorizationParameters` computes the authorization URL, client id, scopes
  and extra query parameters. Functions involved cannot persist anything.
- `ConnectionFacadeImpl.resolveOAuth2AuthorizationCode` exchanges the code during `create` (and
  `replaceAuthorizationParameters` on reconnect) and merges `AuthorizationCallbackResponse.result()` into the
  connection's `EncryptedMapWrapper` parameters. That merged map is the only place a function's output is persisted.
- **Security defect (fixed by this design):** for `OAUTH2_AUTHORIZATION_CODE_PKCE`, the server puts `code_verifier`
  into `extraQueryParameters`, and both `client/src/shared/components/connection/oauth2/useOAuth2.ts` and
  `sdks/frontend/embedded/library/src/components/connect-dialog/useOAuth2.ts` spread those into the authorization URL,
  sending the verifier to the authorization server's front channel.
- `ConnectionFacadeImpl.create` strips only `state`; the used `code` and `code_verifier` are persisted.
- The embedded SDK never posts `code_verifier` back, so PKCE connections created through it cannot complete today.
- The default token exchange and refresh require `clientSecret` and never send `resource`.
- The MCP SDK's HTTP 401 is not a `ProviderException`, so `TokenRefreshAspect`'s refresh-on-401 never fires for
  `mcp-client`; only the scheduled `expires_in` refresh runs.
- `McpClientUtils.getAuthorizationType` infers the type from parameter presence; `Context` does not expose the stored
  authorization type.
- `TokenRefreshAspect` wraps `ClusterElementDefinitionServiceImpl.executeTool`, which for `McpClientTool` covers only
  `createMcpSyncClient` and `initialize()`. The tool callbacks it returns run later inside Spring AI's tool loop,
  outside the aspect.
- The embedded flow (`ConnectedUserIntegrationFacadeImpl.getConnectedUserIntegration(s)`) calls
  `getOAuth2AuthorizationParameters` while serving the integration DTO, including the list endpoint, so OAuth2
  parameters are computed on every page load rather than when the user clicks Sign in.
- `platform-oauth2-service` currently depends only on `app-config`, `commons-util` and `platform-oauth2-api`; it has
  no encryption, tenant or HTTP client dependency.

## Design

### 1. Component API

New function on `Authorization` (component-api) and its DSL builder in `ComponentDsl`:

```java
@FunctionalInterface
interface ProtectedResourceUrlFunction {
    String apply(Parameters connectionParameters, Context context) throws Exception;
}
```

```java
authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
    .title("OAuth2 (automatic)")
    .protectedResourceUrl((connectionParameters, context) -> connectionParameters.getRequiredString(URL))
```

When `protectedResourceUrl` is present the platform runs discovery and registration instead of reading
`authorizationUrl`, `tokenUrl`, `clientId` and `clientSecret` from parameters. Such an authorization declares no
properties.

### 2. Platform units (`platform-oauth2`)

New dependencies for `platform-oauth2-service`: `:server:libs:core:encryption:encryption-api` (sealing),
`:server:libs:core:tenant:tenant-api` (`TenantContext.getCurrentTenantId()`), `:server:libs:platform:platform-api`
(`SecurityUtils.fetchCurrentUserLogin()`). HTTP calls use `java.net.http.HttpClient` with redirects disabled.

| Unit | Responsibility |
|---|---|
| `OAuth2DiscoveryClient` | Unauthenticated probe of the protected resource; read `resource_metadata` from the 401 `WWW-Authenticate` header, falling back to `/.well-known/oauth-protected-resource{path}` then `/.well-known/oauth-protected-resource`. Fetch authorization server metadata trying, in order, `/.well-known/oauth-authorization-server{path}`, `/.well-known/openid-configuration{path}`, `{issuer}/.well-known/openid-configuration`. Returns a `DiscoveredAuthorizationServer` record: issuer, authorization endpoint, token endpoint, registration endpoint, resource, scopes (protected resource scopes preferred over authorization server scopes), supported PKCE methods, supported token endpoint auth methods. |
| `OAuth2DynamicClientRegistrar` | RFC 7591 POST to the registration endpoint with `client_name: "ByteChef"`, `redirect_uris: [<redirectUri>]`, `grant_types: ["authorization_code", "refresh_token"]`, `response_types: ["code"]` and `token_endpoint_auth_method`. No `scope` is sent at registration: the dialog lets the user narrow scopes afterwards, and the authorization request carries the final set. Auth method preference: `none` if supported, else `client_secret_basic`, else `client_secret_post`. If the server does not advertise methods, request `none`. The method in the registration response wins over the requested one (a server may issue a secret anyway). Returns `RegisteredClient(clientId, clientSecret?, tokenEndpointAuthMethod)`. |
| `OAuth2AuthorizationContextSealer` | Seal and unseal an `OAuth2AuthorizationContext` record with `Encryption` (AES-GCM, `v2:` format). Payload: client id, client secret, token endpoint auth method, token endpoint, authorization endpoint, issuer, resource, PKCE verifier, tenant id (`TenantContext.getCurrentTenantId()`), authenticated principal name (`SecurityUtils.fetchCurrentUserLogin()`), component name, connection version, expiry; the redirect URI is not sealed, since the server always uses its configured redirect URI. Expiry is 10 minutes for discovery contexts (they hold a registered client) and 1 hour for plain-PKCE contexts (verifier only; the embedded flow creates these at page load). Unseal rejects tampered, expired (injected `Clock`) or foreign (tenant, principal, component mismatch) contexts. |

### 3. Authorization parameters (`ConnectionDefinitionServiceImpl.getOAuth2AuthorizationParameters`)

- **Discovery mode** (`protectedResourceUrl` present): discover → register → generate PKCE pair → seal. Return:
  - `authorizationUrl` = discovered authorization endpoint
  - `clientId` = registered client id
  - `scopes` = discovered scopes
  - `extraQueryParameters` = `resource`, `code_challenge`, `code_challenge_method=S256`
  - `authorizationContext` = sealed context (new field)
- **All PKCE flows** (discovery or not): the verifier is placed only in the sealed context, never in
  `extraQueryParameters`. Existing PKCE components (Canva, X, …) get the security fix without code changes; their
  context carries only the verifier and binding fields.
- The `OAuth2AuthorizationParameters` record in `platform-component-api` gains `authorizationContext`, as do its
  REST models in `platform-configuration-rest-impl/openapi.yaml` and EE
  `embedded-configuration-public-rest/openapi.yaml` (the latter via `ConnectedUserIntegrationDTO`).
- **Embedded exclusion:** the embedded facade does not compute OAuth2 parameters for a discovery-mode authorization (the DTO carries `null`), so listing integrations never registers a client. A connection posted with a discovery context and `PlatformType.EMBEDDED` is rejected by `ConnectionFacadeImpl` with `OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE`. Only the automation `ConnectionDialog` path runs discovery.
- **Registration is a side effect of this request.** The dialog's `useGetOAuth2AuthorizationParametersQuery` must
  not refetch on window focus or remount while on the OAuth step (`staleTime: Infinity`,
  `refetchOnWindowFocus: false`); each fetch would register another client.

### 4. Code exchange (`ConnectionFacadeImpl.resolveOAuth2AuthorizationCode`)

- If parameters contain `authorizationContext`: unseal, verify binding, and exchange the code at the sealed token
  endpoint with the sealed client credentials (per `tokenEndpointAuthMethod`), `code_verifier` and `resource`.
- Persist: `clientId`, `clientSecret` (if issued), `tokenEndpointAuthMethod`, `authorizationUrl`, `tokenUrl`,
  `resource`, `access_token`, `refresh_token`, `expires_in`.
- For non-discovery PKCE contexts, the verifier from the context replaces the browser-posted `code_verifier`, and the
  existing callback function runs unchanged.
- Always strip `code`, `code_verifier`, `authorizationContext` and `state` before saving.
- Connections without `authorizationContext` (manual OAuth2) behave exactly as today.
- The same handling applies on the reconnect path (`replaceAuthorizationParameters`).

### 5. Refresh

The default refresh function:

- sends `resource` when the parameter is stored;
- treats `clientSecret` as optional when `tokenEndpointAuthMethod` is `none`, and authenticates per the stored method
  otherwise (`client_secret_basic` header or `client_secret_post` body).

Connections without these parameters refresh exactly as today.

### 6. Client and embedded SDK

- `useOAuth2.ts` (main client and embedded SDK): keep `authorizationContext` in a ref and include it in the
  code-success payload; remove the `code_verifier` handling. The authorization URL is built from
  `extraQueryParameters`, which no longer contain the verifier.
- `OAuth2Button.tsx` and `ConnectionDialog.tsx`: pass `authorizationContext` through. An authorization with no
  properties already renders only connection properties, then the OAuth step.
- Errors from discovery and registration surface through the existing `oAuth2Error` display.
- Embedded SDK (`@bytechef/embedded`, 0.4.0) gets a minor release. Older SDKs cannot use discovery-mode connections,
  which they could not complete before either.

### 7. `mcp-client`

- `McpClientConnection`: add **OAuth2 (automatic)** as the first authorization (`OAUTH2_AUTHORIZATION_CODE_PKCE` with
  `protectedResourceUrl` reading `url`). Manual OAuth2, Bearer Token and Client Credentials remain.
- Runtime token application: discovery connections persist `authorizationUrl`, so the existing
  `getAuthorizationType` inference selects the code-flow apply (`Authorization: Bearer <access_token>`).
- `McpClientUtils`: translate the MCP SDK's HTTP 401 transport error into `ProviderException` with status 401 so
  `TokenRefreshAspect` refreshes and retries. This covers the Call Tool action, the tool options and properties
  lookups, and `McpClientTool`'s `initialize()`. It does **not** cover tool calls made later inside the AI agent's
  loop, which run outside the aspect; for those the scheduled `expires_in` refresh is the mechanism, and a token
  that expires mid-run fails that run. The exact SDK exception type is confirmed during planning.

## Security

**Server-side requests (SSRF)**

- Every discovery, registration and token request URL — the MCP server URL, `resource_metadata`, authorization server
  metadata, and the advertised registration and token endpoints — is checked with
  `UrlValidatorUtils.validate(url, allowedHosts)`.
- Redirects are not followed on these requests.
- New property `bytechef.oauth2.discovery.allowed-hosts`, default empty, declared in the yml configuration files.
- `https` is required, except for allowlisted hosts and `localhost` in the dev profile.
- 10 second timeouts; metadata responses capped at 64 KB.
- Known limit: validation resolves DNS before the request, so DNS rebinding is not prevented. `mcp-client` already
  connects to arbitrary user URLs at runtime, so discovery adds no new exposure.

**Metadata validation**

- Protected resource `resource` must share the origin of the MCP server URL.
- Authorization server `issuer` must equal the URL it was discovered from (RFC 8414 §3.3).
- PKCE `S256` is required: abort if `code_challenge_methods_supported` is advertised without it; send S256 if absent.
- A missing `registration_endpoint` aborts with guidance to use **OAuth2 (manual)**.

**Sealed context**

- AES-GCM via the instance encryption key; tamper-evident.
- Expires after 10 minutes for discovery contexts (popup timeout is 5 minutes); 1 hour for plain-PKCE contexts.
- Bound to tenant, authenticated principal and component.
- Replay within the window gains nothing: the code is single-use and the verifier is useless without it.
- Never logged; neither are client secrets or tokens anywhere in the flow.

## Error Handling

Each case maps to a specific `ConnectionDefinitionErrorType` and reaches the dialog's `oAuth2Error`.

| Situation | User-facing message gist |
|---|---|
| URL blocked (private address, scheme, not allowlisted) | This server address isn't allowed |
| No OAuth metadata (no 401 / metadata missing) | This server doesn't advertise OAuth; try Bearer Token or no authentication |
| No registration endpoint | This server doesn't allow automatic registration; use OAuth2 (manual) |
| Registration rejected (4xx) | Server's `error_description` |
| Sealed context expired or foreign | Sign-in took too long; try again |
| Discovery-mode authorization requested through the embedded flow | Automatic OAuth2 isn't available for embedded integrations yet; use OAuth2 (manual) |
| Token exchange failure | Server's `error` / `error_description` |

Refresh failures follow existing behavior (connection marked INVALID, dependent workflows disabled). An
`invalid_client` refresh error tells the user to reconnect, which re-registers.

## Testing

One test class per source class (`<Class>Test` / `<Class>IntTest`); each new test is shown failing first.

**Unit (`platform-oauth2-service`, WireMock)**

- `OAuth2DiscoveryClientTest`: `WWW-Authenticate` discovery; `.well-known` fallbacks with and without path; RFC 8414 /
  OIDC path order; resource origin, issuer and S256 rejections; private address blocked unless allowlisted; redirects
  not followed; timeout and size cap.
- `OAuth2DynamicClientRegistrarTest`: auth method selection; request body; missing endpoint; 4xx passthrough.
- `OAuth2AuthorizationContextSealerTest`: round trip; tampered; expired for each mode (injected `Clock`); tenant,
  principal and component mismatch.

**Platform**

- `ConnectionDefinitionServiceTest`: discovery-mode parameters including `authorizationContext` and no
  `code_verifier`; regular PKCE no longer leaks `code_verifier`; refresh sends `resource` and supports public clients.
- `ConnectedUserIntegrationFacade` test: a discovery-mode authorization fails with the embedded-exclusion error and
  registers nothing.
- `oauth2.queries.ts` test: the authorization-parameters query does not refetch on window focus.
- `ConnectionFacadeImpl` test: unseal, exchange, persisted parameters, stripped parameters; unchanged manual flow.
- `OAuth2ApiController` WebMvc IntTest: `authorizationContext` in the response.
- End-to-end `*IntTest` with WireMock as MCP server and authorization server: parameters → simulated callback → create
  → refresh; asserts stored parameters and `resource` on refresh.

**`mcp-client`**

- `McpClientConnection` definition snapshot regenerated with the new authorization first.
- `McpClientUtilsTest`: discovery connection applies the bearer token; SDK 401 maps to `ProviderException(401)`.

**Client (Vitest)**

- `useOAuth2` tests (main client and embedded SDK): `authorizationContext` in the payload; no `code_verifier` in the URL.
- `OAuth2Button.test.tsx`, `ConnectionDialog.test.tsx`: property-less authorization flows straight to Connect and
  posts `authorizationContext`.

**Gates**

- `./gradlew check` and `testIntegration` on every touched module.
- `npm run check` in `client/` and in the embedded SDK library.
- Manual check against one public-client server (Todoist or Linear) and one confidential-client server (Figma).
- `./gradlew generateDocumentation` to refresh the `mcp-client` reference page.

## Out of Scope

- Catalog "pick a known server" UI and loading `servers.json`.
- Discovery mode in the embedded (connected-user) flow. Requires the embedded facade to compute OAuth2 parameters
  lazily (an on-demand public endpoint) instead of while serving integration DTOs.
- Proactive token refresh before an AI agent run when `expires_in` indicates the token is about to expire.
- Client ID metadata documents (required by servers such as Reckon).
- Registration management or deletion (RFC 7592).
- Sharing one registered client across connections.
- Step-up authorization for additional scopes.
- DNS rebinding protection.
