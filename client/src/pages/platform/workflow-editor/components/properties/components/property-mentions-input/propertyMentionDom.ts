import {escapeHtmlForParagraph} from '@/pages/platform/workflow-editor/utils/encodingUtils';
import {decode} from 'html-entities';
import sanitizeHtml from 'sanitize-html';

/**
 * DOM structure for TipTap property mentions (see Mention.configure renderHTML in PropertyMentionsInputEditor).
 * Root keeps data-type / data-id for parse and serialization; chip carries pill visuals so unavailable styling
 * can target the chip without fighting classes on the root.
 */
/**
 * Matches one ${nodeName.path} data pill; group 1 is the path. Shared by the stored-value load path and the
 * editor's paste rule so pasted text and loaded values produce the same mention nodes.
 */
export const DATA_PILL_REGEX = /\$\{([^}]+)}/g;

export const PROPERTY_MENTION_CHIP_CLASS = 'property-mention-chip';

export const PROPERTY_MENTION_LABEL_CLASS = 'property-mention-label';

export const PROPERTY_MENTION_ROOT_CLASS = 'property-mention';

/**
 * Converts a stored property value into editor HTML: lines become paragraphs, escaped for the plain-text control
 * types (RICH_TEXT html is decoded and sanitized instead), and each ${nodeName.path} data pill becomes a
 * span[data-type="mention"][data-id] so TipTap's Mention extension renders it as a visual chip. This is the forward
 * direction of replaceMentionNodesInHtmlWithVariables.
 */
export function buildPropertyMentionsContent(value?: string, controlType?: string): string | undefined {
    if (typeof value !== 'string') {
        return;
    }

    if (!value) {
        return '';
    }

    let content = value;
    let contentIsDecodedHtml = false;

    if (
        controlType === 'RICH_TEXT' &&
        (content.includes('&lt;') || content.includes('&gt;') || content.includes('&amp;'))
    ) {
        content = decode(content);

        content = sanitizeHtml(content);

        contentIsDecodedHtml = true;
    }

    const isPlainText = controlType === 'TEXT_AREA' || controlType === 'TEXT' || controlType === 'FORMULA_MODE';

    if (!contentIsDecodedHtml && (content.includes('\n') || isPlainText)) {
        const valueLines = content.split('\n');

        const paragraphedLines = isPlainText
            ? valueLines.map((valueLine) => `<p>${escapeHtmlForParagraph(valueLine)}</p>`)
            : valueLines.map((valueLine) => `<p>${valueLine}</p>`);

        content = paragraphedLines.join('');
    }

    const matches = value.match(DATA_PILL_REGEX)?.map((match) => match.slice(2, -1));

    if (matches) {
        for (const match of matches) {
            const dataPill = `\${${match}}`;

            // Plain text was escaped above, so a pill whose path holds &, < or > (a bracketed key such as
            // ${trigger_1['Q&A']}) is only found in its escaped form.
            content = content.replace(
                isPlainText ? escapeHtmlForParagraph(dataPill) : dataPill,
                `<span data-type="mention" class="${PROPERTY_MENTION_ROOT_CLASS}" data-id="${match}"></span>`
            );
        }
    }

    return content;
}

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
