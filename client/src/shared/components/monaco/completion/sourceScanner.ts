import type {ConnectionDeclarationType, ScriptLanguageType} from './completionTypes';

export type EnclosingActionType = {
    name: string;
    propertyNames: string[];
};

export type EnclosingTaskType = {
    connections: ConnectionDeclarationType[];
    earlierTaskNames: string[];
    inputNames: string[];
    name: string;
};

type TokenType = {kind: 'id' | 'node' | 'sep' | 'str'; node?: ScanNodeType; text: string};

type ScanNodeType = {
    children: ScanNodeType[];
    end: number;
    key?: string;
    kind: 'array' | 'object' | 'root';
    parent?: ScanNodeType;
    start: number;
    tokens: TokenType[];
};

const IDENTIFIER_CHARACTER = /[A-Za-z0-9_$]/;

function isKeyToken(token: TokenType | undefined): token is TokenType {
    return token?.kind === 'id' || token?.kind === 'str';
}

function skipString(source: string, startIndex: number, language: ScriptLanguageType): number {
    const quote = source[startIndex];

    if (language === 'python' && source.startsWith(quote.repeat(3), startIndex)) {
        const closing = source.indexOf(quote.repeat(3), startIndex + 3);

        return closing === -1 ? source.length : closing + 3;
    }

    let index = startIndex + 1;

    while (index < source.length) {
        const character = source[index];

        if (character === '\\') {
            index += 2;

            continue;
        }

        if (character === quote) {
            return index + 1;
        }

        if (character === '\n' && quote !== '`') {
            return index;
        }

        index++;
    }

    return source.length;
}

export function skipComment(source: string, startIndex: number, language: ScriptLanguageType): number | undefined {
    const isLineComment = language === 'javascript' ? source.startsWith('//', startIndex) : source[startIndex] === '#';

    if (isLineComment) {
        const lineEnd = source.indexOf('\n', startIndex);

        return lineEnd === -1 ? source.length : lineEnd;
    }

    if (language === 'javascript' && source.startsWith('/*', startIndex)) {
        const closing = source.indexOf('*/', startIndex + 2);

        return closing === -1 ? source.length : closing + 2;
    }

    return undefined;
}

function unquote(raw: string): string {
    const quote = raw[0];
    const tripleLength = raw.length >= 6 && raw.startsWith(quote.repeat(3)) ? 3 : 1;
    let inner = raw.slice(tripleLength);

    if (inner.endsWith(quote.repeat(tripleLength))) {
        inner = inner.slice(0, inner.length - tripleLength);
    }

    return inner;
}

