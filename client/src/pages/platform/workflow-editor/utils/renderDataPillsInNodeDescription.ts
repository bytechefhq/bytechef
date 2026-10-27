import sanitize from 'sanitize-html';

const DATA_PILL_REFERENCE_PATTERN = /\$\{([^{}<>]+)\}/g;

const DATA_PILL_CLASS =
    'inline-flex items-center gap-1 rounded-full bg-muted px-2 py-0.5 font-mono text-xs text-content-neutral-primary';

function escapeAttribute(value: string): string {
    return value.replaceAll('&', '&amp;').replaceAll('"', '&quot;');
}

export default function renderDataPillsInNodeDescription(
    nodeDescription: string,
    getIconSource: (reference: string) => string
): string {
    const sanitizedNodeDescription = sanitize(nodeDescription, {
        allowedAttributes: {
            div: ['class'],
            table: ['class'],
            td: ['class'],
            tr: ['class'],
        },
    });

    return sanitizedNodeDescription.replace(DATA_PILL_REFERENCE_PATTERN, (match, reference: string) => {
        const iconSource = escapeAttribute(getIconSource(reference));

        return `<span class="${DATA_PILL_CLASS}"><img alt="" class="size-4" src="${iconSource}" />${reference}</span>`;
    });
}
