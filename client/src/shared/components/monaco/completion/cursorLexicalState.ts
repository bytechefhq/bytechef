import type {ScriptLanguageType} from './completionTypes';

export type CursorLexicalStateType = {kind: 'code'} | {kind: 'comment'} | {kind: 'string'; start: number};

type CodeModeType = {braceDepth: number; interpolated: boolean; kind: 'code'};

type StringModeType = {
    interpolation?: '#{' | '${' | '{';
    kind: 'string';
    quote: string;
    start: number;
};

type ModeType = CodeModeType | StringModeType;

const PYTHON_STRING_PREFIX = /[rRbBfFuU]{1,2}$/;
const RUBY_BLOCK_COMMENT_END = /\n=end\b/;
const RUBY_BLOCK_COMMENT_START = /^=begin\b/;

function findCommentEnd(text: string, position: number, language: ScriptLanguageType): number | undefined {
    if (language === 'javascript') {
        if (text.startsWith('//', position)) {
            return text.indexOf('\n', position);
        }

        if (text.startsWith('/*', position)) {
            const closeIndex = text.indexOf('*/', position + 2);

            return closeIndex === -1 ? -1 : closeIndex + 2;
        }

        return undefined;
    }

    if (text[position] === '#') {
        return text.indexOf('\n', position);
    }

    if (
        language === 'ruby' &&
        (position === 0 || text[position - 1] === '\n') &&
        RUBY_BLOCK_COMMENT_START.test(text.slice(position, position + 7))
    ) {
        const closeMatch = RUBY_BLOCK_COMMENT_END.exec(text.slice(position));

        return closeMatch ? position + closeMatch.index + closeMatch[0].length : -1;
    }

    return undefined;
}

function openString(text: string, position: number, language: ScriptLanguageType): StringModeType | undefined {
    const character = text[position];

    if (character !== '"' && character !== "'" && !(character === '`' && language === 'javascript')) {
        return undefined;
    }

    if (language === 'python') {
        const prefix = PYTHON_STRING_PREFIX.exec(text.slice(Math.max(0, position - 2), position))?.[0] ?? '';
        const triple = text.startsWith(character.repeat(3), position);

        return {
            interpolation: /f/i.test(prefix) ? '{' : undefined,
            kind: 'string',
            quote: triple ? character.repeat(3) : character,
            start: position,
        };
    }

    if (language === 'javascript' && character === '`') {
        return {interpolation: '${', kind: 'string', quote: character, start: position};
    }

    return {
        interpolation: language === 'ruby' && character === '"' ? '#{' : undefined,
        kind: 'string',
        quote: character,
        start: position,
    };
}

export function getCursorLexicalState(
    source: string,
    cursorOffset: number,
    language: ScriptLanguageType
): CursorLexicalStateType {
    const text = source.slice(0, cursorOffset);
    const modes: ModeType[] = [{braceDepth: 0, interpolated: false, kind: 'code'}];

    let position = 0;

    while (position < text.length) {
        const mode = modes[modes.length - 1];
        const character = text[position];

        if (mode.kind === 'string') {
            if (character === '\\') {
                position += 2;
            } else if (text.startsWith(mode.quote, position)) {
                modes.pop();

                position += mode.quote.length;
            } else if (character === '\n' && mode.quote.length === 1 && mode.quote !== '`') {
                modes.pop();

                position++;
            } else if (mode.interpolation === '{' && text.startsWith('{{', position)) {
                position += 2;
            } else if (mode.interpolation && text.startsWith(mode.interpolation, position)) {
                modes.push({braceDepth: 0, interpolated: true, kind: 'code'});

                position += mode.interpolation.length;
            } else {
                position++;
            }

            continue;
        }

        const commentEnd = findCommentEnd(text, position, language);

        if (commentEnd !== undefined) {
            if (commentEnd === -1) {
                return {kind: 'comment'};
            }

            position = commentEnd;

            continue;
        }

        const stringMode = openString(text, position, language);

        if (stringMode) {
            modes.push(stringMode);

            position += stringMode.quote.length;

            continue;
        }

        if (character === '{') {
            mode.braceDepth++;
        } else if (character === '}') {
            if (mode.braceDepth === 0 && mode.interpolated) {
                modes.pop();
            } else {
                mode.braceDepth = Math.max(0, mode.braceDepth - 1);
            }
        }

        position++;
    }

    const mode = modes[modes.length - 1];

    return mode.kind === 'string' ? {kind: 'string', start: mode.start} : {kind: 'code'};
}
