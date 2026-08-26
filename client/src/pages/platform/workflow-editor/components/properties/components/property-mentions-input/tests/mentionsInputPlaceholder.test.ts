import {describe, expect, it} from 'vitest';

import {TOOL_PROPERTY_PLACEHOLDER, getMentionsInputPlaceholder} from '../mentionsInputPlaceholder';

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
});
