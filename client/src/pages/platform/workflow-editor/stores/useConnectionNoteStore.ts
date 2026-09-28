import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface ConnectionNoteStateI {
    showConnectionNote: boolean;
    setShowConnectionNote: (showConnectionNote: boolean) => void;
}

export const useConnectionNoteStore = create<ConnectionNoteStateI>()(
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
                name: 'bytechef.connection-note',
            }
        ),
        {
            name: 'connection-note',
        }
    )
);
