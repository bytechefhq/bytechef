import {getCompletionContext} from './completionContextRegistry';
import {
    getLanguageServiceCompletionItems,
    isLanguageServiceCompletionItem,
    resolveLanguageServiceCompletionItem,
} from './javaScriptLanguageServiceCompletions';
import {type CompletionItemDraftType, getScriptCompletionResult} from './scriptCompletions';
import {getShellCompletions} from './shellCompletions';
import {getWorkflowDefinitionCompletions} from './workflowDefinitionCompletions';

import type {Position, editor, languages} from 'monaco-editor';

export const COMPLETION_LANGUAGES = ['javascript', 'python', 'ruby', 'shell', 'json', 'yaml'] as const;

type MonacoType = typeof import('monaco-editor');

const IDENTIFIER_BEFORE_CURSOR = /[A-Za-z_$][\w$]*$/;

const LANGUAGE_SERVICE_TRIGGER_CHARACTER = '.';

const TRIGGER_CHARACTERS = ['.', '{', '$', "'", '"', '/'];

let completionProvidersRegistered = false;

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

export function registerCompletionProviders(monaco: MonacoType): void {
    if (completionProvidersRegistered) {
        return;
    }

    completionProvidersRegistered = true;

    const {javascriptDefaults} = monaco.typescript;

    javascriptDefaults.setModeConfiguration({...javascriptDefaults.modeConfiguration, completionItems: false});

    const provider = buildCompletionProvider(monaco);
    const javaScriptProvider = buildJavaScriptCompletionProvider(monaco);

    COMPLETION_LANGUAGES.forEach((language) => {
        monaco.languages.registerCompletionItemProvider(
            language,
            language === 'javascript' ? javaScriptProvider : provider
        );
    });
}
