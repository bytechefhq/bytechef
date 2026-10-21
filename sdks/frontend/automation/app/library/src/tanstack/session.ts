import type {TokenSetI} from '../core/auth';

export const SESSION_SECRET_ENV = 'BYTECHEF_SESSION_SECRET';

export const MIN_SESSION_SECRET_LENGTH = 32;

export const SESSION_COOKIE = 'bytechef_session';

export const PENDING_LOGIN_COOKIE = 'bytechef_login';

export const MAX_COOKIE_BYTES = 4096;

export const DEFAULT_SESSION_MAX_AGE_SECONDS = 3600;

export const PENDING_LOGIN_MAX_AGE_SECONDS = 600;

export const DEFAULT_AUTH_BASE_PATH = '/auth';

const SEAL_VERSION = 'v1';

const NONCE_BYTES = 12;

const KEY_SALT = 'bytechef-app-session-v1';

const BASE64URL = /^[A-Za-z0-9_-]+$/;

export interface SessionI {
    accessToken: string;
    accessTokenExpiresAt: number | null;
    expiresAt: number;
    refreshToken: string | null;
}

export interface PendingLoginI {
    codeVerifier: string;
    expiresAt: number;
    redirectUri: string;
    returnTo: string;
    state: string;
}

export interface CookieOptionsI {
    maxAgeSeconds: number;
    path: string;
    secure: boolean;
}

export interface SessionSealerI {
    seal(purpose: string, value: unknown): Promise<string>;
    unseal(purpose: string, sealed: string): Promise<unknown>;
}

function readEnv(name: string): string | undefined {
    const value = (globalThis as {process?: {env?: Record<string, string | undefined>}}).process?.env?.[name];

    return value === undefined || value === '' ? undefined : value;
}

export function resolveSessionSecret(secret: string | undefined): string {
    const resolved = secret ?? readEnv(SESSION_SECRET_ENV);

    if (!resolved || resolved.length < MIN_SESSION_SECRET_LENGTH) {
        throw new Error(
            `${SESSION_SECRET_ENV} is not set or shorter than ${MIN_SESSION_SECRET_LENGTH} characters. Set it in the server environment to a random value of at least ${MIN_SESSION_SECRET_LENGTH} characters (for example \`openssl rand -base64 48\`); never under a VITE_ prefix.`
        );
    }

    return resolved;
}

