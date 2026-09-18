import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export const STICKY_NOTE_RECENT_COLORS_LIMIT = 8;

interface StickyNoteColorsStateI {
    addRecentColor: (color: string) => void;
    recentColors: Array<string>;
}

/**
 * Recently used custom sticky note colors (hex values), most recent first,
 * capped at {@link STICKY_NOTE_RECENT_COLORS_LIMIT} and persisted locally.
 */
export const useStickyNoteColorsStore = create<StickyNoteColorsStateI>()(
    devtools(
        persist(
            (set) => ({
                addRecentColor: (color) =>
                    set((state) => ({
                        recentColors: [
                            color,
                            ...state.recentColors.filter((recentColor) => recentColor !== color),
                        ].slice(0, STICKY_NOTE_RECENT_COLORS_LIMIT),
                    })),
                recentColors: [],
            }),
            {
                name: 'bytechef.sticky-note-colors',
            }
        ),
        {
            name: 'sticky-note-colors',
        }
    )
);

export default useStickyNoteColorsStore;
