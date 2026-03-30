import {replaceMentionNodesInHtmlWithVariables} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/propertyMentionDom';
import {describe, expect, it} from 'vitest';

describe('replaceMentionNodesInHtmlWithVariables', () => {
    it('replaces a flat mention span with ${id}', () => {
        const html = '<span data-type="mention" class="property-mention" data-id="gmail.subject">x</span>';

        expect(replaceMentionNodesInHtmlWithVariables(html)).toBe('${gmail.subject}');
    });

    it('replaces a nested chip mention with ${id}', () => {
        const html =
            '<span data-type="mention" class="property-mention" data-id="accelo.field">' +
            '<span class="property-mention-chip"><img src="x"/><span class="property-mention-label">accelo.field</span></span>' +
            '</span>';

        expect(replaceMentionNodesInHtmlWithVariables(html)).toBe('${accelo.field}');
    });

    it('leaves html without mentions unchanged', () => {
        const html = '<p>plain text</p>';

        expect(replaceMentionNodesInHtmlWithVariables(html)).toBe(html);
    });
});
