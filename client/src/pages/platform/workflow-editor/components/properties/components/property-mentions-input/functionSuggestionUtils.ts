import {EvaluatorFunctionDefinition, EvaluatorFunctionType} from '@/shared/middleware/graphql';
import {Editor} from '@tiptap/react';

const MAX_RESULTS = 50;
const MINIMUM_QUERY_LENGTH = 2;
const ALPHABETIC_CHARACTER = /[A-Za-z]/;
const ALPHANUMERIC_CHARACTER = /[A-Za-z0-9]/;

const FUNCTION_TYPE_LABELS: Record<EvaluatorFunctionType, string> = {
    [EvaluatorFunctionType.Any]: 'Any',
    [EvaluatorFunctionType.Boolean]: 'Boolean',
    [EvaluatorFunctionType.Byte]: 'Integer',
    [EvaluatorFunctionType.Character]: 'String',
    [EvaluatorFunctionType.Datetime]: 'Date Time',
    [EvaluatorFunctionType.Double]: 'Number',
    [EvaluatorFunctionType.Float]: 'Number',
    [EvaluatorFunctionType.Integer]: 'Integer',
    [EvaluatorFunctionType.List]: 'Array',
    [EvaluatorFunctionType.Long]: 'Integer',
    [EvaluatorFunctionType.Map]: 'Object',
    [EvaluatorFunctionType.Number]: 'Number',
    [EvaluatorFunctionType.Short]: 'Integer',
    [EvaluatorFunctionType.String]: 'String',
};

function formatFunctionType(type: EvaluatorFunctionType): string {
    return FUNCTION_TYPE_LABELS[type] ?? type;
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
    if (query === '') {
        return definitions;
    }

    const lowercaseQuery = query.toLowerCase();

    return definitions
        .filter(
            (definition) =>
                definition.name.toLowerCase().startsWith(lowercaseQuery) ||
                definition.title.toLowerCase().includes(lowercaseQuery)
        )
        .slice(0, MAX_RESULTS);
}

export function buildFunctionInsertion(name: string, textAfterRange = ''): {caretOffset: number; content: string} {
    if (textAfterRange.startsWith('(')) {
        return {caretOffset: name.length + 1, content: name};
    }

    return {caretOffset: name.length + 1, content: `${name}()`};
}

const NUMERIC_FUNCTION_TYPES: string[] = [
    EvaluatorFunctionType.Byte,
    EvaluatorFunctionType.Double,
    EvaluatorFunctionType.Float,
    EvaluatorFunctionType.Integer,
    EvaluatorFunctionType.Long,
    EvaluatorFunctionType.Number,
    EvaluatorFunctionType.Short,
];

const TEXT_FUNCTION_TYPES: string[] = [EvaluatorFunctionType.Character, EvaluatorFunctionType.String];

function getFunctionTypeFamily(type: EvaluatorFunctionType): string {
    if (NUMERIC_FUNCTION_TYPES.includes(type)) {
        return EvaluatorFunctionType.Number;
    }

    if (TEXT_FUNCTION_TYPES.includes(type)) {
        return EvaluatorFunctionType.String;
    }

    return type;
}

export function isFunctionTypeCompatible(expectedType: EvaluatorFunctionType, actualType: EvaluatorFunctionType) {
    if (expectedType === EvaluatorFunctionType.Any || actualType === EvaluatorFunctionType.Any) {
        return true;
    }

    return getFunctionTypeFamily(expectedType) === getFunctionTypeFamily(actualType);
}

const PROPERTY_FUNCTION_TYPES: Record<string, EvaluatorFunctionType> = {
    ARRAY: EvaluatorFunctionType.List,
    BOOLEAN: EvaluatorFunctionType.Boolean,
    DATE: EvaluatorFunctionType.Datetime,
    DATE_TIME: EvaluatorFunctionType.Datetime,
    INTEGER: EvaluatorFunctionType.Integer,
    NUMBER: EvaluatorFunctionType.Number,
    OBJECT: EvaluatorFunctionType.Map,
    STRING: EvaluatorFunctionType.String,
    TIME: EvaluatorFunctionType.Datetime,
};

export function getPropertyFunctionType(propertyType?: string): EvaluatorFunctionType | null {
    return (propertyType && PROPERTY_FUNCTION_TYPES[propertyType]) || null;
}

function findExpectedArgumentTypes(
    definitions: EvaluatorFunctionDefinition[],
    enclosingCalls: EnclosingFunctionCallI[],
    expressionType: EvaluatorFunctionType | null
): EvaluatorFunctionType[] | null {
    let expectedTypes: EvaluatorFunctionType[] | null = expressionType ? [expressionType] : null;

    for (const enclosingCall of enclosingCalls) {
        if (!enclosingCall.name) {
            continue;
        }

        const overloads = definitions.filter(
            (definition) =>
                definition.name === enclosingCall.name && definition.parameters.length > enclosingCall.argIndex
        );

        if (overloads.length === 0) {
            expectedTypes = null;

            continue;
        }

        const currentExpectedTypes = expectedTypes;

        const fittingOverloads = currentExpectedTypes
            ? overloads.filter((overload) =>
                  currentExpectedTypes.some((expectedType) =>
                      isFunctionTypeCompatible(expectedType, overload.returnType)
                  )
              )
            : overloads;

        const parameterTypes = (fittingOverloads.length > 0 ? fittingOverloads : overloads).map(
            (overload) => overload.parameters[enclosingCall.argIndex].type
        );

        expectedTypes = parameterTypes.includes(EvaluatorFunctionType.Any) ? null : parameterTypes;
    }

    return expectedTypes;
}

