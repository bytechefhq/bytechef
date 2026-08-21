const LOGIN_PATH = '/login';
const REDIRECT_PARAM = 'redirect';

interface RedirectLocationI {
    hash?: string;
    pathname: string;
    search?: string;
}

const isSafeRedirect = (target: string): boolean =>
    target.startsWith('/') && !target.startsWith('//') && !target.startsWith('/\\');

export const buildLoginPath = (location: RedirectLocationI): string => {
    if (location.pathname === LOGIN_PATH) {
        return LOGIN_PATH;
    }

    const target = `${location.pathname}${location.search ?? ''}${location.hash ?? ''}`;

    if (target === '/' || !isSafeRedirect(target)) {
        return LOGIN_PATH;
    }

    return `${LOGIN_PATH}?${REDIRECT_PARAM}=${encodeURIComponent(target)}`;
};

export const getLoginRedirect = (search: string): string | undefined => {
    const target = new URLSearchParams(search).get(REDIRECT_PARAM);

    if (!target || !isSafeRedirect(target)) {
        return undefined;
    }

    return target;
};
