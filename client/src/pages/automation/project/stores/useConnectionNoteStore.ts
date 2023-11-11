import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ConnectionNoteState {
    showConnectionNote: boolean;
    setShowConnectionNote: (showConnectionNote: boolean) => void;
}

export const useConnectionNoteStore = create<ConnectionNoteState>()(
    devtools(
        (set) => ({
            setShowConnectionNote: (connectionNoteStatus) =>
                set(() => ({
                    showConnectionNote: connectionNoteStatus,
                })),
            showConnectionNote: true,
        }),
        {
            name: 'connection-note',
        }
    )
);
