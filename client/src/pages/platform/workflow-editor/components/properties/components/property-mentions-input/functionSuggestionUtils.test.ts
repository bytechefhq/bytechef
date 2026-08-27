import {EvaluatorFunctionDefinition, EvaluatorFunctionType} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {
    buildFunctionInsertion,
    filterFunctionDefinitions,
    findEnclosingFunctionCall,
    findFunctionSuggestionMatch,
    formatFunctionSignature,
    formatFunctionSignatureParts,
    isFormulaModeActive,
} from './functionSuggestionUtils';

function definition(overrides: Partial<EvaluatorFunctionDefinition>): EvaluatorFunctionDefinition {
    return {
        category: 'STRING',
        description: '',
        example: '',
        name: 'fn',
        parameters: [],
        returnType: 'STRING',
        title: '',
        ...overrides,
    } as EvaluatorFunctionDefinition;
}

describe('formatFunctionSignature', () => {
    it('formats a multi-parameter signature with a return type', () => {
        const result = formatFunctionSignature(
            definition({
                name: 'substring',
                parameters: [
                    {description: '', name: 'value', required: true, type: EvaluatorFunctionType.String},
                    {description: '', name: 'beginIndex', required: true, type: EvaluatorFunctionType.Integer},
                ],
                returnType: EvaluatorFunctionType.String,
            })
        );

        expect(result).toBe('(value: String, beginIndex: Integer): String');
    });

    it('formats a zero-argument signature', () => {
        const result = formatFunctionSignature(
            definition({name: 'uuid', parameters: [], returnType: EvaluatorFunctionType.String})
        );

        expect(result).toBe('(): String');
    });
});

describe('filterFunctionDefinitions', () => {
    const definitions = [
        definition({name: 'concat', title: 'Concatenate'}),
        definition({name: 'contains', title: 'Contains'}),
        definition({name: 'substring', title: 'Substring'}),
        definition({name: 'join', title: 'Join collection'}),
    ];

    it('matches by case-insensitive name prefix', () => {
        const result = filterFunctionDefinitions(definitions, 'CON');

        expect(result.map((definition) => definition.name)).toEqual(['concat', 'contains']);
    });

    it('falls back to a title substring match when the name does not prefix-match', () => {
        const result = filterFunctionDefinitions(definitions, 'collection');

        expect(result.map((definition) => definition.name)).toEqual(['join']);
    });

    it('caps the number of returned results', () => {
        const many = Array.from({length: 100}, (_, index) => definition({name: `fn${index}`}));

        expect(filterFunctionDefinitions(many, 'fn').length).toBe(50);
    });
});

describe('buildFunctionInsertion', () => {
    it('builds the call text and the caret offset between the parens', () => {
        expect(buildFunctionInsertion('concat')).toEqual({caretOffset: 7, content: 'concat()'});
    });
});

describe('findFunctionSuggestionMatch', () => {
    it('matches the trailing word of 2+ characters before the caret', () => {
        const $position = {nodeBefore: {isText: true, text: 'conc'}, pos: 6} as never;

        expect(findFunctionSuggestionMatch({$position})).toEqual({
            query: 'conc',
            range: {from: 2, to: 6},
            text: 'conc',
        });
    });

    it('returns null for a single trailing character', () => {
        const $position = {nodeBefore: {isText: true, text: 'c'}, pos: 3} as never;

        expect(findFunctionSuggestionMatch({$position})).toBeNull();
    });

    it('returns null when there is no trailing word', () => {
        const $position = {nodeBefore: {isText: true, text: 'add('}, pos: 5} as never;

        expect(findFunctionSuggestionMatch({$position})).toBeNull();
    });

    it('returns null when nodeBefore is not a text node', () => {
        const $position = {nodeBefore: {isText: false, text: undefined}, pos: 4} as never;

        expect(findFunctionSuggestionMatch({$position})).toBeNull();
    });

    it('returns null when the word is the tail of a $ datapill token', () => {
        const $position = {nodeBefore: {isText: true, text: '$conc'}, pos: 7} as never;

        expect(findFunctionSuggestionMatch({$position})).toBeNull();
    });
});

