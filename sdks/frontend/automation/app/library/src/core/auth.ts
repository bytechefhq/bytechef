import {assertServerRuntime, type ResolvedConfig} from './config';
import type {CallOptions} from './connections';
import type {ContextApiI} from './context';
import {BytechefAppError} from './errors';
import {createCodeChallenge, generateCodeVerifier, generateState} from './pkce';
import {abortError, type Requester, untilAborted} from './request';
import {expectObject} from './response';
import type {AppContext, AppCurrentUser} from './types';

export const SIGN_IN_SCOPE = 'openid profile app:use';

export interface AuthorizationRequestOptionsI extends CallOptions {
    redirectUri: string;
}

export interface AuthorizationRequestI {
    codeVerifier: string;
    state: string;
    url: string;
}

export interface ExchangeCodeOptionsI extends CallOptions {
    code: string;
    codeVerifier: string;
    redirectUri: string;
}

export interface RefreshOptionsI extends CallOptions {
    refreshToken: string;
}

export interface TokenSetI {
    accessToken: string;
    /** Epoch milliseconds; null when the authorization server did not say. */
    expiresAt: number | null;
    refreshToken: string | null;
    scope: string | null;
    tokenType: string;
}

export interface RefreshedTokenSetI extends TokenSetI {
    refreshToken: string;
}

export interface AuthApiI {
    /**
     * Builds the authorization code request with PKCE S256 for the instance's OAuth2 client, from `GET /context`. Keep
     * `state` and `codeVerifier` on the server side of the session until the callback.
     */
    createAuthorizationRequest(options: AuthorizationRequestOptionsI): Promise<AuthorizationRequestI>;
    exchangeCode(options: ExchangeCodeOptionsI): Promise<TokenSetI>;
    /** The signed-in user from `GET /me`. Throws `APP_USER_REQUIRED` when the client has no `userToken`. */
    getCurrentUser(options?: CallOptions): Promise<AppCurrentUser>;
    /** Refresh tokens are rotated: store the returned `refreshToken`, the one sent is no longer valid. */
    refresh(options: RefreshOptionsI): Promise<RefreshedTokenSetI>;
}

interface SignInSettingsI {
    authorizationEndpoint: string;
    clientId: string;
    tokenEndpoint: string;
}

interface TokenResponseI {
    access_token?: unknown;
    error?: unknown;
    expires_in?: unknown;
    refresh_token?: unknown;
    scope?: unknown;
    token_type?: unknown;
}

const OAUTH_ERROR_CODE = /^[a-z_]{1,64}$/;

function signInSettings(context: AppContext): SignInSettingsI {
    const {authorizationEndpoint, clientId, tokenEndpoint} = context;

    if (!authorizationEndpoint || !clientId || !tokenEndpoint) {
        throw new BytechefAppError(
            'SIGN_IN_UNAVAILABLE',
            'The app instance has no OAuth2 client, so its users cannot sign in; set the instance URL and its sign-in mode in ByteChef'
        );
    }

    return {authorizationEndpoint, clientId, tokenEndpoint};
}

function parseTokenResponse(text: string): TokenResponseI | undefined {
    try {
        const parsed: unknown = JSON.parse(text);

        return typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)
            ? (parsed as TokenResponseI)
            : undefined;
    } catch {
        return undefined;
    }
}

function invalidTokenResponse(status: number, expected: string): BytechefAppError {
    return new BytechefAppError(
        'INVALID_RESPONSE',
        `The authorization server answered ${status} to the token request without ${expected}`,
        {status}
    );
}

function toTokenSet(
    tokenResponse: TokenResponseI | undefined,
    status: number,
    refreshTokenRequired: boolean
): TokenSetI {
    if (typeof tokenResponse?.access_token !== 'string' || tokenResponse.access_token === '') {
        throw invalidTokenResponse(status, 'an access token');
    }

    if (
        refreshTokenRequired &&
        (typeof tokenResponse.refresh_token !== 'string' || tokenResponse.refresh_token === '')
    ) {
        throw invalidTokenResponse(status, 'a new refresh token');
    }

    const {access_token: accessToken, expires_in: expiresIn, refresh_token: refreshToken, scope} = tokenResponse;

    return {
        accessToken,
        expiresAt: typeof expiresIn === 'number' && Number.isFinite(expiresIn) ? Date.now() + expiresIn * 1000 : null,
        refreshToken: typeof refreshToken === 'string' && refreshToken !== '' ? refreshToken : null,
        scope: typeof scope === 'string' ? scope : null,
        tokenType: typeof tokenResponse.token_type === 'string' ? tokenResponse.token_type : 'Bearer',
    };
}

function tokenRequestError(tokenResponse: TokenResponseI | undefined, status: number): BytechefAppError {
    const oauthError =
        typeof tokenResponse?.error === 'string' && OAUTH_ERROR_CODE.test(tokenResponse.error)
            ? tokenResponse.error
            : undefined;

    if (oauthError === 'invalid_grant') {
        return new BytechefAppError(
            'SESSION_ENDED',
            'The authorization server refused the grant (invalid_grant); sign the user in again',
            {status}
        );
    }

    if (status === 429) {
        return new BytechefAppError('RATE_LIMITED', 'The authorization server is rate limiting the token requests', {
            status,
        });
    }

    if (status >= 400 && status < 500) {
        return new BytechefAppError(
            'SIGN_IN_MISCONFIGURED',
            `The authorization server refused the token request (${oauthError ?? status}); check the app instance's OAuth2 client, its redirect URI and the token endpoint`,
            {status}
        );
    }

    return new BytechefAppError('HTTP_ERROR', `The authorization server answered ${status} to the token request`, {
        status,
    });
}

