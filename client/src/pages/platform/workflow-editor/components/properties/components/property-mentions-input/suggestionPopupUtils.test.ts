import {EditorState} from '@tiptap/pm/state';
import {describe, expect, it, vi} from 'vitest';

import {FunctionSuggestionPluginKey} from './FunctionSuggestion.extension';
import {isSuggestionPopupActive} from './suggestionPopupUtils';

vi.mock('tippy.js', () => ({default: vi.fn(() => [{popper: {style: {}}}])}));

function createState(functionSuggestionActive?: boolean): EditorState {
    const state: Record<string, unknown> = {};

    if (functionSuggestionActive !== undefined) {
        state[(FunctionSuggestionPluginKey as unknown as {key: string}).key] = {active: functionSuggestionActive};
    }

    return state as unknown as EditorState;
}

describe('isSuggestionPopupActive', () => {
    it('is false when the function suggestion plugin is not registered', () => {
        expect(isSuggestionPopupActive(createState())).toBe(false);
    });

    it('is false when the function suggestion is inactive', () => {
        expect(isSuggestionPopupActive(createState(false))).toBe(false);
    });

    it('is true when the function suggestion is active', () => {
        expect(isSuggestionPopupActive(createState(true))).toBe(true);
    });
});
