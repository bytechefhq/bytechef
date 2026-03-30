/**
 * DOM structure for TipTap property mentions (see Mention.configure renderHTML in PropertyMentionsInputEditor).
 * Root keeps data-type / data-id for parse and serialization; chip carries pill visuals so unavailable styling
 * can target the chip without fighting classes on the root.
 */
export const PROPERTY_MENTION_CHIP_CLASS = 'property-mention-chip';

export const PROPERTY_MENTION_LABEL_CLASS = 'property-mention-label';

export const PROPERTY_MENTION_ROOT_CLASS = 'property-mention';

/**
 * Replaces each span[data-type="mention"][data-id] subtree with ${id} for persistence.
 * Nested chip markup makes a single-regex approach unsafe; this walks the parsed DOM.
 */
export function replaceMentionNodesInHtmlWithVariables(html: string): string {
    if (!html.includes('data-type="mention"')) {
        return html;
    }

    if (typeof DOMParser === 'undefined') {
        return html;
    }

    const documentInstance = new DOMParser().parseFromString(
        `<div id="property-mention-serialize-root">${html}</div>`,
        'text/html'
    );
    const root = documentInstance.querySelector('#property-mention-serialize-root');

    if (!root) {
        return html;
    }

    root.querySelectorAll('span[data-type="mention"][data-id]').forEach((mentionElement) => {
        const mentionId = mentionElement.getAttribute('data-id');

        if (!mentionId) {
            return;
        }

        mentionElement.replaceWith(documentInstance.createTextNode(`\${${mentionId}}`));
    });

    return root.innerHTML;
}