function buildTree(source: string, language: ScriptLanguageType): ScanNodeType {
    const root: ScanNodeType = {children: [], end: source.length, kind: 'root', start: -1, tokens: []};
    const stack: ScanNodeType[] = [root];
    const parenDepths: number[] = [0];

    function addToken(token: TokenType) {
        if (parenDepths[parenDepths.length - 1] === 0) {
            stack[stack.length - 1].tokens.push(token);
        }
    }

    function open(kind: 'array' | 'object', index: number) {
        const parent = stack[stack.length - 1];
        const tokens = parenDepths[parenDepths.length - 1] === 0 ? parent.tokens : [];
        const separator = tokens[tokens.length - 1];
        const keyToken = tokens[tokens.length - 2];
        const node: ScanNodeType = {
            children: [],
            end: source.length,
            key: separator?.kind === 'sep' && isKeyToken(keyToken) ? keyToken.text : undefined,
            kind,
            parent,
            start: index,
            tokens: [],
        };

        parent.children.push(node);

        addToken({kind: 'node', node, text: ''});

        stack.push(node);
        parenDepths.push(0);
    }

    function close(kind: 'array' | 'object', index: number) {
        for (let depth = stack.length - 1; depth > 0; depth--) {
            if (stack[depth].kind === kind) {
                for (let popIndex = stack.length - 1; popIndex >= depth; popIndex--) {
                    stack[popIndex].end = index;
                }

                stack.length = depth;
                parenDepths.length = depth;

                return;
            }
        }
    }

    let index = 0;

    while (index < source.length) {
        const character = source[index];
        const commentEnd = skipComment(source, index, language);

        if (commentEnd !== undefined) {
            index = commentEnd;
        } else if (character === '"' || character === "'" || character === '`') {
            const stringEnd = skipString(source, index, language);

            addToken({kind: 'str', text: unquote(source.slice(index, stringEnd))});

            index = stringEnd;
        } else if (character === '{') {
            open('object', index);

            index++;
        } else if (character === '[') {
            open('array', index);

            index++;
        } else if (character === '}') {
            close('object', index);

            index++;
        } else if (character === ']') {
            close('array', index);

            index++;
        } else if (character === '(') {
            parenDepths[parenDepths.length - 1]++;

            index++;
        } else if (character === ')') {
            parenDepths[parenDepths.length - 1] = Math.max(0, parenDepths[parenDepths.length - 1] - 1);

            index++;
        } else if (character === ':' || source.startsWith('=>', index)) {
            addToken({kind: 'sep', text: character === ':' ? ':' : '=>'});

            index += character === ':' ? 1 : 2;
        } else if (IDENTIFIER_CHARACTER.test(character)) {
            let identifierEnd = index + 1;

            while (identifierEnd < source.length && IDENTIFIER_CHARACTER.test(source[identifierEnd])) {
                identifierEnd++;
            }

            addToken({kind: 'id', text: source.slice(index, identifierEnd)});

            index = identifierEnd;
        } else {
            index++;
        }
    }

    return root;
}

function getValueToken(node: ScanNodeType, key: string): TokenType | undefined {
    const {tokens} = node;

    for (let index = 0; index + 2 < tokens.length; index++) {
        if (isKeyToken(tokens[index]) && tokens[index].text === key && tokens[index + 1].kind === 'sep') {
            return tokens[index + 2];
        }
    }

    return undefined;
}

function getStringValue(node: ScanNodeType, key: string): string | undefined {
    const token = getValueToken(node, key);

    return token?.kind === 'str' ? token.text : undefined;
}

function getNodeValue(node: ScanNodeType, key: string, kind: 'array' | 'object'): ScanNodeType | undefined {
    const token = getValueToken(node, key);

    return token?.kind === 'node' && token.node?.kind === kind ? token.node : undefined;
}

function getObjectNames(arrayNode: ScanNodeType | undefined): string[] {
    if (!arrayNode) {
        return [];
    }

    const names: string[] = [];

    for (const child of arrayNode.children) {
        const name = child.kind === 'object' ? getStringValue(child, 'name') : undefined;

        if (name !== undefined) {
            names.push(name);
        }
    }

    return names;
}

function getEnclosingChain(root: ScanNodeType, cursorOffset: number): ScanNodeType[] {
    const chain: ScanNodeType[] = [];
    let current: ScanNodeType | undefined = root;

    while (current) {
        chain.push(current);

        const parent: ScanNodeType = current;

        current = parent.children.find((child) => child.start < cursorOffset && cursorOffset <= child.end);
    }

    return chain;
}

function isArrayItemOf(node: ScanNodeType, arrayKey: string): boolean {
    return node.kind === 'object' && node.parent?.kind === 'array' && node.parent.key === arrayKey;
}

