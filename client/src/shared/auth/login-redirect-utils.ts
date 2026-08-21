const LOGIN_PATH = '/login';
const REDIRECT_PARAM = 'redirect';
const REDIRECT_STORAGE_KEY = 'bytechef.loginRedirect';
const SENTINEL_ORIGIN = 'https://bytechef.invalid';

interface RedirectLocationI {
    hash?: string;
    pathname: string;
    search?: string;
}

const normalizeRedirect = (target: string): string | undefined => {
    if (!target.startsWith('/')) {
        return undefined;
    }

    let url: URL;

    try {
        url = new URL(target, SENTINEL_ORIGIN);
    } catch {
        return undefined;
    }

    if (url.origin !== SENTINEL_ORIGIN) {
        return undefined;
    }

    return `${url.pathname}${url.search}${url.hash}`;
};

export const buildLoginPath = (location: RedirectLocationI): string => {
    if (location.pathname === LOGIN_PATH) {
        return LOGIN_PATH;
    }

    const target = normalizeRedirect(`${location.pathname}${location.search ?? ''}${location.hash ?? ''}`);

    if (!target || target === '/') {
        return LOGIN_PATH;
    }

    return `${LOGIN_PATH}?${REDIRECT_PARAM}=${encodeURIComponent(target)}`;
};

export const getLoginRedirect = (search: string): string | undefined => {
    const target = new URLSearchParams(search).get(REDIRECT_PARAM);

    return target ? normalizeRedirect(target) : undefined;
};

export const rememberLoginRedirect = (target: string | undefined): void => {
    try {
        if (target) {
            window.sessionStorage.setItem(REDIRECT_STORAGE_KEY, target);
        } else {
            window.sessionStorage.removeItem(REDIRECT_STORAGE_KEY);
        }
    } catch {
        return;
    }
};

export const consumeLoginRedirect = (): string | undefined => {
    try {
        const stored = window.sessionStorage.getItem(REDIRECT_STORAGE_KEY);

        window.sessionStorage.removeItem(REDIRECT_STORAGE_KEY);

        return stored ? normalizeRedirect(stored) : undefined;
    } catch {
        return undefined;
    }
};
