import {ConnectionDeclarationType} from './completionTypes';

export type WorkflowDocumentPositionType = {
    enclosingTaskConnections: ConnectionDeclarationType[];
    enclosingTaskIndex?: number;
    enclosingTaskInputKeys: string[];
    enclosingTaskType?: string;
    earlierNodeNames: string[];
    inString: boolean;
    keySlot?: KeySlotType;
    keysOf: (containerPath: string[]) => string[];
    literalValuesOf: (containerPath: string[]) => Record<string, unknown>;
    nameOf: (nodePath: string[]) => string | undefined;
    path: string[];
    stringValueBeforeCursor?: string;
    typeOf: (nodePath: string[]) => string | undefined;
};

export type KeySlotType = {
    containerPath: string[];
    existingKeys: string[];
    prefix: string;
};

type ScannedKeySlotType = {
    containerPath: string[];
    name?: string;
    prefix: string;
};

type ScalarType = {
    before: (cursorOffset: number) => string;
    end: number;
    isString: boolean;
    path: string[];
    start: number;
    value: string;
};

type CommentType = {
    end: number;
    start: number;
};

type ScannedDocumentType = {
    comments: CommentType[];
    keySlotAt: (cursorOffset: number) => ScannedKeySlotType | undefined;
    keys: string[][];
    pathAt: (cursorOffset: number) => string[];
    scalars: ScalarType[];
};

type YamlContextType = {
    indent: number;
    kind: 'map' | 'seq';
    nextIndex: number;
    path: string[];
    pending?: string[];
};

type YamlLineType = {
    start: number;
    text: string;
};

type YamlSnapshotEntryType = {
    indent: number;
    path: string[];
    pending?: string[];
};

const MAX_JSON_DEPTH = 200;

const JSON_ESCAPES: Record<string, string> = {
    '"': '"',
    '/': '/',
    '\\': '\\',
    b: '\b',
    f: '\f',
    n: '\n',
    r: '\r',
    t: '\t',
};

function unescapeJson(raw: string): string {
    let result = '';

    for (let index = 0; index < raw.length; index++) {
        const character = raw[index];

        if (character !== '\\') {
            result += character;

            continue;
        }

        if (index + 1 >= raw.length) {
            break;
        }

        const escaped = raw[index + 1];

        if (escaped === 'u') {
            const hex = raw.slice(index + 2, index + 6);

            if (/^[0-9a-fA-F]{4}$/.test(hex)) {
                result += String.fromCharCode(parseInt(hex, 16));
                index += 5;

                continue;
            }
        }

        result += JSON_ESCAPES[escaped] ?? escaped;
        index++;
    }

    return result;
}

