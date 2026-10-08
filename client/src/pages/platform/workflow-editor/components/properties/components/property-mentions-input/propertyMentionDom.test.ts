import {
    buildPropertyMentionsContent,
    replaceMentionNodesInHtmlWithVariables,
} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/propertyMentionDom';
import {describe, expect, it} from 'vitest';

describe('buildPropertyMentionsContent', () => {
    it('converts each ${id} pill into a mention span', () => {
        expect(buildPropertyMentionsContent('Hi ${trigger_1.firstName}', 'TEXT')).toBe(
            '<p>Hi <span data-type="mention" class="property-mention" data-id="trigger_1.firstName"></span></p>'
        );
    });

    it('converts multiple occurrences of the same pill', () => {
        expect(buildPropertyMentionsContent('${a.b} and ${a.b}', 'TEXT')).toBe(
            '<p><span data-type="mention" class="property-mention" data-id="a.b"></span> and ' +
                '<span data-type="mention" class="property-mention" data-id="a.b"></span></p>'
        );
    });

    it('wraps plain constant text in a paragraph', () => {
        expect(buildPropertyMentionsContent('a constant value', 'TEXT')).toBe('<p>a constant value</p>');
    });

    it('escapes markup around a pill on a single line', () => {
        expect(buildPropertyMentionsContent('<Paragraph>${anthropic_1.body} </Paragraph>', 'TEXT')).toBe(
            '<p>&lt;Paragraph&gt;<span data-type="mention" class="property-mention" data-id="anthropic_1.body"></span> &lt;/Paragraph&gt;</p>'
        );
    });

    it('keeps a pill whose bracketed path contains markup characters', () => {
        expect(buildPropertyMentionsContent("${trigger_1['Q&A']} & more", 'TEXT')).toBe(
            '<p><span data-type="mention" class="property-mention" data-id="trigger_1[\'Q&A\']"></span> &amp; more</p>'
        );
    });

    it('splits multi-line plain text into escaped paragraphs', () => {
        expect(buildPropertyMentionsContent('a<b\n${x.y}', 'TEXT_AREA')).toBe(
            '<p>a&lt;b</p><p><span data-type="mention" class="property-mention" data-id="x.y"></span></p>'
        );
    });

    it('decodes and sanitizes encoded RICH_TEXT html instead of escaping it', () => {
        expect(buildPropertyMentionsContent('&lt;p&gt;Hi ${a.b}&lt;/p&gt;', 'RICH_TEXT')).toBe(
            '<p>Hi <span data-type="mention" class="property-mention" data-id="a.b"></span></p>'
        );
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
