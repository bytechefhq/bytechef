import {getCookie} from '@/shared/util/cookie-utils';

export const APP_ICON_ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/webp'];

export const APP_ICON_MAX_BYTES = 256 * 1024;

export const getAppIconUrl = (appId: string) => `/api/automation/internal/apps/${encodeURIComponent(appId)}/icon`;

const appIconVersions = new Map<string, number>();

export const getAppIconPreviewUrl = (appId: string) => {
    const iconVersion = appIconVersions.get(appId);

    return `${import.meta.env.VITE_API_BASE_PATH || ''}${getAppIconUrl(appId)}${iconVersion ? `?v=${iconVersion}` : ''}`;
};

export const validateAppIconFile = (file: File): string | undefined => {
    if (!APP_ICON_ACCEPTED_TYPES.includes(file.type)) {
        return 'The icon must be a PNG, JPEG or WebP image.';
    }

    if (file.size > APP_ICON_MAX_BYTES) {
        return 'The icon cannot be larger than 256 KB.';
    }

    return undefined;
};

const readErrorMessage = async (response: Response) => {
    try {
        const problemDetail = (await response.json()) as {detail?: string; title?: string};

        return problemDetail.detail || problemDetail.title || `Request failed with status ${response.status}`;
    } catch {
        return `Request failed with status ${response.status}`;
    }
};

const sendAppIconRequest = async (appId: string, init: RequestInit) => {
    const response = await fetch(getAppIconUrl(appId), {
        ...init,
        headers: {
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
    });

    if (!response.ok) {
        throw new Error(await readErrorMessage(response));
    }

    appIconVersions.set(appId, Date.now());
};

export const uploadAppIcon = (appId: string, file: File) => {
    const formData = new FormData();

    formData.append('file', file);

    return sendAppIconRequest(appId, {body: formData, method: 'POST'});
};

export const deleteAppIcon = (appId: string) => sendAppIconRequest(appId, {method: 'DELETE'});
