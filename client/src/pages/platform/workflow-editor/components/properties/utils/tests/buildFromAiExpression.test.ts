import {describe, expect, it} from 'vitest';

import buildFromAiExpression from '../buildFromAiExpression';

describe('buildFromAiExpression', () => {
    it('builds expression with description options and required', () => {
        expect(
            buildFromAiExpression({
                description: "It's",
                name: 'format',
                options: [{value: 'JSON'}, {value: 'XML'}],
                required: true,
                type: 'STRING',
            })
        ).toBe("=fromAi('format', 'STRING', {'description': 'It''s', 'options': {'JSON', 'XML'}, 'required': true})");
    });

    it('skips expression shaped default values', () => {
        expect(buildFromAiExpression({defaultValue: "=fromAi('x', 'STRING', {})", name: 'x', type: 'STRING'})).toBe(
            "=fromAi('x', 'STRING', {'required': false})"
        );
    });

    it('includes plain default values with escaping', () => {
        expect(buildFromAiExpression({defaultValue: "a'b", name: 'x', type: 'STRING'})).toBe(
            "=fromAi('x', 'STRING', {'defaultValue': 'a''b', 'required': false})"
        );
    });

    it('qualifies array item names', () => {
        expect(buildFromAiExpression({arrayName: 'items', name: '0', type: 'STRING'})).toBe(
            "=fromAi('items_0', 'STRING', {'required': false})"
        );
    });

    it('defaults required to false', () => {
        expect(buildFromAiExpression({name: 'a', type: 'NUMBER'})).toBe("=fromAi('a', 'NUMBER', {'required': false})");
    });
});
