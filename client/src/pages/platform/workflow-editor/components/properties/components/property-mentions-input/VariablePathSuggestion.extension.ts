import {DataPillType} from '@/shared/types';
import {Extension} from '@tiptap/core';
import {PluginKey} from '@tiptap/pm/state';
import {Suggestion} from '@tiptap/suggestion';

import {getVariablePathSuggestionOptions} from './variablePathSuggestionOptions';

export const VariablePathSuggestionPluginKey = new PluginKey('variablePathSuggestion');

export const VariablePathSuggestion = Extension.create({
    addProseMirrorPlugins() {
        return [
            Suggestion<DataPillType, {id: string}>({
                editor: this.editor,
                pluginKey: VariablePathSuggestionPluginKey,
                ...getVariablePathSuggestionOptions(),
            }),
        ];
    },
    name: 'VariablePathSuggestion',
});
