import {beforeEach, describe, expect, it} from 'vitest';

import useStickyNoteColorsStore, {STICKY_NOTE_RECENT_COLORS_LIMIT} from '../useStickyNoteColorsStore';

describe('useStickyNoteColorsStore', () => {
    beforeEach(() => {
        useStickyNoteColorsStore.setState({recentColors: []});
    });

    it('should keep the most recently used color first', () => {
        useStickyNoteColorsStore.getState().addRecentColor('#111111');
        useStickyNoteColorsStore.getState().addRecentColor('#222222');

        expect(useStickyNoteColorsStore.getState().recentColors).toEqual(['#222222', '#111111']);
    });

    it('should move a repeated color to the front instead of duplicating it', () => {
        useStickyNoteColorsStore.getState().addRecentColor('#111111');
        useStickyNoteColorsStore.getState().addRecentColor('#222222');
        useStickyNoteColorsStore.getState().addRecentColor('#111111');

        expect(useStickyNoteColorsStore.getState().recentColors).toEqual(['#111111', '#222222']);
    });

    it('should cap the remembered colors', () => {
        for (let index = 0; index < STICKY_NOTE_RECENT_COLORS_LIMIT + 3; index++) {
            useStickyNoteColorsStore.getState().addRecentColor(`#0000${String(index).padStart(2, '0')}`);
        }

        expect(useStickyNoteColorsStore.getState().recentColors).toHaveLength(STICKY_NOTE_RECENT_COLORS_LIMIT);
    });
});