function scanJson(source: string): ScannedDocumentType {
    const comments: CommentType[] = [];
    const containers: {end: number; path: string[]; start: number}[] = [];
    const keyStrings: {containerPath: string[]; end: number; name: string; start: number}[] = [];
    const keys: string[][] = [];
    const scalars: ScalarType[] = [];

    let position = 0;

    function skipWhitespace(): void {
        while (position < source.length) {
            const character = source[position];

            if (/\s/.test(character)) {
                position++;

                continue;
            }

            if (character === '/' && source[position + 1] === '/') {
                const lineEnd = source.indexOf('\n', position);
                const end = lineEnd === -1 ? source.length : lineEnd;

                comments.push({end, start: position});

                position = end;

                continue;
            }

            if (character === '/' && source[position + 1] === '*') {
                const closeIndex = source.indexOf('*/', position + 2);
                const end = closeIndex === -1 ? source.length : closeIndex + 1;

                comments.push({end, start: position});

                position = closeIndex === -1 ? source.length : closeIndex + 2;

                continue;
            }

            break;
        }
    }

    function readString(): {contentEnd: number; contentStart: number; value: string} {
        const contentStart = position + 1;

        let cursor = contentStart;

        while (cursor < source.length) {
            const character = source[cursor];

            if (character === '\\') {
                cursor += 2;

                continue;
            }

            if (character === '"' || character === '\n') {
                break;
            }

            cursor++;
        }

        const contentEnd = Math.min(cursor, source.length);

        position = source[contentEnd] === '"' ? contentEnd + 1 : contentEnd;

        return {contentEnd, contentStart, value: unescapeJson(source.slice(contentStart, contentEnd))};
    }

    function parseValue(path: string[], depth: number): void {
        skipWhitespace();

        if (position >= source.length) {
            return;
        }

        const character = source[position];

        if (character === '{' && depth < MAX_JSON_DEPTH) {
            parseObject(path, depth);
        } else if (character === '[' && depth < MAX_JSON_DEPTH) {
            parseArray(path, depth);
        } else if (character === '"') {
            const {contentEnd, contentStart, value} = readString();

            scalars.push({
                before: (cursorOffset) =>
                    unescapeJson(
                        source.slice(contentStart, Math.min(Math.max(cursorOffset, contentStart), contentEnd))
                    ),
                end: contentEnd,
                isString: true,
                path,
                start: contentStart,
                value,
            });
        } else {
            const start = position;

            while (position < source.length && !/[\s,}\]]/.test(source[position])) {
                position++;
            }

            if (position === start) {
                position++;

                return;
            }

            scalars.push({
                before: () => '',
                end: position,
                isString: false,
                path,
                start,
                value: source.slice(start, position),
            });
        }
    }

    function parseObject(path: string[], depth: number): void {
        const container = {end: source.length, path, start: position};

        containers.push(container);

        position++;

        while (position < source.length) {
            skipWhitespace();

            if (position >= source.length) {
                break;
            }

            const character = source[position];

            if (character === '}') {
                container.end = position;
                position++;

                return;
            }

            if (character === ']') {
                container.end = position;

                return;
            }

            if (character === '"') {
                const {contentEnd, contentStart, value: keyName} = readString();
                const keyPath = [...path, keyName];

                keys.push(keyPath);
                keyStrings.push({containerPath: path, end: contentEnd, name: keyName, start: contentStart});

                skipWhitespace();

                if (source[position] === ':') {
                    position++;

                    skipWhitespace();

                    const next = source[position];

                    if (next !== undefined && next !== ',' && next !== '}' && next !== ']') {
                        parseValue(keyPath, depth + 1);
                    }
                }

                continue;
            }

            position++;
        }
    }

    function parseArray(path: string[], depth: number): void {
        const container = {end: source.length, path, start: position};

        containers.push(container);

        position++;

        let index = 0;

        while (position < source.length) {
            skipWhitespace();

            if (position >= source.length) {
                break;
            }

            const character = source[position];

            if (character === ']') {
                container.end = position;
                position++;

                return;
            }

            if (character === '}') {
                container.end = position;

                return;
            }

            if (character === ',') {
                position++;

                continue;
            }

            parseValue([...path, String(index)], depth + 1);

            index++;
        }
    }

    parseValue([], 0);

    return {
        comments,
        keySlotAt: (cursorOffset) => {
            const keyString = keyStrings.find(
                (candidate) => cursorOffset >= candidate.start && cursorOffset <= candidate.end
            );

            if (!keyString) {
                return undefined;
            }

            return {
                containerPath: keyString.containerPath,
                name: keyString.name,
                prefix: unescapeJson(source.slice(keyString.start, cursorOffset)),
            };
        },
        keys,
        pathAt: (cursorOffset) => {
            let deepest: {path: string[]; start: number} | undefined;

            for (const container of containers) {
                if (
                    container.start < cursorOffset &&
                    cursorOffset <= container.end &&
                    (!deepest || container.start > deepest.start)
                ) {
                    deepest = container;
                }
            }

            return deepest?.path ?? [];
        },
        scalars,
    };
}

