import {EvaluatorFunctionDefinition, EvaluatorFunctionType} from '@/shared/middleware/graphql';
import {Editor} from '@tiptap/react';

const MAX_RESULTS = 50;
const MINIMUM_QUERY_LENGTH = 2;
const TRAILING_WORD = /[A-Za-z][A-Za-z0-9]*$/;

// SCREAMING_SNAKE enum value (e.g. "STRING", "INTEGER") -> readable label (e.g. "String", "Integer").
function formatFunctionType(type: EvaluatorFunctionType): string {
    return type.charAt(0) + type.slice(1).toLowerCase();
}

export function formatFunctionSignature(definition: EvaluatorFunctionDefinition): string {
    const parameters = definition.parameters
        .map((parameter) => `${parameter.name}: ${formatFunctionType(parameter.type)}`)
        .join(', ');

    return `(${parameters}): ${formatFunctionType(definition.returnType)}`;
}

export function filterFunctionDefinitions(
    definitions: EvaluatorFunctionDefinition[],
    query: string
): EvaluatorFunctionDefinition[] {
    const lowercaseQuery = query.toLowerCase();

    return definitions
        .filter(
            (definition) =>
                definition.name.toLowerCase().startsWith(lowercaseQuery) ||
                definition.title.toLowerCase().includes(lowercaseQuery)
        )
        .slice(0, MAX_RESULTS);
}

export function buildFunctionInsertion(name: string): {caretOffset: number; content: string} {
    return {caretOffset: name.length + 1, content: `${name}()`};
}

interface SuggestionMatchResultI {
    query: string;
    range: {from: number; to: number};
    text: string;
}

// Custom findSuggestionMatch: trigger on the trailing word before the caret (no explicit trigger char).
// nodeBefore is the contiguous text node ending at the caret, so its trailing-word length maps 1:1 to
// document positions even when a datapill node precedes it on the same line.
export function findFunctionSuggestionMatch({
    $position,
}: {
    $position: {nodeBefore: {isText: boolean; text?: string | null} | null; pos: number};
}): SuggestionMatchResultI | null {
    const nodeBefore = $position.nodeBefore;

    if (!nodeBefore || !nodeBefore.isText || !nodeBefore.text) {
        return null;
    }

    const match = nodeBefore.text.match(TRAILING_WORD);

    if (!match || match[0].length < MINIMUM_QUERY_LENGTH) {
        return null;
    }

    const word = match[0];

    // Don't compete with the `$` datapill suggestion: if the matched word is the tail of a `$`-prefixed
    // token (e.g. "$conc"), let the datapill source own it so both popups don't open at once.
    if (nodeBefore.text[nodeBefore.text.length - word.length - 1] === '$') {
        return null;
    }

    const to = $position.pos;
    const from = to - word.length;

    return {query: word, range: {from, to}, text: word};
}

export function isFormulaModeActive(
    editor: Pick<Editor, 'storage'> & Partial<Pick<Editor, 'extensionManager'>>
): boolean {
    // Prefer the FormulaMode extension's live getter over the storage flag. The storage flag is seeded
    // once at editor creation and synced via a React effect, so it lags the actual formula-mode state
    // after a page reload (the saved "=" value loads asynchronously). The getter reads the ref that
    // also drives the f(x) icon, so it always reflects the current state — without it the function
    // suggestion never activates on a reloaded expression until formula mode is toggled off and on.
    const formulaModeExtension = editor.extensionManager?.extensions?.find(
        (extension) => extension.name === 'FormulaMode'
    );
    const getIsFormulaMode = formulaModeExtension?.options?.getIsFormulaMode as (() => boolean) | undefined;

    if (typeof getIsFormulaMode === 'function') {
        return getIsFormulaMode() === true;
    }

    return editor.storage?.FormulaMode?.isFormulaMode === true;
}

export interface EnclosingFunctionCallI {
    argIndex: number;
    name: string;
}

const IDENTIFIER_CHARACTER = /[A-Za-z0-9_]/;

// Counts closing parens in `text` that are not matched by an opening paren within `text` — i.e. the
// parens that close scopes opened before this text. Used to tell whether the caret is still enclosed
// by a call whose ")" comes later, or has already moved past it.
function countUnmatchedClosingParens(text: string): number {
    let depth = 0;
    let unmatched = 0;
    let quote: string | null = null;

    for (const character of text) {
        if (quote) {
            if (character === quote) {
                quote = null;
            }

            continue;
        }

        if (character === '"' || character === "'") {
            quote = character;
        } else if (character === '(') {
            depth += 1;
        } else if (character === ')') {
            if (depth === 0) {
                unmatched += 1;
            } else {
                depth -= 1;
            }
        }
    }

    return unmatched;
}

export function findEnclosingFunctionCall(textBeforeCaret: string, textAfterCaret = ''): EnclosingFunctionCallI | null {
    const stack: EnclosingFunctionCallI[] = [];

    let word = '';
    let quote: string | null = null;

    for (const character of textBeforeCaret) {
        if (quote) {
            if (character === quote) {
                quote = null;
            }

            continue;
        }

        if (character === '"' || character === "'") {
            quote = character;
            word = '';
        } else if (IDENTIFIER_CHARACTER.test(character)) {
            word += character;
        } else if (character === '(') {
            stack.push({argIndex: 0, name: word});
            word = '';
        } else if (character === ')') {
            stack.pop();
            word = '';
        } else if (character === ',') {
            if (stack.length > 0) {
                stack[stack.length - 1].argIndex += 1;
            }

            word = '';
        } else {
            word = '';
        }
    }

    const unmatchedClosesAfterCaret = countUnmatchedClosingParens(textAfterCaret);

    for (let index = stack.length - 1; index >= 0; index--) {
        if (stack[index].name) {
            // The enclosing call only counts when its own ")" sits at or after the caret — there must
            // be enough unmatched ")" following the caret to pop the stack back down to this call.
            // Otherwise the caret has moved past the call's end (or it was never closed), so the
            // signature tooltip stays hidden. Outer calls need even more closes, so once the innermost
            // named call fails this check none can pass.
            if (unmatchedClosesAfterCaret < stack.length - index) {
                return null;
            }

            return {argIndex: stack[index].argIndex, name: stack[index].name};
        }
    }

    return null;
}

export interface FunctionSignaturePartsI {
    params: string[];
    returnType: string;
}

export function formatFunctionSignatureParts(definition: EvaluatorFunctionDefinition): FunctionSignaturePartsI {
    return {
        params: definition.parameters.map((parameter) => `${parameter.name}: ${formatFunctionType(parameter.type)}`),
        returnType: formatFunctionType(definition.returnType),
    };
}
