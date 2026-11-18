import {describe, expect, it} from 'vitest';

import {getCursorLexicalState} from './cursorLexicalState';

import type {ScriptLanguageType} from './completionTypes';

const kindAt = (source: string, language: ScriptLanguageType) =>
    getCursorLexicalState(source, source.length, language).kind;

describe('getCursorLexicalState', () => {
    it.each<[string, ScriptLanguageType, string]>([
        ['x = 1; // context.', 'javascript', 'comment'],
        ['/* context.', 'javascript', 'comment'],
        ['/* a */ context.', 'javascript', 'code'],
        ["const a = 'context.", 'javascript', 'string'],
        ['const a = `${input.', 'javascript', 'code'],
        ['const a = `${f({a: 1})} input.', 'javascript', 'string'],
        ['const a = "x"\ncontext.', 'javascript', 'code'],
        ['# context.', 'python', 'comment'],
        ['"""doc\ncontext.', 'python', 'string'],
        ['"""doc"""\ncontext.', 'python', 'code'],
        ['f"{input.', 'python', 'code'],
        ['f"{{input.', 'python', 'string'],
        ['"#{input.', 'python', 'string'],
        ['"#{input.', 'ruby', 'code'],
        ["'#{input.", 'ruby', 'string'],
        ['x = 1 # context.', 'ruby', 'comment'],
        ['=begin\ncontext.', 'ruby', 'comment'],
        ['=begin\nx\n=end\ncontext.', 'ruby', 'code'],
        ['a == begin\ncontext.', 'ruby', 'code'],
    ])('reads %j in %s as %s', (source, language, kind) => {
        expect(kindAt(source, language)).toBe(kind);
    });

    it('returns the start of the open string', () => {
        expect(getCursorLexicalState("input['cu", 9, 'python')).toEqual({kind: 'string', start: 6});
    });
});
