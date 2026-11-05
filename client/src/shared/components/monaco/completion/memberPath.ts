export type MemberPathType = {
    callSegments: boolean[];
    root: string;
    segments: string[];
};

interface ElementI {
    bracket: boolean;
    call: boolean;
    name: string;
    start: number;
}

const IDENTIFIER_CHARACTER = /[\w$]/;
const IDENTIFIER = /^[A-Za-z_$][\w$]*$/;
const BRACKET_ACCESS = /\[\s*(['"])((?:(?!\1).)*)\1\s*\]$/;

function readIdentifierBackwards(text: string, end: number): string {
    let start = end;

    while (start > 0 && IDENTIFIER_CHARACTER.test(text.charAt(start - 1))) {
        start--;
    }

    return text.slice(start, end);
}

function findOpeningParenthesis(text: string, closeIndex: number): number {
    let depth = 0;

    for (let index = closeIndex; index >= 0; index--) {
        const character = text.charAt(index);

        if (character === ')') {
            depth++;
        } else if (character === '(') {
            depth--;

            if (depth === 0) {
                return index;
            }
        }
    }

    return -1;
}

function readElementBackwards(text: string, end: number): ElementI | undefined {
    const lastCharacter = text.charAt(end - 1);

    if (lastCharacter === ']') {
        const match = BRACKET_ACCESS.exec(text.slice(0, end));

        if (!match) {
            return undefined;
        }

        return {bracket: true, call: false, name: match[2], start: match.index};
    }

    let identifierEnd = end;
    let call = false;

    if (lastCharacter === ')') {
        const openIndex = findOpeningParenthesis(text, end - 1);

        if (openIndex < 0) {
            return undefined;
        }

        identifierEnd = openIndex;
        call = true;
    }

    const name = readIdentifierBackwards(text, identifierEnd);

    if (!name) {
        return undefined;
    }

    return {bracket: false, call, name, start: identifierEnd - name.length};
}

export function parseMemberPath(textBeforeCursor: string): MemberPathType | undefined {
    const text = textBeforeCursor;
    const trailing = readIdentifierBackwards(text, text.length);
    const elements: ElementI[] = [{bracket: false, call: false, name: trailing, start: text.length - trailing.length}];

    let position = text.length - trailing.length;

    if (text.charAt(position - 1) === '.') {
        position--;
    } else if (!trailing) {
        return undefined;
    } else {
        return buildPath(elements);
    }

    for (;;) {
        const element = readElementBackwards(text, position);

        if (!element) {
            return undefined;
        }

        elements.unshift(element);

        position = element.start;

        if (element.bracket) {
            continue;
        }

        if (text.charAt(position - 1) === '.') {
            position--;

            continue;
        }

        break;
    }

    return buildPath(elements);
}

function buildPath(elements: ElementI[]): MemberPathType | undefined {
    const [rootElement, ...segmentElements] = elements;

    if (rootElement.bracket || !IDENTIFIER.test(rootElement.name)) {
        return undefined;
    }

    return {
        callSegments: segmentElements.map((element) => element.call),
        root: rootElement.name,
        segments: segmentElements.map((element) => element.name),
    };
}
