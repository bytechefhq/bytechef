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

    it('returns null when the message carries no fenced block', () => {
        expect(extractDefinitionFromMessage('  label: x  ')).toBeNull();
    });

    it('returns null for a prose reply that only mentions a definition', () => {
        expect(extractDefinitionFromMessage('I cannot build that workflow without a trigger.')).toBeNull();
    });

    it('returns null when the fenced block is blank', () => {
        expect(extractDefinitionFromMessage('```json\n   \n```')).toBeNull();
    });

    it('concatenates array text parts faithfully before extracting', () => {
        const content = [
            {text: 'Here you go:\n```json\n{"a":1}', type: 'text'},
            {text: '}\n```', type: 'text'},
        ];

        expect(extractDefinitionFromMessage(content)).toBe('{"a":1}}');
    });

    it('returns null for empty content', () => {
        expect(extractDefinitionFromMessage(undefined)).toBeNull();
    });
});
