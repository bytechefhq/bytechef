import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {Extension} from '@tiptap/core';
import {PluginKey} from '@tiptap/pm/state';
import {Suggestion} from '@tiptap/suggestion';

import {getFunctionSuggestionOptions} from './functionSuggestionOptions';

export const FunctionSuggestionPluginKey = new PluginKey('functionSuggestion');

declare module '@tiptap/core' {
    // eslint-disable-next-line @typescript-eslint/naming-convention
    interface Storage {
        FunctionSuggestion: {
            functionDefinitions: EvaluatorFunctionDefinition[];
        };
    }
}

export const FunctionSuggestion = Extension.create({
    addProseMirrorPlugins() {
        return [
            Suggestion({
                editor: this.editor,
                pluginKey: FunctionSuggestionPluginKey,
                ...getFunctionSuggestionOptions(),
            }),
        ];
    },
    addStorage() {
        return {
            functionDefinitions: [],
        };
    },
    name: 'FunctionSuggestion',
});
