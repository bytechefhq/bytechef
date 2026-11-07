declare global {
    interface Window {
        __BYTECHEF_ICON_BASE_URL__?: string;
    }
}

const ICON_BASE_URL: string =
    window.__BYTECHEF_ICON_BASE_URL__ ?? import.meta.env.VITE_ICON_BASE_URL ?? 'http://localhost:9555/icons';

export function buildIconUrl(componentName: string) {
    return `${ICON_BASE_URL}/${encodeURIComponent(componentName)}.svg`;
}
