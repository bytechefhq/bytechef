import {
    buildPropertyMentionsContent,
    replaceMentionNodesInHtmlWithVariables,
} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/propertyMentionDom';
import {describe, expect, it} from 'vitest';

describe('buildPropertyMentionsContent', () => {
    it('converts each ${id} pill into a mention span', () => {
        expect(buildPropertyMentionsContent('Hi ${trigger_1.firstName}', 'TEXT')).toBe(
            'Hi <span data-type="mention" class="property-mention" data-id="trigger_1.firstName"></span>'
        );
    });

    it('converts multiple occurrences of the same pill', () => {
        expect(buildPropertyMentionsContent('${a.b} and ${a.b}', 'TEXT')).toBe(
            '<span data-type="mention" class="property-mention" data-id="a.b"></span> and ' +
                '<span data-type="mention" class="property-mention" data-id="a.b"></span>'
        );
    });

    it('returns plain constant text unchanged', () => {
        expect(buildPropertyMentionsContent('a constant value', 'TEXT')).toBe('a constant value');
    });

    it('returns empty string for empty input and undefined for non-string input', () => {
        expect(buildPropertyMentionsContent('', 'TEXT')).toBe('');
        expect(buildPropertyMentionsContent(undefined, 'TEXT')).toBeUndefined();
    });
});

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
