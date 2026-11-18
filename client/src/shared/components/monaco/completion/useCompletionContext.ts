import {useEffect} from 'react';

import {registerCompletionContext, unregisterCompletionContext} from './completionContextRegistry';

import type {editor} from 'monaco-editor';

import type {CompletionContextType} from './completionTypes';

export function useCompletionContext(
    model: editor.ITextModel | undefined,
    context: CompletionContextType | undefined
): void {
    useEffect(() => {
        if (!model || !context) {
            return;
        }

        const modelUri = model.uri.toString();

        registerCompletionContext(modelUri, context);

        return () => unregisterCompletionContext(modelUri);
    }, [model, context]);
}
