import {EvaluatorFunctionDefinition, EvaluatorFunctionType} from '@/shared/middleware/graphql';
import {Editor} from '@tiptap/react';

const MAX_RESULTS = 50;
const MINIMUM_QUERY_LENGTH = 2;
const ALPHABETIC_CHARACTER = /[A-Za-z]/;
const ALPHANUMERIC_CHARACTER = /[A-Za-z0-9]/;

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

// The trailing run of letters and digits at the end of `text`, starting at its first letter — the
// word the caret is typing. Scanned backwards by hand rather than with an end-anchored regex: an
// unanchored-at-the-start pattern like /[A-Za-z][A-Za-z0-9]*$/ retries from every position and
// degrades super-linearly on long text that ends in a non-word character.
function findTrailingWord(text: string): string {
    let start = text.length;

    while (start > 0 && ALPHANUMERIC_CHARACTER.test(text[start - 1])) {
        start -= 1;
    }

    while (start < text.length && !ALPHABETIC_CHARACTER.test(text[start])) {
        start += 1;
    }

    return text.slice(start);
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

    const word = findTrailingWord(nodeBefore.text);

    if (word.length < MINIMUM_QUERY_LENGTH) {
        return null;
    }

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

const IDENTIFIER_CHARACTER = /\w/;

// Blanks out string literals — the quotes and everything between them — so the scanners below can look
// for structural characters without each re-implementing quote tracking. Length is preserved, and a
// blank is not an identifier character, so a literal still terminates the word that precedes it.
function maskQuotedSegments(text: string): string {
    let masked = '';
    let quote: string | null = null;

    for (const character of text) {
        if (quote) {
            masked += ' ';
            quote = character === quote ? null : quote;
        } else if (character === '"' || character === "'") {
            masked += ' ';
            quote = character;
        } else {
            masked += character;
        }
    }

    return masked;
}

// Counts closing parens in `text` that are not matched by an opening paren within `text` — i.e. the
// parens that close scopes opened before this text. Used to tell whether the caret is still enclosed
// by a call whose ")" comes later, or has already moved past it.
function countUnmatchedClosingParens(text: string): number {
    let depth = 0;
    let unmatched = 0;

    for (const character of maskQuotedSegments(text)) {
        if (character === '(') {
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

// The calls still open at the end of `text`, outermost first — each with the argument index the text
// ends in. An unnamed entry is a plain grouping paren rather than a call.
function collectOpenFunctionCalls(text: string): EnclosingFunctionCallI[] {
    const stack: EnclosingFunctionCallI[] = [];

    let word = '';

    for (const character of maskQuotedSegments(text)) {
        if (IDENTIFIER_CHARACTER.test(character)) {
            word += character;

            continue;
        }

        if (character === '(') {
            stack.push({argIndex: 0, name: word});
        } else if (character === ')') {
            stack.pop();
        } else if (character === ',') {
            const innermostCall = stack.at(-1);

            if (innermostCall) {
                innermostCall.argIndex += 1;
            }
        }

        word = '';
    }

    return stack;
}

export function findEnclosingFunctionCall(textBeforeCaret: string, textAfterCaret = ''): EnclosingFunctionCallI | null {
    const stack = collectOpenFunctionCalls(textBeforeCaret);
    const unmatchedClosesAfterCaret = countUnmatchedClosingParens(textAfterCaret);

    for (let index = stack.length - 1; index >= 0; index--) {
        const call = stack[index];

        if (!call.name) {
            continue;
        }

        // The enclosing call only counts when its own ")" sits at or after the caret — there must
        // be enough unmatched ")" following the caret to pop the stack back down to this call.
        // Otherwise the caret has moved past the call's end (or it was never closed), so the
        // signature tooltip stays hidden. Outer calls need even more closes, so once the innermost
        // named call fails this check none can pass.
        if (unmatchedClosesAfterCaret < stack.length - index) {
            return null;
        }

        return {argIndex: call.argIndex, name: call.name};
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
