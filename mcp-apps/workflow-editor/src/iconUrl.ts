// Component icons are the widget's only network dependency. The host base URL is
// injected at build time (VITE_ICON_BASE_URL) and must be allowed by the img-src of
// the CSP declared in the workflow tools' _meta.ui. Icons load as plain <img> tags,
// so no icon bytes ever pass through model context; a failed load falls back to a
// generic glyph in ComponentImage.
const ICON_BASE_URL: string =
    import.meta.env.VITE_ICON_BASE_URL ?? 'http://localhost:6123/integration/component-icons';

export function buildIconUrl(componentName: string) {
    return `${ICON_BASE_URL}/${encodeURIComponent(componentName)}`;
}
