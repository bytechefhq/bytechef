import {getCompletionContext} from './completionContextRegistry';
import {type CompletionItemDraftType, getScriptCompletions} from './scriptCompletions';
import {getWorkflowDefinitionCompletions} from './workflowDefinitionCompletions';

import type {languages} from 'monaco-editor';

export const COMPLETION_LANGUAGES = ['javascript', 'python', 'ruby', 'json', 'yaml'] as const;

type MonacoType = typeof import('monaco-editor');

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

export function buildCompletionProvider(monaco: MonacoType): languages.CompletionItemProvider {
    return {
        async provideCompletionItems(model, position) {
            const context = getCompletionContext(model.uri.toString());

            if (!context) {
                return {suggestions: []};
            }

            try {
                const source = model.getValue();
                const cursorOffset = model.getOffsetAt(position);

                const drafts =
                    context.kind === 'workflowDefinition'
                        ? await getWorkflowDefinitionCompletions({context, cursorOffset, source})
                        : await getScriptCompletions({context, cursorOffset, source});

                const word = model.getWordUntilPosition(position);

                const range = {
                    endColumn: word.endColumn,
                    endLineNumber: position.lineNumber,
                    startColumn: word.startColumn,
                    startLineNumber: position.lineNumber,
                };

                return {
                    suggestions: drafts.map((draft) => ({
                        detail: draft.detail,
                        documentation: draft.documentation,
                        filterText: draft.filterText,
                        insertText: draft.insertText,
                        insertTextRules: draft.insertAsSnippet
                            ? monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
                            : undefined,
                        kind: toCompletionItemKind(monaco, draft.kind),
                        label: draft.label,
                        range,
                        sortText: `0${draft.sortText ?? draft.label}`,
                    })),
                };
            } catch (error) {
                console.error('Monaco completion provider failed', error);

                return {suggestions: []};
            }
        },
        triggerCharacters: ['.', '{', '$', "'", '"', '/'],
    };
}

export function registerCompletionProviders(monaco: MonacoType): void {
    if (completionProvidersRegistered) {
        return;
    }

    completionProvidersRegistered = true;

    const provider = buildCompletionProvider(monaco);

    COMPLETION_LANGUAGES.forEach((language) => {
        monaco.languages.registerCompletionItemProvider(language, provider);
    });
}
