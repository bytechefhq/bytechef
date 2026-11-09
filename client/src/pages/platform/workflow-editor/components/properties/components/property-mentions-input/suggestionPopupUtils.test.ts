import {EditorState} from '@tiptap/pm/state';
import {describe, expect, it, vi} from 'vitest';

import {FunctionSuggestionPluginKey} from './FunctionSuggestion.extension';
import {VariablePathSuggestionPluginKey} from './VariablePathSuggestion.extension';
import {isSuggestionPopupActive} from './suggestionPopupUtils';

vi.mock('tippy.js', () => ({default: vi.fn(() => [{popper: {style: {}}}])}));

function createState(functionSuggestionActive?: boolean, variablePathSuggestionActive?: boolean): EditorState {
    const state: Record<string, unknown> = {};

    if (functionSuggestionActive !== undefined) {
        state[(FunctionSuggestionPluginKey as unknown as {key: string}).key] = {active: functionSuggestionActive};
    }

    if (variablePathSuggestionActive !== undefined) {
        state[(VariablePathSuggestionPluginKey as unknown as {key: string}).key] = {
            active: variablePathSuggestionActive,
        };
    }

    return state as unknown as EditorState;
}

describe('isSuggestionPopupActive', () => {
    it('is false when neither suggestion plugin is registered', () => {
        expect(isSuggestionPopupActive(createState())).toBe(false);
    });

    it('is false when both suggestions are inactive', () => {
        expect(isSuggestionPopupActive(createState(false, false))).toBe(false);
    });

    it('is true when the function suggestion is active', () => {
        expect(isSuggestionPopupActive(createState(true, false))).toBe(true);
    });

    it('is true when the variable path suggestion is active', () => {
        expect(isSuggestionPopupActive(createState(false, true))).toBe(true);
    });
});
