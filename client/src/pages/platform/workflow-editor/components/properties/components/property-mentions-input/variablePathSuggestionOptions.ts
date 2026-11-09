import {DataPillType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import {SuggestionOptions} from '@tiptap/suggestion';

import PropertyMentionsInputEditorSuggestionList from './PropertyMentionsInputEditorSuggestionList';
import {filterDataPillSuggestions} from './dataPillSuggestionUtils';
import {isFormulaModeActive} from './functionSuggestionUtils';
import {createSuggestionPopupRenderer} from './suggestionPopupRenderer';
import {findVariablePathSuggestionMatch} from './variablePathSuggestionUtils';

function getDataPills(editor: Editor): DataPillType[] {
    return editor.storage.MentionStorage?.dataPills ?? [];
}

export function getVariablePathSuggestionOptions(): Omit<SuggestionOptions<DataPillType, {id: string}>, 'editor'> {
    return {
        allow: ({editor, isActive}) => (editor.isFocused || isActive === true) && isFormulaModeActive(editor),
        char: '',
        command: ({editor, props, range}) => {
            editor.chain().focus().insertContentAt(range, props.id).run();
        },
        findSuggestionMatch: findVariablePathSuggestionMatch,
        items: ({editor, query}) => filterDataPillSuggestions(getDataPills(editor), query),
        render: createSuggestionPopupRenderer<DataPillType>(PropertyMentionsInputEditorSuggestionList),
        shouldShow: ({editor, query}: {editor: Editor; query: string}) =>
            filterDataPillSuggestions(getDataPills(editor), query).length > 0,
    };
}
