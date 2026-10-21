import {type BytechefClient, createBytechefClient} from '../core/client';
import type {BytechefClientOptions} from '../core/config';
import {BytechefAppError} from '../core/errors';
import {isSameState} from '../core/pkce';
import {createSessionStore, normalizeBasePath, safeReturnPath, type SessionStoreI} from './session';

export interface BytechefAuthOptionsI extends BytechefClientOptions {
    /**
     * The app's public origin, for the redirect URI the instance's OAuth2 client has registered
     * (`<appUrl><basePath>/callback`), for the cookies' `Secure` flag (set unless its host is localhost) and for the
     * logout origin check. Defaults to the request's URL, which is wrong behind a proxy that changes the host or the
     * scheme: set it there.
     */
    appUrl?: string;
    /**
     * Where the three sign-in routes live. Defaults to `/auth`, and keep that: ByteChef registers only
     * `<instance url>/auth/callback` (plus `http://localhost:3000/auth/callback` in DEVELOPMENT) as the instance's
     * redirect URI, so a sign-in from another base path is refused at the authorization endpoint.
     */
    basePath?: string;
    /** The session cookie's lifetime; keep it no longer than the refresh token's. Defaults to 3600 (one hour). */
    sessionMaxAgeSeconds?: number;
    /** Seals the session cookies. Defaults to `BYTECHEF_SESSION_SECRET`; at least 32 characters. */
    sessionSecret?: string;
}

export interface AuthRouteContextI {
    request: Request;
}

export type AuthRouteHandler = (context: AuthRouteContextI) => Promise<Response>;

export interface AuthRuntimeI {
    appUrl?: string;
    basePath: string;
    client(): BytechefClient;
    clientOptions: BytechefClientOptions;
    store: SessionStoreI;
}

export function createAuthRuntime(options: BytechefAuthOptionsI): () => AuthRuntimeI {
    let runtime: AuthRuntimeI | undefined;

    return () => {
        if (runtime === undefined) {
            const {appUrl, basePath, sessionMaxAgeSeconds, sessionSecret, ...clientOptions} = options;
            let client: BytechefClient | undefined;

            runtime = {
                appUrl,
                basePath: normalizeBasePath(basePath),
                client: () => {
                    client ??= createBytechefClient(clientOptions);

                    return client;
                },
                clientOptions,
                store: createSessionStore({appUrl, basePath, sessionMaxAgeSeconds, sessionSecret}),
            };
        }

        return runtime;
    };
}

export function redirectResponse(location: string, cookies: string[], status = 302): Response {
    const headers = new Headers({'cache-control': 'no-store', location});

    for (const cookie of cookies) {
        headers.append('set-cookie', cookie);
    }

    return new Response(null, {headers, status});
}

function textResponse(status: number, text: string, cookies: string[] = [], extraHeaders: HeadersInit = {}): Response {
    const headers = new Headers(extraHeaders);

    headers.set('cache-control', 'no-store');
    headers.set('content-type', 'text/plain; charset=utf-8');

    for (const cookie of cookies) {
        headers.append('set-cookie', cookie);
    }

    return new Response(text, {headers, status});
}

const SIGN_IN_NOT_SET_UP =
    "Sign-in is not set up for this app yet. In ByteChef, set this app instance's URL so its sign-in can be registered, then open the app again.";

const START_AGAIN = 'The sign-in could not be completed. Open the app again to start a new sign-in.';

function isSignInNotSetUp(error: unknown): boolean {
    return (
        error instanceof BytechefAppError &&
        (error.code === 'SIGN_IN_MISCONFIGURED' || error.code === 'SIGN_IN_UNAVAILABLE')
    );
}

function exchangeFailure(error: unknown, cookies: string[]): Response {
    if (!(error instanceof BytechefAppError)) {
        throw error;
    }

    if (error.code === 'SESSION_ENDED') {
        return textResponse(400, START_AGAIN, cookies);
    }

    if (error.code === 'SIGN_IN_MISCONFIGURED' || error.code === 'SIGN_IN_UNAVAILABLE') {
        return textResponse(500, 'Sign-in is not set up correctly for this app.', cookies);
    }

    if (error.code === 'RATE_LIMITED') {
        return textResponse(503, 'Too many sign-ins right now. Try again in a moment.', cookies);
    }

    return textResponse(502, 'The sign-in service could not be reached. Try again later.', cookies);
}

