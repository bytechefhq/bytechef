import {describe, expect, it} from 'vitest';

/**
 * Tests for PropertyComboBox keywords filtering logic.
 *
 * The PropertyComboBox passes a `keywords` prop to each CommandItem so cmdk
 * searches by label/description text instead of the raw value. This is the
 * logic extracted from the JSX:
 *
 *   keywords={[
 *       typeof option.label === 'string' ? option.label : undefined,
 *       option.description,
 *   ].filter(Boolean) as string[]}
 */

function buildKeywords(option: {description?: string; label: string | object}): string[] {
    return [typeof option.label === 'string' ? option.label : undefined, option.description].filter(
        Boolean
    ) as string[];
}

describe('PropertyComboBox keywords filtering', () => {
    it('should include string label in keywords', () => {
        const keywords = buildKeywords({label: 'My Label'});

        expect(keywords).toContain('My Label');
    });

    it('should include description in keywords', () => {
        const keywords = buildKeywords({description: 'A helpful description', label: 'Label'});

        expect(keywords).toEqual(['Label', 'A helpful description']);
    });

    it('should exclude non-string labels from keywords', () => {
        const keywords = buildKeywords({label: {complex: 'object'}});

        expect(keywords).toEqual([]);
    });

    it('should include both label and description when both are strings', () => {
        const keywords = buildKeywords({description: 'Desc', label: 'Label'});

        expect(keywords).toHaveLength(2);
        expect(keywords[0]).toBe('Label');
        expect(keywords[1]).toBe('Desc');
    });

    it('should handle undefined description', () => {
        const keywords = buildKeywords({label: 'Only Label'});

        expect(keywords).toEqual(['Only Label']);
    });

    it('should handle non-string label with description', () => {
        const keywords = buildKeywords({description: 'Has description', label: {jsx: true}});

        expect(keywords).toEqual(['Has description']);
    });

    it('should return empty array when label is non-string and no description', () => {
        const keywords = buildKeywords({label: {jsx: true}});

        expect(keywords).toEqual([]);
    });

    it('should filter out empty string description', () => {
        const keywords = buildKeywords({description: '', label: 'Label'});

        expect(keywords).toEqual(['Label']);
    });
});
