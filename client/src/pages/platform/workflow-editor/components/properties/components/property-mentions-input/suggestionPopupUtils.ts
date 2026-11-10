import {EditorState} from '@tiptap/pm/state';

import {FunctionSuggestionPluginKey} from './FunctionSuggestion.extension';

export function isSuggestionPopupActive(state: EditorState): boolean {
    return FunctionSuggestionPluginKey.getState(state)?.active === true;
}
