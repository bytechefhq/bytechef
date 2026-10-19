import type {ScriptLanguageType} from './completionTypes';

export type ActionCallKeySlotType = {
    calleeText: string;
    existingKeys: string[];
    prefix: string;
    propertyPath: string[];
    rootKeys: string[];
    rootValues: Record<string, unknown>;
};

export type ActionCallValueSlotType = {
    calleeText: string;
    prefix: string;
    propertyPath: string[];
    quote?: string;
    rootKeys: string[];
    rootValues: Record<string, unknown>;
};

type ScanResultType = {
    frames: FrameType[];
    openString?: {start: number; value: string};
    text: string;
};

type FirstArgumentLocationType = {
    calleeText: string;
    propertyPath: string[];
    rootFrame: FrameType;
};

type LiteralType = {value: unknown};

type FrameType = {
    arrayItems: unknown[];
    calleeText?: string;
    character: '(' | '[' | '{';
    elementIndex: number;
    firstArgumentOfCall: boolean;
    keyForValue?: string;
    keys: string[];
    literalValue?: LiteralType;
    literals: Record<string, unknown>;
    segmentKey?: string;
    segmentStart: number;
    tokenCount: number;
    tokenValue?: string;
    valueStart?: number;
};

const IDENTIFIER_START = /[A-Za-z_$]/;
const IDENTIFIER_PART = /[\w$]/;
const KEY_PREFIX = /^\s*([A-Za-z_$][\w$]*)?$/;
const VALUE_PREFIX = /^\s*([\w.-]*)$/;
const TRAILING_MEMBER_CHAIN = /[\w$.[\]'"]+$/;
const NUMBER_LITERAL = /^-?\d+(\.\d+)?/;

const LITERAL_IDENTIFIERS: Record<ScriptLanguageType, Record<string, unknown>> = {
    javascript: {false: false, null: null, true: true},
    python: {False: false, None: null, True: true},
    ruby: {false: false, nil: null, true: true},
};

function readString(text: string, start: number): {end: number; terminated: boolean; value: string} {
    const quote = text[start];

    let cursor = start + 1;

    while (cursor < text.length) {
        const character = text[cursor];

        if (character === '\\') {
            cursor += 2;

            continue;
        }

        if (character === quote) {
            return {end: cursor + 1, terminated: true, value: text.slice(start + 1, cursor)};
        }

        if (character === '\n' && quote !== '`') {
            break;
        }

        cursor++;
    }

    return {end: Math.min(cursor, text.length), terminated: false, value: text.slice(start + 1, cursor)};
}

function skipComment(text: string, start: number, language: ScriptLanguageType): number | undefined {
    if (language === 'javascript') {
        if (text.startsWith('//', start)) {
            const lineEnd = text.indexOf('\n', start);

            return lineEnd === -1 ? text.length : lineEnd;
        }

        if (text.startsWith('/*', start)) {
            const closeIndex = text.indexOf('*/', start + 2);

            return closeIndex === -1 ? text.length : closeIndex + 2;
        }

        return undefined;
    }

    if (text[start] === '#') {
        const lineEnd = text.indexOf('\n', start);

        return lineEnd === -1 ? text.length : lineEnd;
    }

    return undefined;
}

function previousSignificantCharacter(text: string, index: number): string {
    let cursor = index - 1;

    while (cursor >= 0 && /\s/.test(text[cursor])) {
        cursor--;
    }

    return cursor >= 0 ? text[cursor] : '';
}

function recordToken(frame: FrameType | undefined, value?: string, literal?: LiteralType): void {
    if (frame?.character === '{') {
        frame.tokenCount++;
        frame.tokenValue = value;
        frame.literalValue = frame.tokenCount === 3 ? literal : undefined;
    }
}

function recordKey(frame: FrameType): void {
    if (frame.tokenCount === 1 && frame.tokenValue !== undefined) {
        frame.segmentKey = frame.tokenValue;

        if (!frame.keys.includes(frame.tokenValue)) {
            frame.keys.push(frame.tokenValue);
        }
    }

    frame.tokenCount = 2;
}

function closeSegment(frame: FrameType): void {
    if (frame.segmentKey !== undefined && frame.tokenCount === 3 && frame.literalValue) {
        frame.literals[frame.segmentKey] = frame.literalValue.value;
    }

    frame.literalValue = undefined;
    frame.segmentKey = undefined;
    frame.tokenCount = 0;
    frame.tokenValue = undefined;
    frame.valueStart = undefined;
}

function attachClosedValue(parent: FrameType | undefined, value: unknown): void {
    if (parent?.character === '{' && parent.segmentKey !== undefined && parent.tokenCount === 3) {
        parent.literalValue = {value};
    } else if (parent?.character === '[' && typeof value === 'object') {
        parent.arrayItems[parent.elementIndex] = value;
    }
}

function nestedRootValues(frames: FrameType[], rootFrame: FrameType): Record<string, unknown> {
    const rootValues: Record<string, unknown> = {...rootFrame.literals};

    let container: Record<string | number, unknown> = rootValues;

    for (let index = frames.indexOf(rootFrame) + 1; index < frames.length; index++) {
        const frame = frames[index];

        if (frame.keyForValue === undefined || frame.character === '(') {
            break;
        }

        const value: Record<string | number, unknown> =
            frame.character === '{'
                ? {...frame.literals}
                : ([...frame.arrayItems] as unknown as Record<number, unknown>);

        container[frames[index - 1].character === '[' ? Number(frame.keyForValue) : frame.keyForValue] = value;
        container = value;
    }

    return rootValues;
}

function isInterpolated(value: string, quote: string, language: ScriptLanguageType): boolean {
    if (language === 'javascript') {
        return quote === '`' && value.includes('${');
    }

    return language === 'ruby' && quote === '"' && value.includes('#{');
}

function scanActionCall(source: string, cursorOffset: number, language: ScriptLanguageType): ScanResultType {
    const text = source.slice(0, cursorOffset);
    const frames: FrameType[] = [];

    let openString: {start: number; value: string} | undefined;
    let position = 0;

    while (position < text.length) {
        const character = text[position];
        const top = frames[frames.length - 1];
        const commentEnd = skipComment(text, position, language);

        if (commentEnd !== undefined) {
            position = commentEnd;

            continue;
        }

        if (character === '"' || character === "'" || character === '`') {
            const string = readString(text, position);

            if (!string.terminated && string.end >= text.length) {
                openString = {start: position, value: string.value};

                break;
            }

            recordToken(
                top,
                string.value,
                isInterpolated(string.value, character, language) ? undefined : {value: string.value}
            );

            position = string.end;

            continue;
        }

        if (IDENTIFIER_START.test(character)) {
            let end = position + 1;

            while (end < text.length && IDENTIFIER_PART.test(text[end])) {
                end++;
            }

            const identifier = text.slice(position, end);
            const literalIdentifiers = LITERAL_IDENTIFIERS[language];

            recordToken(
                top,
                identifier,
                Object.hasOwn(literalIdentifiers, identifier) ? {value: literalIdentifiers[identifier]} : undefined
            );

            position = end;

            continue;
        }

        const numberMatch =
            /[\d-]/.test(character) && !IDENTIFIER_PART.test(text[position - 1] ?? '')
                ? NUMBER_LITERAL.exec(text.slice(position))
                : null;

        if (numberMatch) {
            recordToken(top, undefined, {value: Number(numberMatch[0])});

            position += numberMatch[0].length;

            continue;
        }

        if (character === '(' || character === '[' || character === '{') {
            const previous = previousSignificantCharacter(text, position);
            const keyForValue =
                top?.character === '{' && top.tokenCount === 2 && (previous === ':' || previous === '>')
                    ? top.keys[top.keys.length - 1]
                    : undefined;

            recordToken(top);

            frames.push({
                arrayItems: [],
                calleeText:
                    character === '('
                        ? (TRAILING_MEMBER_CHAIN.exec(text.slice(0, position).trimEnd())?.[0] ?? '')
                        : undefined,
                character,
                elementIndex: 0,
                firstArgumentOfCall:
                    character === '{' && top?.character === '(' && previous === '(' && top.tokenCount === 0,
                keyForValue: keyForValue ?? (top?.character === '[' ? String(top.elementIndex) : undefined),
                keys: [],
                literals: {},
                segmentStart: position + 1,
                tokenCount: 0,
            });

            position++;

            continue;
        }

        if (character === ')' || character === ']' || character === '}') {
            const closedFrame = frames.pop();

            if (closedFrame?.character === '{') {
                closeSegment(closedFrame);
            }

            if (closedFrame && closedFrame.character !== '(') {
                attachClosedValue(
                    frames[frames.length - 1],
                    closedFrame.character === '{' ? closedFrame.literals : closedFrame.arrayItems
                );
            }

            position++;

            continue;
        }

        if (top?.character === '{' && character === ',') {
            closeSegment(top);

            top.segmentStart = position + 1;

            position++;

            continue;
        }

        if (top?.character === '[' && character === ',') {
            top.elementIndex++;

            position++;

            continue;
        }

        if (top?.character === '(' && character === ',') {
            top.tokenCount++;

            position++;

            continue;
        }

        if (top?.character === '{' && (character === ':' || (character === '=' && text[position + 1] === '>'))) {
            position += character === '=' ? 2 : 1;

            if (top.tokenCount === 1) {
                recordKey(top);

                top.valueStart = position;
            } else {
                recordToken(top);
            }

            continue;
        }

        if (!/\s/.test(character)) {
            recordToken(top);
        }

        position++;
    }

    return {frames, openString, text};
}

function locateFirstArgument(frames: FrameType[]): FirstArgumentLocationType | undefined {
    const propertyPath: string[] = [];

    let index = frames.length - 1;

    while (index > 0) {
        const frame = frames[index];

        if (frame.character === '{' && frame.firstArgumentOfCall) {
            return {calleeText: frames[index - 1].calleeText ?? '', propertyPath, rootFrame: frame};
        }

        if (frame.keyForValue === undefined) {
            return undefined;
        }

        propertyPath.unshift(frame.keyForValue);

        index--;
    }

    return undefined;
}

export function findActionCallKeySlot(
    source: string,
    cursorOffset: number,
    language: ScriptLanguageType
): ActionCallKeySlotType | undefined {
    const {frames, openString, text} = scanActionCall(source, cursorOffset, language);
    const objectFrame = frames[frames.length - 1];

    if (objectFrame?.character !== '{') {
        return undefined;
    }

    const segment = text.slice(objectFrame.segmentStart, openString ? openString.start : text.length);
    const prefixMatch = KEY_PREFIX.exec(segment);

    if (!prefixMatch || (openString && prefixMatch[1])) {
        return undefined;
    }

    const prefix = openString ? openString.value : (prefixMatch[1] ?? '');

    if (openString && !KEY_PREFIX.test(prefix)) {
        return undefined;
    }

    const location = locateFirstArgument(frames);

    if (!location) {
        return undefined;
    }

    return {
        calleeText: location.calleeText,
        existingKeys: objectFrame.keys,
        prefix,
        propertyPath: location.propertyPath,
        rootKeys: location.rootFrame.keys,
        rootValues: nestedRootValues(frames, location.rootFrame),
    };
}

export function findActionCallValueSlot(
    source: string,
    cursorOffset: number,
    language: ScriptLanguageType
): ActionCallValueSlotType | undefined {
    const {frames, openString, text} = scanActionCall(source, cursorOffset, language);
    const objectFrame = frames[frames.length - 1];

    if (
        objectFrame?.character !== '{' ||
        objectFrame.segmentKey === undefined ||
        objectFrame.valueStart === undefined
    ) {
        return undefined;
    }

    const valueText = text.slice(objectFrame.valueStart, openString ? openString.start : text.length);
    const prefixMatch = openString ? /^\s*$/.exec(valueText) : VALUE_PREFIX.exec(valueText);

    if (!prefixMatch) {
        return undefined;
    }

    const location = locateFirstArgument(frames);

    if (!location) {
        return undefined;
    }

    return {
        calleeText: location.calleeText,
        prefix: openString ? text.slice(openString.start + 1) : (prefixMatch[1] ?? ''),
        propertyPath: [...location.propertyPath, objectFrame.segmentKey],
        ...(openString && {quote: text[openString.start]}),
        rootKeys: location.rootFrame.keys,
        rootValues: nestedRootValues(frames, location.rootFrame),
    };
}