function isDashAt(text: string, column: number): boolean {
    return text[column] === '-' && (column + 1 >= text.length || text[column + 1] === ' ');
}

function leadingSpaces(text: string): number {
    return text.length - text.trimStart().length;
}

function findYamlQuoteEnd(text: string, column: number): number {
    const quote = text[column];

    let cursor = column + 1;

    while (cursor < text.length) {
        if (quote === '"' && text[cursor] === '\\') {
            cursor += 2;

            continue;
        }

        if (text[cursor] === quote) {
            if (quote === "'" && text[cursor + 1] === "'") {
                cursor += 2;

                continue;
            }

            return cursor;
        }

        cursor++;
    }

    return text.length;
}

function readYamlKey(text: string, column: number): {name: string; valueColumn: number} | undefined {
    const first = text[column];

    if (first === undefined || first === '{' || first === '[' || first === '|' || first === '>' || first === '#') {
        return undefined;
    }

    let nameEnd = -1;
    let name = '';

    if (first === '"' || first === "'") {
        let cursor = findYamlQuoteEnd(text, column);

        if (cursor >= text.length) {
            return undefined;
        }

        const raw = text.slice(column + 1, cursor);

        name = first === '"' ? unescapeJson(raw) : raw.replace(/''/g, "'");

        cursor++;

        while (text[cursor] === ' ') {
            cursor++;
        }

        if (text[cursor] !== ':' || (cursor + 1 < text.length && text[cursor + 1] !== ' ')) {
            return undefined;
        }

        nameEnd = cursor;
    } else {
        for (let cursor = column; cursor < text.length; cursor++) {
            if (text[cursor] === '#' && cursor > column && text[cursor - 1] === ' ') {
                return undefined;
            }

            if (text[cursor] === ':' && (cursor + 1 >= text.length || text[cursor + 1] === ' ') && cursor > column) {
                nameEnd = cursor;
                name = text.slice(column, cursor).trimEnd();

                break;
            }
        }

        if (nameEnd === -1) {
            return undefined;
        }
    }

    let valueColumn = nameEnd + 1;

    while (text[valueColumn] === ' ') {
        valueColumn++;
    }

    return {name, valueColumn};
}

