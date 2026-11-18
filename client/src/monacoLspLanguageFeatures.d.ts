declare module 'monaco-editor/languages/features/common/lspLanguageFeatures.js' {
    type CompletionItemProviderType = import('monaco-editor').languages.CompletionItemProvider;
    type UriType = import('monaco-editor').Uri;

    export class CompletionAdapter implements CompletionItemProviderType {
        constructor(worker: (...uris: UriType[]) => Promise<unknown>, triggerCharacters: string[]);

        readonly triggerCharacters: string[];

        provideCompletionItems: CompletionItemProviderType['provideCompletionItems'];
    }
}
