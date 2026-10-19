import {skipComment} from './sourceScanner';

import type {PerformScopeType, ScriptLanguageType} from './completionTypes';

type BodyKindType = 'braces' | 'expression' | 'indentation' | 'line' | 'ruby';

interface FunctionHeadI {
    bodyKind: BodyKindType;
    bodyStart: number;
    indent: number;
    name?: string;
    parameters: string[];
    start: number;
}

const IDENTIFIER = '[A-Za-z_$][\\w$]*';
const NON_FUNCTION_KEYWORDS = new Set(['catch', 'for', 'function', 'if', 'return', 'switch', 'while', 'with']);
const PRECEDING_NAME = /["']?([A-Za-z_$][\w$]*)["']?\s*(?:=>|:|=)\s*$/;

function parseParameters(rawParameters: string | undefined): string[] {
    if (!rawParameters || !rawParameters.trim()) {
        return [];
    }

    return rawParameters.split(',').map((parameter) => {
        const match = /^\s*(\*{0,2}|\.{3})([A-Za-z_$][\w$]*)/.exec(parameter);

        if (!match || match[1]) {
            return '';
        }

        return match[2];
    });
}

function lookupPrecedingName(source: string, start: number): string | undefined {
    const match = PRECEDING_NAME.exec(source.slice(Math.max(0, start - 80), start));

    return match?.[1];
}

function collectJavaScriptHeads(source: string): FunctionHeadI[] {
    const heads: FunctionHeadI[] = [];
    const functionPattern = new RegExp(`function\\s*(${IDENTIFIER})?\\s*\\(([^()]*)\\)\\s*\\{`, 'g');
    const shorthandPattern = new RegExp(`(${IDENTIFIER})\\s*\\(([^()]*)\\)\\s*\\{`, 'g');
    const arrowPattern = new RegExp(
        `(?:(${IDENTIFIER})\\s*[:=]\\s*)?(?:async\\s+)?(?:\\(([^()]*)\\)|(${IDENTIFIER}))\\s*=>\\s*(\\{)?`,
        'g'
    );

    for (const match of source.matchAll(functionPattern)) {
        heads.push({
            bodyKind: 'braces',
            bodyStart: match.index + match[0].length,
            indent: 0,
            name: match[1] || lookupPrecedingName(source, match.index),
            parameters: parseParameters(match[2]),
            start: match.index,
        });
    }

    for (const match of source.matchAll(shorthandPattern)) {
        if (NON_FUNCTION_KEYWORDS.has(match[1])) {
            continue;
        }

        const bodyStart = match.index + match[0].length;

        if (heads.some((head) => head.bodyStart === bodyStart)) {
            continue;
        }

        heads.push({
            bodyKind: 'braces',
            bodyStart,
            indent: 0,
            name: match[1],
            parameters: parseParameters(match[2]),
            start: match.index,
        });
    }

    for (const match of source.matchAll(arrowPattern)) {
        heads.push({
            bodyKind: match[4] ? 'braces' : 'expression',
            bodyStart: match.index + match[0].length,
            indent: 0,
            name: match[1],
            parameters: parseParameters(match[2] ?? match[3]),
            start: match.index,
        });
    }

    return heads;
}

function collectPythonHeads(source: string): FunctionHeadI[] {
    const heads: FunctionHeadI[] = [];
    const definitionPattern = new RegExp(`^([ \\t]*)def\\s+(${IDENTIFIER})\\s*\\(([^()]*)\\)[^:\\n]*:`, 'gm');
    const lambdaPattern = /\blambda\b\s*([^:\n]*):/g;

    for (const match of source.matchAll(definitionPattern)) {
        heads.push({
            bodyKind: 'indentation',
            bodyStart: match.index + match[0].length,
            indent: match[1].length,
            name: match[2],
            parameters: parseParameters(match[3]),
            start: match.index + match[1].length,
        });
    }

    for (const match of source.matchAll(lambdaPattern)) {
        heads.push({
            bodyKind: 'line',
            bodyStart: match.index + match[0].length,
            indent: 0,
            name: lookupPrecedingName(source, match.index),
            parameters: parseParameters(match[1]),
            start: match.index,
        });
    }

    return heads;
}

function collectRubyHeads(source: string): FunctionHeadI[] {
    const heads: FunctionHeadI[] = [];
    const definitionPattern = /\bdef\s+([\w.?!=]+)[ \t]*(?:\(([^()]*)\))?/g;
    const lambdaPattern = /\blambda\s*(?:\{|do\b)\s*(?:\|([^|]*)\|)?/g;
    const stabbyPattern = /->\s*(?:\(([^()]*)\)\s*)?(?:\{|do\b)/g;

    for (const match of source.matchAll(definitionPattern)) {
        heads.push({
            bodyKind: 'ruby',
            bodyStart: match.index + match[0].length,
            indent: 0,
            name: match[1],
            parameters: parseParameters(match[2]),
            start: match.index,
        });
    }

    for (const match of source.matchAll(lambdaPattern)) {
        heads.push({
            bodyKind: 'ruby',
            bodyStart: match.index + match[0].length,
            indent: 0,
            name: lookupPrecedingName(source, match.index),
            parameters: parseParameters(match[1]),
            start: match.index,
        });
    }

    for (const match of source.matchAll(stabbyPattern)) {
        heads.push({
            bodyKind: 'ruby',
            bodyStart: match.index + match[0].length,
            indent: 0,
            name: lookupPrecedingName(source, match.index),
            parameters: parseParameters(match[1]),
            start: match.index,
        });
    }

    return heads;
}

function skipString(source: string, index: number, end: number): number {
    const quote = source.charAt(index);

    let position = index + 1;

    while (position < end) {
        const character = source.charAt(position);

        if (character === '\\') {
            position += 2;

            continue;
        }

        if (character === quote || (character === '\n' && quote !== '`')) {
            break;
        }

        position++;
    }

    return position;
}

function collectOpenBraceBodyStarts(source: string, cursorOffset: number, language: ScriptLanguageType): Set<number> {
    const openBodyStarts: number[] = [];

    for (let index = 0; index < cursorOffset; index++) {
        const character = source.charAt(index);

        if (character === '"' || character === "'" || (character === '`' && language === 'javascript')) {
            index = skipString(source, index, cursorOffset);

            continue;
        }

        const commentEnd = skipComment(source, index, language);

        if (commentEnd !== undefined) {
            index = commentEnd - 1;

            continue;
        }

        if (character === '{') {
            openBodyStarts.push(index + 1);
        } else if (character === '}') {
            openBodyStarts.pop();
        }
    }

    return new Set(openBodyStarts);
}

function expressionBodyContains(source: string, bodyStart: number, cursorOffset: number): boolean {
    let depth = 0;

    for (let index = bodyStart; index < cursorOffset; index++) {
        const character = source.charAt(index);

        if (character === '\n') {
            return false;
        }

        if (character === '"' || character === "'" || character === '`') {
            index = skipString(source, index, cursorOffset);

            continue;
        }

        const commentEnd = skipComment(source, index, 'javascript');

        if (commentEnd !== undefined) {
            if (source.slice(index, Math.min(commentEnd, cursorOffset)).includes('\n')) {
                return false;
            }

            index = commentEnd - 1;

            continue;
        }

        if (character === '(' || character === '[' || character === '{') {
            depth++;
        } else if (character === ')' || character === ']' || character === '}') {
            depth--;

            if (depth < 0) {
                return false;
            }
        } else if ((character === ',' || character === ';') && depth === 0) {
            return false;
        }
    }

    return true;
}

function indentationBodyContains(source: string, head: FunctionHeadI, cursorOffset: number): boolean {
    const rest = source.slice(head.bodyStart, cursorOffset);
    const lines = rest.split('\n');

    for (let index = 1; index < lines.length; index++) {
        const line = lines[index];

        if (!line.trim() || line.trimStart().startsWith('#')) {
            continue;
        }

        const indent = line.length - line.trimStart().length;

        if (indent <= head.indent) {
            return false;
        }
    }

    return true;
}

function maskRubyCommentsAndStrings(source: string, bodyStart: number, cursorOffset: number): string {
    let body = '';
    let index = bodyStart;

    while (index < cursorOffset) {
        const character = source.charAt(index);
        const commentEnd = skipComment(source, index, 'ruby');
        const segmentEnd =
            commentEnd ??
            (character === '"' || character === "'" ? skipString(source, index, cursorOffset) + 1 : undefined);

        if (segmentEnd === undefined) {
            body += character;
            index++;

            continue;
        }

        const end = Math.min(segmentEnd, cursorOffset);

        body += source.slice(index, end).replace(/[^\n]/g, ' ');
        index = end;
    }

    return body;
}

function rubyBodyContains(source: string, bodyStart: number, cursorOffset: number): boolean {
    const body = maskRubyCommentsAndStrings(source, bodyStart, cursorOffset);
    const tokenPattern =
        /(^[ \t]*(?:if|unless|while|until)\b)|(=[ \t]*(?:if|unless|while|until)\b)|\b(do|def|class|module|begin|case)\b|(\bend\b)|([{}])/gm;

    let depth = 1;

    for (const match of body.matchAll(tokenPattern)) {
        if (match[4] || match[5] === '}') {
            depth--;

            if (depth === 0) {
                return false;
            }
        } else {
            depth++;
        }
    }

    return true;
}

function headContainsCursor(
    source: string,
    head: FunctionHeadI,
    cursorOffset: number,
    openBraceBodyStarts: () => Set<number>
) {
    if (head.bodyStart > cursorOffset) {
        return false;
    }

    switch (head.bodyKind) {
        case 'braces':
            return openBraceBodyStarts().has(head.bodyStart);
        case 'expression':
            return expressionBodyContains(source, head.bodyStart, cursorOffset);
        case 'indentation':
            return indentationBodyContains(source, head, cursorOffset);
        case 'line':
            return !source.slice(head.bodyStart, cursorOffset).includes('\n');
        default:
            return rubyBodyContains(source, head.bodyStart, cursorOffset);
    }
}

function collectHeads(source: string, language: ScriptLanguageType): FunctionHeadI[] {
    if (language === 'python') {
        return collectPythonHeads(source);
    }

    if (language === 'ruby') {
        return collectRubyHeads(source);
    }

    return collectJavaScriptHeads(source);
}

function findContainingHeads(source: string, cursorOffset: number, language: ScriptLanguageType): FunctionHeadI[] {
    let openBraceBodyStarts: Set<number> | undefined;

    const getOpenBraceBodyStarts = () => {
        openBraceBodyStarts ??= collectOpenBraceBodyStarts(source, cursorOffset, language);

        return openBraceBodyStarts;
    };

    return collectHeads(source, language)
        .filter((head) => head.start < cursorOffset)
        .sort((first, second) => second.start - first.start || second.bodyStart - first.bodyStart)
        .filter((head) => headContainsCursor(source, head, cursorOffset, getOpenBraceBodyStarts));
}

function findEnclosingHead(
    source: string,
    cursorOffset: number,
    language: ScriptLanguageType
): FunctionHeadI | undefined {
    return findContainingHeads(source, cursorOffset, language)[0];
}

function toPerformScope(head: FunctionHeadI): PerformScopeType {
    const scope: PerformScopeType = {performStart: head.start};
    const [first, second, third] = head.parameters.map((parameter) => parameter || undefined);
    const assignments: [keyof Omit<PerformScopeType, 'performStart'>, string | undefined][] =
        head.parameters.length >= 3
            ? [
                  ['inputName', first],
                  ['connectionName', second],
                  ['contextName', third],
              ]
            : head.parameters.length === 2
              ? [
                    ['inputName', first],
                    ['contextName', second],
                ]
              : [['contextName', first]];

    for (const [key, value] of assignments) {
        if (value) {
            scope[key] = value;
        }
    }

    return scope;
}

export function findPerformScope(
    source: string,
    cursorOffset: number,
    language: ScriptLanguageType,
    root?: string
): PerformScopeType | undefined {
    const heads = findContainingHeads(source, cursorOffset, language);
    const performHead = heads.at(-1);

    if (!performHead) {
        return undefined;
    }

    if (root === undefined) {
        return toPerformScope(performHead);
    }

    const bindingHead = heads.find((head) => head.parameters.includes(root));

    return bindingHead === performHead ? toPerformScope(performHead) : undefined;
}

export function findEnclosingFunctionName(
    source: string,
    cursorOffset: number,
    language: ScriptLanguageType
): string | undefined {
    return findEnclosingHead(source, cursorOffset, language)?.name;
}
