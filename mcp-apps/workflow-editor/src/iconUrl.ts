// Component icons are the widget's only network dependency. Icons load as plain <img>
// tags, so no icon bytes ever pass through model context; a failed load falls back to
// a generic glyph in ComponentImage.
//
// The base URL resolves in order:
// 1. window.__BYTECHEF_ICON_BASE_URL__ — injected at serve time by McpAppWorkflowEditor
//    (the single-file bundle cannot know the deployment URL at build time). The origin
//    is allowed via the ui resource's csp.resourceDomains.
// 2. VITE_ICON_BASE_URL — build-time override for dev/testing.
// 3. The local dev server's anonymous /icons/components endpoint.
declare global {
    interface Window {
        __BYTECHEF_ICON_BASE_URL__?: string;
    }
}

const ICON_BASE_URL: string =
    window.__BYTECHEF_ICON_BASE_URL__ ??
    import.meta.env.VITE_ICON_BASE_URL ??
    'http://localhost:8080/icons/components';

export function buildIconUrl(componentName: string) {
    return `${ICON_BASE_URL}/${encodeURIComponent(componentName)}`;
}