function scanYaml(source: string, cursorOffset: number): ScannedDocumentType {
    const comments: CommentType[] = [];
    const keys: string[][] = [];
    const lines: YamlLineType[] = [];
    const scalars: ScalarType[] = [];
    const snapshots = new Map<number, YamlSnapshotEntryType[]>();
    const stack: YamlContextType[] = [{indent: -1, kind: 'map', nextIndex: 0, path: [], pending: []}];

    let lineStart = 0;

    for (const rawLine of source.split('\n')) {
        lines.push({start: lineStart, text: rawLine.endsWith('\r') ? rawLine.slice(0, -1) : rawLine});

        lineStart += rawLine.length + 1;
    }

    function addTrailingComment(lineIndex: number, fromColumn: number): void {
        const {start, text} = lines[lineIndex];
        const rest = text.slice(fromColumn);
        const match = /(^|\s)#/.exec(rest);

        if (match) {
            comments.push({end: start + text.length, start: start + fromColumn + match.index + match[1].length});
        }
    }

    function processBlockScalar(lineIndex: number, path: string[], parentIndent: number): number {
        let lastContent = lineIndex;
        let scan = lineIndex + 1;

        while (scan < lines.length) {
            const text = lines[scan].text;

            if (text.trim() === '') {
                scan++;

                continue;
            }

            if (leadingSpaces(text) > parentIndent) {
                lastContent = scan;
                scan++;

                continue;
            }

            break;
        }

        const cursorLine = lines.findIndex(
            (line) => cursorOffset >= line.start && cursorOffset <= line.start + line.text.length
        );

        if (
            cursorLine > lastContent &&
            lines[cursorLine].text.trim() === '' &&
            lines[cursorLine].text.length > parentIndent &&
            lines.slice(lastContent + 1, cursorLine).every((line) => line.text.trim() === '')
        ) {
            lastContent = cursorLine;
        }

        if (lastContent === lineIndex) {
            return lineIndex + 1;
        }

        const blockLines = lines.slice(lineIndex + 1, lastContent + 1);
        const firstContent = blockLines.find((line) => line.text.trim() !== '');
        const blockIndent = firstContent
            ? leadingSpaces(firstContent.text)
            : blockLines[blockLines.length - 1].text.length;
        const dedent = (text: string): string => text.slice(Math.min(blockIndent, text.length));
        const start = blockLines[0].start;
        const lastLine = blockLines[blockLines.length - 1];

        scalars.push({
            before: (cursor) => {
                const parts: string[] = [];

                for (const line of blockLines) {
                    if (cursor < line.start) {
                        break;
                    }

                    if (cursor <= line.start + line.text.length) {
                        parts.push(dedent(line.text.slice(0, cursor - line.start)));

                        break;
                    }

                    parts.push(dedent(line.text));
                }

                return parts.join('\n');
            },
            end: lastLine.start + lastLine.text.length,
            isString: true,
            path,
            start,
            value: blockLines.map((line) => dedent(line.text)).join('\n'),
        });

        return lastContent + 1;
    }

    function processValue(lineIndex: number, column: number, path: string[], parentIndent: number): number {
        const {start: lineOffset, text} = lines[lineIndex];
        const first = text[column];

        if ((first === '|' || first === '>') && /^[|>][+\-0-9]*\s*(#.*)?$/.test(text.slice(column))) {
            addTrailingComment(lineIndex, column + 1);

            return processBlockScalar(lineIndex, path, parentIndent);
        }

        if (first === '{' || first === '[') {
            return lineIndex + 1;
        }

        if (first === '"' || first === "'") {
            const cursor = findYamlQuoteEnd(text, column);

            const contentEnd = Math.min(cursor, text.length);
            const raw = text.slice(column + 1, contentEnd);
            const decode = (value: string): string => (first === '"' ? unescapeJson(value) : value.replace(/''/g, "'"));
            const absoluteStart = lineOffset + column + 1;
            const absoluteEnd = lineOffset + contentEnd;

            scalars.push({
                before: (cursor) =>
                    decode(source.slice(absoluteStart, Math.min(Math.max(cursor, absoluteStart), absoluteEnd))),
                end: absoluteEnd,
                isString: true,
                path,
                start: absoluteStart,
                value: decode(raw),
            });

            if (contentEnd < text.length) {
                addTrailingComment(lineIndex, contentEnd + 1);
            }

            return lineIndex + 1;
        }

        const rest = text.slice(column);
        const commentMatch = /\s#/.exec(rest);
        const plain = (commentMatch ? rest.slice(0, commentMatch.index) : rest).trimEnd();
        const absoluteStart = lineOffset + column;

        if (commentMatch) {
            addTrailingComment(lineIndex, column);
        }

        scalars.push({
            before: (cursor) =>
                source.slice(absoluteStart, Math.min(Math.max(cursor, absoluteStart), absoluteStart + plain.length)),
            end: absoluteStart + plain.length,
            isString: true,
            path,
            start: absoluteStart,
            value: plain,
        });

        return lineIndex + 1;
    }

    function processContent(lineIndex: number, column: number): number {
        const {text} = lines[lineIndex];
        const top = stack[stack.length - 1];

        if (isDashAt(text, column)) {
            if (top.kind !== 'seq') {
                return lineIndex + 1;
            }

            const itemPath = [...top.path, String(top.nextIndex++)];

            let restColumn = column + 1;

            while (text[restColumn] === ' ') {
                restColumn++;
            }

            if (restColumn >= text.length || text[restColumn] === '#') {
                top.pending = itemPath;

                addTrailingComment(lineIndex, restColumn);

                return lineIndex + 1;
            }

            if (isDashAt(text, restColumn)) {
                stack.push({indent: restColumn, kind: 'seq', nextIndex: 0, path: itemPath});

                return processContent(lineIndex, restColumn);
            }

            if (readYamlKey(text, restColumn)) {
                stack.push({indent: restColumn, kind: 'map', nextIndex: 0, path: itemPath});

                return processContent(lineIndex, restColumn);
            }

            return processValue(lineIndex, restColumn, itemPath, column);
        }

        const keyInfo = readYamlKey(text, column);

        if (!keyInfo || top.kind !== 'map') {
            return lineIndex + 1;
        }

        const keyPath = [...top.path, keyInfo.name];

        keys.push(keyPath);

        if (keyInfo.valueColumn >= text.length || text[keyInfo.valueColumn] === '#') {
            const colonIndex = text.lastIndexOf(':', keyInfo.valueColumn - 1);
            const emptyStart = lines[lineIndex].start + colonIndex + (text[colonIndex + 1] === ' ' ? 2 : 1);

            scalars.push({
                before: () => '',
                end: Math.max(lines[lineIndex].start + keyInfo.valueColumn, emptyStart),
                isString: true,
                path: keyPath,
                start: emptyStart,
                value: '',
            });

            top.pending = keyPath;

            addTrailingComment(lineIndex, keyInfo.valueColumn);

            return lineIndex + 1;
        }

        return processValue(lineIndex, keyInfo.valueColumn, keyPath, column);
    }

    let lineIndex = 0;

    while (lineIndex < lines.length) {
        const {start, text} = lines[lineIndex];
        const column = leadingSpaces(text);

        if (text.trim() === '') {
            lineIndex++;

            continue;
        }

        if (text[column] === '#') {
            comments.push({end: start + text.length, start: start + column});
            lineIndex++;

            continue;
        }

        if (column === 0 && (text === '---' || text === '...')) {
            lineIndex++;

            continue;
        }

        const dash = isDashAt(text, column);

        for (;;) {
            const top = stack[stack.length - 1];

            if (top.indent > column) {
                stack.pop();

                continue;
            }

            if (top.indent === column) {
                if (top.kind === 'seq' && !dash) {
                    stack.pop();

                    continue;
                }

                if (top.kind === 'map' && dash && top.pending) {
                    stack.push({indent: column, kind: 'seq', nextIndex: 0, path: top.pending});

                    top.pending = undefined;
                }
            } else if (top.pending) {
                stack.push({indent: column, kind: dash ? 'seq' : 'map', nextIndex: 0, path: top.pending});

                top.pending = undefined;
            }

            break;
        }

        const nextLineIndex = processContent(lineIndex, column);

        snapshots.set(
            lineIndex,
            stack.map((context) => ({indent: context.indent, path: context.path, pending: context.pending}))
        );

        lineIndex = nextLineIndex;
    }

    function pathAt(offset: number): string[] {
        const cursorLineIndex = lines.findIndex(
            (line) => offset >= line.start && offset <= line.start + line.text.length
        );

        if (cursorLineIndex === -1) {
            return [];
        }

        const ownSnapshot = snapshots.get(cursorLineIndex);

        if (ownSnapshot) {
            return ownSnapshot[ownSnapshot.length - 1].path;
        }

        let snapshot: YamlSnapshotEntryType[] | undefined;

        for (let previous = cursorLineIndex - 1; previous >= 0 && !snapshot; previous--) {
            snapshot = snapshots.get(previous);
        }

        if (!snapshot) {
            return [];
        }

        const column = offset - lines[cursorLineIndex].start;
        const contexts = snapshot.filter((context) => context.indent <= column);
        const top = contexts[contexts.length - 1];

        if (!top) {
            return [];
        }

        return top.pending && column > top.indent ? top.pending : top.path;
    }

    return {
        comments,
        keySlotAt: (offset) => {
            const cursorLine = lines.find((line) => offset >= line.start && offset <= line.start + line.text.length);

            if (!cursorLine) {
                return undefined;
            }

            const column = offset - cursorLine.start;
            const match = /^\s*([A-Za-z_][\w-]*)?$/.exec(cursorLine.text.slice(0, column));

            if (!match || cursorLine.text.slice(column).trim() !== '') {
                return undefined;
            }

            return {containerPath: pathAt(offset), prefix: match[1] ?? ''};
        },
        keys,
        pathAt,
        scalars,
    };
}

function isIndex(segment: string | undefined): segment is string {
    return segment !== undefined && /^\d+$/.test(segment);
}

function collectNodeNames(
    scalarByPath: Map<string, ScalarType>,
    scalars: ScalarType[],
    section: string
): {index: number; name: string}[] {
    const nodes: {index: number; name: string}[] = [];

    for (const scalar of scalars) {
        if (
            scalar.path.length === 3 &&
            scalar.path[0] === section &&
            isIndex(scalar.path[1]) &&
            scalar.path[2] === 'name' &&
            scalar.value !== '' &&
            scalarByPath.get(scalar.path.join('\u0000')) === scalar
        ) {
            nodes.push({index: Number(scalar.path[1]), name: scalar.value});
        }
    }

    return nodes.sort((first, second) => first.index - second.index);
}

function collectConnections(scalars: ScalarType[], taskIndex: number): ConnectionDeclarationType[] {
    const groups = new Map<string, Record<string, string>>();

    for (const scalar of scalars) {
        if (
            scalar.path.length === 5 &&
            scalar.path[0] === 'tasks' &&
            scalar.path[1] === String(taskIndex) &&
            scalar.path[2] === 'connections'
        ) {
            const group = groups.get(scalar.path[3]) ?? {};

            group[scalar.path[4]] ??= scalar.value;

            groups.set(scalar.path[3], group);
        }
    }

    const connections: ConnectionDeclarationType[] = [];

    for (const [groupKey, group] of groups) {
        if (!group.componentName) {
            continue;
        }

        const version = Number(group.componentVersion);

        connections.push({
            componentName: group.componentName,
            componentVersion: group.componentVersion !== undefined && Number.isFinite(version) ? version : 1,
            name: group.name ?? groupKey,
        });
    }

    return connections;
}

function isChildPath(path: string[], containerPath: string[]): boolean {
    return path.length === containerPath.length + 1 && containerPath.every((segment, index) => path[index] === segment);
}

function coerceJsonLiteral(value: string): unknown {
    if (value === 'true' || value === 'false') {
        return value === 'true';
    }

    if (value === 'null') {
        return null;
    }

    const number = Number(value);

    return Number.isFinite(number) ? number : value;
}

function coercePlainScalar(value: string, quoted: boolean): unknown {
    return !quoted && /^(true|false|null|-?\d+(\.\d+)?)$/.test(value) ? coerceJsonLiteral(value) : value;
}

export function scanWorkflowDocument(
    source: string,
    cursorOffset: number,
    format: 'json' | 'yaml'
): WorkflowDocumentPositionType | undefined {
    if (format !== 'json' && format !== 'yaml') {
        return undefined;
    }

    const cursor = Math.min(Math.max(cursorOffset, 0), source.length);
    const document = format === 'json' ? scanJson(source) : scanYaml(source, cursor);

    if (document.comments.some((comment) => comment.start < cursor && cursor <= comment.end)) {
        return undefined;
    }

    const hit = document.scalars.find((scalar) => scalar.isString && cursor >= scalar.start && cursor <= scalar.end);
    const path = hit ? hit.path : document.pathAt(cursor);
    const enclosingTaskIndex = path[0] === 'tasks' && isIndex(path[1]) ? Number(path[1]) : undefined;

    const scalarByPath = new Map<string, ScalarType>();

    for (const scalar of document.scalars) {
        const pathKey = scalar.path.join('\u0000');

        if (!scalarByPath.has(pathKey)) {
            scalarByPath.set(pathKey, scalar);
        }
    }

    const earlierNodeNames = [
        ...collectNodeNames(scalarByPath, document.scalars, 'triggers').map((node) => node.name),
        ...collectNodeNames(scalarByPath, document.scalars, 'tasks')
            .filter((node) => enclosingTaskIndex === undefined || node.index < enclosingTaskIndex)
            .map((node) => node.name),
    ];

    const scannedKeySlot = hit ? undefined : document.keySlotAt(cursor);
    const isOwnKey = (path: string[]): boolean =>
        scannedKeySlot?.name !== undefined &&
        isChildPath(path, scannedKeySlot.containerPath) &&
        path[path.length - 1] === scannedKeySlot.name;

    const result: WorkflowDocumentPositionType = {
        earlierNodeNames,
        enclosingTaskConnections: [],
        enclosingTaskInputKeys: [],
        inString: !!hit,
        keysOf: (containerPath) => [
            ...new Set(
                document.keys
                    .filter((keyPath) => isChildPath(keyPath, containerPath) && !isOwnKey(keyPath))
                    .map((keyPath) => keyPath[keyPath.length - 1])
            ),
        ],
        literalValuesOf: (containerPath) => {
            const values: Record<string, unknown> = {};

            for (const scalar of document.scalars) {
                if (
                    isChildPath(scalar.path, containerPath) &&
                    !isOwnKey(scalar.path) &&
                    scalar.value !== '' &&
                    !scalar.value.includes('${')
                ) {
                    const quoted = format === 'json' || /['"]/.test(source[scalar.start - 1]);

                    values[scalar.path[scalar.path.length - 1]] = scalar.isString
                        ? coercePlainScalar(scalar.value, quoted)
                        : coerceJsonLiteral(scalar.value);
                }
            }

            return values;
        },
        nameOf: (nodePath) => scalarByPath.get([...nodePath, 'name'].join('\u0000'))?.value || undefined,
        path,
        typeOf: (nodePath) => scalarByPath.get([...nodePath, 'type'].join('\u0000'))?.value || undefined,
    };

    if (hit) {
        result.stringValueBeforeCursor = hit.before(cursor);
    } else {
        if (scannedKeySlot) {
            const {containerPath, name, prefix} = scannedKeySlot;
            const existingKeys: string[] = [];
            let skippedOwnKey = false;

            for (const keyPath of document.keys) {
                if (
                    keyPath.length !== containerPath.length + 1 ||
                    containerPath.some((segment, index) => keyPath[index] !== segment)
                ) {
                    continue;
                }

                const keyName = keyPath[keyPath.length - 1];

                if (!skippedOwnKey && name !== undefined && keyName === name) {
                    skippedOwnKey = true;

                    continue;
                }

                if (!existingKeys.includes(keyName)) {
                    existingKeys.push(keyName);
                }
            }

            result.keySlot = {containerPath, existingKeys, prefix};
        }
    }

    if (enclosingTaskIndex !== undefined) {
        const taskType = scalarByPath.get(['tasks', String(enclosingTaskIndex), 'type'].join('\u0000'));

        result.enclosingTaskIndex = enclosingTaskIndex;
        result.enclosingTaskConnections = collectConnections(document.scalars, enclosingTaskIndex);

        if (taskType?.value) {
            result.enclosingTaskType = taskType.value;
        }

        const inputKeys: string[] = [];

        for (const keyPath of document.keys) {
            if (
                keyPath.length === 5 &&
                keyPath[0] === 'tasks' &&
                keyPath[1] === String(enclosingTaskIndex) &&
                keyPath[2] === 'parameters' &&
                keyPath[3] === 'input' &&
                !inputKeys.includes(keyPath[4])
            ) {
                inputKeys.push(keyPath[4]);
            }
        }

        result.enclosingTaskInputKeys = inputKeys;
    }

    return result;
}
