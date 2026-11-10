import {EvaluatorFunctionDefinition, EvaluatorFunctionType} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {
    buildFunctionInsertion,
    filterFunctionDefinitions,
    filterFunctionDefinitionsForArgument,
    findEnclosingFunctionCall,
    findEnclosingFunctionCallChain,
    findForcedFunctionSuggestionMatch,
    findForcedFunctionSuggestionStart,
    findFunctionSuggestionMatch,
    formatFunctionSignature,
    formatFunctionSignatureParts,
    getPropertyFunctionType,
    groupFunctionOverloads,
    isFormulaModeActive,
    isFunctionTypeCompatible,
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

    it('names types the way data pills do', () => {
        const result = formatFunctionSignature(
            definition({
                name: 'convert',
                parameters: [
                    {description: '', name: 'list', required: true, type: EvaluatorFunctionType.List},
                    {description: '', name: 'map', required: true, type: EvaluatorFunctionType.Map},
                    {description: '', name: 'long', required: true, type: EvaluatorFunctionType.Long},
                    {description: '', name: 'double', required: true, type: EvaluatorFunctionType.Double},
                    {description: '', name: 'character', required: true, type: EvaluatorFunctionType.Character},
                ],
                returnType: EvaluatorFunctionType.Datetime,
            })
        );

        expect(result).toBe('(list: Array, map: Object, long: Integer, double: Number, character: String): Date Time');
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

    it('returns every definition for an empty query', () => {
        const many = Array.from({length: 100}, (_, index) => definition({name: `fn${index}`}));

        expect(filterFunctionDefinitions(many, '').length).toBe(100);
    });
});

describe('buildFunctionInsertion', () => {
    it('builds the call text and the caret offset between the parens', () => {
        expect(buildFunctionInsertion('concat')).toEqual({caretOffset: 7, content: 'concat()'});
    });

    it('inserts only the name when an argument list already follows', () => {
        expect(buildFunctionInsertion('concat', '(a, b)')).toEqual({caretOffset: 7, content: 'concat'});
    });
});

describe('isFunctionTypeCompatible', () => {
    it('accepts any type for an Any parameter or an Any return type', () => {
        expect(isFunctionTypeCompatible(EvaluatorFunctionType.Any, EvaluatorFunctionType.Map)).toBe(true);
        expect(isFunctionTypeCompatible(EvaluatorFunctionType.List, EvaluatorFunctionType.Any)).toBe(true);
    });

    it('treats every numeric type as one family', () => {
        expect(isFunctionTypeCompatible(EvaluatorFunctionType.Number, EvaluatorFunctionType.Integer)).toBe(true);
        expect(isFunctionTypeCompatible(EvaluatorFunctionType.Long, EvaluatorFunctionType.Double)).toBe(true);
    });

    it('treats a character as a string', () => {
        expect(isFunctionTypeCompatible(EvaluatorFunctionType.String, EvaluatorFunctionType.Character)).toBe(true);
    });

    it('rejects a different type', () => {
        expect(isFunctionTypeCompatible(EvaluatorFunctionType.Number, EvaluatorFunctionType.String)).toBe(false);
    });
});

describe('filterFunctionDefinitionsForArgument', () => {
    const definitions = [
        definition({name: 'concat', returnType: EvaluatorFunctionType.String}),
        definition({name: 'size', returnType: EvaluatorFunctionType.Integer}),
        definition({name: 'contains', returnType: EvaluatorFunctionType.Boolean}),
        definition({
            name: 'lower',
            parameters: [{description: '', name: 'text', required: true, type: EvaluatorFunctionType.String}],
            returnType: EvaluatorFunctionType.String,
        }),
        definition({
            name: 'getOrDefault',
            parameters: [
                {description: '', name: 'dataPill', required: true, type: EvaluatorFunctionType.Any},
                {description: '', name: 'defaultValue', required: true, type: EvaluatorFunctionType.Number},
            ],
            returnType: EvaluatorFunctionType.Number,
        }),
        definition({
            name: 'getOrDefault',
            parameters: [
                {description: '', name: 'dataPill', required: true, type: EvaluatorFunctionType.Any},
                {description: '', name: 'defaultValue', required: true, type: EvaluatorFunctionType.String},
            ],
            returnType: EvaluatorFunctionType.String,
        }),
    ];

    it('keeps every function outside a call', () => {
        expect(filterFunctionDefinitionsForArgument(definitions, [])).toBe(definitions);
    });

    it('keeps every function for an Any parameter', () => {
        expect(filterFunctionDefinitionsForArgument(definitions, [{argIndex: 0, name: 'getOrDefault'}])).toBe(
            definitions
        );
    });

    it('keeps every function inside an unknown call', () => {
        expect(filterFunctionDefinitionsForArgument(definitions, [{argIndex: 0, name: 'unknown'}])).toBe(definitions);
    });

    it('keeps the functions that fit the parameter of any overload', () => {
        const result = filterFunctionDefinitionsForArgument(definitions, [{argIndex: 1, name: 'getOrDefault'}]);

        expect(result.map((item) => item.name)).toEqual(['concat', 'size', 'lower', 'getOrDefault', 'getOrDefault']);
    });

    it('picks the overload whose return type fits the expression type', () => {
        const result = filterFunctionDefinitionsForArgument(
            definitions,
            [{argIndex: 1, name: 'getOrDefault'}],
            EvaluatorFunctionType.Integer
        );

        expect(result.map((item) => item.name)).toEqual(['size', 'getOrDefault']);
    });

    it('keeps every overload when none fits the expression type', () => {
        const result = filterFunctionDefinitionsForArgument(
            definitions,
            [{argIndex: 1, name: 'getOrDefault'}],
            EvaluatorFunctionType.Boolean
        );

        expect(result.map((item) => item.name)).toEqual(['concat', 'size', 'lower', 'getOrDefault', 'getOrDefault']);
    });

    it('narrows a nested call by the parameter of the call around it', () => {
        const result = filterFunctionDefinitionsForArgument(definitions, [
            {argIndex: 0, name: 'lower'},
            {argIndex: 1, name: 'getOrDefault'},
        ]);

        expect(result.map((item) => item.name)).toEqual(['concat', 'lower', 'getOrDefault']);
    });

    it('passes the expected type through a grouping paren', () => {
        const result = filterFunctionDefinitionsForArgument(definitions, [
            {argIndex: 0, name: 'lower'},
            {argIndex: 0, name: ''},
        ]);

        expect(result.map((item) => item.name)).toEqual(['concat', 'lower', 'getOrDefault']);
    });

    it('keeps every function past the last parameter', () => {
        expect(filterFunctionDefinitionsForArgument(definitions, [{argIndex: 2, name: 'getOrDefault'}])).toBe(
            definitions
        );
    });
});

describe('getPropertyFunctionType', () => {
    it('maps a property type to the function type its value must have', () => {
        expect(getPropertyFunctionType('INTEGER')).toBe(EvaluatorFunctionType.Integer);
        expect(getPropertyFunctionType('ARRAY')).toBe(EvaluatorFunctionType.List);
        expect(getPropertyFunctionType('OBJECT')).toBe(EvaluatorFunctionType.Map);
        expect(getPropertyFunctionType('DATE_TIME')).toBe(EvaluatorFunctionType.Datetime);
    });

    it('returns null for a type without a function type', () => {
        expect(getPropertyFunctionType('FILE_ENTRY')).toBeNull();
        expect(getPropertyFunctionType(undefined)).toBeNull();
    });
});

describe('findEnclosingFunctionCallChain', () => {
    it('lists the closed calls around the caret, outermost first', () => {
        expect(findEnclosingFunctionCallChain('getOrDefault(x, lower(', ')) + 1')).toEqual({
            calls: [
                {argIndex: 1, name: 'getOrDefault'},
                {argIndex: 0, name: 'lower'},
            ],
            isOutermostCallIncluded: true,
        });
    });

    it('leaves out an outer call that is not closed yet', () => {
        expect(findEnclosingFunctionCallChain('getOrDefault(x, lower(', ')')).toEqual({
            calls: [{argIndex: 0, name: 'lower'}],
            isOutermostCallIncluded: false,
        });
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

    it('returns null inside a string literal', () => {
        const $position = {nodeBefore: {isText: true, text: "=getOrDefault('at"}, pos: 19} as never;

        expect(findFunctionSuggestionMatch({$position})).toBeNull();
    });

    it('matches again after a closed string literal', () => {
        const $position = {nodeBefore: {isText: true, text: "=concat('a', conc"}, pos: 19} as never;

        expect(findFunctionSuggestionMatch({$position})?.query).toBe('conc');
    });

    it('covers the rest of the word after the caret', () => {
        const $position = {
            nodeAfter: {isText: true, text: 'cat(a)'},
            nodeBefore: {isText: true, text: 'con'},
            pos: 4,
        } as never;

        expect(findFunctionSuggestionMatch({$position})).toEqual({
            query: 'con',
            range: {from: 1, to: 7},
            text: 'concat',
        });
    });

    it('returns null when the word is the tail of a $ datapill token', () => {
        const $position = {nodeBefore: {isText: true, text: '$conc'}, pos: 7} as never;

        expect(findFunctionSuggestionMatch({$position})).toBeNull();
    });
});

describe('findForcedFunctionSuggestionStart', () => {
    it('starts at the caret when no word is being typed', () => {
        const $position = {nodeBefore: {isText: true, text: 'add('}, pos: 5} as never;

        expect(findForcedFunctionSuggestionStart({$position})).toBe(5);
    });

    it('starts at the beginning of the word being typed', () => {
        const $position = {nodeBefore: {isText: true, text: 'add(c'}, pos: 6} as never;

        expect(findForcedFunctionSuggestionStart({$position})).toBe(5);
    });

    it('starts at the caret in an empty editor', () => {
        const $position = {nodeBefore: null, pos: 1} as never;

        expect(findForcedFunctionSuggestionStart({$position})).toBe(1);
    });

    it('returns null after a $ datapill token', () => {
        const $position = {nodeBefore: {isText: true, text: '$co'}, pos: 4} as never;

        expect(findForcedFunctionSuggestionStart({$position})).toBeNull();
    });
});

describe('findForcedFunctionSuggestionMatch', () => {
    it('matches an empty query at the forced start', () => {
        const $position = {nodeBefore: null, pos: 1} as never;

        expect(findForcedFunctionSuggestionMatch({$position}, 1)).toEqual({
            query: '',
            range: {from: 1, to: 1},
            text: '',
        });
    });

    it('matches a single typed character after the forced start', () => {
        const $position = {nodeBefore: {isText: true, text: 'add(c'}, pos: 6} as never;

        expect(findForcedFunctionSuggestionMatch({$position}, 5)).toEqual({
            query: 'c',
            range: {from: 5, to: 6},
            text: 'c',
        });
    });

    it('returns null once the text after the forced start is no longer a word', () => {
        const $position = {nodeBefore: {isText: true, text: 'add(c,'}, pos: 7} as never;

        expect(findForcedFunctionSuggestionMatch({$position}, 5)).toBeNull();
    });

    it('covers the rest of the word after the caret', () => {
        const $position = {
            nodeAfter: {isText: true, text: 'Default(x)'},
            nodeBefore: {isText: true, text: 'getOr'},
            pos: 6,
        } as never;

        expect(findForcedFunctionSuggestionMatch({$position}, 1)).toEqual({
            query: 'getOr',
            range: {from: 1, to: 13},
            text: 'getOrDefault',
        });
    });

    it('does not cover a value that starts right after the caret', () => {
        const $position = {
            nodeAfter: {isText: true, text: '0)'},
            nodeBefore: {isText: true, text: 'add(x, '},
            pos: 8,
        } as never;

        expect(findForcedFunctionSuggestionMatch({$position}, 8)).toEqual({
            query: '',
            range: {from: 8, to: 8},
            text: '',
        });
    });

    it('returns null when the caret moved before the forced start', () => {
        const $position = {nodeBefore: {isText: true, text: 'ad'}, pos: 3} as never;

        expect(findForcedFunctionSuggestionMatch({$position}, 5)).toBeNull();
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

describe('groupFunctionOverloads', () => {
    it('collapses definitions sharing a name in first-appearance order', () => {
        const first = {name: 'getOrDefault'} as EvaluatorFunctionDefinition;
        const other = {name: 'concat'} as EvaluatorFunctionDefinition;
        const second = {name: 'getOrDefault'} as EvaluatorFunctionDefinition;

        expect(groupFunctionOverloads([first, other, second])).toEqual([
            {name: 'getOrDefault', overloads: [first, second]},
            {name: 'concat', overloads: [other]},
        ]);
    });
});
