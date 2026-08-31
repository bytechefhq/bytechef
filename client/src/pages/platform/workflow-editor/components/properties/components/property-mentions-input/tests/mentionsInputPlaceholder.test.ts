import {describe, expect, it} from 'vitest';

import {
    FORMULA_MODE_PLACEHOLDER,
    TOOL_PROPERTY_FORMULA_MODE_PLACEHOLDER,
    TOOL_PROPERTY_PLACEHOLDER,
    getMentionsInputPlaceholder,
} from '../mentionsInputPlaceholder';

describe('getMentionsInputPlaceholder', () => {
    it('uses the tool-property placeholder for tool properties', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: true, toolProperty: true})).toBe(
            TOOL_PROPERTY_PLACEHOLDER
        );
    });

    it('uses the data-pill placeholder for non-tool properties', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: true, toolProperty: false})).toBe(
            "Use '$' for data pills and '=' for an expression"
        );
    });

    it('prefers an explicit placeholder over the tool-property default', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: true, placeholder: 'Custom', toolProperty: true})).toBe(
            'Custom'
        );
    });

    it('returns empty (or explicit) when expressions are disabled', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: false, toolProperty: true})).toBe('');
        expect(getMentionsInputPlaceholder({expressionEnabled: false, placeholder: 'X', toolProperty: true})).toBe('X');
    });

    it('hints at expression syntax in formula mode', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: true, formulaMode: true, toolProperty: false})).toBe(
            FORMULA_MODE_PLACEHOLDER
        );
    });

    it('hints at fromAi for a tool property in formula mode', () => {
        expect(getMentionsInputPlaceholder({expressionEnabled: true, formulaMode: true, toolProperty: true})).toBe(
            TOOL_PROPERTY_FORMULA_MODE_PLACEHOLDER
        );
    });

    // The editor strips the leading "=" from expression content, so a sample carrying one would not
    // match what the user types over it.
    it('omits the leading = from the formula mode samples', () => {
        expect(FORMULA_MODE_PLACEHOLDER.startsWith('=')).toBe(false);
        expect(TOOL_PROPERTY_FORMULA_MODE_PLACEHOLDER.startsWith('=')).toBe(false);
    });

    it('still prefers an explicit placeholder in formula mode', () => {
        expect(
            getMentionsInputPlaceholder({
                expressionEnabled: true,
                formulaMode: true,
                placeholder: 'Custom',
                toolProperty: true,
            })
        ).toBe('Custom');
    });
});
