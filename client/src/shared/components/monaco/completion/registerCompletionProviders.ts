import {CompletionAdapter} from 'monaco-editor/languages/features/common/lspLanguageFeatures.js';

import {getCompletionContext} from './completionContextRegistry';
import {
    getLanguageServiceCompletionItems,
    isLanguageServiceCompletionItem,
    resolveLanguageServiceCompletionItem,
} from './javaScriptLanguageServiceCompletions';
import {type CompletionItemDraftType, getScriptCompletionResult} from './scriptCompletions';
import {getShellCompletions} from './shellCompletions';
import {getWorkflowDefinitionCompletions} from './workflowDefinitionCompletions';

import type {IDisposable, Position, editor, languages} from 'monaco-editor';

export const COMPLETION_LANGUAGES = ['javascript', 'python', 'ruby', 'shell', 'json', 'yaml'] as const;

type MonacoType = typeof import('monaco-editor');

const IDENTIFIER_BEFORE_CURSOR = /[A-Za-z_$][\w$]*$/;

const LANGUAGE_SERVICE_TRIGGER_CHARACTER = '.';

const TRIGGER_CHARACTERS = ['.', '{', '$', "'", '"', '/'];

const JSON_LANGUAGE_SERVICE_TRIGGER_CHARACTERS = [' ', ':', '"'];

let completionProviderRegistrations: IDisposable[] | undefined;

function toCompletionItemKind(monaco: MonacoType, kind: CompletionItemDraftType['kind']): languages.CompletionItemKind {
    const kinds = monaco.languages.CompletionItemKind;

    switch (kind) {
        case 'function':
            return kinds.Function;
        case 'method':
            return kinds.Method;
        case 'module':
            return kinds.Module;
        case 'property':
            return kinds.Property;
        case 'value':
            return kinds.Value;
        default:
            return kinds.Variable;
    }
}

interface ByteChefCompletionsI {
    exclusive: boolean;
    suggestions: languages.CompletionItem[];
}

async function provideByteChefCompletions(
    monaco: MonacoType,
    model: editor.ITextModel,
    position: Position
): Promise<ByteChefCompletionsI> {
    const context = getCompletionContext(model.uri.toString());

    if (!context) {
        return {exclusive: false, suggestions: []};
    }

    try {
        const source = model.getValue();
        const cursorOffset = model.getOffsetAt(position);

        let drafts: CompletionItemDraftType[];
        let exclusive = false;

        if (context.kind === 'workflowDefinition') {
            drafts = await getWorkflowDefinitionCompletions({context, cursorOffset, source});
        } else if (context.kind === 'shell') {
            drafts = getShellCompletions({context, cursorOffset, source});
        } else {
            const result = await getScriptCompletionResult({context, cursorOffset, source});

            drafts = result.items;
            exclusive = result.exclusive;
        }

        const linePrefix = model.getLineContent(position.lineNumber).slice(0, position.column - 1);
        const identifierLength = IDENTIFIER_BEFORE_CURSOR.exec(linePrefix)?.[0].length ?? 0;

        const rangeOf = (replaceLength: number) => ({
            endColumn: position.column,
            endLineNumber: position.lineNumber,
            startColumn: position.column - Math.min(replaceLength, linePrefix.length),
            startLineNumber: position.lineNumber,
        });

        return {
            exclusive,
            suggestions: drafts.map((draft) => ({
                command: draft.retrigger ? {id: 'editor.action.triggerSuggest', title: 'Suggest'} : undefined,
                detail: draft.detail,
                documentation: draft.documentation,
                filterText: draft.filterText,
                insertText: draft.insertText,
                insertTextRules: draft.insertAsSnippet
                    ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
                    : undefined,
                kind: toCompletionItemKind(monaco, draft.kind),
                label: draft.label,
                range: rangeOf(draft.replaceLength ?? identifierLength),
                sortText: `0${draft.sortText ?? draft.label}`,
            })),
        };
    } catch (error) {
        console.error('Monaco completion provider failed', error);

        return {exclusive: false, suggestions: []};
    }
}