function readConnections(taskNode: ScanNodeType): ConnectionDeclarationType[] {
    const connections: ConnectionDeclarationType[] = [];
    const listNode = getNodeValue(taskNode, 'connections', 'array');
    const mapNode = getNodeValue(taskNode, 'connections', 'object');

    function toDeclaration(entry: ScanNodeType, fallbackName?: string) {
        const componentName = getStringValue(entry, 'componentName');
        const name = getStringValue(entry, 'name') ?? fallbackName;

        if (componentName === undefined || name === undefined) {
            return;
        }

        const versionToken = getValueToken(entry, 'componentVersion');
        const parsedVersion = versionToken?.kind === 'id' ? Number(versionToken.text) : NaN;

        connections.push({
            componentName,
            componentVersion: Number.isFinite(parsedVersion) ? parsedVersion : 1,
            name,
        });
    }

    if (listNode) {
        for (const child of listNode.children) {
            if (child.kind === 'object') {
                toDeclaration(child);
            }
        }
    }

    if (mapNode) {
        const {tokens} = mapNode;

        for (let index = 0; index + 2 < tokens.length; index++) {
            const valueToken = tokens[index + 2];

            if (
                isKeyToken(tokens[index]) &&
                tokens[index + 1].kind === 'sep' &&
                valueToken.kind === 'node' &&
                valueToken.node?.kind === 'object'
            ) {
                toDeclaration(valueToken.node, tokens[index].text);
            }
        }
    }

    return connections;
}

function toTask(taskNode: ScanNodeType): EnclosingTaskType | undefined {
    const name = getStringValue(taskNode, 'name');

    if (name === undefined) {
        return undefined;
    }

    const tasksArray = taskNode.parent;
    const workflowNode = tasksArray?.parent;
    const earlierTaskNames: string[] = [];

    for (const sibling of tasksArray?.children ?? []) {
        if (sibling === taskNode) {
            break;
        }

        const siblingName = sibling.kind === 'object' ? getStringValue(sibling, 'name') : undefined;

        if (siblingName !== undefined) {
            earlierTaskNames.push(siblingName);
        }
    }

    return {
        connections: readConnections(taskNode),
        earlierTaskNames,
        inputNames: workflowNode ? getObjectNames(getNodeValue(workflowNode, 'inputs', 'array')) : [],
        name,
    };
}

function collectNodes(node: ScanNodeType, nodes: ScanNodeType[] = []): ScanNodeType[] {
    nodes.push(node);

    for (const child of node.children) {
        collectNodes(child, nodes);
    }

    return nodes;
}

export function findEnclosingAction(
    source: string,
    cursorOffset: number,
    language: ScriptLanguageType
): EnclosingActionType | undefined {
    try {
        const chain = getEnclosingChain(buildTree(source, language), cursorOffset);
        const actionNode = [...chain].reverse().find((node) => isArrayItemOf(node, 'actions'));

        if (!actionNode) {
            return undefined;
        }

        const name = getStringValue(actionNode, 'name');

        if (name === undefined) {
            return undefined;
        }

        return {name, propertyNames: getObjectNames(getNodeValue(actionNode, 'properties', 'array'))};
    } catch {
        return undefined;
    }
}

export function findComponentConnectionPropertyNames(source: string, language: ScriptLanguageType): string[] {
    try {
        const connectionNode = collectNodes(buildTree(source, language)).find(
            (node) => node.kind === 'object' && node.key === 'connection'
        );

        return connectionNode ? getObjectNames(getNodeValue(connectionNode, 'properties', 'array')) : [];
    } catch {
        return [];
    }
}

export function findEnclosingTask(
    source: string,
    cursorOffset: number,
    language: ScriptLanguageType
): EnclosingTaskType | undefined {
    try {
        const chain = getEnclosingChain(buildTree(source, language), cursorOffset);
        const taskNode = [...chain].reverse().find((node) => isArrayItemOf(node, 'tasks'));

        return taskNode ? toTask(taskNode) : undefined;
    } catch {
        return undefined;
    }
}

export function findTaskByPerformName(
    source: string,
    functionName: string,
    language: ScriptLanguageType
): EnclosingTaskType | undefined {
    try {
        const taskNode = collectNodes(buildTree(source, language)).find((node) => {
            if (!isArrayItemOf(node, 'tasks')) {
                return false;
            }

            const performToken = getValueToken(node, 'perform');

            return performToken?.kind === 'id' && performToken.text === functionName;
        });

        return taskNode ? toTask(taskNode) : undefined;
    } catch {
        return undefined;
    }
}