describe('isFormulaModeActive', () => {
    it('is true only when FormulaMode storage flag is set', () => {
        expect(isFormulaModeActive({storage: {FormulaMode: {isFormulaMode: true}}} as never)).toBe(true);
        expect(isFormulaModeActive({storage: {FormulaMode: {isFormulaMode: false}}} as never)).toBe(false);
        expect(isFormulaModeActive({storage: {}} as never)).toBe(false);
    });
});

describe('findEnclosingFunctionCall', () => {
    it('returns the call and arg 0 right after the open paren when a closing paren follows', () => {
        expect(findEnclosingFunctionCall('contains(', ')')).toEqual({argIndex: 0, name: 'contains'});
    });

    it('advances the arg index past commas', () => {
        expect(findEnclosingFunctionCall('contains(a, ', ')')).toEqual({argIndex: 1, name: 'contains'});
        expect(findEnclosingFunctionCall('contains(a, b', ')')).toEqual({argIndex: 1, name: 'contains'});
    });

    it('returns the innermost named call', () => {
        expect(findEnclosingFunctionCall('concat(a, substring(x, ', '))')).toEqual({argIndex: 1, name: 'substring'});
    });

    it('attributes commas inside a grouping paren to the grouping, not the outer call', () => {
        expect(findEnclosingFunctionCall('concat(a, (b, c), ', ')')).toEqual({argIndex: 2, name: 'concat'});
    });

    it('ignores commas inside quotes', () => {
        expect(findEnclosingFunctionCall('contains("a, b", ', ')')).toEqual({argIndex: 1, name: 'contains'});
    });

    it('returns null when there is no open call', () => {
        expect(findEnclosingFunctionCall('contains(a)', '')).toBeNull();
        expect(findEnclosingFunctionCall('abc', '')).toBeNull();
    });

    it('returns null when the caret sits past the last closing paren (outside all calls)', () => {
        expect(findEnclosingFunctionCall('contains(contains(x, true))', '')).toBeNull();
    });

    it('returns null when the enclosing call is never closed after the caret (unbalanced)', () => {
        // contains(contains(x, true)| — outer paren is never closed, caret is right after the inner ).
        expect(findEnclosingFunctionCall('contains(contains(x, true)', '')).toBeNull();
    });

    it('shows the outer call when the caret is inside it but before its closing paren', () => {
        // contains(contains(x, true)|) — caret is inside the outer call; its ) still follows the caret.
        expect(findEnclosingFunctionCall('contains(contains(x, true)', ')')).toEqual({argIndex: 0, name: 'contains'});
    });

    it('shows the inner call when the caret is inside it', () => {
        expect(findEnclosingFunctionCall('contains(contains(x', ', true))')).toEqual({argIndex: 0, name: 'contains'});
    });

    it('ignores parens opened and closed again after the caret', () => {
        expect(findEnclosingFunctionCall('contains(', '(a))')).toEqual({argIndex: 0, name: 'contains'});
    });

    it('skips an unnamed grouping paren and reports the call enclosing it', () => {
        expect(findEnclosingFunctionCall('contains((', '))')).toEqual({argIndex: 0, name: 'contains'});
    });
});

describe('formatFunctionSignatureParts', () => {
    it('splits params and return type', () => {
        const result = formatFunctionSignatureParts({
            category: 'STRING',
            description: '',
            example: '',
            name: 'substring',
            parameters: [
                {description: '', name: 'value', required: true, type: 'STRING'},
                {description: '', name: 'beginIndex', required: true, type: 'INTEGER'},
            ],
            returnType: 'STRING',
            title: '',
        } as never);

        expect(result.params).toEqual(['value: String', 'beginIndex: Integer']);
        expect(result.returnType).toBe('String');
    });
});
