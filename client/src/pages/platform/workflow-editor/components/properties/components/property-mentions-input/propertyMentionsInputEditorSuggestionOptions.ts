import PropertyMentionsInputEditorSuggestionList from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditorSuggestionList';
import {DataPillType} from '@/shared/types';
import {MentionOptions} from '@tiptap/extension-mention';
import {PluginKey} from '@tiptap/pm/state';
import {Editor} from '@tiptap/react';

import {filterDataPillSuggestions} from './dataPillSuggestionUtils';
import {createSuggestionPopupRenderer} from './suggestionPopupRenderer';

export const DataPillSuggestionPluginKey = new PluginKey('dataPillSuggestion');

export function getSuggestionOptions(): MentionOptions['suggestion'] {
    return {
        allow: ({editor, isActive, range}) => {
            if (!editor.isFocused && !isActive) {
                return false;
            }

            const editorContent = editor.state.doc.textContent;

            if (range.from === 2) {
                return editorContent.charAt(0) !== '#';
            }

            return true;
        },
        allowedPrefixes: null,
        char: '$',
        // Prevent space insertion after adding mention
        command: ({editor, props, range}) => {
            editor
                .chain()
                .focus()
                .insertContentAt(range, [
                    {
                        attrs: props,
                        type: 'mention',
                    },
                ])
                .run();
        },
        items: ({editor, query}: {editor: Editor; query: string}): DataPillType[] => {
            const dataPills: DataPillType[] = editor.storage.MentionStorage.dataPills ?? [];

            return filterDataPillSuggestions(dataPills, query);
        },
        pluginKey: DataPillSuggestionPluginKey,
        render: createSuggestionPopupRenderer<DataPillType>(PropertyMentionsInputEditorSuggestionList),
    };
}