export function filterFunctionDefinitionsForArgument(
    definitions: EvaluatorFunctionDefinition[],
    enclosingCalls: EnclosingFunctionCallI[],
    expressionType: EvaluatorFunctionType | null = null
): EvaluatorFunctionDefinition[] {
    if (!enclosingCalls.some((enclosingCall) => enclosingCall.name)) {
        return definitions;
    }

    const expectedTypes = findExpectedArgumentTypes(definitions, enclosingCalls, expressionType);

    if (!expectedTypes) {
        return definitions;
    }

    return definitions.filter((definition) =>
        expectedTypes.some((expectedType) => isFunctionTypeCompatible(expectedType, definition.returnType))
    );
}

interface SuggestionMatchResultI {
    query: string;
    range: {from: number; to: number};
    text: string;
}

interface SuggestionTextNodeI {
    isText: boolean;
    text?: string | null;
}

interface SuggestionPositionI {
    $position: {nodeAfter?: SuggestionTextNodeI | null; nodeBefore: SuggestionTextNodeI | null; pos: number};
}

const FORCED_QUERY = /^([A-Za-z][A-Za-z0-9]*)?$/;

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

function findLeadingWordRest({$position}: SuggestionPositionI): string {
    const nodeAfter = $position.nodeAfter;
    const text = nodeAfter?.isText && nodeAfter.text ? nodeAfter.text : '';

    let end = 0;

    while (end < text.length && ALPHANUMERIC_CHARACTER.test(text[end])) {
        end += 1;
    }

    return text.slice(0, end);
}

// Custom findSuggestionMatch: trigger on the trailing word before the caret (no explicit trigger char).
// nodeBefore is the contiguous text node ending at the caret, so its trailing-word length maps 1:1 to
// document positions even when a datapill node precedes it on the same line.
export function findFunctionSuggestionMatch({$position}: SuggestionPositionI): SuggestionMatchResultI | null {
    const nodeBefore = $position.nodeBefore;

    if (!nodeBefore || !nodeBefore.isText || !nodeBefore.text) {
        return null;
    }

    const word = findTrailingWord(nodeBefore.text);

    if (word.length < MINIMUM_QUERY_LENGTH) {
        return null;
    }

    if (isInsideStringLiteral(nodeBefore.text)) {
        return null;
    }

    // Don't compete with the `$` datapill suggestion: if the matched word is the tail of a `$`-prefixed
    // token (e.g. "$conc"), let the datapill source own it so both popups don't open at once.
    if (nodeBefore.text[nodeBefore.text.length - word.length - 1] === '$') {
        return null;
    }

    const from = $position.pos - word.length;
    const wordRest = findLeadingWordRest({$position});

    return {query: word, range: {from, to: $position.pos + wordRest.length}, text: word + wordRest};
}

export function findForcedFunctionSuggestionStart({$position}: SuggestionPositionI): number | null {
    const nodeBefore = $position.nodeBefore;
    const text = nodeBefore?.isText && nodeBefore.text ? nodeBefore.text : '';
    const word = findTrailingWord(text);

    if (text[text.length - word.length - 1] === '$') {
        return null;
    }

    return $position.pos - word.length;
}

export function findForcedFunctionSuggestionMatch(
    {$position}: SuggestionPositionI,
    forcedFrom: number
): SuggestionMatchResultI | null {
    const caret = $position.pos;
    const nodeBefore = $position.nodeBefore;
    const text = nodeBefore?.isText && nodeBefore.text ? nodeBefore.text : '';
    const textStart = caret - text.length;

    if (forcedFrom > caret || forcedFrom < textStart) {
        return null;
    }

    const query = text.slice(forcedFrom - textStart);

    if (!FORCED_QUERY.test(query)) {
        return null;
    }

    const wordRest = query ? findLeadingWordRest({$position}) : '';

    return {query, range: {from: forcedFrom, to: caret + wordRest.length}, text: query + wordRest};
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

function isInsideStringLiteral(text: string): boolean {
    let quote: string | null = null;

    for (const character of text) {
        if (quote) {
            quote = character === quote ? null : quote;
        } else if (character === '"' || character === "'") {
            quote = character;
        }
    }

    return quote !== null;
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

export interface EnclosingFunctionCallChainI {
    calls: EnclosingFunctionCallI[];
    isOutermostCallIncluded: boolean;
}

export function findEnclosingFunctionCallChain(
    textBeforeCaret: string,
    textAfterCaret = ''
): EnclosingFunctionCallChainI {
    const stack = collectOpenFunctionCalls(textBeforeCaret);
    const unmatchedClosesAfterCaret = countUnmatchedClosingParens(textAfterCaret);
    const firstClosedIndex = Math.max(stack.length - unmatchedClosesAfterCaret, 0);

    return {calls: stack.slice(firstClosedIndex), isOutermostCallIncluded: firstClosedIndex === 0};
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

export interface FunctionOverloadGroupI {
    name: string;
    overloads: EvaluatorFunctionDefinition[];
}

// Collapses overloads (definitions sharing a name) into one entry, in order of each name's first appearance.
export function groupFunctionOverloads(definitions: EvaluatorFunctionDefinition[]): FunctionOverloadGroupI[] {
    const groupsByName = new Map<string, FunctionOverloadGroupI>();

    for (const definition of definitions) {
        const group = groupsByName.get(definition.name);

        if (group) {
            group.overloads.push(definition);
        } else {
            groupsByName.set(definition.name, {name: definition.name, overloads: [definition]});
        }
    }

    return [...groupsByName.values()];
}
