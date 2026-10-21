export interface BytechefClientOptions {
    appKey?: string;
    fetch?: typeof fetch;
    timeoutMs?: number;
    url?: string;
}

export interface ResolvedConfig {
    appKey: string;
    baseUrl: string;
    fetch: typeof fetch;
    timeoutMs: number;
}

export const DEFAULT_TIMEOUT_MS = 70_000;

const KEY_HELP =
    'BYTECHEF_APP_KEY is not set. Create or rotate the app key on the app in ByteChef (App → Rotate key) and set it in the server environment; never under a VITE_ prefix.';

export function assertServerRuntime(): void {
    if (typeof window !== 'undefined') {
        throw new Error(
            '@bytechef/app runs on the server only: it holds the app key. Call it from server code, never from the browser.'
        );
    }
}

export function joinUrl(baseUrl: string, path: string): string {
    return `${baseUrl.replace(/\/+$/, '')}${path}`;
}

function readEnv(name: string): string | undefined {
    const value = (globalThis as {process?: {env?: Record<string, string | undefined>}}).process?.env?.[name];

    return value === undefined || value === '' ? undefined : value;
}

export function resolveConfig(options: BytechefClientOptions): ResolvedConfig {
    const url = options.url ?? readEnv('BYTECHEF_URL');
    const appKey = options.appKey ?? readEnv('BYTECHEF_APP_KEY');

    if (options.timeoutMs !== undefined && !(Number.isFinite(options.timeoutMs) && options.timeoutMs > 0)) {
        throw new Error('timeoutMs must be a positive number of milliseconds');
    }

    if (!url) {
        throw new Error(
            'BYTECHEF_URL is not set. Set it to the ByteChef server URL, for example https://app.bytechef.io.'
        );
    }

    if (!appKey) {
        throw new Error(KEY_HELP);
    }

    return {
        appKey,
        baseUrl: url.replace(/\/+$/, ''),
        fetch: options.fetch ?? globalThis.fetch.bind(globalThis),
        timeoutMs: options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
    };
}
