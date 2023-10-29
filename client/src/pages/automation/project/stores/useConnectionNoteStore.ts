import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface ConnectionNoteState {
    showConnectionNote: boolean;
    setShowConnectionNote: (showConnectionNote: boolean) => void;
}

export const useConnectionNoteStore = create<ConnectionNoteState>()(
    devtools(
        persist(
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
    )
);
