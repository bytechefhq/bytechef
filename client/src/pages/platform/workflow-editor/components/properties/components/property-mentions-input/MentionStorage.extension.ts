import {Extension} from '@tiptap/core';

import type {DataPillType} from '@/shared/types';
import type {Editor} from '@tiptap/react';

declare module '@tiptap/core' {
    // eslint-disable-next-line  @typescript-eslint/naming-convention
    interface Storage {
        MentionStorage: {
            controlType?: string;
            dataPills: DataPillType[];
            /** Whether a `$` data-pill or `=` function suggestion popup is showing over this editor. */
            suggestionOpen: boolean;
        };
    }
}

export const MentionStorage = Extension.create({
    addStorage() {
        return {
            dataPills: [],
            suggestionOpen: false,
        };
    },
    name: 'MentionStorage',
});

/**
 * Records that a suggestion popup is, or is no longer, showing over `editor`.
 *
 * Both suggestion renderers report through here so the editor can refuse to replace its document
 * while one is open: `setContent` rebuilds the doc, which drops the match the suggestion plugin is
 * tracking and takes the popup down with it, mid-keystroke.
 */
export function setMentionSuggestionOpen(editor: Editor, suggestionOpen: boolean): void {
    if (editor.storage.MentionStorage) {
        editor.storage.MentionStorage.suggestionOpen = suggestionOpen;
    }
}
