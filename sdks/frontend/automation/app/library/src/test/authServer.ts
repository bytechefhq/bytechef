import type {AppContext, AppCurrentUser, AppUserAuthMode} from '../core/types';
import {createSessionStore, type SessionStoreI} from '../tanstack/session';
import {jsonResponse} from './mockFetch';

export const SESSION_SECRET = 'a-session-secret-of-forty-characters-xx';

export const TOKEN_ENDPOINT = 'https://bc.example/oauth2/token';

export function appContext(userAuthMode: AppUserAuthMode = 'REQUIRED'): AppContext {
    return {
        appId: 1,
        appInstanceId: 2,
        appInstanceName: 'Production',
        appName: 'CRM',
        authorizationEndpoint: 'https://bc.example/oauth2/authorize',
        clientId: 'app-1',
        configuration: {},
        environment: 'PRODUCTION',
        issuer: 'https://bc.example',
        tokenEndpoint: TOKEN_ENDPOINT,
        userAuthMode,
    };
}

export function user(login: string, id = 1): AppCurrentUser {
    return {email: `${login}@example.com`, firstName: login, id, lastName: null, login, scopes: ['APP_USE']};
}

export interface RecordedCallI {
    body: string;
    method: string;
    url: string;
    userToken: string | null;
}

export interface FakeBytechefI {
    calls: RecordedCallI[];
    fetch: typeof fetch;
    tokenCalls(): RecordedCallI[];
}

export interface FakeBytechefOptionsI {
    mode?: AppUserAuthMode;
    noClient?: boolean;
    token?: (form: URLSearchParams) => Response | Promise<Response>;
    users?: Record<string, AppCurrentUser | Response>;
}

export function fakeBytechef(options: FakeBytechefOptionsI = {}): FakeBytechefI {
    const calls: RecordedCallI[] = [];

    const fetchFake = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
        const url = String(input);
        const headers = new Headers(init?.headers);
        const body = typeof init?.body === 'string' ? init.body : '';
        const userToken = headers.get('x-bytechef-user-token');

        calls.push({body, method: init?.method ?? 'GET', url, userToken});

        if (url.endsWith('/api/app/v1/context')) {
            const context = appContext(options.mode);

            return jsonResponse({body: options.noClient ? {...context, clientId: null} : context});
        }

        if (url.endsWith('/api/app/v1/me')) {
            const found = userToken === null ? undefined : options.users?.[userToken];

            if (found instanceof Response) {
                return found;
            }

            return found
                ? jsonResponse({body: found})
                : jsonResponse({body: {code: 'APP_USER_TOKEN_INVALID', message: 'invalid'}, status: 401});
        }

        if (url === TOKEN_ENDPOINT) {
            return options.token
                ? options.token(new URLSearchParams(body))
                : jsonResponse({body: {error: 'invalid_grant'}, status: 400});
        }

        return jsonResponse({body: []});
    };

    return {
        calls,
        fetch: fetchFake as typeof fetch,
        tokenCalls: () => calls.filter((call) => call.url === TOKEN_ENDPOINT),
    };
}

export function tokenResponse(accessToken: string, refreshToken: string, expiresIn = 300): Response {
    return jsonResponse({
        body: {access_token: accessToken, expires_in: expiresIn, refresh_token: refreshToken, token_type: 'Bearer'},
    });
}

export function testSessionStore(): SessionStoreI {
    return createSessionStore({sessionSecret: SESSION_SECRET});
}

export function cookiePair(setCookie: string): string {
    return setCookie.split(';')[0]!;
}

export async function sessionCookieHeader(
    accessToken: string,
    refreshToken: string | null,
    accessTokenExpiresAt: number | null
): Promise<string> {
    const setCookie = await testSessionStore().sessionCookie(new Request('https://crm.example.com/'), {
        accessToken,
        expiresAt: accessTokenExpiresAt,
        refreshToken,
        scope: null,
        tokenType: 'Bearer',
    });

    return cookiePair(setCookie);
}

export function setCookies(response: Response): string[] {
    return response.headers.getSetCookie();
}

export function findSetCookie(response: Response, name: string): string | undefined {
    return setCookies(response).find((cookie) => cookie.startsWith(`${name}=`));
}
