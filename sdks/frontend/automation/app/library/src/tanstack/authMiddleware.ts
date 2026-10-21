import {createCsrfMiddleware, createMiddleware} from '@tanstack/react-start';

import type {RefreshedTokenSetI} from '../core/auth';
import {type BytechefClient, createBytechefClient} from '../core/client';
import {BytechefAppError, isSignInRequired} from '../core/errors';
import type {AppCurrentUser} from '../core/types';
import {type BytechefAuthOptionsI, createAuthRuntime, redirectResponse} from './authHandlers';
import type {SessionI} from './session';

export const DEFAULT_REFRESH_MARGIN_MS = 30_000;

export const MINIMUM_TANSTACK_START_VERSION = '1.168.10';

const CSRF_MIDDLEWARE_HELP = `bytechefRequestMiddleware() needs @tanstack/react-start ${MINIMUM_TANSTACK_START_VERSION} or later, the first version that exports createCsrfMiddleware; without it server functions would run with no CSRF check. Upgrade @tanstack/react-start.`;

const REFRESH_REUSE_MS = 30_000;

const REQUEST_MIDDLEWARE_HELP =
    "bytechefAuthMiddleware() is request middleware: register it in createStart({requestMiddleware: [...]}) or on a route's server.middleware. Inside a server function chain alone it cannot write the session cookie.";

export interface BytechefAuthMiddlewareOptionsI extends BytechefAuthOptionsI {
    /** Refresh the access token when it expires within this many milliseconds. Defaults to 30 000. */
    refreshMarginMs?: number;
}

export interface BytechefAuthContextI {
    /** A client for this request only: the instance key plus, when someone is signed in, their user token. */
    bytechef: BytechefClient;
    /** The signed-in user, from `GET /me`; undefined when no one is signed in or the instance has sign-in off. */
    user?: AppCurrentUser;
}

interface RefreshEntryI {
    settledAt?: number;
    tokens: Promise<RefreshedTokenSetI>;
}

interface SignedInI {
    client: BytechefClient;
    user: AppCurrentUser;
}

function needsRefresh(session: SessionI, refreshMarginMs: number): boolean {
    return session.accessTokenExpiresAt !== null && session.accessTokenExpiresAt - Date.now() <= refreshMarginMs;
}

function isExpired(session: SessionI): boolean {
    return session.accessTokenExpiresAt !== null && session.accessTokenExpiresAt <= Date.now();
}

function withCookies<T extends {response: Response}>(result: T, cookies: string[]): T {
    if (cookies.length === 0) {
        return result;
    }

    let response = result.response;

    try {
        for (const cookie of cookies) {
            response.headers.append('set-cookie', cookie);
        }
    } catch {
        response = new Response(response.body, response);

        for (const cookie of cookies) {
            response.headers.append('set-cookie', cookie);
        }
    }

    response.headers.set('cache-control', 'no-store');

    return response === result.response ? result : {...result, response};
}

function forbiddenResponse(cookies: string[]): Response {
    const headers = new Headers({'cache-control': 'no-store', 'content-type': 'text/plain; charset=utf-8'});

    for (const cookie of cookies) {
        headers.append('set-cookie', cookie);
    }

    return new Response('You do not have access to this app.', {headers, status: 403});
}

function unavailableResponse(cookies: string[]): Response {
    const headers = new Headers({'cache-control': 'no-store', 'content-type': 'text/plain; charset=utf-8'});

    for (const cookie of cookies) {
        headers.append('set-cookie', cookie);
    }

    return new Response('ByteChef could not be reached. Try again in a moment.', {headers, status: 503});
}

function signInRequiredResponse(handlerType: string, loginPath: string, returnTo: string, cookies: string[]): Response {
    if (handlerType === 'serverFn') {
        const headers = new Headers({'cache-control': 'no-store'});

        for (const cookie of cookies) {
            headers.append('set-cookie', cookie);
        }

        return Response.json({code: 'APP_USER_REQUIRED', message: 'Sign in to use this app'}, {headers, status: 401});
    }

    return redirectResponse(`${loginPath}?returnTo=${encodeURIComponent(returnTo)}`, cookies);
}

/**
 * Request middleware that signs the request's user in from the sealed session cookie. It puts a per-request client
 * on `context.bytechef` (carrying the user's token when someone is signed in) and the user on `context.user`.
 *
 * The instance's mode, from `GET /context`, decides the rest: `REQUIRED` redirects a page request without a session
 * to `<basePath>/login` (a server function call gets 401 `APP_USER_REQUIRED`), `OPTIONAL` lets it through without a
 * user, and `NONE` never reads the cookie and adds no user token. An access token that expires within
 * `refreshMarginMs` is refreshed once per request, and concurrent requests of one session share that refresh. A
 * session the authorization server or ByteChef no longer accepts is cleared and treated as none. When reading the user
 * fails after a refresh, the request answers 503 with the rotated session cookie, so the browser never keeps a spent
 * refresh token. The sign-in routes under `basePath` pass through untouched.
 *
 * Register it in `createStart({requestMiddleware})` (pages and server functions; keep a CSRF middleware there, since
 * Start drops its default one once `requestMiddleware` is set) or on a route's `server.middleware` (pages only). List
 * it in a server function's `.middleware([...])` for the context types; Start runs it once.
 */