export function buildCompletionProvider(monaco: MonacoType): languages.CompletionItemProvider {
    return {
        async provideCompletionItems(model, position) {
            const {suggestions} = await provideByteChefCompletions(monaco, model, position);

            return {suggestions};
        },
        triggerCharacters: TRIGGER_CHARACTERS,
    };
}

function shouldAskLanguageService(monaco: MonacoType, completionContext: languages.CompletionContext): boolean {
    return (
        completionContext.triggerKind !== monaco.languages.CompletionTriggerKind.TriggerCharacter ||
        completionContext.triggerCharacter === LANGUAGE_SERVICE_TRIGGER_CHARACTER
    );
}

export function buildJavaScriptCompletionProvider(monaco: MonacoType): languages.CompletionItemProvider {
    return {
        async provideCompletionItems(model, position, completionContext) {
            const byteChefCompletions = await provideByteChefCompletions(monaco, model, position);

            if (byteChefCompletions.exclusive || !shouldAskLanguageService(monaco, completionContext)) {
                return {suggestions: byteChefCompletions.suggestions};
            }

            try {
                const languageServiceItems = await getLanguageServiceCompletionItems(monaco, model, position);

                return {suggestions: [...byteChefCompletions.suggestions, ...languageServiceItems]};
            } catch (error) {
                console.error('JavaScript language service completion failed', error);

                return {suggestions: byteChefCompletions.suggestions};
            }
        },
        async resolveCompletionItem(item) {
            if (!isLanguageServiceCompletionItem(item)) {
                return item;
            }

            try {
                return await resolveLanguageServiceCompletionItem(monaco, item);
            } catch (error) {
                console.error('JavaScript language service completion details failed', error);

                return item;
            }
        },
        triggerCharacters: TRIGGER_CHARACTERS,
    };
}

export function disposeCompletionProviders(): void {
    completionProviderRegistrations?.forEach((registration) => registration.dispose());

    completionProviderRegistrations = undefined;
}

export function buildJsonCompletionProvider(
    monaco: MonacoType,
    languageServiceProvider: languages.CompletionItemProvider
): languages.CompletionItemProvider {
    return {
        async provideCompletionItems(model, position, completionContext, token) {
            const {suggestions} = await provideByteChefCompletions(monaco, model, position);

            if (getCompletionContext(model.uri.toString())?.kind === 'workflowDefinition') {
                return {suggestions};
            }

            try {
                const languageServiceCompletions = await languageServiceProvider.provideCompletionItems(
                    model,
                    position,
                    completionContext,
                    token
                );

                return {suggestions: [...suggestions, ...(languageServiceCompletions?.suggestions ?? [])]};
            } catch (error) {
                console.error('JSON language service completion failed', error);

                return {suggestions};
            }
        },
        triggerCharacters: [...new Set([...TRIGGER_CHARACTERS, ...JSON_LANGUAGE_SERVICE_TRIGGER_CHARACTERS])],
    };
}

export function registerCompletionProviders(monaco: MonacoType): void {
    if (completionProviderRegistrations) {
        return;
    }

    const {javascriptDefaults} = monaco.typescript;
    const {jsonDefaults} = monaco.json;

    javascriptDefaults.setModeConfiguration({...javascriptDefaults.modeConfiguration, completionItems: false});
    jsonDefaults.setModeConfiguration({...jsonDefaults.modeConfiguration, completionItems: false});

    const jsonLanguageServiceProvider = new CompletionAdapter(
        async (...uris) => (await monaco.json.getWorker())(...uris),
        JSON_LANGUAGE_SERVICE_TRIGGER_CHARACTERS
    );
    const providers: Partial<Record<(typeof COMPLETION_LANGUAGES)[number], languages.CompletionItemProvider>> = {
        javascript: buildJavaScriptCompletionProvider(monaco),
        json: buildJsonCompletionProvider(monaco, jsonLanguageServiceProvider),
    };
    const provider = buildCompletionProvider(monaco);

    completionProviderRegistrations = COMPLETION_LANGUAGES.map((language) =>
        monaco.languages.registerCompletionItemProvider(language, providers[language] ?? provider)
    );
}

import.meta.hot?.dispose(disposeCompletionProviders);