function isRedirectResponse(response: Response): boolean {
    return response.type === 'opaqueredirect' || (response.status >= 300 && response.status < 400);
}

async function postTokenRequest(
    config: ResolvedConfig,
    tokenEndpoint: string,
    form: URLSearchParams,
    signal: AbortSignal | undefined,
    refreshTokenRequired: boolean,
    onClientRejected: () => void
): Promise<TokenSetI> {
    if (signal?.aborted) {
        throw abortError(signal);
    }

    const controller = new AbortController();
    const onAbort = () =>
        controller.abort(new BytechefAppError('ABORTED', 'The token request was aborted', {cause: signal?.reason}));
    const timeout = setTimeout(
        () =>
            controller.abort(
                new BytechefAppError('TIMEOUT', `The authorization server did not answer within ${config.timeoutMs} ms`)
            ),
        config.timeoutMs
    );

    signal?.addEventListener('abort', onAbort, {once: true});

    try {
        let response: Response;

        try {
            response = await config.fetch(tokenEndpoint, {
                body: form.toString(),
                headers: {accept: 'application/json', 'content-type': 'application/x-www-form-urlencoded'},
                method: 'POST',
                redirect: 'manual',
                signal: controller.signal,
            });
        } catch (error) {
            if (controller.signal.aborted) {
                throw abortError(controller.signal);
            }

            throw new BytechefAppError('NETWORK_ERROR', 'The authorization server could not be reached', {
                cause: error,
            });
        }

        if (isRedirectResponse(response)) {
            await response.body?.cancel();

            throw new BytechefAppError(
                'SIGN_IN_MISCONFIGURED',
                'The token endpoint answered with a redirect, which is not followed; check the token endpoint the instance reports',
                {status: response.status}
            );
        }

        let text: string;

        try {
            text = await untilAborted(response.text(), controller.signal);
        } catch (error) {
            if (error instanceof BytechefAppError) {
                throw error;
            }

            text = '';
        }

        const tokenResponse = parseTokenResponse(text);

        if (!response.ok) {
            if (tokenResponse?.error === 'invalid_client') {
                onClientRejected();
            }

            throw tokenRequestError(tokenResponse, response.status);
        }

        return toTokenSet(tokenResponse, response.status, refreshTokenRequired);
    } finally {
        clearTimeout(timeout);
        signal?.removeEventListener('abort', onAbort);
    }
}

export function createAuthApi(request: Requester, getConfig: () => ResolvedConfig, context: ContextApiI): AuthApiI {
    const settings = async (signal: AbortSignal | undefined) => {
        const appContext = await context.get({signal});

        try {
            return signInSettings(appContext);
        } catch (error) {
            context.invalidate();

            throw error;
        }
    };
    const requestTokens = (
        tokenEndpoint: string,
        form: URLSearchParams,
        signal: AbortSignal | undefined,
        refreshTokenRequired: boolean
    ) => postTokenRequest(getConfig(), tokenEndpoint, form, signal, refreshTokenRequired, () => context.invalidate());

    return {
        async createAuthorizationRequest({redirectUri, signal}: AuthorizationRequestOptionsI) {
            assertServerRuntime();

            const {authorizationEndpoint, clientId} = await settings(signal);
            const codeVerifier = generateCodeVerifier();
            const state = generateState();
            const url = new URL(authorizationEndpoint);
            const query = {
                client_id: clientId,
                code_challenge: await createCodeChallenge(codeVerifier),
                code_challenge_method: 'S256',
                redirect_uri: redirectUri,
                response_type: 'code',
                scope: SIGN_IN_SCOPE,
                state,
            };

            for (const [name, value] of Object.entries(query)) {
                url.searchParams.set(name, value);
            }

            return {codeVerifier, state, url: url.toString()};
        },

        async exchangeCode({code, codeVerifier, redirectUri, signal}: ExchangeCodeOptionsI) {
            assertServerRuntime();

            const {clientId, tokenEndpoint} = await settings(signal);
            const form = new URLSearchParams({
                client_id: clientId,
                code,
                code_verifier: codeVerifier,
                grant_type: 'authorization_code',
                redirect_uri: redirectUri,
            });

            return requestTokens(tokenEndpoint, form, signal, false);
        },

        async getCurrentUser(options: CallOptions = {}) {
            assertServerRuntime();

            if (!getConfig().userToken) {
                throw new BytechefAppError(
                    'APP_USER_REQUIRED',
                    'No user is signed in: create the client with the userToken of the signed-in user',
                    {status: 401}
                );
            }

            const path = '/me';

            return expectObject<AppCurrentUser>(await request({method: 'GET', path, signal: options.signal}), path);
        },

        async refresh({refreshToken, signal}: RefreshOptionsI) {
            assertServerRuntime();

            const {clientId, tokenEndpoint} = await settings(signal);
            const form = new URLSearchParams({
                client_id: clientId,
                grant_type: 'refresh_token',
                refresh_token: refreshToken,
            });
            const tokens = await requestTokens(tokenEndpoint, form, signal, true);

            return tokens as RefreshedTokenSetI;
        },
    };
}
