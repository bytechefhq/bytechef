import {EditorState} from '@tiptap/pm/state';

import {FunctionSuggestionPluginKey} from './FunctionSuggestion.extension';
import {VariablePathSuggestionPluginKey} from './VariablePathSuggestion.extension';

export function isSuggestionPopupActive(state: EditorState): boolean {
    const functionSuggestionActive = FunctionSuggestionPluginKey.getState(state)?.active === true;
    const variablePathSuggestionActive = VariablePathSuggestionPluginKey.getState(state)?.active === true;

    return functionSuggestionActive || variablePathSuggestionActive;
}
