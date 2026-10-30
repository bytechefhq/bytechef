import type {ShellCompletionContextI} from './completionTypes';
import type {CompletionItemDraftType} from './scriptCompletions';

export interface ShellCompletionRequestI {
    context: ShellCompletionContextI;
    cursorOffset: number;
    source: string;
}

type LineStateType = {
    inComment: boolean;
    inDoubleQuotes: boolean;
    inSingleQuotes: boolean;
};

const RUNNER_VARIABLES = [
    {
        documentation: 'The JSON file holding the task input.',
        name: 'BYTECHEF_INPUT_FILE',
    },
    {
        documentation:
            'The output directory. Write output.json here to return it as vars; files matching Output Files are collected from here.',
        name: 'BYTECHEF_OUTPUT_DIR',
    },
    {
        documentation: 'The working directory the script runs in. Input Files are written here.',
        name: 'BYTECHEF_WORKING_DIR',
    },
    {
        documentation: "The JSON file holding the task's connections, when the runner passes them to the script.",
        name: 'BYTECHEF_CONNECTIONS_FILE',
    },
];

const INPUT_VARIABLE_PREFIX = 'INPUT_';

const VARIABLE_NAME = /^[A-Za-z_][A-Za-z0-9_]*$/;

const BRACED_VARIABLE_BEFORE_CURSOR = /\$\{([A-Za-z_][A-Za-z0-9_]*)?$/;

const VARIABLE_BEFORE_CURSOR = /\$([A-Za-z_][A-Za-z0-9_]*)?$/;

const WORD_BEFORE_CURSOR = /[A-Za-z_][\w.]*$/;

const SCRIPT_VARIABLE_DECLARATIONS = [
    /(?:^|[\s;&|(])(?:export\s+|local\s+|readonly\s+|declare\s+(?:-\w+\s+)*)?([A-Za-z_][A-Za-z0-9_]*)\+?=/gm,
    /(?:^|[\s;&|(])for[ \t]+([A-Za-z_][A-Za-z0-9_]*)[ \t]+in\b/gm,
    /(?:^|[\s;&|(])read[ \t]+(?:-\w+[ \t]+)*([A-Za-z_][A-Za-z0-9_]*(?:[ \t]+[A-Za-z_][A-Za-z0-9_]*)*)/gm,
];

const WRITE_OUTPUT_COMMAND = `printf '{"value": "%s"}\\n' "$value" > "$BYTECHEF_OUTPUT_DIR/output.json"`;

function getLineState(linePrefix: string): LineStateType {
    let inDoubleQuotes = false;
    let inSingleQuotes = false;

    for (let index = 0; index < linePrefix.length; index++) {
        const character = linePrefix[index];

        if (inSingleQuotes) {
            if (character === "'") {
                inSingleQuotes = false;
            }

            continue;
        }

        if (character === '\\') {
            index++;

            continue;
        }

        if (character === '"') {
            inDoubleQuotes = !inDoubleQuotes;
        } else if (!inDoubleQuotes && character === "'") {
            inSingleQuotes = true;
        } else if (!inDoubleQuotes && character === '#' && (index === 0 || /\s/.test(linePrefix[index - 1]))) {
            return {inComment: true, inDoubleQuotes, inSingleQuotes};
        }
    }

    return {inComment: false, inDoubleQuotes, inSingleQuotes};
}

function getScriptVariableNames(source: string): string[] {
    const names = new Set<string>();

    for (const declaration of SCRIPT_VARIABLE_DECLARATIONS) {
        for (const match of source.matchAll(declaration)) {
            match[1].split(/[ \t]+/).forEach((name) => names.add(name));
        }
    }

    return [...names];
}

function variableItems(context: ShellCompletionContextI, source: string, typed: string): CompletionItemDraftType[] {
    const items = new Map<string, CompletionItemDraftType>();

    const addVariable = (name: string, detail: string, documentation?: string) => {
        if (!items.has(name) && VARIABLE_NAME.test(name)) {
            items.set(name, {
                detail,
                documentation,
                insertAsSnippet: false,
                insertText: name,
                kind: 'variable',
                label: name,
                replaceLength: typed.length,
            });
        }
    };

    RUNNER_VARIABLES.forEach(({documentation, name}) => addVariable(name, 'Task runner', documentation));

    getInputVariableNames(context).forEach(([name, key]) =>
        addVariable(name, 'Input', `The "${key}" input value; a non-string value is JSON.`)
    );

    context.env.forEach((name) => addVariable(name, 'Environment'));

    getScriptVariableNames(source).forEach((name) => addVariable(name, 'Script variable'));

    return [...items.values()];
}

function getInputVariableNames(context: ShellCompletionContextI): Array<[string, string]> {
    return Object.keys(context.input)
        .filter((key) => VARIABLE_NAME.test(key))
        .map((key) => [`${INPUT_VARIABLE_PREFIX}${key}`, key]);
}

function inputItems(
    context: ShellCompletionContextI,
    typed: string,
    inDoubleQuotes: boolean
): CompletionItemDraftType[] {
    return getInputVariableNames(context).map(([name, key]) => ({
        detail: 'Input value',
        documentation: `Expands to the "${key}" input value, exported as $${name}; a non-string value is JSON.`,
        filterText: `input.${key}`,
        insertAsSnippet: false,
        insertText: inDoubleQuotes ? `$${name}` : `"$${name}"`,
        kind: 'property',
        label: `input.${key}`,
        replaceLength: typed.length,
    }));
}

function writeOutputItem(typed: string): CompletionItemDraftType {
    return {
        detail: 'Return vars',
        documentation: 'Writes a JSON object to $BYTECHEF_OUTPUT_DIR/output.json, which the task returns as vars.',
        filterText: 'output.json',
        insertAsSnippet: false,
        insertText: WRITE_OUTPUT_COMMAND,
        kind: 'function',
        label: 'output.json',
        replaceLength: typed.length,
    };
}

export function getShellCompletions({
    context,
    cursorOffset,
    source,
}: ShellCompletionRequestI): CompletionItemDraftType[] {
    const beforeCursor = source.slice(0, cursorOffset);
    const linePrefix = beforeCursor.slice(beforeCursor.lastIndexOf('\n') + 1);

    const {inComment, inDoubleQuotes, inSingleQuotes} = getLineState(linePrefix);

    if (inComment || inSingleQuotes) {
        return [];
    }

    const variableMatch = BRACED_VARIABLE_BEFORE_CURSOR.exec(linePrefix) ?? VARIABLE_BEFORE_CURSOR.exec(linePrefix);

    if (variableMatch) {
        if (linePrefix[variableMatch.index - 1] === '\\') {
            return [];
        }

        return variableItems(context, source, variableMatch[1] ?? '');
    }

    const typed = WORD_BEFORE_CURSOR.exec(linePrefix)?.[0] ?? '';

    if (inDoubleQuotes) {
        return inputItems(context, typed, true);
    }

    return [...inputItems(context, typed, false), writeOutputItem(typed)];
}
