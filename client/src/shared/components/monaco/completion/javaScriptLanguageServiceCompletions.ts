import type {Position, Uri, editor, languages} from 'monaco-editor';

type MonacoType = typeof import('monaco-editor');

interface LanguageServiceEntryI {
    kind: string;
    kindModifiers?: string;
    name: string;
    replacementSpan?: {length: number; start: number};
    sortText: string;
}

interface LanguageServiceDisplayPartI {
    text: string;
}

interface LanguageServiceEntryDetailsI {
    displayParts?: LanguageServiceDisplayPartI[];
    documentation?: LanguageServiceDisplayPartI[];
    kind: string;
    name: string;
}

export interface LanguageServiceCompletionItemI extends languages.CompletionItem {
    languageServiceOffset: number;
    languageServiceUri: Uri;
}

const FIELD_KINDS = new Set(['getter', 'property', 'setter']);
const FUNCTION_KINDS = new Set(['call', 'construct', 'function', 'index', 'method']);
const KEYWORD_KINDS = new Set(['keyword', 'primitive type']);
const VARIABLE_KINDS = new Set(['local var', 'var']);

export function toLanguageServiceCompletionItemKind(monaco: MonacoType, kind: string): languages.CompletionItemKind {
    const kinds = monaco.languages.CompletionItemKind;

    if (KEYWORD_KINDS.has(kind)) {
        return kinds.Keyword;
    }

    if (VARIABLE_KINDS.has(kind)) {
        return kinds.Variable;
    }

    if (FIELD_KINDS.has(kind)) {
        return kinds.Field;
    }

    if (FUNCTION_KINDS.has(kind)) {
        return kinds.Function;
    }

    switch (kind) {
        case 'class':
            return kinds.Class;
        case 'enum':
            return kinds.Enum;
        case 'interface':
            return kinds.Interface;
        case 'module':
            return kinds.Module;
        case 'warning':
            return kinds.File;
        default:
            return kinds.Property;
    }
}

function displayPartsToString(displayParts: LanguageServiceDisplayPartI[] | undefined): string {
    return (displayParts ?? []).map((displayPart) => displayPart.text).join('');
}

export function isLanguageServiceCompletionItem(
    item: languages.CompletionItem
): item is LanguageServiceCompletionItemI {
    return 'languageServiceUri' in item;
}

export async function getLanguageServiceCompletionItems(
    monaco: MonacoType,
    model: editor.ITextModel,
    position: Position
): Promise<LanguageServiceCompletionItemI[]> {
    const getWorker = await monaco.typescript.getJavaScriptWorker();
    const worker = await getWorker(model.uri);

    if (model.isDisposed()) {
        return [];
    }

    const offset = model.getOffsetAt(position);
    const completionInfo = (await worker.getCompletionsAtPosition(model.uri.toString(), offset)) as
        {entries: LanguageServiceEntryI[]} | undefined;

    if (!completionInfo || model.isDisposed()) {
        return [];
    }

    const word = model.getWordUntilPosition(position);
    const wordRange = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);

    return completionInfo.entries.map((entry) => {
        let range = wordRange;

        if (entry.replacementSpan) {
            const start = model.getPositionAt(entry.replacementSpan.start);
            const end = model.getPositionAt(entry.replacementSpan.start + entry.replacementSpan.length);

            range = new monaco.Range(start.lineNumber, start.column, end.lineNumber, end.column);
        }

        const deprecated = entry.kindModifiers?.includes('deprecated');

        return {
            insertText: entry.name,
            kind: toLanguageServiceCompletionItemKind(monaco, entry.kind),
            label: entry.name,
            languageServiceOffset: offset,
            languageServiceUri: model.uri,
            range,
            sortText: entry.sortText,
            tags: deprecated ? [monaco.languages.CompletionItemTag.Deprecated] : [],
        };
    });
}

export async function resolveLanguageServiceCompletionItem(
    monaco: MonacoType,
    item: LanguageServiceCompletionItemI
): Promise<languages.CompletionItem> {
    const getWorker = await monaco.typescript.getJavaScriptWorker();
    const worker = await getWorker(item.languageServiceUri);
    const label = typeof item.label === 'string' ? item.label : item.label.label;
    const details = (await worker.getCompletionEntryDetails(
        item.languageServiceUri.toString(),
        item.languageServiceOffset,
        label
    )) as LanguageServiceEntryDetailsI | undefined;

    if (!details) {
        return item;
    }

    return {
        ...item,
        detail: displayPartsToString(details.displayParts),
        documentation: {value: displayPartsToString(details.documentation)},
        kind: toLanguageServiceCompletionItemKind(monaco, details.kind),
    };
}