function toBase64Url(bytes: Uint8Array): string {
    let binary = '';

    for (const byte of bytes) {
        binary += String.fromCharCode(byte);
    }

    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function fromBase64Url(text: string): Uint8Array | undefined {
    if (!BASE64URL.test(text)) {
        return undefined;
    }

    try {
        const binary = atob(text.replace(/-/g, '+').replace(/_/g, '/'));
        const bytes = new Uint8Array(binary.length);

        for (let index = 0; index < binary.length; index++) {
            bytes[index] = binary.charCodeAt(index);
        }

        return bytes;
    } catch {
        return undefined;
    }
}

export function createSessionSealer(secret: string): SessionSealerI {
    const encoder = new TextEncoder();
    const keys = new Map<string, Promise<CryptoKey>>();

    const keyFor = (purpose: string): Promise<CryptoKey> => {
        let key = keys.get(purpose);

        if (key === undefined) {
            key = globalThis.crypto.subtle
                .importKey('raw', encoder.encode(secret), 'HKDF', false, ['deriveKey'])
                .then((baseKey) =>
                    globalThis.crypto.subtle.deriveKey(
                        {hash: 'SHA-256', info: encoder.encode(purpose), name: 'HKDF', salt: encoder.encode(KEY_SALT)},
                        baseKey,
                        {length: 256, name: 'AES-GCM'},
                        false,
                        ['encrypt', 'decrypt']
                    )
                );

            keys.set(purpose, key);
        }

        return key;
    };

    return {
        async seal(purpose: string, value: unknown): Promise<string> {
            const nonce = globalThis.crypto.getRandomValues(new Uint8Array(NONCE_BYTES));
            const ciphertext = await globalThis.crypto.subtle.encrypt(
                {additionalData: encoder.encode(purpose), iv: nonce, name: 'AES-GCM'},
                await keyFor(purpose),
                encoder.encode(JSON.stringify(value))
            );

            return `${SEAL_VERSION}.${toBase64Url(nonce)}.${toBase64Url(new Uint8Array(ciphertext))}`;
        },

        async unseal(purpose: string, sealed: string): Promise<unknown> {
            const parts = sealed.split('.');

            if (parts.length !== 3 || parts[0] !== SEAL_VERSION) {
                return undefined;
            }

            const nonce = fromBase64Url(parts[1]!);
            const ciphertext = fromBase64Url(parts[2]!);

            if (nonce?.length !== NONCE_BYTES || ciphertext === undefined) {
                return undefined;
            }

            try {
                const plaintext = await globalThis.crypto.subtle.decrypt(
                    {additionalData: encoder.encode(purpose), iv: nonce, name: 'AES-GCM'},
                    await keyFor(purpose),
                    ciphertext
                );

                return JSON.parse(new TextDecoder('utf-8', {fatal: true}).decode(plaintext));
            } catch {
                return undefined;
            }
        },
    };
}

function isObject(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function isNonEmptyString(value: unknown): value is string {
    return typeof value === 'string' && value !== '';
}

export function toSession(value: unknown, now: number): SessionI | undefined {
    if (!isObject(value)) {
        return undefined;
    }

    const {accessToken, accessTokenExpiresAt, expiresAt, refreshToken} = value;

    if (
        !isNonEmptyString(accessToken) ||
        !(accessTokenExpiresAt === null || typeof accessTokenExpiresAt === 'number') ||
        typeof expiresAt !== 'number' ||
        !(refreshToken === null || isNonEmptyString(refreshToken)) ||
        expiresAt <= now
    ) {
        return undefined;
    }

    return {accessToken, accessTokenExpiresAt, expiresAt, refreshToken};
}

export function toPendingLogin(value: unknown, now: number): PendingLoginI | undefined {
    if (!isObject(value)) {
        return undefined;
    }

    const {codeVerifier, expiresAt, redirectUri, returnTo, state} = value;

    if (
        !isNonEmptyString(codeVerifier) ||
        typeof expiresAt !== 'number' ||
        !isNonEmptyString(redirectUri) ||
        !isNonEmptyString(returnTo) ||
        !isNonEmptyString(state) ||
        expiresAt <= now
    ) {
        return undefined;
    }

    return {codeVerifier, expiresAt, redirectUri, returnTo: safeReturnPath(returnTo), state};
}

export function readCookie(request: Request, name: string): string | undefined {
    const header = request.headers.get('cookie');

    if (!header) {
        return undefined;
    }

    for (const pair of header.split(';')) {
        const separator = pair.indexOf('=');

        if (separator !== -1 && pair.slice(0, separator).trim() === name) {
            return pair.slice(separator + 1).trim();
        }
    }

    return undefined;
}

export function isLocalhost(url: URL): boolean {
    const host = url.hostname;

    return host === 'localhost' || host.endsWith('.localhost') || host === '127.0.0.1' || host === '[::1]';
}

export function cookieOptions(request: Request, path: string, maxAgeSeconds: number, appUrl?: string): CookieOptionsI {
    const secure = !isLocalhost(new URL(appUrl ?? request.url));

    return {maxAgeSeconds: Math.max(0, Math.floor(maxAgeSeconds)), path, secure};
}

export function serializeCookie(name: string, value: string, options: CookieOptionsI): string {
    const attributes = [
        `${name}=${value}`,
        `Path=${options.path}`,
        `Max-Age=${options.maxAgeSeconds}`,
        'HttpOnly',
        'SameSite=Lax',
    ];

    if (options.secure) {
        attributes.push('Secure');
    }

    const cookie = attributes.join('; ');

    if (new TextEncoder().encode(cookie).length > MAX_COOKIE_BYTES) {
        throw new Error(
            `The ${name} cookie is ${cookie.length} bytes, over the ${MAX_COOKIE_BYTES} bytes a browser keeps; the access token the authorization server issues is too large to keep in a cookie`
        );
    }

    return cookie;
}

export function clearCookie(name: string, options: Omit<CookieOptionsI, 'maxAgeSeconds'>): string {
    return serializeCookie(name, '', {...options, maxAgeSeconds: 0});
}

function hasControlCharacter(value: string): boolean {
    for (let index = 0; index < value.length; index++) {
        const charCode = value.charCodeAt(index);

        if (charCode < 0x20 || charCode === 0x7f) {
            return true;
        }
    }

    return false;
}

/**
 * Accepts only a path on the app's own origin: one leading `/`, not `//`, no backslash, no control character and no
 * scheme, both as given and after dot segments are resolved. Anything else becomes `/`, so a crafted `returnTo`
 * cannot send the user to another site after sign-in.
 */
function isSameOriginPath(value: string): boolean {
    return (
        value.startsWith('/') &&
        !value.startsWith('//') &&
        value.length <= 2048 &&
        !value.includes('\\') &&
        !hasControlCharacter(value)
    );
}

export function safeReturnPath(value: string | null | undefined): string {
    if (typeof value !== 'string' || !isSameOriginPath(value)) {
        return '/';
    }

    const base = 'https://return-path.invalid';
    let parsed: URL;

    try {
        parsed = new URL(value, base);
    } catch {
        return '/';
    }

    const normalized = `${parsed.pathname}${parsed.search}${parsed.hash}`;

    return parsed.origin === base && isSameOriginPath(normalized) ? normalized : '/';
}

export interface SessionStoreOptionsI {
    appUrl?: string;
    basePath?: string;
    sessionMaxAgeSeconds?: number;
    sessionSecret?: string;
}

export interface ReadSessionI {
    present: boolean;
    session?: SessionI;
}

export interface SessionStoreI {
    clearPendingLoginCookie(request: Request): string;
    clearSessionCookie(request: Request): string;
    pendingLoginCookie(request: Request, pendingLogin: Omit<PendingLoginI, 'expiresAt'>): Promise<string>;
    readPendingLogin(request: Request): Promise<PendingLoginI | undefined>;
    readSession(request: Request): Promise<ReadSessionI>;
    requireSecret(): void;
    sessionCookie(request: Request, tokens: TokenSetI): Promise<string>;
}

export function normalizeBasePath(basePath: string | undefined): string {
    const trimmed = (basePath ?? DEFAULT_AUTH_BASE_PATH).replace(/\/+$/, '');

    if (!trimmed.startsWith('/') || trimmed.startsWith('//') || safeReturnPath(trimmed) !== trimmed) {
        throw new Error(`basePath must be an absolute path such as ${DEFAULT_AUTH_BASE_PATH}`);
    }

    return trimmed;
}

export function createSessionStore(options: SessionStoreOptionsI = {}): SessionStoreI {
    const basePath = normalizeBasePath(options.basePath);
    const appUrl = options.appUrl;
    const sessionMaxAgeSeconds = options.sessionMaxAgeSeconds ?? DEFAULT_SESSION_MAX_AGE_SECONDS;
    let sealer: SessionSealerI | undefined;

    if (!(Number.isFinite(sessionMaxAgeSeconds) && sessionMaxAgeSeconds > 0)) {
        throw new Error('sessionMaxAgeSeconds must be a positive number of seconds');
    }

    if (appUrl !== undefined && !URL.canParse(appUrl)) {
        throw new Error('appUrl must be an absolute URL such as https://app.example.com');
    }

    const getSealer = () => {
        sealer ??= createSessionSealer(resolveSessionSecret(options.sessionSecret));

        return sealer;
    };

    return {
        clearPendingLoginCookie(request: Request): string {
            return clearCookie(PENDING_LOGIN_COOKIE, cookieOptions(request, basePath, 0, appUrl));
        },

        clearSessionCookie(request: Request): string {
            return clearCookie(SESSION_COOKIE, cookieOptions(request, '/', 0, appUrl));
        },

        async pendingLoginCookie(request: Request, pendingLogin: Omit<PendingLoginI, 'expiresAt'>): Promise<string> {
            const value: PendingLoginI = {
                ...pendingLogin,
                expiresAt: Date.now() + PENDING_LOGIN_MAX_AGE_SECONDS * 1000,
            };
            const sealed = await getSealer().seal(PENDING_LOGIN_COOKIE, value);

            return serializeCookie(
                PENDING_LOGIN_COOKIE,
                sealed,
                cookieOptions(request, basePath, PENDING_LOGIN_MAX_AGE_SECONDS, appUrl)
            );
        },

        async readPendingLogin(request: Request): Promise<PendingLoginI | undefined> {
            const currentSealer = getSealer();
            const sealed = readCookie(request, PENDING_LOGIN_COOKIE);

            return sealed
                ? toPendingLogin(await currentSealer.unseal(PENDING_LOGIN_COOKIE, sealed), Date.now())
                : undefined;
        },

        async readSession(request: Request): Promise<ReadSessionI> {
            const currentSealer = getSealer();
            const sealed = readCookie(request, SESSION_COOKIE);

            if (!sealed) {
                return {present: false};
            }

            return {present: true, session: toSession(await currentSealer.unseal(SESSION_COOKIE, sealed), Date.now())};
        },

        requireSecret(): void {
            getSealer();
        },

        async sessionCookie(request: Request, tokens: TokenSetI): Promise<string> {
            const now = Date.now();
            let maxAgeSeconds = sessionMaxAgeSeconds;

            if (tokens.refreshToken === null && tokens.expiresAt !== null) {
                maxAgeSeconds = Math.min(maxAgeSeconds, Math.max(0, Math.floor((tokens.expiresAt - now) / 1000)));
            }

            const session: SessionI = {
                accessToken: tokens.accessToken,
                accessTokenExpiresAt: tokens.expiresAt,
                expiresAt: now + maxAgeSeconds * 1000,
                refreshToken: tokens.refreshToken,
            };
            const sealed = await getSealer().seal(SESSION_COOKIE, session);

            return serializeCookie(SESSION_COOKIE, sealed, cookieOptions(request, '/', maxAgeSeconds, appUrl));
        },
    };
}