function isCrossSite(request: Request, appUrl: string | undefined): boolean {
    const fetchSite = request.headers.get('sec-fetch-site');

    if (fetchSite !== null && fetchSite !== 'same-origin' && fetchSite !== 'none') {
        return true;
    }

    const origin = request.headers.get('origin');

    if (origin === null) {
        return false;
    }

    try {
        return new URL(origin).host !== new URL(appUrl ?? request.url).host;
    } catch {
        return true;
    }
}

/**
 * `GET <basePath>/login?returnTo=/path`: starts the sign-in. Sends the browser to ByteChef's authorization endpoint
 * with PKCE, keeping `state`, the verifier and the return path in a short-lived sealed cookie. `returnTo` must be a
 * path on the app's origin; anything else returns to `/`. With sign-in off on the instance it only redirects back.
 */
export function loginHandler(options: BytechefAuthOptionsI = {}): AuthRouteHandler {
    const getRuntime = createAuthRuntime(options);

    return async ({request}) => {
        const {appUrl, basePath, client, store} = getRuntime();
        const url = new URL(request.url);
        const returnTo = safeReturnPath(url.searchParams.get('returnTo'));
        const {userAuthMode} = await client().context.get();

        if (userAuthMode === 'NONE') {
            return redirectResponse(returnTo, []);
        }

        store.requireSecret();

        const redirectUri = new URL(`${basePath}/callback`, appUrl ?? url.origin).toString();
        let authorizationRequest;

        try {
            authorizationRequest = await client().auth.createAuthorizationRequest({redirectUri});
        } catch (error) {
            if (isSignInNotSetUp(error)) {
                return textResponse(503, SIGN_IN_NOT_SET_UP);
            }

            throw error;
        }

        const pendingLoginCookie = await store.pendingLoginCookie(request, {
            codeVerifier: authorizationRequest.codeVerifier,
            redirectUri,
            returnTo,
            state: authorizationRequest.state,
        });

        return redirectResponse(authorizationRequest.url, [pendingLoginCookie]);
    };
}

/**
 * `GET <basePath>/callback`: completes the sign-in. Refuses a callback whose `state` does not match the sealed
 * pending sign-in (compared in constant time), exchanges the code with the verifier, stores the tokens in the sealed
 * session cookie and returns to the path the sign-in started from. Errors answer with a fixed text, never with the
 * code, the tokens or the authorization server's description.
 */
export function callbackHandler(options: BytechefAuthOptionsI = {}): AuthRouteHandler {
    const getRuntime = createAuthRuntime(options);

    return async ({request}) => {
        const {client, store} = getRuntime();
        const parameters = new URL(request.url).searchParams;
        const pendingLogin = await store.readPendingLogin(request);
        const cleared = [store.clearPendingLoginCookie(request)];

        if (pendingLogin === undefined || !isSameState(pendingLogin.state, parameters.get('state'))) {
            return textResponse(400, START_AGAIN, cleared);
        }

        if (parameters.has('error')) {
            return parameters.get('error') === 'access_denied'
                ? textResponse(403, 'You do not have access to this app.', cleared)
                : textResponse(400, START_AGAIN, cleared);
        }

        const code = parameters.get('code');

        if (!code) {
            return textResponse(400, START_AGAIN, cleared);
        }

        let sessionCookie: string;

        try {
            const tokens = await client().auth.exchangeCode({
                code,
                codeVerifier: pendingLogin.codeVerifier,
                redirectUri: pendingLogin.redirectUri,
            });

            sessionCookie = await store.sessionCookie(request, tokens);
        } catch (error) {
            return exchangeFailure(error, cleared);
        }

        return redirectResponse(pendingLogin.returnTo, [sessionCookie, ...cleared]);
    };
}

/**
 * `POST <basePath>/logout`: clears the session cookie and sends the browser to `/` (303). A GET answers 405 and a
 * cross-site POST 403, so a link or a form on another site cannot sign the user out.
 *
 * This only forgets the session in this browser. ByteChef offers an app no token revocation endpoint, so the access
 * and refresh tokens the cookie held stay valid until they expire (the access token after minutes, the refresh token
 * after at most the session lifetime). Revoking them before that takes an administrator in ByteChef (suspending the
 * instance, or the grant on the Registered Clients settings page).
 */
export function logoutHandler(options: BytechefAuthOptionsI = {}): AuthRouteHandler {
    const getRuntime = createAuthRuntime(options);

    return async ({request}) => {
        const {appUrl, store} = getRuntime();

        if (request.method !== 'POST') {
            return textResponse(405, 'Sign out with a POST.', [], {allow: 'POST'});
        }

        if (isCrossSite(request, appUrl)) {
            return textResponse(403, 'Sign out from the app itself.');
        }

        return redirectResponse('/', [store.clearSessionCookie(request)], 303);
    };
}
