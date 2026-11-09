import {EvaluatorFunctionDefinition} from '@/shared/middleware/graphql';
import {Extension} from '@tiptap/core';
import {PluginKey} from '@tiptap/pm/state';
import {Suggestion} from '@tiptap/suggestion';

import {getFunctionSuggestionOptions} from './functionSuggestionOptions';
import {findForcedFunctionSuggestionStart, isFormulaModeActive} from './functionSuggestionUtils';

export const FunctionSuggestionPluginKey = new PluginKey('functionSuggestion');

declare module '@tiptap/core' {
    // eslint-disable-next-line @typescript-eslint/naming-convention
    interface Storage {
        FunctionSuggestion: {
            forcedFrom: number | null;
            functionDefinitions: EvaluatorFunctionDefinition[];
        };
    }
}

export const FunctionSuggestion = Extension.create({
    addKeyboardShortcuts() {
        return {
            'Ctrl-Space': ({editor}) => {
                const {selection} = editor.state;

                if (!isFormulaModeActive(editor) || !selection.empty) {
                    return false;
                }

                const forcedFrom = findForcedFunctionSuggestionStart({$position: selection.$from});

                if (forcedFrom === null) {
                    return false;
                }

                this.storage.forcedFrom = forcedFrom;

                editor.view.dispatch(editor.state.tr);

                return true;
            },
        };
    },
    addProseMirrorPlugins() {
        return [
            Suggestion({
                editor: this.editor,
                pluginKey: FunctionSuggestionPluginKey,
                ...getFunctionSuggestionOptions({
                    clear: () => {
                        this.storage.forcedFrom = null;
                    },
                    getFrom: () => this.storage.forcedFrom,
                }),
            }),
        ];
    },
    addStorage() {
        return {
            forcedFrom: null,
            functionDefinitions: [],
        };
    },
    name: 'FunctionSuggestion',
});
