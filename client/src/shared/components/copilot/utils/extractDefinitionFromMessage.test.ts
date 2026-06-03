import {describe, expect, it} from 'vitest';

import {extractDefinitionFromMessage} from './extractDefinitionFromMessage';

describe('extractDefinitionFromMessage', () => {
    it('extracts a fenced json block', () => {
        const content = 'Here you go:\n```json\n{"label":"x"}\n```\nDone.';

        expect(extractDefinitionFromMessage(content)).toBe('{"label":"x"}');
    });

    it('extracts a fenced yaml block', () => {
        const content = '```yaml\nlabel: x\n```';

        expect(extractDefinitionFromMessage(content)).toBe('label: x');
    });

    it('falls back to trimmed whole text when no fence', () => {
        expect(extractDefinitionFromMessage('  label: x  ')).toBe('label: x');
    });

    it('concatenates array text parts faithfully before extracting', () => {
        const content = [
            {text: 'Here you go:\n```json\n{"a":1}', type: 'text'},
            {text: '}\n```', type: 'text'},
        ];

        expect(extractDefinitionFromMessage(content)).toBe('{"a":1}}');
    });

    it('returns empty string for empty content', () => {
        expect(extractDefinitionFromMessage(undefined)).toBe('');
    });
});