export function bytechefAuthMiddleware(options: BytechefAuthMiddlewareOptionsI = {}) {
    const {refreshMarginMs = DEFAULT_REFRESH_MARGIN_MS, ...authOptions} = options;
    const getRuntime = createAuthRuntime(authOptions);
    const refreshes = new Map<string, RefreshEntryI>();

    if (!(Number.isFinite(refreshMarginMs) && refreshMarginMs >= 0)) {
        throw new Error('refreshMarginMs must be zero or a positive number of milliseconds');
    }

    const refreshOnce = (client: BytechefClient, refreshToken: string): Promise<RefreshedTokenSetI> => {
        const now = Date.now();

        for (const [key, entry] of refreshes) {
            if (entry.settledAt !== undefined && now - entry.settledAt > REFRESH_REUSE_MS) {
                refreshes.delete(key);
            }
        }

        const existing = refreshes.get(refreshToken);

        if (existing !== undefined) {
            return existing.tokens;
        }

        const entry: RefreshEntryI = {tokens: client.auth.refresh({refreshToken})};

        refreshes.set(refreshToken, entry);

        entry.tokens.then(
            () => {
                entry.settledAt = Date.now();
            },
            () => {
                if (refreshes.get(refreshToken) === entry) {
                    refreshes.delete(refreshToken);
                }
            }
        );

        return entry.tokens;
    };

    return createMiddleware({type: 'request'}).server(async ({handlerType, next, pathname, request}) => {
        if (typeof pathname !== 'string' || typeof handlerType !== 'string') {
            throw new Error(REQUEST_MIDDLEWARE_HELP);
        }

        const {basePath, client, clientOptions, store} = getRuntime();
        const sharedClient = client();
        const anonymous: BytechefAuthContextI = {bytechef: sharedClient, user: undefined};

        if (pathname === basePath || pathname.startsWith(`${basePath}/`)) {
            return next({context: anonymous});
        }

        const {userAuthMode} = await sharedClient.context.get();

        if (userAuthMode === 'NONE') {
            return next({context: anonymous});
        }

        const cookies: string[] = [];
        const clearSession = () => {
            cookies.push(store.clearSessionCookie(request));
        };
        const read = await store.readSession(request);
        let session = read.session;
        let refreshed = false;

        if (read.present && session === undefined) {
            clearSession();
        }

        if (session !== undefined && needsRefresh(session, refreshMarginMs)) {
            if (session.refreshToken !== null) {
                try {
                    const tokens = await refreshOnce(sharedClient, session.refreshToken);

                    cookies.push(await store.sessionCookie(request, tokens));

                    session = {...session, accessToken: tokens.accessToken, refreshToken: tokens.refreshToken};
                    refreshed = true;
                } catch (error) {
                    if (!isSignInRequired(error)) {
                        throw error;
                    }

                    session = undefined;

                    clearSession();
                }
            } else if (isExpired(session)) {
                session = undefined;

                clearSession();
            }
        }

        let signedIn: SignedInI | undefined;

        if (session !== undefined) {
            const userClient = createBytechefClient({...clientOptions, userToken: session.accessToken});

            try {
                signedIn = {client: userClient, user: await userClient.auth.getCurrentUser()};
            } catch (error) {
                const forbidden = error instanceof BytechefAppError && error.code === 'APP_USER_FORBIDDEN';

                if (isSignInRequired(error)) {
                    clearSession();
                } else if (forbidden && userAuthMode === 'REQUIRED') {
                    return forbiddenResponse(cookies);
                } else if (!forbidden && refreshed) {
                    return unavailableResponse(cookies);
                } else if (!forbidden) {
                    throw error;
                }
            }
        }

        if (signedIn === undefined && userAuthMode === 'REQUIRED') {
            const url = new URL(request.url);

            return signInRequiredResponse(handlerType, `${basePath}/login`, `${url.pathname}${url.search}`, cookies);
        }

        const context: BytechefAuthContextI = {bytechef: signedIn?.client ?? sharedClient, user: signedIn?.user};

        return withCookies(await next({context}), cookies);
    });
}

/**
 * Returns the `createCsrfMiddleware` export of `@tanstack/react-start`, or throws when the installed version does not
 * have it, so an app never runs its server functions without a CSRF check.
 */
export function requireCsrfMiddlewareFactory<FactoryT>(factory: FactoryT | undefined): FactoryT {
    if (typeof factory !== 'function') {
        throw new Error(CSRF_MIDDLEWARE_HELP);
    }

    return factory;
}

/**
 * The request middleware an app registers: `createStart(() => ({requestMiddleware: bytechefRequestMiddleware()}))`.
 * It is Start's CSRF check for server function calls (`Sec-Fetch-Site` must be `same-origin`; without it the `Origin`,
 * then the `Referer`, must be the app's own, and a request with none of them is refused), followed by
 * `bytechefAuthMiddleware(options)`. Start applies its default CSRF check only while no `requestMiddleware` is set,
 * so registering the auth middleware alone would drop it. Page requests are not checked. With `appUrl` set, the
 * expected origin is `appUrl`'s. It throws when the installed `@tanstack/react-start` is older than
 * {@link MINIMUM_TANSTACK_START_VERSION}, which has no `createCsrfMiddleware`.
 */
export function bytechefRequestMiddleware(options: BytechefAuthMiddlewareOptionsI = {}) {
    const appOrigin = options.appUrl === undefined ? undefined : new URL(options.appUrl).origin;
    const createCsrfCheck = requireCsrfMiddlewareFactory(createCsrfMiddleware);
    const csrfMiddleware = createCsrfCheck({
        filter: (context) => context.handlerType === 'serverFn',
        origin: (origin, context) => origin === (appOrigin ?? new URL(context.request.url).origin),
    });

    return [csrfMiddleware, bytechefAuthMiddleware(options)] as const;
}
